package com.hotelos.shared.events;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestCheckedInEvent {
    private Long guestId;
    private String guestName;
    private Long roomId;
    private String roomNumber;
    private LocalDateTime checkedInAt;
}
