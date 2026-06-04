# HotelOS — Database Documentation

## 1. Database Architecture

Each microservice has its **own independent PostgreSQL database** to ensure data isolation in the microservice architecture.

| Service | Database Name | Port |
|---------|---------------|------|
| Reception | `hotelos_reception` | 5432 |
| Housekeeping | `hotelos_housekeeping` | 5432 |
| Room Service | `hotelos_roomservice` | 5432 |
| Maintenance | `hotelos_maintenance` | 5432 |

Schema is managed by Hibernate with `ddl-auto: update` (auto-creates/updates tables).

## 2. Entity Relationship Diagram

```
┌──────────────┐       ┌──────────────┐       ┌───────────────────┐
│    rooms     │       │    guests    │       │   reservations    │
├──────────────┤       ├──────────────┤       ├───────────────────┤
│ id (PK)      │◄──┐   │ id (PK)      │◄──┐   │ id (PK)           │
│ roomNumber   │   │   │ firstName    │   │   │ guest_id (FK) ────┤──▶ guests
│ floor        │   │   │ lastName     │   │   │ room_id (FK) ─────┤──▶ rooms
│ roomType     │   ├───┤─ room_id     │   │   │ roomType          │
│ status       │   │   │ email        │   │   │ checkInDate       │
│ pricePerNight│   │   │ phoneNumber  │   │   │ checkOutDate      │
│ nearLift     │   │   │ idDocument   │   │   │ totalAmount       │
│ nearStairs   │   │   │ status       │   │   │ active            │
│ lastCleanedAt│   │   │ preferredFloor│  │   │ createdAt         │
│ active       │   │   │ preferLift   │   │   └───────────────────┘
└──────────────┘   │   │ checkInTime  │   │
                   │   │ checkOutTime │   │
                   │   └──────────────┘   │
                   │                      │
       ┌───────────┤   ┌──────────────────┤
       │           │   │                  │
       │  ┌────────────────────┐  ┌──────────────────────────┐
       │  │room_service_orders │  │    maintenance_requests   │
       │  ├────────────────────┤  ├──────────────────────────┤
       │  │ id (PK)            │  │ id (PK)                  │
       │  │ orderNumber        │  │ title                    │
       │  │ guest_id (FK) ─────┤  │ description              │
       │  │ room_id (FK) ──────┤  │ room_id (FK) ────────────┤
       │  │ guestId            │  │ roomId                   │
       │  │ roomId             │  │ roomNumber               │
       │  │ roomNumber         │  │ priority                 │
       │  │ items              │  │ status                   │
       │  │ totalAmount        │  │ assignedTechnician       │
       │  │ status             │  │ reportedAt               │
       │  │ notes              │  │ resolvedAt               │
       │  │ orderedAt          │  └──────────────────────────┘
       │  │ deliveredAt        │
       │  └────────────────────┘  ┌──────────────────────────┐
       │                          │   housekeeping_tasks     │
       │                          ├──────────────────────────┤
       └──────────────────────────┤ id (PK)                  │
                                  │ room_id (FK) ────────────┤
                                  │ roomId                   │
                                  │ roomNumber               │
                                  │ status                   │
                                  │ assignedStaff            │
                                  │ notes                    │
                                  │ createdAt                │
                                  │ startedAt                │
                                  │ completedAt              │
                                  └──────────────────────────┘
```

## 3. Entity Definitions

### 3.1 Room

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| room_number | VARCHAR | NOT NULL, UNIQUE | Room number (e.g., "201") |
| floor | INT | NOT NULL | Floor number |
| room_type | VARCHAR | NOT NULL (enum) | SINGLE, DOUBLE, SUITE, DELUXE, PRESIDENTIAL |
| status | VARCHAR | NOT NULL (enum) | CLEAN, AVAILABLE, OCCUPIED, DIRTY, CLEANING, OUT_OF_ORDER |
| price_per_night | DECIMAL(10,2) | NOT NULL | Nightly rate |
| near_lift | BOOLEAN | | Is room near the lift |
| near_stairs | BOOLEAN | | Is room near the stairs |
| last_cleaned_at | TIMESTAMP | | Last time room was cleaned |
| active | BOOLEAN | NOT NULL, DEFAULT true | Soft-delete flag |

### 3.2 Guest

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| first_name | VARCHAR | NOT NULL | Guest first name |
| last_name | VARCHAR | NOT NULL | Guest last name |
| email | VARCHAR | NOT NULL, UNIQUE | Guest email |
| phone_number | VARCHAR | | Contact phone |
| id_document | VARCHAR | | Passport/ID number |
| status | VARCHAR | NOT NULL (enum) | REGISTERED, CHECKED_IN, CHECKED_OUT |
| preferred_floor | INT | | Floor preference |
| prefer_lift | BOOLEAN | | Lift proximity preference |
| check_in_time | TIMESTAMP | | Check-in timestamp |
| check_out_time | TIMESTAMP | | Check-out timestamp |
| room_id | BIGINT | FK → rooms | Assigned room |

### 3.3 Reservation

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| guest_id | BIGINT | FK → guests, NOT NULL | Guest reference |
| room_id | BIGINT | FK → rooms | Room reference |
| room_type | VARCHAR | NOT NULL (enum) | Requested room type |
| check_in_date | DATE | NOT NULL | Check-in date |
| check_out_date | DATE | NOT NULL | Check-out date |
| total_amount | DECIMAL(10,2) | | Final bill amount |
| active | BOOLEAN | DEFAULT true | Active reservation flag |
| created_at | TIMESTAMP | | Creation timestamp |

### 3.4 RoomServiceOrder

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| order_number | VARCHAR | NOT NULL | Generated order number |
| guest_id | BIGINT | FK → guests | Guest reference |
| room_id | BIGINT | FK → rooms | Room reference |
| guest_id_data | BIGINT | | Denormalized guest ID (cross-service) |
| room_id_data | BIGINT | | Denormalized room ID (cross-service) |
| room_number | VARCHAR | | Denormalized room number |
| items | VARCHAR | NOT NULL | Order items description |
| total_amount | DECIMAL(10,2) | NOT NULL | Order total |
| status | VARCHAR | NOT NULL (enum) | RECEIVED, PREPARING, DELIVERING, DELIVERED, CANCELLED |
| notes | VARCHAR | | Special instructions |
| ordered_at | TIMESTAMP | | Order timestamp |
| delivered_at | TIMESTAMP | | Delivery timestamp |

### 3.5 MaintenanceRequest

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| title | VARCHAR | NOT NULL | Issue title |
| description | TEXT | | Detailed description |
| room_id | BIGINT | FK → rooms | Affected room |
| room_id_data | BIGINT | | Denormalized room ID (cross-service) |
| room_number | VARCHAR | | Denormalized room number |
| priority | VARCHAR | NOT NULL (enum) | CRITICAL, HIGH, NORMAL, LOW |
| status | VARCHAR | NOT NULL (enum) | REPORTED, IN_PROGRESS, RESOLVED, CLOSED |
| assigned_technician | VARCHAR | | Assigned technician name |
| reported_at | TIMESTAMP | | Issue reported timestamp |
| resolved_at | TIMESTAMP | | Resolution timestamp |

### 3.6 HousekeepingTask

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGINT | PK, Auto-increment | Unique identifier |
| room_id | BIGINT | FK → rooms | Room to clean |
| room_id_data | BIGINT | | Denormalized room ID (cross-service) |
| room_number | VARCHAR | | Denormalized room number |
| status | VARCHAR | NOT NULL (enum) | PENDING, IN_PROGRESS, COMPLETED |
| assigned_staff | VARCHAR | | Assigned cleaner name |
| notes | VARCHAR | | Task notes |
| created_at | TIMESTAMP | | Task creation timestamp |
| started_at | TIMESTAMP | | When cleaning started |
| completed_at | TIMESTAMP | | When cleaning completed |

## 4. Enumerations

### RoomType
`SINGLE` · `DOUBLE` · `SUITE` · `DELUXE` · `PRESIDENTIAL`

### RoomStatus
`AVAILABLE` · `OCCUPIED` · `DIRTY` · `CLEANING` · `CLEAN` · `OUT_OF_ORDER`

### GuestStatus
`REGISTERED` · `CHECKED_IN` · `CHECKED_OUT`

### OrderStatus
`RECEIVED` · `PREPARING` · `DELIVERING` · `DELIVERED` · `CANCELLED`

### MaintenancePriority
`CRITICAL` · `HIGH` · `NORMAL` · `LOW`

### MaintenanceStatus
`REPORTED` · `IN_PROGRESS` · `RESOLVED` · `CLOSED`

### TaskStatus
`PENDING` · `IN_PROGRESS` · `COMPLETED`

## 5. Required Data Structures

As specified in the requirements:

| Structure | Implementation | Location |
|-----------|---------------|----------|
| **List** | `List<Room>` | `RoomRepository.findByRoomTypeAndStatus()` |
| **Map** | `Map<String, Room>` | `GuestRepository.findByEmail()` → lookup by email |
| **Queue** | `ConcurrentLinkedQueue<RoomServiceOrder>` | `RoomOrderService.processingQueue` (in-memory) |
| **PriorityQueue** | `PriorityQueue<MaintenanceRequest>` | `MaintenanceService.priorityQueue` (in-memory) |

### PriorityQueue Comparator
```java
Comparator.comparingInt(request -> switch(request.priority) {
    case CRITICAL -> 0;
    case HIGH -> 1;
    case NORMAL -> 2;
    case LOW -> 3;
})
.thenComparing(MaintenanceRequest::getReportedAt)
```

## 6. Default Room Inventory

Each service's `DataInitializer` seeds 15 rooms across 3 floors:

| Room | Floor | Type | Price/Night | Near Lift | Near Stairs |
|------|-------|------|-------------|-----------|-------------|
| 101 | 1 | SINGLE | $99.99 | ✅ | ❌ |
| 102 | 1 | SINGLE | $99.99 | ❌ | ✅ |
| 103 | 1 | DOUBLE | $149.99 | ✅ | ❌ |
| 104 | 1 | DOUBLE | $149.99 | ❌ | ✅ |
| 105 | 1 | SUITE | $249.99 | ✅ | ❌ |
| 201 | 2 | DOUBLE | $159.99 | ✅ | ❌ |
| 202 | 2 | DOUBLE | $159.99 | ❌ | ✅ |
| 203 | 2 | SUITE | $259.99 | ✅ | ❌ |
| 204 | 2 | SUITE | $259.99 | ❌ | ✅ |
| 205 | 2 | DELUXE | $349.99 | ✅ | ❌ |
| 301 | 3 | SUITE | $279.99 | ✅ | ❌ |
| 302 | 3 | DELUXE | $359.99 | ✅ | ❌ |
| 303 | 3 | DELUXE | $359.99 | ❌ | ✅ |
| 304 | 3 | PRESIDENTIAL | $599.99 | ✅ | ❌ |
| 305 | 3 | SINGLE | $89.99 | ❌ | ✅ |
