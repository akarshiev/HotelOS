# HotelOS — Setup & Deployment Guide

## 1. Prerequisites

| Software | Version | Purpose |
|----------|---------|---------|
| Java (JDK) | 17+ | Build & run Spring Boot services |
| PostgreSQL | 14+ | Database for all services |
| RabbitMQ | 3.x | Message broker |
| Gradle | 8.12 (via wrapper) | Build tool |
| Web Browser | Chrome/Firefox/Safari | Frontend |

## 2. Installation

### 2.1 Install Java 17

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
# openjdk version "17.x.x"
```

**macOS (Homebrew):**
```bash
brew install openjdk@17
```

**Windows:** Download from [Adoptium](https://adoptium.net/)

### 2.2 Install PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install postgresql postgresql-contrib

# Start service
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

### 2.3 Install RabbitMQ

```bash
# Ubuntu/Debian
sudo apt install rabbitmq-server

# Enable management plugin (optional, for web UI)
sudo rabbitmq-plugins enable rabbitmq_management

# Start service
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server
```

**Management UI:** http://localhost:15672 (guest/guest)

## 3. Database Setup

Create 4 databases (one per microservice):

```bash
sudo -u postgres psql
```

```sql
CREATE DATABASE hotelos_reception;
CREATE DATABASE hotelos_housekeeping;
CREATE DATABASE hotelos_roomservice;
CREATE DATABASE hotelos_maintenance;

-- Set password (if needed)
ALTER USER postgres PASSWORD 'postgres';

\q
```

> **Note:** Tables are auto-created by Hibernate on first startup (`ddl-auto: update`).
> Room data is seeded automatically by each service's `DataInitializer`.

## 4. Build the Project

```bash
# Navigate to project root
cd HotelOS

# Build all modules
./gradlew build

# Expected output: BUILD SUCCESSFUL
```

## 5. Run the Services

Each service runs on its own port. Open **4 separate terminals**:

### Terminal 1 — Reception Service (Port 8081)
```bash
cd reception-service
../gradlew bootRun
```

### Terminal 2 — Housekeeping Service (Port 8082)
```bash
cd housekeeping-service
../gradlew bootRun
```

### Terminal 3 — Room Service (Port 8083)
```bash
cd room-service
../gradlew bootRun
```

### Terminal 4 — Maintenance Service (Port 8084)
```bash
cd maintenance-service
../gradlew bootRun
```

**Wait for all 4 services to start** (check for "Started ...Application" in logs) before proceeding.

## 6. Run the Frontend

```bash
cd frontend
```

Choose one of:

**Python:**
```bash
python3 -m http.server 3000
```

**Node.js:**
```bash
npx serve -p 3000
```

**PHP:**
```bash
php -S localhost:3000
```

**Open in browser:** http://localhost:3000

**Login:** `admin` / `admin123`

## 7. Configuration

### 7.1 Service Ports

| Service | Default Port | Config File |
|---------|-------------|-------------|
| Reception | 8081 | `reception-service/src/main/resources/application.yml` |
| Housekeeping | 8082 | `housekeeping-service/src/main/resources/application.yml` |
| Room Service | 8083 | `room-service/src/main/resources/application.yml` |
| Maintenance | 8084 | `maintenance-service/src/main/resources/application.yml` |
| Frontend | 3000 | Configured in your HTTP server |

### 7.2 Database Configuration

Each service's `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hotelos_reception
    username: postgres
    password: postgres
```

### 7.3 RabbitMQ Configuration

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### 7.4 Change Admin Credentials

Edit `SecurityConfig.java` in each service:
```java
var admin = User.builder()
    .username("admin")           // ← Change this
    .password(encoder.encode("admin123"))  // ← Change this
    .roles("ADMIN")
    .build();
```

Also update `AuthController.java` login check.

### 7.5 Frontend Service URLs

Edit `frontend/js/app.js`:
```javascript
const SERVICES = {
    reception:     'http://localhost:8081/api',
    housekeeping:  'http://localhost:8082/api',
    roomservice:   'http://localhost:8083/api',
    maintenance:   'http://localhost:8084/api',
};
```

## 8. Verification Checklist

After starting all services, verify:

| Check | Expected Result |
|-------|----------------|
| `curl http://localhost:8081/api/auth/verify -u admin:admin123` | `{"authenticated":true,...}` |
| `curl http://localhost:8081/api/reception/rooms -u admin:admin123` | Returns 15 rooms |
| `curl http://localhost:8081/api/reception/dashboard/stats -u admin:admin123` | Returns stats JSON |
| Open http://localhost:3000 | Login page loads |
| Login with admin/admin123 | Dashboard shows with live stats |

## 9. Test Scenarios

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

### TS-02: Place Room Service Order
```bash
curl -X POST http://localhost:8083/api/room-service/orders \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "guestId": 1,
    "roomId": 5,
    "items": "2x Caesar Salad, 1x Grilled Salmon",
    "totalAmount": 45.99
  }'
```

### TS-03: Report Maintenance Issue
```bash
curl -X POST http://localhost:8084/api/maintenance/requests \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "title": "AC not working",
    "description": "Air conditioning in room 201 not cooling",
    "roomId": 6,
    "priority": "HIGH"
  }'
```

### TS-04: Guest Check-Out (with billing)
```bash
curl -X POST http://localhost:8081/api/reception/checkout \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{"guestId": 1}'
```

### TS-05: WebSocket Real-Time
Open browser console on http://localhost:3000:
```javascript
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);
stompClient.connect({}, () => {
    stompClient.subscribe('/topic/rooms', msg => console.log('Room:', JSON.parse(msg.body)));
});
```
Then perform a check-in — room status change should appear in console.

## 10. Troubleshooting

| Problem | Solution |
|---------|----------|
| `Connection refused` on port 8081 | Reception service not running |
| `No rooms available` on check-in | Wrong DB or DataInitializer didn't run |
| WebSocket shows "Offline" | Check if reception service port 8081 is accessible |
| `AMQP ConnectionException` | RabbitMQ not running: `sudo systemctl start rabbitmq-server` |
| `PSQLException: database does not exist` | Create databases per section 3 |
| Frontend can't reach housekeeping service | Check CORS, ensure housekeeping on port 8082 |
| Build fails with `Java 21` error | Project targets Java 17; update `build.gradle` or install JDK 17 |
