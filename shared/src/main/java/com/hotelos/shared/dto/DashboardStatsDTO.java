package com.hotelos.shared.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {
    private Long totalRooms;
    private Long availableRooms;
    private Long occupiedRooms;
    private Long dirtyRooms;
    private Long cleaningRooms;
    private Long outOfOrderRooms;
    private Long totalGuests;
    private Long activeGuests;
    private Long pendingOrders;
    private Long activeMaintenanceIssues;
    private Long pendingHousekeepingTasks;
    private BigDecimal totalRevenue;
    private LocalDateTime lastUpdated;
}
