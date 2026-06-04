# HotelOS — RabbitMQ Event-Driven Communication

## 1. Overview

HotelOS uses a **Topic Exchange** named `hotelos.exchange` for all inter-service communication. Services **never** call each other's REST APIs directly. All cross-service communication flows through RabbitMQ events.

```
                    ┌─────────────────────────────────────┐
                    │        hotelos.exchange              │
                    │        (Topic Exchange)              │
                    │        Durable: true                 │
                    └──────────┬──────────────────────────┘
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                     │
    event.room.*          event.guest.*          event.order.*
    event.maintenance.*   event.billing.*
```

## 2. Exchange & Routing

| Exchange | Type | Durable |
|----------|------|---------|
| `hotelos.exchange` | Topic | ✅ |

### Routing Keys

| Routing Key | Publisher | Consumers | Description |
|-------------|-----------|-----------|-------------|
| `event.room.released` | Reception | Housekeeping | Room released after checkout |
| `event.room.status` | Reception | (broadcast) | Room status changed |
| `event.guest.checkedin` | Reception | (broadcast) | Guest checked in |
| `event.guest.checkedout` | Reception | (broadcast) | Guest checked out |
| `event.order.status` | Room Service | (broadcast) | Order status changed |
| `event.maintenance.status` | Maintenance | (broadcast) | Maintenance status changed |

## 3. Event Schemas

### 3.1 RoomReleasedEvent

**Published by:** Reception Service (on check-out)
**Consumed by:** Housekeeping Service
**Routing Key:** `event.room.released`

```json
{
  "roomId": 5,
  "roomNumber": "201",
  "floor": 2,
  "roomType": "DOUBLE",
  "releasedAt": "2026-06-04T15:30:22"
}
```

**Trigger:** When a guest checks out, the reception service marks the room as DIRTY and publishes this event.

**Consumer Action:** Housekeeping service automatically creates a PENDING cleaning task.

---

### 3.2 RoomStatusChangedEvent

**Published by:** Reception Service, Housekeeping Service
**Consumed by:** (broadcast)
**Routing Key:** `event.room.status`

```json
{
  "roomId": 5,
  "roomNumber": "201",
  "oldStatus": "CLEAN",
  "newStatus": "OCCUPIED",
  "changedAt": "2026-06-04T14:20:00"
}
```

**Status Transitions:**
```
CLEAN → OCCUPIED    (check-in)
OCCUPIED → DIRTY    (check-out)
DIRTY → CLEANING    (housekeeping starts)
CLEANING → CLEAN    (housekeeping completes)
```

---

### 3.3 GuestCheckedInEvent

**Published by:** Reception Service
**Consumed by:** (broadcast)
**Routing Key:** `event.guest.checkedin`

```json
{
  "guestId": 1,
  "guestName": "John Doe",
  "roomId": 5,
  "roomNumber": "201",
  "checkedInAt": "2026-06-04T14:20:00"
}
```

---

### 3.4 GuestCheckedOutEvent

**Published by:** Reception Service
**Consumed by:** (broadcast)
**Routing Key:** `event.guest.checkedout`

```json
{
  "guestId": 1,
  "guestName": "John Doe",
  "roomId": 5,
  "roomNumber": "201",
  "totalAmount": 495.96,
  "checkedOutAt": "2026-06-04T15:30:22"
}
```

---

### 3.5 OrderStatusChangedEvent

**Published by:** Room Service
**Consumed by:** (broadcast)
**Routing Key:** `event.order.status`

```json
{
  "orderId": 1,
  "orderNumber": "ORD-20260604153022-0042",
  "oldStatus": "RECEIVED",
  "newStatus": "PREPARING",
  "guestName": "John Doe",
  "roomNumber": "Room 5",
  "changedAt": "2026-06-04T15:32:00"
}
```

---

### 3.6 MaintenanceStatusChangedEvent

**Published by:** Maintenance Service
**Consumed by:** (broadcast)
**Routing Key:** `event.maintenance.status`

```json
{
  "requestId": 1,
  "title": "AC not cooling",
  "priority": "HIGH",
  "oldStatus": "REPORTED",
  "newStatus": "IN_PROGRESS",
  "roomNumber": "Room 6",
  "assignedTechnician": "Mike Johnson",
  "changedAt": "2026-06-04T16:00:00"
}
```

## 4. Queue Configuration

### Housekeeping Queue (Consumer)

| Property | Value |
|----------|-------|
| Queue Name | `housekeeping.room-released` |
| Durable | ✅ |
| Binding Key | `event.room.released` |
| Exchange | `hotelos.exchange` |

```java
@Bean
public Queue roomReleasedQueue() {
    return QueueBuilder.durable("housekeeping.room-released").build();
}

@Bean
public Binding roomReleasedBinding(Queue roomReleasedQueue, TopicExchange exchange) {
    return BindingBuilder.bind(roomReleasedQueue)
        .to(exchange)
        .with("event.room.released");
}
```

### Reception Queue (Publisher only)

Reception publishes events but does not consume from other services.

## 5. Message Converter

All services use `Jackson2JsonMessageConverter` for automatic JSON serialization/deserialization of event objects:

```java
@Bean
public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
}

@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(jsonMessageConverter());
    return template;
}
```

## 6. Event Flow Diagrams

### Check-In Flow
```
Frontend ──POST /checkin──▶ Reception Service
                               │
                               ├── RoomAssignmentAlgorithm.assignRoom()
                               ├── Guest.save()
                               ├── Reservation.save()
                               │
                               ├──▶ RabbitMQ: event.guest.checkedin
                               ├──▶ RabbitMQ: event.room.status (CLEAN→OCCUPIED)
                               ├──▶ WebSocket: /topic/guests
                               └──▶ WebSocket: /topic/rooms
```

### Check-Out Flow
```
Frontend ──POST /checkout──▶ Reception Service
                                │
                                ├── BillingAlgorithm.calculateBill()
                                ├── Guest.status = CHECKED_OUT
                                ├── Room.status = DIRTY
                                ├── Reservation.active = false
                                │
                                ├──▶ RabbitMQ: event.room.released
                                │         │
                                │         └──▶ Housekeeping Service
                                │                   └── Creates PENDING task
                                │
                                ├──▶ RabbitMQ: event.guest.checkedout
                                ├──▶ RabbitMQ: event.room.status (→DIRTY)
                                ├──▶ WebSocket: /topic/guests
                                ├──▶ WebSocket: /topic/rooms
                                └──▶ WebSocket: /topic/billing
```

### Room Service Order Flow
```
Frontend ──POST /orders──▶ Room Service
                               │
                               ├── Order.save() (status=RECEIVED)
                               ├── processingQueue.add(order)
                               │
                               ├──▶ RabbitMQ: event.order.status
                               └──▶ WebSocket: /topic/orders

Frontend ──PUT /orders/{id}/status──▶ Room Service
                                         │
                                         ├── Validate transition
                                         ├── Order.updateStatus()
                                         │
                                         ├──▶ RabbitMQ: event.order.status
                                         └──▶ WebSocket: /topic/orders
```

### Maintenance Issue Flow
```
Frontend ──POST /requests──▶ Maintenance Service
                                  │
                                  ├── Request.save() (status=REPORTED)
                                  ├── priorityQueue.add(request)
                                  │
                                  ├──▶ RabbitMQ: event.maintenance.status
                                  └──▶ WebSocket: /topic/maintenance
```

## 7. Reliability

- All exchanges and queues are **durable** (survive RabbitMQ restart)
- Messages are persisted to disk by RabbitMQ
- `@RabbitListener` provides automatic acknowledgment
- JSON serialization ensures cross-service compatibility
- Jackson2 handles LocalDateTime serialization via `jackson-datatype-jsr310`
