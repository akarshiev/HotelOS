# HotelOS — Architecture Documentation

## 1. System Overview

HotelOS is a **real-time hotel management system** built for the GrandStay hotel. It is designed around a **microservices architecture** where each department operates as an independent, self-contained service that communicates exclusively through **RabbitMQ message broker** — never via direct REST calls between services.

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Frontend (SPA)                                 │
│              HTML5 + CSS3 + Vanilla JavaScript                         │
│              Port: 3000 (static file server)                           │
└───────────────────────┬─────────────────────────────────────────────────┘
                        │ HTTP (fetch API)
                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Security (Basic Auth)                        │
│                    admin / admin123                                     │
└───────────────────────┬─────────────────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┬───────────────┐
        ▼               ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  Reception   │ │ Housekeeping │ │ Room Service │ │ Maintenance  │
│  :8081       │ │  :8082       │ │  :8083       │ │  :8084       │
│              │ │              │ │              │ │              │
│ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │
│ hotelos_     │ │ hotelos_     │ │ hotelos_     │ │ hotelos_     │
│ reception    │ │ housekeeping │ │ roomservice  │ │ maintenance  │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │                │
       └────────────────┴───────┬────────┴────────────────┘
                                │
                                ▼
                    ┌───────────────────────┐
                    │       RabbitMQ        │
                    │   hotelos.exchange    │
                    │   (Topic Exchange)    │
                    └───────────────────────┘
```

## 2. Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.4.5 |
| Build Tool | Gradle | 8.12 |
| Database | PostgreSQL | 14+ |
| Message Broker | RabbitMQ | 3.x |
| WebSocket | Spring WebSocket (STOMP + SockJS) | via Spring Boot |
| Auth | Spring Security (Basic Auth) | via Spring Boot |
| ORM | Spring Data JPA (Hibernate) | via Spring Boot |
| Frontend | HTML5 + CSS3 + Vanilla JS | — |
| Libraries | Lombok, Jackson, Jakarta Validation | — |

## 3. Microservices

### 3.1 Reception Service (Port 8081)
**Responsibility:** Guest lifecycle management, room assignment, billing.

- Guest Check-In / Check-Out
- Room Assignment Algorithm (with pessimistic locking)
- Billing Algorithm
- Room inventory management
- Publishes: `GuestCheckedIn`, `GuestCheckedOut`, `RoomReleased` events
- Exposes: Dashboard stats, guest/room CRUD

### 3.2 Housekeeping Service (Port 8082)
**Responsibility:** Cleaning task management.

- Listens for `RoomReleased` events from Reception
- Auto-creates PENDING cleaning tasks
- Task assignment and completion workflow
- Publishes: `RoomStatusChanged` (CLEANING → CLEAN)
- Tasks ordered by creation time (FIFO queue)

### 3.3 Room Service (Port 8083)
**Responsibility:** Food & beverage order management.

- Order lifecycle: RECEIVED → PREPARING → DELIVERING → DELIVERED
- Uses in-memory `ConcurrentLinkedQueue` for order processing
- Status transitions are validated (no skipping states)
- Publishes: `OrderStatusChanged` on every status update

### 3.4 Maintenance Service (Port 8084)
**Responsibility:** Issue tracking and repair management.

- Uses `PriorityQueue` (CRITICAL > HIGH > NORMAL > LOW)
- Technician assignment workflow
- Status: REPORTED → IN_PROGRESS → RESOLVED → CLOSED
- Publishes: `MaintenanceStatusChanged` on every update

## 4. Communication Pattern

### 4.1 Inter-Service (RabbitMQ)
Services communicate **only** through RabbitMQ. No service calls another service's REST API.

```
Exchange: hotelos.exchange (Topic)
├── event.room.released     → Housekeeping consumes
├── event.room.status       → (broadcast)
├── event.guest.checkedin   → (broadcast)
├── event.guest.checkedout  → (broadcast)
├── event.order.status      → (broadcast)
└── event.maintenance.status → (broadcast)
```

### 4.2 Client → Service (REST + WebSocket)
- **REST API:** Synchronous request/response for CRUD operations
- **WebSocket (STOMP):** Real-time push notifications to the frontend

```
Frontend ──HTTP──▶ Reception Service ──RabbitMQ──▶ Housekeeping Service
                    │                                    │
                    ├──WebSocket──▶ /topic/rooms         │
                    ├──WebSocket──▶ /topic/guests        │
                    ├──WebSocket──▶ /topic/orders        │
                    └──WebSocket──▶ /topic/maintenance   │
```

## 5. Data Isolation

Each microservice has its **own PostgreSQL database**:

| Service | Database | Tables |
|---------|----------|--------|
| Reception | `hotelos_reception` | rooms, guests, reservations, room_service_orders, housekeeping_tasks, maintenance_requests |
| Housekeeping | `hotelos_housekeeping` | rooms, housekeeping_tasks |
| Room Service | `hotelos_roomservice` | rooms, guests, room_service_orders |
| Maintenance | `hotelos_maintenance` | rooms, maintenance_requests |

Each service's `DataInitializer` seeds room data independently. Cross-service data consistency is maintained via events.

## 6. Security

- **Spring Security** with HTTP Basic Authentication
- Credentials: `admin` / `admin123` (in-memory `UserDetailsService`)
- Stateless sessions (no server-side session)
- CSRF disabled (API-only, no cookie-based auth)
- WebSocket endpoints (`/ws/**`) are permitted without auth
- Auth endpoints (`/api/auth/**`) are public

## 7. Error Handling

Global exception handler (`GlobalExceptionHandler`) catches all exceptions and returns structured JSON responses:

| Exception | HTTP Status | Response |
|-----------|-------------|----------|
| `ResourceNotFoundException` | 404 | `{ timestamp, status, error, message }` |
| `BadRequestException` | 400 | `{ timestamp, status, error, message }` |
| `ConflictException` | 409 | `{ timestamp, status, error, message }` |
| `MethodArgumentNotValidException` | 400 | `{ timestamp, status, error, errors: {field: msg} }` |
| `IllegalArgumentException` | 400 | `{ timestamp, status, error, message }` |
| `Exception` (generic) | 500 | `{ timestamp, status, error: "Internal Server Error", message: "..." }` |

Stack traces are **never** exposed to clients.

## 8. Project Module Structure

```
HotelOS/
├── build.gradle                    # Root build config (Java 17, Spring Boot 3.4.5)
├── settings.gradle                 # Multi-module: shared + 4 services
├── gradlew / gradlew.bat           # Gradle wrapper
├── .gitignore
├── README.md
├── docs/                           # This documentation
├── shared/                         # Shared library module
│   ├── build.gradle
│   └── src/main/java/com/hotelos/shared/
│       ├── enums/                  # 7 enums (RoomType, RoomStatus, etc.)
│       ├── entities/               # 6 JPA entities
│       ├── events/                 # 6 event POJOs
│       ├── dto/                    # 6 DTOs
│       └── exception/              # Global exception handler + custom exceptions
├── reception-service/              # Port 8081
│   ├── build.gradle
│   ├── src/main/resources/application.yml
│   └── src/main/java/com/hotelos/reception/
│       ├── ReceptionServiceApplication.java
│       ├── config/                 # SecurityConfig, WebSocketConfig, RabbitMQConfig, DataInitializer
│       ├── repository/             # RoomRepository, GuestRepository, ReservationRepository
│       ├── service/                # ReceptionService, RoomAssignmentAlgorithm, BillingAlgorithm
│       └── controller/             # ReceptionController, AuthController
├── housekeeping-service/           # Port 8082
│   └── ... (same structure)
├── room-service/                   # Port 8083
│   └── ... (same structure)
├── maintenance-service/            # Port 8084
│   └── ... (same structure)
└── frontend/                       # SPA
    ├── index.html
    ├── css/styles.css
    └── js/
        ├── app.js                  # Main application logic
        └── dashboard-charts.js     # Optional chart enhancements
```
