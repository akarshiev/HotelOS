package com.hotelos.roomservice.repository;

import com.hotelos.shared.entities.RoomServiceOrder;
import com.hotelos.shared.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomServiceOrderRepository extends JpaRepository<RoomServiceOrder, Long> {

    Optional<RoomServiceOrder> findByOrderNumber(String orderNumber);

    List<RoomServiceOrder> findByStatus(OrderStatus status);

    List<RoomServiceOrder> findByStatusOrderByOrderedAtAsc(OrderStatus status);

    List<RoomServiceOrder> findByGuestId(Long guestId);

    List<RoomServiceOrder> findByRoomId(Long roomId);

    @Query("SELECT o FROM RoomServiceOrder o ORDER BY CASE o.status WHEN 'RECEIVED' THEN 0 WHEN 'PREPARING' THEN 1 WHEN 'DELIVERING' THEN 2 ELSE 3 END, o.orderedAt ASC")
    List<RoomServiceOrder> findAllOrderByStatus();

    boolean existsByOrderNumber(String orderNumber);

    long countByStatus(OrderStatus status);
}
