package com.hotelos.reception.service;

import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.enums.RoomType;
import com.hotelos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAssignmentAlgorithm {

    private final RoomRepository roomRepository;

    /**
     * Assigns a room based on the algorithm:
     * 1. Room type match
     * 2. Only CLEAN status rooms
     * 3. Longest-cleaned room first
     * 4. Floor preference
     * 5. Lift/stairs proximity preference
     *
     * Uses pessimistic locking to prevent double-booking in concurrent scenarios.
     */
    @Transactional
    public Room assignRoom(RoomType roomType, Integer preferredFloor, Boolean preferLift) {
        log.info("Assigning room - Type: {}, Floor Preference: {}, Prefer Lift: {}", roomType, preferredFloor, preferLift);

        // Step 1 & 2: Get CLEAN rooms of the requested type
        List<Room> availableRooms = roomRepository.findByRoomTypeAndStatus(roomType, RoomStatus.CLEAN);

        if (availableRooms.isEmpty()) {
            throw new BadRequestException("No rooms available for type " + roomType);
        }

        // Step 3: Sort by longest time since cleaned (earliest lastCleanedAt first)
        availableRooms.sort(Comparator.comparing(
            r -> r.getLastCleanedAt() != null ? r.getLastCleanedAt() : LocalDateTime.MIN
        ));

        // Step 4: Floor preference - prefer matching floor
        Optional<Room> floorMatch = availableRooms.stream()
            .filter(r -> preferredFloor != null && r.getFloor().equals(preferredFloor))
            .findFirst();

        if (floorMatch.isPresent()) {
            if (preferLift != null && preferLift) {
                Optional<Room> liftMatch = availableRooms.stream()
                    .filter(r -> preferredFloor != null && r.getFloor().equals(preferredFloor))
                    .filter(Room::getNearLift)
                    .findFirst();
                if (liftMatch.isPresent()) {
                    return assignAndOccupy(liftMatch.get());
                }
            }
            return assignAndOccupy(floorMatch.get());
        }

        // No floor preference match - return first available (longest clean)
        Room assigned = availableRooms.get(0);

        if (preferLift != null && preferLift) {
            Optional<Room> liftMatch = availableRooms.stream()
                .filter(Room::getNearLift)
                .findFirst();
            if (liftMatch.isPresent()) {
                assigned = liftMatch.get();
            }
        }

        return assignAndOccupy(assigned);
    }

    /**
     * Re-check room availability with pessimistic lock before assigning.
     * Prevents two concurrent requests from assigning the same room.
     */
    private Room assignAndOccupy(Room room) {
        // Re-fetch with pessimistic lock to ensure no concurrent assignment
        Room lockedRoom = roomRepository.findByIdForUpdate(room.getId())
            .orElseThrow(() -> new BadRequestException("Room no longer available: " + room.getRoomNumber()));

        if (lockedRoom.getStatus() != RoomStatus.CLEAN) {
            throw new BadRequestException("Room " + lockedRoom.getRoomNumber() + " is no longer available (now " + lockedRoom.getStatus() + ")");
        }

        lockedRoom.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(lockedRoom);
        log.info("Room {} assigned and set to OCCUPIED (pessimistic lock)", lockedRoom.getRoomNumber());
        return lockedRoom;
    }
}
