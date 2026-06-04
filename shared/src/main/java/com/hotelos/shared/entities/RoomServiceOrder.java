package com.hotelos.shared.entities;

import com.hotelos.shared.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "room_service_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomServiceOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private Long guestId;

    private Long roomId;

    private String roomNumber;

    @Column(nullable = false)
    private String items;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    private String notes;

    private LocalDateTime orderedAt;

    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = OrderStatus.RECEIVED;
        if (orderedAt == null) orderedAt = LocalDateTime.now();
    }
}
