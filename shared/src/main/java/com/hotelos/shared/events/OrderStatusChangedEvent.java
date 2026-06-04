package com.hotelos.shared.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusChangedEvent {
    private Long orderId;
    private String orderNumber;
    private Long guestId;
    private Long roomId;
    private String items;
    private BigDecimal totalAmount;
    private String oldStatus;
    private String newStatus;
    private String guestName;
    private String roomNumber;
    private LocalDateTime changedAt;
}
