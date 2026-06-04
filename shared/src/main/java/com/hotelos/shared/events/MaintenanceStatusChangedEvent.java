package com.hotelos.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceStatusChangedEvent {
    private Long requestId;
    private String title;
    private String priority;
    private String oldStatus;
    private String newStatus;
    private String roomNumber;
    private String assignedTechnician;
    private LocalDateTime changedAt;
}
