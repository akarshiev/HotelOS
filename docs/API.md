# HotelOS — REST API Reference

All services require **HTTP Basic Authentication** (except `/api/auth/**` and `/ws/**`).

Default credentials: `admin` / `admin123`

---

## 1. Authentication Service (Port 8081)

### `POST /api/auth/login`

Authenticate a user.

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response (200):**
```json
{
  "success": true,
  "username": "admin",
  "role": "ADMIN",
  "message": "Login successful"
}
```

**Response (401):**
```json
{
  "success": false,
  "message": "Invalid credentials"
}
```

### `GET /api/auth/verify`

Verify current authentication status.

**Response (200):**
```json
{
  "authenticated": true,
  "username": "admin",
  "role": "ADMIN"
}
```

---

## 2. Reception Service (Port 8081)

### Check-In

#### `POST /api/reception/checkin`

Register a new guest and assign a room using the Room Assignment Algorithm.

**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "phoneNumber": "+1 234 567 890",
  "idDocument": "PASS-12345",
  "roomType": "DOUBLE",
  "preferredFloor": 2,
  "preferLift": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| firstName | String | ✅ | 2–100 characters |
| lastName | String | ✅ | 2–100 characters |
| email | String | ✅ | Valid email format, unique |
| phoneNumber | String | ❌ | Max 20 characters |
| idDocument | String | ❌ | — |
| roomType | String | ✅ | SINGLE, DOUBLE, SUITE, DELUXE, PRESIDENTIAL |
| preferredFloor | Integer | ❌ | Floor number |
| preferLift | Boolean | ❌ | true = prefer rooms near lift |

**Response (201):** Full `Guest` entity with assigned `Room`.

**Errors:**
- `409 Conflict` — Guest with this email is already checked in
- `400 Bad Request` — No rooms available for the requested type

---

### Check-Out

#### `POST /api/reception/checkout`

Process guest departure. Calculates the bill and releases the room.

**Request:**
```json
{
  "guestId": 1
}
```

**Response (200):** `BillDTO` (see [Billing](#billing-response))

**Errors:**
- `404 Not Found` — Guest not found
- `400 Bad Request` — Guest is not currently checked in

---

### Guests

#### `GET /api/reception/guests`

Returns all guests (registered, checked in, checked out).

**Response (200):** `List<Guest>`

#### `GET /api/reception/guests/active`

Returns only guests with status `CHECKED_IN`.

**Response (200):** `List<Guest>`

#### `GET /api/reception/guests/{id}`

Returns a specific guest by ID.

**Response (200):** `Guest`

---

### Rooms

#### `GET /api/reception/rooms`

Returns all active rooms.

**Response (200):** `List<Room>`

#### `GET /api/reception/rooms/{id}`

Returns a specific room by ID.

**Response (200):** `Room`

#### `GET /api/reception/rooms/number/{roomNumber}`

Returns a specific room by room number (e.g., "201").

**Response (200):** `Room`

#### `GET /api/reception/rooms/status/{status}`

Returns rooms filtered by status.

| Status | Description |
|--------|-------------|
| `CLEAN` | Ready for new guest |
| `AVAILABLE` | General available |
| `OCCUPIED` | Currently occupied |
| `DIRTY` | Needs cleaning |
| `CLEANING` | Being cleaned |
| `OUT_OF_ORDER` | Under repair |

**Response (200):** `List<Room>`

---

### Dashboard

#### `GET /api/reception/dashboard/stats`

Returns real-time dashboard statistics.

**Response (200):**
```json
{
  "totalRooms": 15,
  "availableRooms": 12,
  "occupiedRooms": 2,
  "dirtyRooms": 1,
  "cleaningRooms": 0,
  "outOfOrderRooms": 0,
  "activeGuests": 2
}
```

---

### Billing Response

```json
{
  "guestId": 1,
  "guestName": "John Doe",
  "roomId": 5,
  "roomNumber": "201",
  "roomRate": 149.99,
  "nightsStayed": 3,
  "roomCharges": 449.97,
  "roomServiceOrders": [
    {
      "orderId": 1,
      "orderNumber": "ORD-20260604153022-0042",
      "items": "2x Caesar Salad, 1x Grilled Salmon",
      "amount": 45.99
    }
  ],
  "roomServiceTotal": 45.99,
  "additionalCharges": 0.00,
  "discounts": 0.00,
  "totalAmount": 495.96,
  "checkInDate": "2026-06-01",
  "checkOutDate": "2026-06-04",
  "generatedAt": "2026-06-04T15:30:22"
}
```

---

## 3. Housekeeping Service (Port 8082)

### Tasks

#### `GET /api/housekeeping/tasks`

Returns all housekeeping tasks ordered by priority (PENDING first, then IN_PROGRESS, then COMPLETED).

**Response (200):** `List<HousekeepingTask>`

#### `GET /api/housekeeping/tasks/{id}`

Returns a specific task.

**Response (200):** `HousekeepingTask`

#### `GET /api/housekeeping/tasks/status/{status}`

Filter tasks by status: `PENDING`, `IN_PROGRESS`, `COMPLETED`.

**Response (200):** `List<HousekeepingTask>`

#### `POST /api/housekeeping/tasks`

Create a manual housekeeping task.

**Request:**
```json
{
  "roomId": 5,
  "roomNumber": "201",
  "notes": "Deep clean required after VIP guest"
}
```

**Response (201):** Created `HousekeepingTask`

#### `PUT /api/housekeeping/tasks/{id}/status`

Update task status.

**Request:**
```json
{
  "status": "COMPLETED"
}
```

**Response (200):** Updated `HousekeepingTask`

**Valid transitions:** PENDING → IN_PROGRESS → COMPLETED

#### `PUT /api/housekeeping/tasks/{id}/assign`

Assign a staff member to a pending task. Automatically sets status to IN_PROGRESS.

**Request:**
```json
{
  "staffName": "Maria Garcia"
}
```

**Response (200):** Updated `HousekeepingTask`

**Errors:**
- `400 Bad Request` — Task is not in PENDING status

---

### Statistics

#### `GET /api/housekeeping/stats`

**Response (200):**
```json
{
  "pending": 3,
  "inProgress": 1,
  "completed": 12
}
```

---

## 4. Room Service (Port 8083)

### Orders

#### `GET /api/room-service/orders`

Returns all orders ordered by status workflow priority.

**Response (200):** `List<RoomServiceOrder>`

#### `GET /api/room-service/orders/{id}`

Returns a specific order.

#### `GET /api/room-service/orders/number/{orderNumber}`

Returns an order by order number (e.g., "ORD-20260604153022-0042").

#### `GET /api/room-service/orders/status/{status}`

Filter by status: `RECEIVED`, `PREPARING`, `DELIVERING`, `DELIVERED`, `CANCELLED`.

#### `POST /api/room-service/orders`

Create a new food/beverage order.

**Request:**
```json
{
  "guestId": 1,
  "roomId": 5,
  "items": "2x Caesar Salad, 1x Grilled Salmon, 1x Sparkling Water",
  "totalAmount": 45.99,
  "notes": "No dressing on the salad"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| guestId | Long | ✅ | — |
| roomId | Long | ✅ | — |
| items | String | ✅ | 1–500 characters |
| totalAmount | BigDecimal | ✅ | > 0 |
| notes | String | ❌ | — |

**Response (201):** Created `RoomServiceOrder` with generated `orderNumber`.

#### `PUT /api/room-service/orders/{id}/status`

Advance order to next status.

**Valid Status Transitions:**
```
RECEIVED → PREPARING → DELIVERING → DELIVERED
RECEIVED → CANCELLED
PREPARING → CANCELLED
```

**Request:**
```json
{
  "status": "PREPARING"
}
```

**Response (200):** Updated `RoomServiceOrder`

**Errors:**
- `400 Bad Request` — Invalid status transition

---

### Statistics

#### `GET /api/room-service/stats`

**Response (200):**
```json
{
  "received": 2,
  "preparing": 1,
  "delivering": 0,
  "delivered": 8
}
```

---

## 5. Maintenance Service (Port 8084)

### Requests

#### `GET /api/maintenance/requests`

Returns all requests ordered by priority (CRITICAL first, then by report time).

**Response (200):** `List<MaintenanceRequest>`

#### `GET /api/maintenance/requests/active`

Returns all non-CLOSED requests ordered by priority.

**Response (200):** `List<MaintenanceRequest>`

#### `GET /api/maintenance/requests/{id}`

Returns a specific request.

#### `GET /api/maintenance/requests/status/{status}`

Filter by status: `REPORTED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.

#### `POST /api/maintenance/requests`

Report a new maintenance issue.

**Request:**
```json
{
  "title": "AC not cooling properly",
  "description": "Air conditioning unit in room 201 blowing warm air",
  "roomId": 6,
  "priority": "HIGH"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| title | String | ✅ | 3–200 characters |
| description | String | ❌ | Max 1000 characters |
| roomId | Long | ✅ | — |
| priority | String | ✅ | CRITICAL, HIGH, NORMAL, LOW |

**Response (201):** Created `MaintenanceRequest`

#### `PUT /api/maintenance/requests/{id}/status`

Update issue status.

**Valid Status Transitions:**
```
REPORTED → IN_PROGRESS → RESOLVED → CLOSED
```

**Request:**
```json
{
  "status": "IN_PROGRESS"
}
```

**Response (200):** Updated `MaintenanceRequest`

#### `PUT /api/maintenance/requests/{id}/assign`

Assign a technician. Automatically sets status to IN_PROGRESS.

**Request:**
```json
{
  "technician": "Mike Johnson"
}
```

**Response (200):** Updated `MaintenanceRequest`

#### `POST /api/maintenance/requests/next`

Process the next highest-priority request from the PriorityQueue. Pops it and sets to IN_PROGRESS.

**Response (200):** `MaintenanceRequest` or `204 No Content` if queue is empty.

---

### Statistics

#### `GET /api/maintenance/stats`

**Response (200):**
```json
{
  "reported": 2,
  "inProgress": 1,
  "resolved": 5,
  "closed": 10
}
```

---

## 6. Error Response Format

All error responses follow a consistent format:

```json
{
  "timestamp": "2026-06-04T15:30:22.456",
  "status": 400,
  "error": "Bad Request",
  "message": "No rooms available for type PRESIDENTIAL"
}
```

For validation errors:
```json
{
  "timestamp": "2026-06-04T15:30:22.456",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "firstName": "First name is required",
    "email": "Email must be valid"
  },
  "message": "Validation failed. Check the errors field for details."
}
```
