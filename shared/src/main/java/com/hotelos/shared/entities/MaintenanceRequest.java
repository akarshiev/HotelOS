package com.hotelos.shared.entities;

import com.hotelos.shared.enums.MaintenancePriority;
import com.hotelos.shared.enums.MaintenanceStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    @JsonIgnore
    private Room room;

    @Column(name = "room_id")
    private Long roomId;

    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;

    private String assignedTechnician;

    private LocalDateTime reportedAt;

    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = MaintenanceStatus.REPORTED;
        if (reportedAt == null) reportedAt = LocalDateTime.now();
    }
}
