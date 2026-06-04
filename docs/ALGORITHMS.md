# HotelOS — Algorithms Documentation

## 1. Room Assignment Algorithm

**File:** `reception-service/.../service/RoomAssignmentAlgorithm.java`

### Purpose
Automatically assigns the most suitable available room to a guest at check-in based on multiple criteria, with concurrency protection to prevent double-booking.

### Algorithm Steps

```
Input:  roomType (SINGLE, DOUBLE, SUITE, DELUXE, PRESIDENTIAL)
        preferredFloor (Integer or null)
        preferLift (Boolean or null)

Output: Room entity (status changed to OCCUPIED)
        OR BadRequestException("No rooms available")
```

#### Step 1: Filter by Room Type
```
rooms = SELECT * FROM rooms
        WHERE roomType = :requestedType
        AND status = 'CLEAN'
        AND active = true
```

#### Step 2: Check Availability
If `rooms` is empty → throw `BadRequestException("No rooms available for type X")`

#### Step 3: Sort by Longest Clean Time
```java
rooms.sort(Comparator.comparing(
    r -> r.getLastCleanedAt() != null ? r.getLastCleanedAt() : LocalDateTime.MIN
))
```
Rooms cleaned longest ago are prioritized first.

#### Step 4: Floor Preference
If guest specified a preferred floor, filter for rooms on that floor:
```
floorMatch = rooms.stream()
    .filter(r -> r.floor == preferredFloor)
    .findFirst()
```

#### Step 5: Lift Proximity Preference
If guest prefers near-lift rooms AND a floor match was found:
```
liftMatch = rooms.stream()
    .filter(r -> r.floor == preferredFloor AND r.nearLift == true)
    .findFirst()
```
If no floor match but lift preferred, search all available rooms for near-lift.

#### Step 6: Assign with Pessimistic Lock
Before marking the room as OCCUPIED, the algorithm re-fetches the room with a **pessimistic write lock** (`SELECT ... FOR UPDATE`) to prevent concurrent double-booking:

```java
Room lockedRoom = roomRepository.findByIdForUpdate(room.getId());

if (lockedRoom.status != CLEAN) {
    throw BadRequestException("Room X is no longer available (now OCCUPIED)");
}

lockedRoom.status = OCCUPIED;
roomRepository.save(lockedRoom);
```

### Concurrency Protection

The pessimistic locking ensures that if two concurrent check-in requests select the same room, only one will succeed. The second request will find the room status changed from CLEAN to OCCUPIED and throw an exception.

```
Thread A: SELECT rooms WHERE type=DOUBLE AND status=CLEAN → finds room 103
Thread B: SELECT rooms WHERE type=DOUBLE AND status=CLEAN → finds room 103
Thread A: SELECT ... FOR UPDATE WHERE id=X → gets lock on room 103
          status = CLEAN ✓ → set to OCCUPIED, release lock
Thread B: SELECT ... FOR UPDATE WHERE id=X → waits for lock
          status = OCCUPIED ✗ → throw "No longer available"
```

### Flow Diagram

```
                    ┌─────────────────┐
                    │  Check-In Request│
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Filter by Type   │
                    │ + CLEAN status   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │  Empty result?  │──YES──▶ throw "No rooms available"
                    └────────┬────────┘
                             │ NO
                    ┌────────▼────────┐
                    │ Sort by longest  │
                    │ clean time       │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Floor preference │
                    │ match?           │──NO──▶ use first available
                    └────────┬────────┘         │
                             │ YES              │
                    ┌────────▼────────┐         │
                    │ Lift preference  │         │
                    │ match?           │         │
                    └────────┬────────┘         │
                             │                  │
                    ┌────────▼──────────────────▼───┐
                    │ SELECT ... FOR UPDATE (lock)   │
                    │ Re-check status == CLEAN?      │
                    └────────┬──────────────────────┘
                             │
                    ┌────────▼────────┐
                    │ status = OCCUPIED│
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Return Room     │
                    └─────────────────┘
```

---

## 2. Billing Algorithm

**File:** `reception-service/.../service/BillingAlgorithm.java`

### Purpose
Calculates the total bill when a guest checks out, combining room charges, room service orders, additional charges, and discounts.

### Formula

```
Total Amount = (Room Price × Nights Stayed)
             + Σ(Room Service Orders)
             + Additional Charges
             − Discounts
```

### Algorithm Steps

#### Step 1: Calculate Nights Stayed
```java
checkInDate = guest.checkInTime.toLocalDate()
checkOutDate = checkOutTime.toLocalDate()
nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate)
if (nights < 1) nights = 1    // Minimum 1 night charge
```

#### Step 2: Calculate Room Charges
```java
roomCharges = room.pricePerNight × nights
```

#### Step 3: Sum Room Service Orders
```java
orders = orderRepository.findByGuestId(guest.id)
roomServiceTotal = orders.stream()
    .mapToDouble(order -> order.totalAmount)
    .sum()

// Also build itemized list for the bill
orderItems = orders.map(order -> {
    orderId, orderNumber, items, amount
})
```

#### Step 4: Apply Additional Charges & Discounts
```java
additionalCharges = BigDecimal.ZERO   // Extensible
discounts = BigDecimal.ZERO           // Extensible
```

#### Step 5: Calculate Total
```java
totalAmount = roomCharges
    .add(roomServiceTotal)
    .add(additionalCharges)
    .subtract(discounts)
```

### BillDTO Output

```json
{
  "guestId": 1,
  "guestName": "John Doe",
  "roomId": 5,
  "roomNumber": "201",
  "roomRate": 149.99,
  "nightsStayed": 3,
  "roomCharges": 449.97,
  "roomServiceOrders": [...],
  "roomServiceTotal": 45.99,
  "additionalCharges": 0.00,
  "discounts": 0.00,
  "totalAmount": 495.96,
  "checkInDate": "2026-06-01",
  "checkOutDate": "2026-06-04",
  "generatedAt": "2026-06-04T15:30:22"
}
```

### Edge Cases

| Scenario | Handling |
|----------|----------|
| Same-day checkout (early checkout) | Minimum 1 night charge |
| Zero room service orders | `roomServiceTotal = 0` |
| Guest with discount | Deducted from total (extensible) |
| Additional charges (minibar, etc.) | Added to total (extensible) |

### Flow Diagram

```
                    ┌─────────────────┐
                    │  Check-Out       │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Calculate nights │
                    │ (min 1)          │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ roomCharges =    │
                    │ price × nights   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Sum all room     │
                    │ service orders   │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ + additional     │
                    │ − discounts      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Total = charges  │
                    │ + orders + add   │
                    │ − discounts      │
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Return BillDTO   │
                    └─────────────────┘
```

---

## 3. Order Status Validation

**File:** `room-service/.../service/RoomOrderService.java`

The Room Service enforces strict status transitions to prevent invalid state changes:

```
RECEIVED ──────▶ PREPARING ──────▶ DELIVERING ──────▶ DELIVERED
    │                │
    └──────▶ CANCELLED
```

| Current Status | Allowed Next States |
|---------------|-------------------|
| RECEIVED | PREPARING, CANCELLED |
| PREPARING | DELIVERING, CANCELLED |
| DELIVERING | DELIVERED |
| DELIVERED | *(terminal)* |
| CANCELLED | *(terminal)* |

Invalid transitions throw `BadRequestException("Invalid status transition from X to Y")`.

---

## 4. Maintenance Priority Queue

**File:** `maintenance-service/.../service/MaintenanceService.java`

### Data Structure
Java `PriorityQueue<MaintenanceRequest>` with comparator:
```java
Comparator.comparingInt(this::getPriorityOrder)
    .thenComparing(MaintenanceRequest::getReportedAt)
```

### Priority Order (lower = higher priority)

| Priority | Order Value | Description |
|----------|-------------|-------------|
| CRITICAL | 0 | Urgent safety/system issues |
| HIGH | 1 | Important functional issues |
| NORMAL | 2 | Standard maintenance |
| LOW | 3 | Cosmetic/minor issues |

### Behavior
- `peek()` returns the highest-priority request without removing it
- `poll()` removes and returns the highest-priority request
- Requests are removed from the queue when status becomes RESOLVED or CLOSED
- The queue is lazily initialized from the database on first access
