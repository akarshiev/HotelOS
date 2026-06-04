# HotelOS — Frontend Documentation

## 1. Overview

The frontend is a **Single-Page Application (SPA)** built with vanilla HTML5, CSS3, and JavaScript — no frameworks. It connects to all 4 backend services and provides a real-time dashboard via WebSocket (STOMP + SockJS).

**Design Philosophy:** Vercel Dashboard-style dark mode UI with minimal aesthetics, smooth transitions, and professional typography.

## 2. Technology

| Component | Technology |
|-----------|-----------|
| Markup | HTML5 |
| Styling | Custom CSS3 (dark theme, CSS variables) |
| Logic | Vanilla JavaScript (ES6+) |
| WebSocket | SockJS + STOMP.js (CDN) |
| Typography | Inter (Google Fonts) |

## 3. File Structure

```
frontend/
├── index.html              # Single HTML file (all pages)
├── css/
│   └── styles.css          # Complete dark theme stylesheet
└── js/
    ├── app.js              # Main application logic (~400 lines)
    └── dashboard-charts.js # Optional chart enhancements
```

## 4. Pages

### 4.1 Login
- Username/password form
- Validates against `POST /api/auth/login`
- Stores auth state in `localStorage`
- Default credentials displayed

### 4.2 Dashboard
- **Stats Grid:** 6 cards showing Total Rooms, Available, Occupied, Dirty, Active Guests, Maintenance
- **Room Status List:** Last 10 rooms with color-coded status dots
- **Recent Activity Feed:** Real-time WebSocket events
- **Connection Status:** Green "Live" badge when WebSocket connected

### 4.3 Rooms
- **Grid View:** All rooms displayed as cards
- **Filter:** Dropdown to filter by status (All, Clean, Occupied, Dirty, Cleaning, Out of Order)
- **Room Card:** Shows number, status badge, type, floor, price, lift proximity

### 4.4 Check-In
- **Form:** 8 fields (First Name, Last Name, Email, Phone, ID Document, Room Type, Floor, Lift preference)
- **Validation:** Client-side + server-side (Jakarta Validation)
- **Success Feedback:** Shows assigned room number
- **Activity Logging:** Adds to dashboard activity feed

### 4.5 Check-Out
- **Active Guests List:** Shows all checked-in guests with avatar, name, room, email
- **Check-Out Button:** Confirmation dialog → processes check-out
- **Bill Display:** Full bill breakdown with room charges, service orders, total

### 4.6 Housekeeping
- **Task List:** Ordered by priority (Pending first)
- **Status Filter:** All, Pending, In Progress, Completed
- **Actions:** Assign (sets staff name), Complete (marks done)
- **Status Badges:** Color-coded (amber=pending, blue=in-progress, green=completed)

### 4.7 Room Service
- **Orders Grid:** Cards showing order number, status, items, amount
- **Status Workflow Buttons:**
  - RECEIVED → "Start Preparing"
  - PREPARING → "Start Delivery"
  - DELIVERING → "Delivered"
- **New Order Modal:** Guest ID, Room ID, Items, Amount, Notes

### 4.8 Maintenance
- **Issues List:** Ordered by priority
- **Priority Badges:** Red=critical, Amber=high, Blue=normal, Gray=low
- **Actions:** Assign Technician, Resolve, Close
- **New Issue Modal:** Title, Description, Room ID, Priority

## 5. Multi-Service API Routing

The frontend routes requests to the correct microservice:

```javascript
const SERVICES = {
    reception:     'http://localhost:8081/api',
    housekeeping:  'http://localhost:8082/api',
    roomservice:   'http://localhost:8083/api',
    maintenance:   'http://localhost:8084/api',
};
```

| Page | Service Used | API Path |
|------|-------------|----------|
| Dashboard | reception | `/reception/dashboard/stats`, `/reception/rooms` |
| Rooms | reception | `/reception/rooms`, `/reception/rooms/status/{s}` |
| Check-In | reception | `/reception/checkin` |
| Check-Out | reception | `/reception/guests/active`, `/reception/checkout` |
| Housekeeping | housekeeping | `/housekeeping/tasks`, `/housekeeping/tasks/{id}/...` |
| Room Service | roomservice | `/room-service/orders`, `/room-service/orders/{id}/...` |
| Maintenance | maintenance | `/maintenance/requests/active`, `/maintenance/requests/{id}/...` |

## 6. WebSocket Integration

### Connection
```javascript
const socket = new SockJS('http://localhost:8081/ws');
const stompClient = Stomp.over(socket);
```

### Subscriptions

| Topic | Updates |
|-------|---------|
| `/topic/rooms` | Room status changes → refresh room cards |
| `/topic/guests` | Check-in/out events → add to activity feed |
| `/topic/orders` | Order status changes → refresh order cards |
| `/topic/maintenance` | Maintenance updates → refresh issue list |
| `/topic/billing` | Bill generated → add to activity feed |

### Auto-Refresh
When a WebSocket message arrives for the current page, the data is automatically reloaded:
```javascript
stompClient.subscribe('/topic/rooms', function(msg) {
    if (currentPage === 'dashboard') loadDashboard();
    if (currentPage === 'rooms') loadRooms();
});
```

### Reconnection
On WebSocket disconnect, auto-reconnects every 5 seconds:
```javascript
stompClient.onclose = function() {
    updateWSStatus(false);
    setTimeout(connectWebSocket, 5000);
};
```

## 7. UI Design System

### Color Palette (CSS Variables)
```css
--bg-primary: #000000;     /* Main background */
--bg-secondary: #0a0a0a;   /* Sidebar background */
--bg-card: #111111;        /* Card background */
--accent: #6366f1;         /* Primary accent (indigo) */
--green: #22c55e;          /* Success/available */
--red: #ef4444;            /* Error/dirty/critical */
--amber: #f59e0b;          /* Warning/occupied/pending */
--blue: #3b82f6;           /* Info/cleaning/delivering */
--purple: #a855f7;         /* Secondary accent */
```

### Components
- **Stat Cards:** Icon + value + label
- **Status Badges:** Rounded pills with color-coded backgrounds
- **Nav Items:** Left sidebar with icon + label, active state highlighting
- **Modals:** Centered overlay with form content
- **Form Inputs:** Dark background, indigo focus ring
- **Buttons:** Primary (indigo), Secondary (gray), Danger (red)
- **Tables:** Bill breakdown with clean borders

### Responsive Design
- Sidebar collapses on mobile (< 768px)
- Grid layouts adapt from multi-column to single-column
- Stats grid: 3 cols → 2 cols → 1 col
- All page content has proper padding and spacing

## 8. Activity Log

The frontend maintains an in-memory activity log that captures real-time events:
```javascript
let activityLog = [];

function addActivity(type, message) {
    activityLog.unshift({ type, message, time: new Date().toLocaleTimeString() });
    if (activityLog.length > 50) activityLog.pop();
}
```

Activity types: `checkin` (green), `checkout` (red), `order` (blue), `maintenance` (orange)
