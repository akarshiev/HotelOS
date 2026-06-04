package com.hotelos.roomservice.controller;

import com.hotelos.roomservice.service.RoomOrderService;
import com.hotelos.shared.dto.CreateOrderRequest;
import com.hotelos.shared.entities.RoomServiceOrder;
import com.hotelos.shared.enums.OrderStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-service")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoomServiceController {

    private final RoomOrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<RoomServiceOrder> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<RoomServiceOrder>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<RoomServiceOrder> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/orders/number/{orderNumber}")
    public ResponseEntity<RoomServiceOrder> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber));
    }

    @GetMapping("/orders/status/{status}")
    public ResponseEntity<List<RoomServiceOrder>> getOrdersByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<RoomServiceOrder> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(orderService.updateStatus(id, newStatus));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
            "received", orderService.countByStatus(OrderStatus.RECEIVED),
            "preparing", orderService.countByStatus(OrderStatus.PREPARING),
            "delivering", orderService.countByStatus(OrderStatus.DELIVERING),
            "delivered", orderService.countByStatus(OrderStatus.DELIVERED)
        );
        return ResponseEntity.ok(stats);
    }
}
