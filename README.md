# HotelOS – Real Vaqtli Mehmonxona Boshqaruv Tizimi

## Loyiha Maqsadi

GrandStay mehmonxonasi uchun real vaqt rejimida ishlovchi HotelOS nomli mehmonxona boshqaruv tizimi. Tizim mikroservis arxitekturasi asosida qurilgan va quyidagi bo'limlarni yagona platformaga birlashtiradi:

- **Reception** (Qabul)
- **Housekeeping** (Tozalash)
- **Room Service** (Xona xizmati)
- **Maintenance** (Texnik xizmat)

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Architecture](docs/ARCHITECTURE.md) | System design, microservice architecture, communication patterns |
| [API Reference](docs/API.md) | Complete REST API documentation for all 4 services |
| [Algorithms](docs/ALGORITHMS.md) | Room Assignment & Billing algorithms with flow diagrams |
| [Events](docs/EVENTS.md) | RabbitMQ event-driven communication & message schemas |
| [Database](docs/DATABASE.md) | Database schemas, entity relationships, data structures |
| [Frontend](docs/FRONTEND.md) | Frontend architecture, pages, WebSocket integration |
| [Setup Guide](docs/SETUP.md) | Installation, configuration, deployment & troubleshooting |

---

## Texnologiyalar

### Backend
- Java 17 (Note: Java 21 is specified in brief but runtime environment is 17)
- Spring Boot 3.4.5 (Note: 4.0.6 is specified in brief but not officially released yet, kept at 3.4.5 for stability)
- Gradle 8.12 (multi-module)
- Spring Web, Spring Data JPA, Spring Security
- Spring WebSocket (STOMP)
- PostgreSQL
- RabbitMQ (Spring AMQP)
- Lombok

### Frontend
- HTML5, CSS3, Vanilla JavaScript
- Dark mode Vercel Dashboard-style UI
- WebSocket real-time updates (SockJS + STOMP)

---

## Loyiha Tuzilishi

```
HotelOS/
├── build.gradle                    # Root Gradle config
├── settings.gradle                 # Multi-module settings
├── shared/                         # Shared entities, events, DTOs, enums
├── reception-service/              # Port 8081 - Qabul xizmati
├── housekeeping-service/           # Port 8082 - Tozalash xizmati
├── room-service/                   # Port 8083 - Xona xizmati
├── maintenance-service/            # Port 8084 - Texnik xizmat
├── frontend/                       # Frontend SPA
└── docs/                           # Project documentation
```

---

## 🚀 How to Run the Project

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| **Java** | 17+ | Runtime for all microservices |
| **PostgreSQL** | 14+ | Database (4 separate databases) |
| **RabbitMQ** | 3.12+ | Message broker for inter-service communication |
| **Gradle** | 8.12 (wrapper included) | Build tool |
| **Python 3** | 3.x | Frontend dev server (optional) |

---

### Step 1: Install & Start PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql

# Create 4 databases for each microservice
sudo -u postgres psql -c "CREATE DATABASE hotelos_reception;"
sudo -u postgres psql -c "CREATE DATABASE hotelos_housekeeping;"
sudo -u postgres psql -c "CREATE DATABASE hotelos_roomservice;"
sudo -u postgres psql -c "CREATE DATABASE hotelos_maintenance;"
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
```

> **Note:** The password must be `postgres` (matches `application.yml` configs).

### Step 2: Install & Start RabbitMQ

```bash
# Ubuntu/Debian
sudo apt install rabbitmq-server
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server  # Optional: auto-start on boot
```

> RabbitMQ runs on default port `5672` with guest/guest credentials.

### Step 3: Build the Project

```bash
cd HotelOS
./gradlew build
```

> This compiles all modules (shared, reception, housekeeping, room-service, maintenance).
> Tests will run automatically. To skip tests: `./gradlew build -x test`

### Step 4: Start the Microservices

Open **4 separate terminals** and run each service:

```bash
# Terminal 1 - Reception Service (port 8081)
cd HotelOS && ./gradlew :reception-service:bootRun

# Terminal 2 - Housekeeping Service (port 8082)
cd HotelOS && ./gradlew :housekeeping-service:bootRun

# Terminal 3 - Room Service (port 8083)
cd HotelOS && ./gradlew :room-service:bootRun

# Terminal 4 - Maintenance Service (port 8084)
cd HotelOS && ./gradlew :maintenance-service:bootRun
```

On first startup, each service will:
- Automatically create its database tables via Hibernate `ddl-auto: update`
- Seed 15 rooms across 3 floors via `DataInitializer`
- Connect to RabbitMQ for event-driven communication

> **Startup order** doesn't matter — services are independent and can start in any order.

### Step 5: Start the Frontend

```bash
cd HotelOS/frontend
python3 -m http.server 3000
```

Open **http://localhost:3000** in your browser.
- **Login:** `admin` / `admin123`

---

### Verify Everything is Running

```bash
# Check each service responds
curl -s -o /dev/null -w 'Reception: %{http_code}\n' http://localhost:8081/api/reception/rooms -u admin:admin123
curl -s -o /dev/null -w 'Housekeeping: %{http_code}\n' http://localhost:8082/api/housekeeping/tasks -u admin:admin123
curl -s -o /dev/null -w 'Room-Service: %{http_code}\n' http://localhost:8083/api/room-service/orders -u admin:admin123
curl -s -o /dev/null -w 'Maintenance: %{http_code}\n' http://localhost:8084/api/maintenance/requests -u admin:admin123
```

Expected output (all return `200` or `401`):
```
Reception: 200
Housekeeping: 200
Room-Service: 200
Maintenance: 200
```

---

### Quick End-to-End Test

```bash
# 1. Check-in a guest
curl -X POST http://localhost:8081/api/reception/checkin \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"firstName":"John","lastName":"Doe","email":"john@test.com","roomType":"DOUBLE","preferredFloor":2,"preferLift":true}'

# 2. Create a room service order
curl -X POST http://localhost:8083/api/room-service/orders \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"guestId":1,"roomId":6,"items":"1x Grilled Salmon","totalAmount":25.99}'

# 3. Advance the order to DELIVERED
curl -X PUT http://localhost:8083/api/room-service/orders/1/status \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"status":"DELIVERING"}'
curl -X PUT http://localhost:8083/api/room-service/orders/1/status \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"status":"DELIVERED"}'

# 4. Report a maintenance issue
curl -X POST http://localhost:8084/api/maintenance/requests \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"title":"AC not working","description":"Room 201 AC is not cooling","roomId":6,"priority":"HIGH"}'

# 5. Check-out and view the bill
curl -X POST http://localhost:8081/api/reception/checkout \
  -H "Content-Type: application/json" -u admin:admin123 \
  -d '{"guestId":1}'
```

---

## WebSocket Ishlatish

### Endpoint
```
ws://localhost:8081/ws
```

### Topic-lar
| Topic | Ma'lumot |
|-------|----------|
| `/topic/rooms` | Xona holati o'zgarishi |
| `/topic/guests` | Mehmon check-in/out |
| `/topic/orders` | Xona xizmati buyurtmalari |
| `/topic/maintenance` | Texnik xizmat yangiliklari |
| `/topic/billing` | Hisob-faktura |

---

## API Endpoint-lar

### Reception Service (port 8081)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| POST | `/api/reception/checkin` | Mehmon joylashtirish |
| POST | `/api/reception/checkout` | Mehmon chiqarish |
| GET | `/api/reception/guests` | Barcha mehmonlar |
| GET | `/api/reception/guests/active` | Faol mehmonlar |
| GET | `/api/reception/rooms` | Barcha xonalar |
| GET | `/api/reception/rooms/status/{status}` | Xonalar holat bo'yicha |
| GET | `/api/reception/dashboard/stats` | Dashboard statistikasi |

### Housekeeping Service (port 8082)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| GET | `/api/housekeeping/tasks` | Barcha vazifalar |
| POST | `/api/housekeeping/tasks` | Yangi vazifa yaratish |
| PUT | `/api/housekeeping/tasks/{id}/status` | Vazifa holatini yangilash |
| PUT | `/api/housekeeping/tasks/{id}/assign` | Vazifani tayinlash |

### Room Service (port 8083)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| POST | `/api/room-service/orders` | Yangi buyurtma |
| GET | `/api/room-service/orders` | Barcha buyurtmalar |
| PUT | `/api/room-service/orders/{id}/status` | Buyurtma holatini yangilash |

### Maintenance Service (port 8084)
| Method | Endpoint | Tavsif |
|--------|----------|--------|
| POST | `/api/maintenance/requests` | Muammoni xabar qilish |
| GET | `/api/maintenance/requests/active` | Faol muammolar |
| PUT | `/api/maintenance/requests/{id}/status` | Muammo holatini yangilash |
| PUT | `/api/maintenance/requests/{id}/assign` | Texnik tayinlash |

---

## Test Qilish Bosqichlari

### TS-01: Guest Check-In
```bash
curl -X POST http://localhost:8081/api/reception/checkin \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "roomType": "DOUBLE",
    "preferredFloor": 2,
    "preferLift": true
  }'
```

### TS-02: Guest Check-Out
```bash
curl -X POST http://localhost:8081/api/reception/checkout \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"guestId": 1}'
```

### TS-03: Room Service Order
```bash
curl -X POST http://localhost:8083/api/room-service/orders \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "guestId": 1,
    "roomId": 1,
    "items": "2x Caesar Salad, 1x Grilled Salmon",
    "totalAmount": 45.99
  }'
```

### TS-04: Maintenance Request
```bash
curl -X POST http://localhost:8084/api/maintenance/requests \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "title": "AC not working",
    "description": "Air conditioning unit in room 201 not cooling",
    "roomId": 6,
    "priority": "HIGH"
  }'
```

---

## Loyiha Xususiyatlari

- ✅ Mikroservis arxitekturasi (4 ta mustaqil xizmat)
- ✅ RabbitMQ orqali event-driven muloqot
- ✅ Room Assignment Algorithm (pessimistic locking, concurrency support)
- ✅ Billing Algorithm (early checkout, zero charges, discounts)
- ✅ Real-time Dashboard (WebSocket/STOMP)
- ✅ Spring Security authentication
- ✅ Global Exception Handling (stack trace hidden)
- ✅ Vercel Dashboard-style dark mode UI
- ✅ Responsive layout
- ✅ PostgreSQL database (4 independent databases)
- ✅ Input validation (Jakarta Validation)
- ✅ PriorityQueue for maintenance, Queue for orders
