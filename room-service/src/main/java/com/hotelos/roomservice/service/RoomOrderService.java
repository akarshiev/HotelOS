package com.hotelos.roomservice.service;

import com.hotelos.roomservice.config.RabbitMQConfig;
import com.hotelos.roomservice.repository.RoomServiceOrderRepository;
import com.hotelos.shared.dto.CreateOrderRequest;
import com.hotelos.shared.entities.Guest;
import com.hotelos.shared.entities.RoomServiceOrder;
import com.hotelos.shared.enums.OrderStatus;
import com.hotelos.shared.events.OrderStatusChangedEvent;
import com.hotelos.shared.exception.BadRequestException;
import com.hotelos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomOrderService {

    private final RoomServiceOrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // In-memory queue for order processing (Queue data structure requirement)
    private final Queue<RoomServiceOrder> processingQueue = new ConcurrentLinkedQueue<>();

    @Transactional
    public RoomServiceOrder createOrder(CreateOrderRequest request) {
        log.info("Creating room service order for room: {}", request.getRoomId());

        String orderNumber = generateOrderNumber();

        RoomServiceOrder order = RoomServiceOrder.builder()
            .orderNumber(orderNumber)
            .guestId(request.getGuestId())
            .roomId(request.getRoomId())
            .items(request.getItems())
            .totalAmount(request.getTotalAmount())
            .notes(request.getNotes())
            .status(OrderStatus.RECEIVED)
            .orderedAt(LocalDateTime.now())
            .build();

        order.setRoomNumber("Room " + request.getRoomId());
        order = orderRepository.save(order);
        processingQueue.add(order);

        // Publish status change event
        publishStatusChange(null, order, order.getRoomNumber());

        log.info("Order created: {} with status RECEIVED", orderNumber);
        return order;
    }

    @Transactional
    public RoomServiceOrder updateStatus(Long orderId, OrderStatus newStatus) {
        RoomServiceOrder order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderStatus oldStatus = order.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            processingQueue.remove(order);
        }

        order = orderRepository.save(order);

        // Publish status change event
        publishStatusChange(oldStatus, order, order.getRoomNumber() != null ? order.getRoomNumber() : "Unknown");

        log.info("Order {} status changed from {} to {}", order.getOrderNumber(), oldStatus, newStatus);
        return order;
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (from) {
            case RECEIVED -> to == OrderStatus.PREPARING || to == OrderStatus.CANCELLED;
            case PREPARING -> to == OrderStatus.DELIVERING || to == OrderStatus.CANCELLED;
            case DELIVERING -> to == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid status transition from " + from + " to " + to);
        }
    }

    private void publishStatusChange(OrderStatus oldStatus, RoomServiceOrder order, String roomNumber) {
        // Guest name is resolved safely - the guest entity may not exist in this service's database
        String guestName = resolveGuestName(order);
        
        OrderStatusChangedEvent event = OrderStatusChangedEvent.builder()
            .orderId(order.getId())
            .orderNumber(order.getOrderNumber())
            .guestId(order.getGuestId())
            .roomId(order.getRoomId())
            .items(order.getItems())
            .totalAmount(order.getTotalAmount())
            .oldStatus(oldStatus != null ? oldStatus.name() : null)
            .newStatus(order.getStatus().name())
            .guestName(guestName)
            .roomNumber(roomNumber)
            .changedAt(LocalDateTime.now())
            .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ORDER_STATUS_KEY, event);
        messagingTemplate.convertAndSend("/topic/orders", event);
    }

    /**
     * Safely resolves the guest name. The guest entity may not exist in this service's database
     * (guests are managed by the reception-service), so we handle lazy-loading failures gracefully.
     */
    private String resolveGuestName(RoomServiceOrder order) {
        try {
            Guest guest = order.getGuest();
            if (guest != null) {
                return guest.getFullName();
            }
        } catch (Exception e) {
            log.warn("Could not resolve guest name for guestId {}: {}", order.getGuestId(), e.getMessage());
        }
        return "Guest #" + order.getGuestId();
    }

    public List<RoomServiceOrder> getAllOrders() {
        return orderRepository.findAllOrderByStatus();
    }

    public RoomServiceOrder getOrderById(Long id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public RoomServiceOrder getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
    }

    public List<RoomServiceOrder> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public long countByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            "-" + String.format("%04d", (int) (Math.random() * 10000));
    }
}
