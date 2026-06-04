package com.hotelos.reception.repository;

import com.hotelos.shared.entities.RoomServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomServiceOrderRepository extends JpaRepository<RoomServiceOrder, Long> {

    List<RoomServiceOrder> findByGuestId(Long guestId);

    List<RoomServiceOrder> findByRoomId(Long roomId);

    List<RoomServiceOrder> findByGuestIdAndDeliveredAtIsNull(Long guestId);
}
