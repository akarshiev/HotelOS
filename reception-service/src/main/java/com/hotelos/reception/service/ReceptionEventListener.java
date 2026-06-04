package com.hotelos.reception.service;

import com.hotelos.reception.config.RabbitMQConfig;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.events.RoomStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceptionEventListener {

    private final RoomRepository roomRepository;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "reception.room-status", durable = "true"),
            exchange = @Exchange(value = RabbitMQConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = RabbitMQConfig.ROOM_STATUS_KEY
    ))
    @Transactional
    public void handleRoomStatusChanged(RoomStatusChangedEvent event) {
        log.info("Received room status change event for room: {} to {}", event.getRoomNumber(), event.getNewStatus());
        
        roomRepository.findById(event.getRoomId()).ifPresent(room -> {
            room.setStatus(RoomStatus.valueOf(event.getNewStatus()));
            if (room.getStatus() == RoomStatus.CLEAN) {
                room.setLastCleanedAt(LocalDateTime.now());
            }
            roomRepository.save(room);
            log.info("Updated room {} status to {}", room.getRoomNumber(), room.getStatus());
        });
    }
}
