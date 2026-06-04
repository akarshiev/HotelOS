package com.hotelos.housekeeping.controller;

import com.hotelos.housekeeping.service.HousekeepingService;
import com.hotelos.shared.entities.HousekeepingTask;
import com.hotelos.shared.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/housekeeping")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    @GetMapping("/tasks")
    public ResponseEntity<List<HousekeepingTask>> getAllTasks() {
        return ResponseEntity.ok(housekeepingService.getAllTasks());
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<HousekeepingTask> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(housekeepingService.getTaskById(id));
    }

    @GetMapping("/tasks/status/{status}")
    public ResponseEntity<List<HousekeepingTask>> getTasksByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(housekeepingService.getTasksByStatus(status));
    }

    @PostMapping("/tasks")
    public ResponseEntity<HousekeepingTask> createTask(@RequestBody HousekeepingTask task) {
        return ResponseEntity.status(HttpStatus.CREATED).body(housekeepingService.createTask(task));
    }

    @PutMapping("/tasks/{id}/status")
    public ResponseEntity<HousekeepingTask> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        TaskStatus newStatus = TaskStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(housekeepingService.updateTaskStatus(id, newStatus));
    }

    @PutMapping("/tasks/{id}/assign")
    public ResponseEntity<HousekeepingTask> assignTask(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(housekeepingService.assignTask(id, body.get("staffName")));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        Map<String, Long> stats = Map.of(
            "pending", housekeepingService.countByStatus(TaskStatus.PENDING),
            "inProgress", housekeepingService.countByStatus(TaskStatus.IN_PROGRESS),
            "completed", housekeepingService.countByStatus(TaskStatus.COMPLETED)
        );
        return ResponseEntity.ok(stats);
    }
}
