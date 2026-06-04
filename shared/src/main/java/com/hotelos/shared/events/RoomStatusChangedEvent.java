package com.hotelos.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStatusChangedEvent {
    private Long roomId;
    private String roomNumber;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime changedAt;
}
