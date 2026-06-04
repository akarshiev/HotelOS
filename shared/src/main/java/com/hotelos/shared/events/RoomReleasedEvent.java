package com.hotelos.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomReleasedEvent {
    private Long roomId;
    private String roomNumber;
    private Integer floor;
    private String roomType;
    private LocalDateTime releasedAt;
}
