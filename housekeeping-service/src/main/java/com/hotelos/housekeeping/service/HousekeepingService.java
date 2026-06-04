package com.hotelos.housekeeping.service;

import com.hotelos.housekeeping.config.RabbitMQConfig;
import com.hotelos.housekeeping.repository.HousekeepingTaskRepository;
import com.hotelos.shared.entities.HousekeepingTask;
import com.hotelos.shared.enums.TaskStatus;
import com.hotelos.shared.events.RoomReleasedEvent;
import com.hotelos.shared.events.RoomStatusChangedEvent;
import com.hotelos.shared.exception.BadRequestException;
import com.hotelos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HousekeepingService {

    private final HousekeepingTaskRepository taskRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.ROOM_RELEASED_QUEUE)
    @Transactional
    public void handleRoomReleased(RoomReleasedEvent event) {
        log.info("Room released event received for room: {} - Creating cleaning task", event.getRoomNumber());

        // Create a new housekeeping task for the dirty room
        HousekeepingTask task = HousekeepingTask.builder()
            .roomId(event.getRoomId())
            .roomNumber(event.getRoomNumber())
            .status(TaskStatus.PENDING)
            .notes("Auto-created from RoomReleased event for room " + event.getRoomNumber())
            .createdAt(LocalDateTime.now())
            .build();

        // We need to create a minimal Room entity reference
        // In a real microservice, we'd use the roomId from the event
        taskRepository.save(task);

        log.info("Housekeeping task created for room: {}", event.getRoomNumber());
    }

    @Transactional
    public HousekeepingTask createTask(HousekeepingTask task) {
        log.info("Creating housekeeping task for room");
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Transactional
    public HousekeepingTask updateTaskStatus(Long taskId, TaskStatus newStatus) {
        HousekeepingTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.IN_PROGRESS) {
            task.setStartedAt(LocalDateTime.now());
        } else if (newStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());

            // Publish room status changed event - room is now CLEAN
            messagingTemplate.convertAndSend("/topic/rooms", RoomStatusChangedEvent.builder()
                .roomId(task.getRoomId())
                .roomNumber(task.getRoomNumber() != null ? task.getRoomNumber() : "Unknown")
                .oldStatus("CLEANING")
                .newStatus("CLEAN")
                .changedAt(LocalDateTime.now())
                .build());
        }

        return taskRepository.save(task);
    }

    @Transactional
    public HousekeepingTask assignTask(Long taskId, String staffName) {
        HousekeepingTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        if (task.getStatus() != TaskStatus.PENDING) {
            throw new BadRequestException("Only pending tasks can be assigned");
        }

        task.setAssignedStaff(staffName);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setStartedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    public List<HousekeepingTask> getAllTasks() {
        return taskRepository.findAllOrderByPriority();
    }

    public List<HousekeepingTask> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public HousekeepingTask getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    public long countByStatus(TaskStatus status) {
        return taskRepository.countByStatus(status);
    }
}
