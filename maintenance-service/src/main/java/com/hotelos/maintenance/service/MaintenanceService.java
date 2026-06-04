package com.hotelos.maintenance.service;

import com.hotelos.maintenance.config.RabbitMQConfig;
import com.hotelos.maintenance.repository.MaintenanceRequestRepository;
import com.hotelos.shared.dto.CreateMaintenanceRequest;
import com.hotelos.shared.entities.MaintenanceRequest;
import com.hotelos.shared.enums.MaintenancePriority;
import com.hotelos.shared.enums.MaintenanceStatus;
import com.hotelos.shared.events.MaintenanceStatusChangedEvent;
import com.hotelos.shared.exception.BadRequestException;
import com.hotelos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceService {

    private final MaintenanceRequestRepository requestRepository;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // Priority Queue for maintenance requests (Priority Queue data structure requirement)
    private PriorityQueue<MaintenanceRequest> priorityQueue;

    private synchronized PriorityQueue<MaintenanceRequest> getPriorityQueue() {
        if (priorityQueue == null) {
            priorityQueue = new PriorityQueue<>(
                Comparator.comparingInt(this::getPriorityOrder)
                    .thenComparing(MaintenanceRequest::getReportedAt)
            );
            requestRepository.findActiveOrderByPriority().forEach(priorityQueue::add);
        }
        return priorityQueue;
    }

    private int getPriorityOrder(MaintenanceRequest request) {
        return switch (request.getPriority()) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case NORMAL -> 2;
            case LOW -> 3;
        };
    }

    @Transactional
    public MaintenanceRequest createRequest(CreateMaintenanceRequest request) {
        log.info("Creating maintenance request: {}", request.getTitle());

        MaintenanceRequest maintenanceRequest = MaintenanceRequest.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .roomId(request.getRoomId())
            .priority(MaintenancePriority.valueOf(request.getPriority()))
            .status(MaintenanceStatus.REPORTED)
            .reportedAt(LocalDateTime.now())
            .build();

        maintenanceRequest = requestRepository.save(maintenanceRequest);

        // Add to priority queue
        getPriorityQueue().add(maintenanceRequest);

        // Publish status change event
        publishStatusChange(null, maintenanceRequest);

        log.info("Maintenance request created: {} with priority {}", maintenanceRequest.getId(), maintenanceRequest.getPriority());
        return maintenanceRequest;
    }

    @Transactional
    public MaintenanceRequest updateStatus(Long requestId, MaintenanceStatus newStatus) {
        MaintenanceRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));

        MaintenanceStatus oldStatus = request.getStatus();
        request.setStatus(newStatus);

        if (newStatus == MaintenanceStatus.RESOLVED || newStatus == MaintenanceStatus.CLOSED) {
            request.setResolvedAt(LocalDateTime.now());
            getPriorityQueue().remove(request);
        }

        request = requestRepository.save(request);

        // Publish status change event
        publishStatusChange(oldStatus, request);

        log.info("Maintenance request {} status changed from {} to {}", requestId, oldStatus, newStatus);
        return request;
    }

    @Transactional
    public MaintenanceRequest assignTechnician(Long requestId, String technicianName) {
        MaintenanceRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + requestId));

        request.setAssignedTechnician(technicianName);
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        request = requestRepository.save(request);

        publishStatusChange(MaintenanceStatus.REPORTED, request);

        log.info("Technician {} assigned to maintenance request {}", technicianName, requestId);
        return request;
    }

    public MaintenanceRequest getNextHighPriority() {
        return getPriorityQueue().peek();
    }

    public MaintenanceRequest processNext() {
        MaintenanceRequest next = getPriorityQueue().poll();
        if (next != null) {
            next.setStatus(MaintenanceStatus.IN_PROGRESS);
            requestRepository.save(next);
        }
        return next;
    }

    private void publishStatusChange(MaintenanceStatus oldStatus, MaintenanceRequest request) {
        MaintenanceStatusChangedEvent event = MaintenanceStatusChangedEvent.builder()
            .requestId(request.getId())
            .title(request.getTitle())
            .priority(request.getPriority().name())
            .oldStatus(oldStatus != null ? oldStatus.name() : null)
            .newStatus(request.getStatus().name())
            .roomNumber(request.getRoomNumber() != null ? request.getRoomNumber() : "Room " + request.getRoomId())
            .assignedTechnician(request.getAssignedTechnician())
            .changedAt(LocalDateTime.now())
            .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.MAINTENANCE_STATUS_KEY, event);
        messagingTemplate.convertAndSend("/topic/maintenance", event);
    }

    public List<MaintenanceRequest> getAllRequests() {
        return requestRepository.findAllOrderByPriority();
    }

    public List<MaintenanceRequest> getActiveRequests() {
        return requestRepository.findActiveOrderByPriority();
    }

    public MaintenanceRequest getRequestById(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found: " + id));
    }

    public List<MaintenanceRequest> getRequestsByStatus(MaintenanceStatus status) {
        return requestRepository.findByStatus(status);
    }

    public long countByStatus(MaintenanceStatus status) {
        return requestRepository.countByStatus(status);
    }
}
