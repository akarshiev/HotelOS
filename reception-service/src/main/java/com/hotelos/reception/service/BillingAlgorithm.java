package com.hotelos.reception.service;

import com.hotelos.reception.repository.ReservationRepository;
import com.hotelos.reception.repository.RoomServiceOrderRepository;
import com.hotelos.shared.dto.BillDTO;
import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.entities.Reservation;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.entities.RoomServiceOrder;
import com.hotelos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingAlgorithm {

    private final ReservationRepository reservationRepository;
    private final RoomServiceOrderRepository orderRepository;

    /**
     * Calculates the total bill:
     * Total = (Room Price × Nights) + Room Service Orders + Additional Charges − Discounts
     */
    public BillDTO calculateBill(Guest guest, Room room, LocalDateTime checkOutTime) {
        log.info("Calculating bill for guest: {} in room: {}", guest.getId(), room.getRoomNumber());

        // Calculate nights stayed
        LocalDate checkInDate = guest.getCheckInTime() != null
            ? guest.getCheckInTime().toLocalDate()
            : LocalDate.now();
        LocalDate checkOutDate = checkOutTime.toLocalDate();

        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights < 1) nights = 1; // Minimum 1 night charge

        // Room charges
        BigDecimal pricePerNight = room.getPricePerNight() != null ? room.getPricePerNight() : BigDecimal.ZERO;
        BigDecimal roomCharges = pricePerNight.multiply(BigDecimal.valueOf(nights));

        // Room service orders
        List<RoomServiceOrder> orders = orderRepository.findByGuestId(guest.getId());
        BigDecimal roomServiceTotal = BigDecimal.ZERO;
        List<BillDTO.OrderItem> orderItems = new ArrayList<>();

        for (RoomServiceOrder order : orders) {
            BigDecimal amount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
            roomServiceTotal = roomServiceTotal.add(amount);
            orderItems.add(BillDTO.OrderItem.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .items(order.getItems() != null ? order.getItems() : "Unknown Items")
                .amount(amount)
                .build());
        }

        // Additional charges and discounts (extensible)
        BigDecimal additionalCharges = BigDecimal.ZERO;
        BigDecimal discounts = BigDecimal.ZERO;

        // Total calculation
        BigDecimal totalAmount = roomCharges
            .add(roomServiceTotal)
            .add(additionalCharges)
            .subtract(discounts);

        return BillDTO.builder()
            .guestId(guest.getId())
            .guestName(guest.getFullName())
            .roomId(room.getId())
            .roomNumber(room.getRoomNumber())
            .roomRate(room.getPricePerNight())
            .nightsStayed((int) nights)
            .roomCharges(roomCharges)
            .roomServiceOrders(orderItems)
            .roomServiceTotal(roomServiceTotal)
            .additionalCharges(additionalCharges)
            .discounts(discounts)
            .totalAmount(totalAmount)
            .checkInDate(checkInDate)
            .checkOutDate(checkOutDate)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
