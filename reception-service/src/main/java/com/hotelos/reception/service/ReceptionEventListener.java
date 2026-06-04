package com.hotelos.reception.service;

import com.hotelos.reception.config.RabbitMQConfig;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.shared.entities.Room;
import com.hotelos.shared.entities.RoomServiceOrder;
import com.hotelos.shared.enums.OrderStatus;
import com.hotelos.shared.enums.RoomStatus;
import com.hotelos.shared.events.OrderStatusChangedEvent;
import com.hotelos.shared.events.RoomStatusChangedEvent;
import com.hotelos.reception.repository.RoomServiceOrderRepository;
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
    private final RoomServiceOrderRepository orderRepository;

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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "reception.order-status", durable = "true"),
            exchange = @Exchange(value = RabbitMQConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = "event.order.status"
    ))
    @Transactional
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received order status change event for order: {}", event.getOrderNumber());
        
        RoomServiceOrder order = orderRepository.findByOrderNumber(event.getOrderNumber()).orElseGet(() -> {
            RoomServiceOrder newOrder = new RoomServiceOrder();
            newOrder.setOrderNumber(event.getOrderNumber());
            newOrder.setGuestId(event.getGuestId());
            newOrder.setRoomId(event.getRoomId());
            newOrder.setRoomNumber(event.getRoomNumber());
            newOrder.setItems(event.getItems());
            newOrder.setTotalAmount(event.getTotalAmount());
            newOrder.setOrderedAt(event.getChangedAt());
            return newOrder;
        });
        
        order.setStatus(OrderStatus.valueOf(event.getNewStatus()));
        if (order.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(event.getChangedAt());
        }
        
        orderRepository.save(order);
        log.info("Updated local order {} status to {}", order.getOrderNumber(), order.getStatus());
    }
}
