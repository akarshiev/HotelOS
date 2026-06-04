package com.hotelos.reception.controller;

import com.hotelos.reception.service.ReceptionService;
import com.hotelos.shared.dto.*;
import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reception")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReceptionController {

    private final ReceptionService receptionService;

    @PostMapping("/checkin")
    public ResponseEntity<Guest> checkIn(@Valid @RequestBody CheckInRequest request) {
        Guest guest = receptionService.checkIn(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(guest);
    }

    @PostMapping("/checkout")
    public ResponseEntity<BillDTO> checkOut(@Valid @RequestBody CheckOutRequest request) {
        BillDTO bill = receptionService.checkOut(request);
        return ResponseEntity.ok(bill);
    }

    @GetMapping("/guests")
    public ResponseEntity<List<Guest>> getAllGuests() {
        return ResponseEntity.ok(receptionService.getAllGuests());
    }

    @GetMapping("/guests/active")
    public ResponseEntity<List<Guest>> getActiveGuests() {
        return ResponseEntity.ok(receptionService.getActiveGuests());
    }

    @GetMapping("/guests/{id}")
    public ResponseEntity<Guest> getGuest(@PathVariable Long id) {
        return ResponseEntity.ok(receptionService.getGuestById(id));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(receptionService.getAllRooms());
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<Room> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(receptionService.getRoomById(id));
    }

    @GetMapping("/rooms/number/{roomNumber}")
    public ResponseEntity<Room> getRoomByNumber(@PathVariable String roomNumber) {
        return ResponseEntity.ok(receptionService.getRoomByNumber(roomNumber));
    }

    @GetMapping("/rooms/status/{status}")
    public ResponseEntity<List<Room>> getRoomsByStatus(@PathVariable RoomStatus status) {
        return ResponseEntity.ok(receptionService.getRoomsByStatus(status));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        List<Room> allRooms = receptionService.getAllRooms();
        List<Guest> activeGuests = receptionService.getActiveGuests();

        long totalRooms = allRooms.size();
        long availableRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEAN || r.getStatus() == RoomStatus.AVAILABLE).count();
        long occupiedRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED).count();
        long dirtyRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.DIRTY).count();
        long cleaningRooms = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.CLEANING).count();
        long outOfOrder = allRooms.stream().filter(r -> r.getStatus() == RoomStatus.OUT_OF_ORDER).count();

        Map<String, Object> stats = Map.of(
            "totalRooms", totalRooms,
            "availableRooms", availableRooms,
            "occupiedRooms", occupiedRooms,
            "dirtyRooms", dirtyRooms,
            "cleaningRooms", cleaningRooms,
            "outOfOrderRooms", outOfOrder,
            "activeGuests", activeGuests.size()
        );
        return ResponseEntity.ok(stats);
    }
}
