package com.hotelos.reception.service;

import com.hotelos.reception.repository.ReservationRepository;
import com.hotelos.reception.repository.RoomServiceOrderRepository;
import com.hotelos.shared.dto.BillDTO;
import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.entities.RoomServiceOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BillingAlgorithmTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomServiceOrderRepository orderRepository;

    @InjectMocks
    private BillingAlgorithm billingAlgorithm;

    private Guest guest;
    private Room room;

    @BeforeEach
    void setUp() {
        guest = new Guest();
        guest.setId(1L);
        guest.setFirstName("John");
        guest.setLastName("Doe");
        guest.setCheckInTime(LocalDateTime.now().minusDays(2));

        room = new Room();
        room.setId(10L);
        room.setRoomNumber("101");
        room.setPricePerNight(new BigDecimal("100.00"));
    }

    @Test
    void calculateBill_ShouldCalculateCorrectly_WithRoomService() {
        RoomServiceOrder order = new RoomServiceOrder();
        order.setId(100L);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setItems("Pizza");

        when(orderRepository.findByGuestId(1L)).thenReturn(Collections.singletonList(order));

        BillDTO bill = billingAlgorithm.calculateBill(guest, room, LocalDateTime.now());

        // 2 nights * 100 = 200 + 50 = 250
        assertEquals(new BigDecimal("250.00"), bill.getTotalAmount());
        assertEquals(2, bill.getNightsStayed());
        assertEquals(new BigDecimal("200.00"), bill.getRoomCharges());
        assertEquals(new BigDecimal("50.00"), bill.getRoomServiceTotal());
    }

    @Test
    void calculateBill_ShouldChargeMinimumOneNight_WhenSameDayCheckout() {
        guest.setCheckInTime(LocalDateTime.now());
        when(orderRepository.findByGuestId(1L)).thenReturn(Collections.emptyList());

        BillDTO bill = billingAlgorithm.calculateBill(guest, room, LocalDateTime.now().plusHours(2));

        assertEquals(new BigDecimal("100.00"), bill.getTotalAmount());
        assertEquals(1, bill.getNightsStayed());
    }
}
