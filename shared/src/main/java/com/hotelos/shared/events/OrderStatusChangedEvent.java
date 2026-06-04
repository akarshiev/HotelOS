package com.hotelos.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChangedEvent {
    private Long orderId;
    private String orderNumber;
    private String oldStatus;
    private String newStatus;
    private String guestName;
    private String roomNumber;
    private LocalDateTime changedAt;
}
