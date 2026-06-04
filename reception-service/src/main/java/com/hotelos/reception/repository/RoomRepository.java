package com.hotelos.reception.repository;

import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByRoomTypeAndStatus(RoomType roomType, RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status AND r.active = true")
    List<Room> findAvailableByRoomType(@Param("roomType") RoomType roomType, @Param("status") RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status AND r.active = true ORDER BY r.lastCleanedAt ASC NULLS FIRST")
    List<Room> findAvailableByRoomTypeOrderByCleanTime(@Param("roomType") RoomType roomType, @Param("status") RoomStatus status);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status AND r.floor = :floor AND r.active = true ORDER BY r.lastCleanedAt ASC NULLS FIRST")
    List<Room> findAvailableByRoomTypeAndFloor(@Param("roomType") RoomType roomType, @Param("status") RoomStatus status, @Param("floor") Integer floor);

    @Query("SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status AND r.active = true ORDER BY r.lastCleanedAt ASC NULLS FIRST")
    List<Room> findAvailableByRoomTypeOrderPreference(@Param("roomType") RoomType roomType, @Param("status") RoomStatus status);

    List<Room> findByActiveTrue();

    long countByStatus(RoomStatus status);

    long countByRoomTypeAndStatus(RoomType roomType, RoomStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
