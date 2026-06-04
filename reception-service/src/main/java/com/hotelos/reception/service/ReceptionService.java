package com.hotelos.reception.service;

import com.hotelos.reception.config.RabbitMQConfig;
import com.hotelos.reception.repository.GuestRepository;
import com.hotelos.reception.repository.ReservationRepository;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.reception.repository.RoomServiceOrderRepository;
import com.hotelos.shared.dto.BillDTO;
import com.hotelos.shared.dto.CheckInRequest;
import com.hotelos.shared.dto.CheckOutRequest;
import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.entities.Reservation;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.GuestStatus;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.enums.RoomType;
import com.hotelos.shared.events.*;
import com.hotelos.shared.exception.BadRequestException;
import com.hotelos.shared.exception.ConflictException;
import com.hotelos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceptionService {

    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final RoomAssignmentAlgorithm roomAssignmentAlgorithm;
    private final BillingAlgorithm billingAlgorithm;
    private final RoomServiceOrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Guest checkIn(CheckInRequest request) {
        log.info("Processing check-in for: {} {}", request.getFirstName(), request.getLastName());

        // Validate guest doesn't already have active stay
        if (guestRepository.existsByEmail(request.getEmail())) {
            Guest existingGuest = guestRepository.findByEmail(request.getEmail()).orElseThrow();
            if (existingGuest.getStatus() == GuestStatus.CHECKED_IN) {
                throw new ConflictException("Guest is already checked in: " + request.getEmail());
            }
        }

        // Assign room using the algorithm
        RoomType roomType = RoomType.valueOf(request.getRoomType());
        Room assignedRoom = roomAssignmentAlgorithm.assignRoom(
            roomType,
            request.getPreferredFloor(),
            request.getPreferLift()
        );

        // Create and save guest
        Guest guest = Guest.builder()
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .phoneNumber(request.getPhoneNumber())
            .idDocument(request.getIdDocument())
            .status(GuestStatus.CHECKED_IN)
            .preferredFloor(request.getPreferredFloor())
            .preferLift(request.getPreferLift())
            .checkInTime(LocalDateTime.now())
            .room(assignedRoom)
            .build();

        guest = guestRepository.save(guest);

        // Create reservation
        Reservation reservation = Reservation.builder()
            .guest(guest)
            .room(assignedRoom)
            .roomType(roomType)
            .checkInDate(LocalDateTime.now().toLocalDate())
            .checkOutDate(LocalDateTime.now().toLocalDate().plusDays(1))
            .totalAmount(assignedRoom.getPricePerNight())
            .active(true)
            .build();
        reservationRepository.save(reservation);

        // Publish events
        GuestCheckedInEvent checkInEvent = GuestCheckedInEvent.builder()
            .guestId(guest.getId())
            .guestName(guest.getFullName())
            .roomId(assignedRoom.getId())
            .roomNumber(assignedRoom.getRoomNumber())
            .checkedInAt(LocalDateTime.now())
            .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.GUEST_CHECKED_IN_KEY, checkInEvent);

        // WebSocket notification
        messagingTemplate.convertAndSend("/topic/guests", checkInEvent);
        messagingTemplate.convertAndSend("/topic/rooms", RoomStatusChangedEvent.builder()
            .roomId(assignedRoom.getId())
            .roomNumber(assignedRoom.getRoomNumber())
            .oldStatus(RoomStatus.CLEAN.name())
            .newStatus(RoomStatus.OCCUPIED.name())
            .changedAt(LocalDateTime.now())
            .build());

        log.info("Check-in completed: Guest {} -> Room {}", guest.getId(), assignedRoom.getRoomNumber());
        return guest;
    }

    @Transactional
    public BillDTO checkOut(CheckOutRequest request) {
        log.info("Processing check-out for guest: {}", request.getGuestId());

        Guest guest = guestRepository.findById(request.getGuestId())
            .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + request.getGuestId()));

        if (guest.getStatus() != GuestStatus.CHECKED_IN) {
            throw new BadRequestException("Guest is not currently checked in");
        }

        // Check for pending room service orders
        List<com.hotelos.shared.entities.RoomServiceOrder> pendingOrders = orderRepository.findByGuestIdAndDeliveredAtIsNull(guest.getId());
        if (!pendingOrders.isEmpty()) {
            throw new BadRequestException("Cannot check-out guest. There are pending room service orders.");
        }

        Room room = guest.getRoom();
        if (room == null) {
            throw new BadRequestException("No room assigned to this guest");
        }

        LocalDateTime checkOutTime = LocalDateTime.now();

        // Calculate bill
        BillDTO bill = billingAlgorithm.calculateBill(guest, room, checkOutTime);

        // Update guest status
        guest.setStatus(GuestStatus.CHECKED_OUT);
        guest.setCheckOutTime(checkOutTime);
        guestRepository.save(guest);

        // Update room status to DIRTY
        RoomStatus oldStatus = room.getStatus();
        room.setStatus(RoomStatus.DIRTY);
        room.setActive(true);
        roomRepository.save(room);

        // Mark reservation as inactive
        reservationRepository.findByGuestId(guest.getId()).stream()
            .filter(Reservation::getActive)
            .forEach(res -> {
                res.setActive(false);
                res.setTotalAmount(bill.getTotalAmount());
                reservationRepository.save(res);
            });

        // Publish RoomReleased event
        RoomReleasedEvent roomReleasedEvent = RoomReleasedEvent.builder()
            .roomId(room.getId())
            .roomNumber(room.getRoomNumber())
            .floor(room.getFloor())
            .roomType(room.getRoomType().name())
            .releasedAt(checkOutTime)
            .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROOM_RELEASED_KEY, roomReleasedEvent);

        // Publish GuestCheckedOut event
        GuestCheckedOutEvent checkOutEvent = GuestCheckedOutEvent.builder()
            .guestId(guest.getId())
            .guestName(guest.getFullName())
            .roomId(room.getId())
            .roomNumber(room.getRoomNumber())
            .totalAmount(bill.getTotalAmount())
            .checkedOutAt(checkOutTime)
            .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.GUEST_CHECKED_OUT_KEY, checkOutEvent);

        // WebSocket notifications
        messagingTemplate.convertAndSend("/topic/guests", checkOutEvent);
        messagingTemplate.convertAndSend("/topic/rooms", RoomStatusChangedEvent.builder()
            .roomId(room.getId())
            .roomNumber(room.getRoomNumber())
            .oldStatus(oldStatus.name())
            .newStatus(RoomStatus.DIRTY.name())
            .changedAt(checkOutTime)
            .build());
        messagingTemplate.convertAndSend("/topic/billing", bill);

        log.info("Check-out completed: Guest {} from Room {}. Total: {}", guest.getId(), room.getRoomNumber(), bill.getTotalAmount());
        return bill;
    }

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public List<Guest> getActiveGuests() {
        return guestRepository.findByStatus(GuestStatus.CHECKED_IN);
    }

    public Guest getGuestById(Long id) {
        return guestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Guest not found: " + id));
    }

    public List<Room> getAllRooms() {
        return roomRepository.findByActiveTrue();
    }

    public Room getRoomById(Long id) {
        return roomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
    }

    public Room getRoomByNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomNumber));
    }

    public List<Room> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status);
    }
}
