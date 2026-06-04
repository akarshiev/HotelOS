package com.hotelos.reception.config;

import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.enums.RoomType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) {
        if (roomRepository.count() == 0) {
            log.info("Initializing default rooms...");
            initializeRooms();
        }
    }

    private void initializeRooms() {
        List.of(
            // Floor 1 - Standard rooms
            Room.builder().roomNumber("101").floor(1).roomType(RoomType.SINGLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("99.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("102").floor(1).roomType(RoomType.SINGLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("99.99")).nearLift(false).nearStairs(true).build(),
            Room.builder().roomNumber("103").floor(1).roomType(RoomType.DOUBLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("149.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("104").floor(1).roomType(RoomType.DOUBLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("149.99")).nearLift(false).nearStairs(true).build(),
            Room.builder().roomNumber("105").floor(1).roomType(RoomType.SUITE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("249.99")).nearLift(true).nearStairs(false).build(),
            // Floor 2 - Deluxe rooms
            Room.builder().roomNumber("201").floor(2).roomType(RoomType.DOUBLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("159.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("202").floor(2).roomType(RoomType.DOUBLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("159.99")).nearLift(false).nearStairs(true).build(),
            Room.builder().roomNumber("203").floor(2).roomType(RoomType.SUITE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("259.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("204").floor(2).roomType(RoomType.SUITE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("259.99")).nearLift(false).nearStairs(true).build(),
            Room.builder().roomNumber("205").floor(2).roomType(RoomType.DELUXE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("349.99")).nearLift(true).nearStairs(false).build(),
            // Floor 3 - Premium rooms
            Room.builder().roomNumber("301").floor(3).roomType(RoomType.SUITE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("279.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("302").floor(3).roomType(RoomType.DELUXE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("359.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("303").floor(3).roomType(RoomType.DELUXE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("359.99")).nearLift(false).nearStairs(true).build(),
            Room.builder().roomNumber("304").floor(3).roomType(RoomType.PRESIDENTIAL).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("599.99")).nearLift(true).nearStairs(false).build(),
            Room.builder().roomNumber("305").floor(3).roomType(RoomType.SINGLE).status(RoomStatus.CLEAN).pricePerNight(new BigDecimal("89.99")).nearLift(false).nearStairs(true).build()
        ).forEach(room -> {
            room.setActive(true);
            roomRepository.save(room);
        });

        log.info("Initialized 15 rooms across 3 floors");
    }
}
