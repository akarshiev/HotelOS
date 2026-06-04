package com.hotelos.maintenance.repository;

import com.hotelos.shared.entities.MaintenanceRequest;
import com.hotelos.shared.enums.MaintenancePriority;
import com.hotelos.shared.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);

    List<MaintenanceRequest> findByPriority(MaintenancePriority priority);

    @Query("SELECT m FROM MaintenanceRequest m ORDER BY CASE m.priority " +
           "WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END, " +
           "m.reportedAt ASC")
    List<MaintenanceRequest> findAllOrderByPriority();

    @Query("SELECT m FROM MaintenanceRequest m WHERE m.status != 'CLOSED' ORDER BY CASE m.priority " +
           "WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END, " +
           "m.reportedAt ASC")
    List<MaintenanceRequest> findActiveOrderByPriority();

    List<MaintenanceRequest> findByRoomId(Long roomId);

    long countByStatus(MaintenanceStatus status);

    long countByPriorityAndStatusNot(MaintenancePriority priority, MaintenanceStatus status);
}
