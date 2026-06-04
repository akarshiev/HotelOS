package com.hotelos.maintenance.controller;

import com.hotelos.maintenance.service.MaintenanceService;
import com.hotelos.shared.dto.CreateMaintenanceRequest;
import com.hotelos.shared.entities.MaintenanceRequest;
import com.hotelos.shared.enums.MaintenanceStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/requests")
    public ResponseEntity<MaintenanceRequest> createRequest(@Valid @RequestBody CreateMaintenanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.createRequest(request));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<MaintenanceRequest>> getAllRequests() {
        return ResponseEntity.ok(maintenanceService.getAllRequests());
    }

    @GetMapping("/requests/active")
    public ResponseEntity<List<MaintenanceRequest>> getActiveRequests() {
        return ResponseEntity.ok(maintenanceService.getActiveRequests());
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<MaintenanceRequest> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(maintenanceService.getRequestById(id));
    }

    @GetMapping("/requests/status/{status}")
    public ResponseEntity<List<MaintenanceRequest>> getRequestsByStatus(@PathVariable MaintenanceStatus status) {
        return ResponseEntity.ok(maintenanceService.getRequestsByStatus(status));
    }

    @PutMapping("/requests/{id}/status")
    public ResponseEntity<MaintenanceRequest> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        MaintenanceStatus newStatus = MaintenanceStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(maintenanceService.updateStatus(id, newStatus));
    }

    @PutMapping("/requests/{id}/assign")
    public ResponseEntity<MaintenanceRequest> assignTechnician(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(maintenanceService.assignTechnician(id, body.get("technician")));
    }

    @PostMapping("/requests/next")
    public ResponseEntity<MaintenanceRequest> processNext() {
        MaintenanceRequest next = maintenanceService.processNext();
        return next != null ? ResponseEntity.ok(next) : ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
            "reported", maintenanceService.countByStatus(MaintenanceStatus.REPORTED),
            "inProgress", maintenanceService.countByStatus(MaintenanceStatus.IN_PROGRESS),
            "resolved", maintenanceService.countByStatus(MaintenanceStatus.RESOLVED),
            "closed", maintenanceService.countByStatus(MaintenanceStatus.CLOSED)
        );
        return ResponseEntity.ok(stats);
    }
}
