package com.hotelos.maintenance.config;

import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.enums.RoomType;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        Long count = (Long) entityManager.createQuery("SELECT COUNT(r) FROM Room r").getSingleResult();
        if (count == 0) {
            log.info("Initializing rooms in maintenance database...");
            initRooms();
        }
    }

    private void initRooms() {
        Room[] rooms = {
            buildRoom("101", 1, RoomType.SINGLE, RoomStatus.CLEAN, "99.99", true, false),
            buildRoom("102", 1, RoomType.SINGLE, RoomStatus.CLEAN, "99.99", false, true),
            buildRoom("103", 1, RoomType.DOUBLE, RoomStatus.CLEAN, "149.99", true, false),
            buildRoom("104", 1, RoomType.DOUBLE, RoomStatus.CLEAN, "149.99", false, true),
            buildRoom("105", 1, RoomType.SUITE, RoomStatus.CLEAN, "249.99", true, false),
            buildRoom("201", 2, RoomType.DOUBLE, RoomStatus.CLEAN, "159.99", true, false),
            buildRoom("202", 2, RoomType.DOUBLE, RoomStatus.CLEAN, "159.99", false, true),
            buildRoom("203", 2, RoomType.SUITE, RoomStatus.CLEAN, "259.99", true, false),
            buildRoom("204", 2, RoomType.SUITE, RoomStatus.CLEAN, "259.99", false, true),
            buildRoom("205", 2, RoomType.DELUXE, RoomStatus.CLEAN, "349.99", true, false),
            buildRoom("301", 3, RoomType.SUITE, RoomStatus.CLEAN, "279.99", true, false),
            buildRoom("302", 3, RoomType.DELUXE, RoomStatus.CLEAN, "359.99", true, false),
            buildRoom("303", 3, RoomType.DELUXE, RoomStatus.CLEAN, "359.99", false, true),
            buildRoom("304", 3, RoomType.PRESIDENTIAL, RoomStatus.CLEAN, "599.99", true, false),
            buildRoom("305", 3, RoomType.SINGLE, RoomStatus.CLEAN, "89.99", false, true),
        };
        for (Room r : rooms) entityManager.persist(r);
        log.info("Initialized 15 rooms in maintenance database");
    }

    private Room buildRoom(String number, int floor, RoomType type, RoomStatus status, String price, boolean nearLift, boolean nearStairs) {
        Room room = new Room();
        room.setRoomNumber(number);
        room.setFloor(floor);
        room.setRoomType(type);
        room.setStatus(status);
        room.setPricePerNight(new BigDecimal(price));
        room.setNearLift(nearLift);
        room.setNearStairs(nearStairs);
        room.setActive(true);
        return room;
    }
}
