package com.hotelos.shared.entities;

import com.hotelos.shared.enums.GuestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    private String idDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuestStatus status;

    private Integer preferredFloor;

    private Boolean preferLift;

    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
    private Room room;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = GuestStatus.REGISTERED;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
