package com.hotelos.shared.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDTO {
    private Long guestId;
    private String guestName;
    private Long roomId;
    private String roomNumber;
    private BigDecimal roomRate;
    private Integer nightsStayed;
    private BigDecimal roomCharges;
    private List<OrderItem> roomServiceOrders;
    private BigDecimal roomServiceTotal;
    private BigDecimal additionalCharges;
    private BigDecimal discounts;
    private BigDecimal totalAmount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime generatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        private Long orderId;
        private String orderNumber;
        private String items;
        private BigDecimal amount;
    }
}
