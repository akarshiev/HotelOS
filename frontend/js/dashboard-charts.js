/* ==========================================
   HotelOS - Dashboard Charts (Optional Enhancement)
   ========================================== */

// Simple room status bar chart using pure CSS/HTML
function renderRoomStatusChart(stats) {
    const total = stats.totalRooms || 1;
    const data = [
        { label: 'Available', value: stats.availableRooms || 0, color: '#22c55e' },
        { label: 'Occupied', value: stats.occupiedRooms || 0, color: '#f59e0b' },
        { label: 'Dirty', value: stats.dirtyRooms || 0, color: '#ef4444' },
        { label: 'Cleaning', value: stats.cleaningRooms || 0, color: '#3b82f6' },
        { label: 'Out of Order', value: stats.outOfOrderRooms || 0, color: '#f97316' },
    ];

    const el = document.getElementById('dashboard-rooms-chart');
    if (!el) return;

    el.innerHTML = data.map(d => {
        const pct = ((d.value / total) * 100).toFixed(1);
        return `
            <div class="chart-bar-row">
                <span class="chart-label">${d.label}</span>
                <div class="chart-bar-track">
                    <div class="chart-bar-fill" style="width:${pct}%;background:${d.color}"></div>
                </div>
                <span class="chart-value">${d.value}</span>
            </div>
        `;
    }).join('');
}
