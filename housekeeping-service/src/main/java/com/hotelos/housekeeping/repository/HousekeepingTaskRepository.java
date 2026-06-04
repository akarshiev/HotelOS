package com.hotelos.housekeeping.repository;

import com.hotelos.shared.entities.HousekeepingTask;
import com.hotelos.shared.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HousekeepingTaskRepository extends JpaRepository<HousekeepingTask, Long> {

    List<HousekeepingTask> findByStatus(TaskStatus status);

    List<HousekeepingTask> findByStatusOrderByCreatedAtAsc(TaskStatus status);

    @Query("SELECT t FROM HousekeepingTask t ORDER BY CASE t.status WHEN 'PENDING' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, t.createdAt ASC")
    List<HousekeepingTask> findAllOrderByPriority();

    long countByStatus(TaskStatus status);
}
