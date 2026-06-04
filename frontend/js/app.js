/* ==========================================
   HotelOS - Frontend Application Logic
   ========================================== */

const SERVICES = {
    reception: 'http://localhost:8081/api',
    housekeeping: 'http://localhost:8082/api',
    roomservice: 'http://localhost:8083/api',
    maintenance: 'http://localhost:8084/api',
};
const WS_URL = 'http://localhost:8081/ws';

// ==========================================
// State
// ==========================================
let currentPage = 'dashboard';
let stompClient = null;
let activityLog = [];

// ==========================================
// API Client
// ==========================================
function getAuthHeaders() {
    const token = localStorage.getItem('hotelos_auth');
    if (token === 'true') {
        const encoded = btoa('admin:admin123');
        return { 'Content-Type': 'application/json', 'Authorization': 'Basic ' + encoded };
    }
    return { 'Content-Type': 'application/json' };
}

const api = {
    async request(service, method, path, body = null) {
        const baseUrl = SERVICES[service] || SERVICES.reception;
        const opts = {
            method,
            headers: getAuthHeaders(),
        };
        if (body) opts.body = JSON.stringify(body);
        try {
            const res = await fetch(`${baseUrl}${path}`, opts);
            if (!res.ok) {
                const err = await res.json().catch(() => ({ message: res.statusText }));
                throw new Error(err.message || `HTTP ${res.status}`);
            }
            const text = await res.text();
            return text ? JSON.parse(text) : null;
        } catch (e) {
            if (e.message === 'Failed to fetch') {
                throw new Error(`Cannot connect to ${service} service. Make sure it is running.`);
            }
            throw e;
        }
    },
    get: (service, path) => api.request(service, 'GET', path),
    post: (service, path, body) => api.request(service, 'POST', path, body),
    put: (service, path, body) => api.request(service, 'PUT', path, body),
};

// ==========================================
// Auth
// ==========================================
async function login(username, password) {
    return api.post('reception', '/auth/login', { username, password });
}

function logout() {
    localStorage.removeItem('hotelos_auth');
    document.getElementById('app').style.display = 'none';
    document.getElementById('login-page').classList.add('active');
    document.getElementById('login-page').style.display = 'block';
    disconnectWebSocket();
}

function isAuthenticated() {
    return localStorage.getItem('hotelos_auth') === 'true';
}

// ==========================================
// Navigation
// ==========================================
function navigateTo(page) {
    currentPage = page;
    document.querySelectorAll('.page-content').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    const pageEl = document.getElementById(`page-${page}`);
    if (pageEl) pageEl.classList.add('active');

    const navEl = document.querySelector(`[data-page="${page}"]`);
    if (navEl) navEl.classList.add('active');

    loadPageData(page);
}

async function loadPageData(page) {
    switch (page) {
        case 'dashboard': await loadDashboard(); break;
        case 'rooms': await loadRooms(); break;
        case 'checkin': break;
        case 'checkout': await loadActiveGuests(); break;
        case 'housekeeping': await loadHousekeepingTasks(); break;
        case 'roomservice': await loadRoomServiceOrders(); break;
        case 'maintenance': await loadMaintenanceIssues(); break;
    }
}

// ==========================================
// Dashboard
// ==========================================
async function loadDashboard() {
    try {
        const stats = await api.get('reception', '/reception/dashboard/stats');
        document.getElementById('stat-total-rooms').textContent = stats.totalRooms || 0;
        document.getElementById('stat-available').textContent = stats.availableRooms || 0;
        document.getElementById('stat-occupied').textContent = stats.occupiedRooms || 0;
        document.getElementById('stat-dirty').textContent = stats.dirtyRooms || 0;
        document.getElementById('stat-active-guests').textContent = stats.activeGuests || 0;

        // Load rooms for dashboard view
        const rooms = await api.get('reception', '/reception/rooms');
        const roomsHtml = rooms.slice(0, 10).map(room => `
            <div class="activity-item">
                <div class="activity-dot ${getStatusColor(room.status)}"></div>
                <div class="activity-text">
                    <strong>Room ${room.roomNumber}</strong> — ${room.roomType} · Floor ${room.floor}
                    <div class="activity-time">${formatStatus(room.status)} · $${room.pricePerNight}/night</div>
                </div>
            </div>
        `).join('');
        document.getElementById('dashboard-rooms-list').innerHTML = roomsHtml || '<p class="empty-state">No rooms found</p>';

        // Activity log
        renderActivityLog();
    } catch (e) {
        console.error('Dashboard load error:', e);
    }
}

function renderActivityLog() {
    const el = document.getElementById('dashboard-activity');
    if (activityLog.length === 0) {
        el.innerHTML = '<p class="empty-state">No recent activity. Events will appear here in real-time.</p>';
        return;
    }
    el.innerHTML = activityLog.slice(0, 15).map(item => `
        <div class="activity-item">
            <div class="activity-dot ${item.type}"></div>
            <div class="activity-text">
                ${item.message}
                <div class="activity-time">${item.time}</div>
            </div>
        </div>
    `).join('');
}

function addActivity(type, message) {
    const now = new Date();
    const time = now.toLocaleTimeString();
    activityLog.unshift({ type, message, time });
    if (activityLog.length > 50) activityLog.pop();
    if (currentPage === 'dashboard') renderActivityLog();
}

// ==========================================
// Rooms
// ==========================================
async function loadRooms(filter = '') {
    try {
        let rooms;
        if (filter) {
            rooms = await api.get('reception', `/reception/rooms/status/${filter}`);
        } else {
            rooms = await api.get('reception', '/reception/rooms');
        }
        const grid = document.getElementById('rooms-grid');
        if (!rooms || rooms.length === 0) {
            grid.innerHTML = '<p class="empty-state">No rooms found</p>';
            return;
        }
        grid.innerHTML = rooms.map(room => `
            <div class="room-card">
                <div class="room-card-header">
                    <span class="room-number">${room.roomNumber}</span>
                    <span class="room-badge ${room.status.toLowerCase().replace('_', '-')}">${formatStatus(room.status)}</span>
                </div>
                <div class="room-details">
                    <div class="room-detail">
                        <span class="room-detail-label">Type</span>
                        <span>${room.roomType}</span>
                    </div>
                    <div class="room-detail">
                        <span class="room-detail-label">Floor</span>
                        <span>${room.floor}</span>
                    </div>
                    <div class="room-detail">
                        <span class="room-detail-label">Price</span>
                        <span>$${room.pricePerNight}/night</span>
                    </div>
                    <div class="room-detail">
                        <span class="room-detail-label">Near Lift</span>
                        <span>${room.nearLift ? 'Yes' : 'No'}</span>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Rooms load error:', e);
    }
}

// ==========================================
// Check-In
// ==========================================
async function handleCheckIn(e) {
    e.preventDefault();
    const errEl = document.getElementById('checkin-error');
    const successEl = document.getElementById('checkin-success');
    errEl.style.display = 'none';
    successEl.style.display = 'none';

    const data = {
        firstName: document.getElementById('ci-firstname').value.trim(),
        lastName: document.getElementById('ci-lastname').value.trim(),
        email: document.getElementById('ci-email').value.trim(),
        phoneNumber: document.getElementById('ci-phone').value.trim() || null,
        idDocument: document.getElementById('ci-id').value.trim() || null,
        roomType: document.getElementById('ci-roomtype').value,
        preferredFloor: document.getElementById('ci-floor').value ? parseInt(document.getElementById('ci-floor').value) : null,
        preferLift: document.getElementById('ci-lift').value ? document.getElementById('ci-lift').value === 'true' : null,
    };

    if (!data.firstName || !data.lastName || !data.email || !data.roomType) {
        errEl.textContent = 'Please fill in all required fields.';
        errEl.style.display = 'block';
        return;
    }

    try {
        const guest = await api.post('reception', '/reception/checkin', data);
        successEl.textContent = `✓ Check-in successful! Guest: ${guest.firstName} ${guest.lastName} → Room ${guest.room?.roomNumber || 'Assigned'}`;
        successEl.style.display = 'block';
        document.getElementById('checkin-form').reset();
        addActivity('checkin', `<strong>${guest.firstName} ${guest.lastName}</strong> checked in to Room ${guest.room?.roomNumber || 'N/A'}`);
    } catch (e) {
        errEl.textContent = e.message;
        errEl.style.display = 'block';
    }
}

// ==========================================
// Check-Out
// ==========================================
async function loadActiveGuests() {
    try {
        const guests = await api.get('reception', '/reception/guests/active');
        const el = document.getElementById('checkout-guests');
        if (!guests || guests.length === 0) {
            el.innerHTML = '<p class="empty-state">No active guests currently checked in.</p>';
            return;
        }
        el.innerHTML = guests.map(guest => `
            <div class="guest-row">
                <div class="guest-info">
                    <div class="guest-avatar">${guest.firstName?.[0] || '?'}${guest.lastName?.[0] || ''}</div>
                    <div>
                        <div class="guest-name">${guest.firstName} ${guest.lastName}</div>
                        <div class="guest-room">Room ${guest.room?.roomNumber || 'N/A'} · ${guest.email}</div>
                    </div>
                </div>
                <button class="btn btn-danger btn-sm" onclick="handleCheckOut(${guest.id})">Check Out</button>
            </div>
        `).join('');
    } catch (e) {
        console.error('Active guests load error:', e);
    }
}

async function handleCheckOut(guestId) {
    if (!confirm('Are you sure you want to check out this guest?')) return;
    try {
        const bill = await api.post('reception', '/reception/checkout', { guestId });
        const billEl = document.getElementById('bill-display');
        const billContent = document.getElementById('bill-content');
        billEl.style.display = 'block';

        let ordersHtml = '';
        if (bill.roomServiceOrders && bill.roomServiceOrders.length > 0) {
            ordersHtml = bill.roomServiceOrders.map(o => `
                <tr><td>${o.orderNumber}</td><td>${o.items}</td><td>$${o.amount.toFixed(2)}</td></tr>
            `).join('');
        } else {
            ordersHtml = '<tr><td colspan="3" style="color: var(--text-muted);">No room service orders</td></tr>';
        }

        billContent.innerHTML = `
            <p style="margin-bottom:16px;color:var(--text-secondary)">Guest: <strong>${bill.guestName}</strong> · Room: <strong>${bill.roomNumber}</strong></p>
            <table class="bill-table">
                <thead><tr><th>Description</th><th>Details</th><th>Amount</th></tr></thead>
                <tbody>
                    <tr><td>Room Charges</td><td>${bill.nightsStayed} nights × $${bill.roomRate.toFixed(2)}</td><td>$${bill.roomCharges.toFixed(2)}</td></tr>
                    ${ordersHtml}
                    ${bill.additionalCharges > 0 ? `<tr><td>Additional Charges</td><td></td><td>$${bill.additionalCharges.toFixed(2)}</td></tr>` : ''}
                    ${bill.discounts > 0 ? `<tr><td>Discounts</td><td></td><td>-$${bill.discounts.toFixed(2)}</td></tr>` : ''}
                </tbody>
            </table>
            <div class="bill-total"><span>Total</span><span>$${bill.totalAmount.toFixed(2)}</span></div>
        `;

        addActivity('checkout', `<strong>${bill.guestName}</strong> checked out from Room ${bill.roomNumber}. Total: $${bill.totalAmount.toFixed(2)}`);
        await loadActiveGuests();
    } catch (e) {
        alert('Check-out failed: ' + e.message);
    }
}

// ==========================================
// Housekeeping
// ==========================================
async function loadHousekeepingTasks(filter = '') {
    try {
        let tasks;
        if (filter) {
            tasks = await api.get('housekeeping', `/housekeeping/tasks/status/${filter}`);
        } else {
            tasks = await api.get('housekeeping', '/housekeeping/tasks');
        }
        const el = document.getElementById('housekeeping-tasks');
        if (!tasks || tasks.length === 0) {
            el.innerHTML = '<p class="empty-state">No housekeeping tasks found.</p>';
            return;
        }
        el.innerHTML = tasks.map(task => `
            <div class="task-item">
                <div class="task-info">
                    <h4>Task #${task.id} — Room ${task.roomNumber || task.room?.roomNumber || 'N/A'}</h4>
                    <p>${task.notes || 'No notes'} ${task.assignedStaff ? '· Assigned: ' + task.assignedStaff : ''}</p>
                </div>
                <div class="task-actions">
                    <span class="status-badge ${task.status.toLowerCase().replace('_', '-')}">${formatStatus(task.status)}</span>
                    ${task.status === 'PENDING' ? `<button class="btn btn-secondary btn-sm" onclick="assignTask(${task.id})">Assign</button>` : ''}
                    ${task.status === 'IN_PROGRESS' ? `<button class="btn btn-primary btn-sm" onclick="completeTask(${task.id})">Complete</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Housekeeping load error:', e);
    }
}

async function assignTask(taskId) {
    const staff = prompt('Enter staff name:');
    if (!staff) return;
    try {
        await api.put('housekeeping', `/housekeeping/tasks/${taskId}/assign`, { staffName: staff });
        await loadHousekeepingTasks();
    } catch (e) {
        alert('Failed to assign task: ' + e.message);
    }
}

async function completeTask(taskId) {
    try {
        await api.put('housekeeping', `/housekeeping/tasks/${taskId}/status`, { status: 'COMPLETED' });
        addActivity('order', `Housekeeping Task #${taskId} completed`);
        await loadHousekeepingTasks();
    } catch (e) {
        alert('Failed to complete task: ' + e.message);
    }
}

// ==========================================
// Room Service Orders
// ==========================================
async function loadRoomServiceOrders() {
    try {
        const orders = await api.get('roomservice', '/room-service/orders');
        const el = document.getElementById('roomservice-orders');
        if (!orders || orders.length === 0) {
            el.innerHTML = '<p class="empty-state">No orders yet. Place the first order!</p>';
            return;
        }
        el.innerHTML = orders.map(order => `
            <div class="order-card">
                <div class="order-header">
                    <span class="order-id">${order.orderNumber}</span>
                    <span class="status-badge ${order.status.toLowerCase()}">${formatStatus(order.status)}</span>
                </div>
                <div class="order-items">${order.items}${order.notes ? '<br><em style="color:var(--text-muted)">' + order.notes + '</em>' : ''}</div>
                <div class="order-footer">
                    <span class="order-amount">$${order.totalAmount.toFixed(2)}</span>
                    <div class="task-actions">
                        ${order.status === 'RECEIVED' ? `<button class="btn btn-secondary btn-sm" onclick="advanceOrder(${order.id}, 'PREPARING')">Start Preparing</button>` : ''}
                        ${order.status === 'PREPARING' ? `<button class="btn btn-secondary btn-sm" onclick="advanceOrder(${order.id}, 'DELIVERING')">Start Delivery</button>` : ''}
                        ${order.status === 'DELIVERING' ? `<button class="btn btn-primary btn-sm" onclick="advanceOrder(${order.id}, 'DELIVERED')">Delivered</button>` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    } catch (e) {
        console.error('Orders load error:', e);
    }
}

async function advanceOrder(orderId, newStatus) {
    try {
        await api.put('roomservice', `/room-service/orders/${orderId}/status`, { status: newStatus });
        addActivity('order', `Order #${orderId} → ${formatStatus(newStatus)}`);
        await loadRoomServiceOrders();
    } catch (e) {
        alert('Failed to update order: ' + e.message);
    }
}

async function handleCreateOrder(e) {
    e.preventDefault();
    const errEl = document.getElementById('order-error');
    const successEl = document.getElementById('order-success');
    errEl.style.display = 'none';
    successEl.style.display = 'none';

    const data = {
        guestId: parseInt(document.getElementById('ord-guestid').value),
        roomId: parseInt(document.getElementById('ord-roomid').value),
        items: document.getElementById('ord-items').value.trim(),
        totalAmount: parseFloat(document.getElementById('ord-amount').value),
        notes: document.getElementById('ord-notes').value.trim() || null,
    };

    try {
        const order = await api.post('roomservice', '/room-service/orders', data);
        successEl.textContent = `✓ Order ${order.orderNumber} placed successfully!`;
        successEl.style.display = 'block';
        document.getElementById('order-form').reset();
        addActivity('order', `New order ${order.orderNumber} — $${order.totalAmount.toFixed(2)}`);
        setTimeout(() => { document.getElementById('order-modal').style.display = 'none'; successEl.style.display = 'none'; }, 1500);
        await loadRoomServiceOrders();
    } catch (e) {
        errEl.textContent = e.message;
        errEl.style.display = 'block';
    }
}

// ==========================================
// Maintenance
// ==========================================
async function loadMaintenanceIssues() {
    try {
        const issues = await api.get('maintenance', '/maintenance/requests/active');
        const el = document.getElementById('maintenance-issues');
        if (!issues || issues.length === 0) {
            el.innerHTML = '<p class="empty-state">No maintenance issues reported.</p>';
            return;
        }
        el.innerHTML = issues.map(issue => `
            <div class="issue-item">
                <div class="issue-info">
                    <h4>${issue.title}</h4>
                    <p>${issue.description || 'No description'} · Room ${issue.roomNumber || issue.room?.roomNumber || 'N/A'} ${issue.assignedTechnician ? '· Tech: ' + issue.assignedTechnician : ''}</p>
                </div>
                <div class="issue-actions">
                    <span class="priority-badge ${issue.priority.toLowerCase()}">${issue.priority}</span>
                    <span class="status-badge ${issue.status.toLowerCase().replace('_', '-')}">${formatStatus(issue.status)}</span>
                    ${issue.status === 'REPORTED' ? `<button class="btn btn-secondary btn-sm" onclick="assignTechnician(${issue.id})">Assign</button>` : ''}
                    ${issue.status === 'IN_PROGRESS' ? `<button class="btn btn-primary btn-sm" onclick="resolveIssue(${issue.id})">Resolve</button>` : ''}
                    ${issue.status === 'RESOLVED' ? `<button class="btn btn-secondary btn-sm" onclick="closeIssue(${issue.id})">Close</button>` : ''}
                </div>
            </div>
        `).join('');

        // Update dashboard maintenance count
        document.getElementById('stat-maintenance').textContent = issues.length;
    } catch (e) {
        console.error('Maintenance load error:', e);
    }
}

async function assignTechnician(issueId) {
    const tech = prompt('Enter technician name:');
    if (!tech) return;
    try {
        await api.put('maintenance', `/maintenance/requests/${issueId}/assign`, { technician: tech });
        addActivity('maintenance', `Technician ${tech} assigned to Issue #${issueId}`);
        await loadMaintenanceIssues();
    } catch (e) {
        alert('Failed to assign: ' + e.message);
    }
}

async function resolveIssue(issueId) {
    try {
        await api.put('maintenance', `/maintenance/requests/${issueId}/status`, { status: 'RESOLVED' });
        addActivity('maintenance', `Issue #${issueId} resolved`);
        await loadMaintenanceIssues();
    } catch (e) {
        alert('Failed to resolve: ' + e.message);
    }
}

async function closeIssue(issueId) {
    try {
        await api.put('maintenance', `/maintenance/requests/${issueId}/status`, { status: 'CLOSED' });
        await loadMaintenanceIssues();
    } catch (e) {
        alert('Failed to close: ' + e.message);
    }
}

async function handleCreateIssue(e) {
    e.preventDefault();
    const errEl = document.getElementById('issue-error');
    const successEl = document.getElementById('issue-success');
    errEl.style.display = 'none';
    successEl.style.display = 'none';

    const data = {
        title: document.getElementById('iss-title').value.trim(),
        description: document.getElementById('iss-desc').value.trim() || null,
        roomId: parseInt(document.getElementById('iss-roomid').value),
        priority: document.getElementById('iss-priority').value,
    };

    try {
        await api.post('maintenance', '/maintenance/requests', data);
        successEl.textContent = '✓ Issue reported successfully!';
        successEl.style.display = 'block';
        document.getElementById('issue-form').reset();
        addActivity('maintenance', `New issue: ${data.title} (${data.priority})`);
        setTimeout(() => { document.getElementById('issue-modal').style.display = 'none'; successEl.style.display = 'none'; }, 1500);
        await loadMaintenanceIssues();
    } catch (e) {
        errEl.textContent = e.message;
        errEl.style.display = 'block';
    }
}

// ==========================================
// WebSocket
// ==========================================
function connectWebSocket() {
    try {
        const socket = new SockJS(WS_URL);
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, function () {
            updateWSStatus(true);

            stompClient.subscribe('/topic/rooms', function (msg) {
                const event = JSON.parse(msg.body);
                addActivity('order', `Room ${event.roomNumber} status: ${formatStatus(event.newStatus)}`);
                if (currentPage === 'dashboard') loadDashboard();
                if (currentPage === 'rooms') loadRooms();
            });

            stompClient.subscribe('/topic/guests', function (msg) {
                const event = JSON.parse(msg.body);
                if (event.checkedInAt) {
                    addActivity('checkin', `<strong>${event.guestName}</strong> checked in → Room ${event.roomNumber}`);
                } else if (event.checkedOutAt) {
                    addActivity('checkout', `<strong>${event.guestName}</strong> checked out from Room ${event.roomNumber}`);
                }
            });

            stompClient.subscribe('/topic/orders', function (msg) {
                const event = JSON.parse(msg.body);
                addActivity('order', `Order ${event.orderNumber}: ${formatStatus(event.newStatus)}`);
                if (currentPage === 'roomservice') loadRoomServiceOrders();
            });

            stompClient.subscribe('/topic/maintenance', function (msg) {
                const event = JSON.parse(msg.body);
                addActivity('maintenance', `${event.title} — ${formatStatus(event.newStatus)}`);
                if (currentPage === 'maintenance') loadMaintenanceIssues();
            });

            stompClient.subscribe('/topic/billing', function (msg) {
                const bill = JSON.parse(msg.body);
                addActivity('checkout', `Bill generated: $${bill.totalAmount?.toFixed(2) || 0}`);
            });
        });

        stompClient.onclose = function () {
            updateWSStatus(false);
            setTimeout(connectWebSocket, 5000);
        };
    } catch (e) {
        console.error('WebSocket connection failed:', e);
        updateWSStatus(false);
    }
}

function updateWSStatus(connected) {
    const el = document.getElementById('ws-status');
    if (!el) return;
    if (connected) {
        el.innerHTML = '<span class="status-dot"></span><span>Live</span>';
        el.style.background = 'var(--green-bg)';
        el.style.borderColor = 'rgba(34, 197, 94, 0.2)';
        el.style.color = 'var(--green)';
    } else {
        el.innerHTML = '<span class="status-dot" style="background:var(--red);animation:none;"></span><span>Offline</span>';
        el.style.background = 'var(--red-bg)';
        el.style.borderColor = 'rgba(239, 68, 68, 0.2)';
        el.style.color = 'var(--red)';
    }
}

// ==========================================
// Helpers
// ==========================================
function formatStatus(status) {
    if (!status) return '';
    return status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

function getStatusColor(status) {
    const map = {
        CLEAN: 'checkin', AVAILABLE: 'checkin',
        OCCUPIED: 'checkout',
        DIRTY: 'maintenance',
        CLEANING: 'order',
    };
    return map[status] || 'checkin';
}

// ==========================================
// Init
// ==========================================
document.addEventListener('DOMContentLoaded', function () {
    // Login form
    document.getElementById('login-form').addEventListener('submit', async function (e) {
        e.preventDefault();
        const errEl = document.getElementById('login-error');
        errEl.style.display = 'none';
        try {
            const res = await login(
                document.getElementById('username').value,
                document.getElementById('password').value
            );
            if (res.success) {
                document.getElementById('login-page').style.display = 'none';
                document.getElementById('app').style.display = 'flex';
                connectWebSocket();
                navigateTo('dashboard');
            } else {
                errEl.textContent = res.message || 'Invalid credentials';
                errEl.style.display = 'block';
            }
        } catch (e) {
            errEl.textContent = e.message;
            errEl.style.display = 'block';
        }
    });

    // Navigation
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            navigateTo(this.dataset.page);
        });
    });

    // Logout
    document.getElementById('logout-btn').addEventListener('click', function () {
        if (stompClient) stompClient.disconnect();
        document.getElementById('app').style.display = 'none';
        document.getElementById('login-page').style.display = 'flex';
    });

    // Room filter
    document.getElementById('room-filter').addEventListener('change', function () {
        loadRooms(this.value);
    });

    // Housekeeping filter
    document.getElementById('hk-filter').addEventListener('change', function () {
        loadHousekeepingTasks(this.value);
    });

    // Check-In form
    document.getElementById('checkin-form').addEventListener('submit', handleCheckIn);

    // Order modal
    document.getElementById('new-order-btn').addEventListener('click', function () {
        document.getElementById('order-modal').style.display = 'flex';
    });
    document.getElementById('close-modal').addEventListener('click', function () {
        document.getElementById('order-modal').style.display = 'none';
    });
    document.getElementById('order-form').addEventListener('submit', handleCreateOrder);

    // Issue modal
    document.getElementById('new-issue-btn').addEventListener('click', function () {
        document.getElementById('issue-modal').style.display = 'flex';
    });
    document.getElementById('close-issue-modal').addEventListener('click', function () {
        document.getElementById('issue-modal').style.display = 'none';
    });
    document.getElementById('issue-form').addEventListener('submit', handleCreateIssue);

    // Close modals on background click
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function (e) {
            if (e.target === this) this.style.display = 'none';
        });
    });
});
