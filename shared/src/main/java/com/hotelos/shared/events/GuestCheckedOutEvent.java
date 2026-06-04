package com.hotelos.shared.events;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestCheckedOutEvent {
    private Long guestId;
    private String guestName;
    private Long roomId;
    private String roomNumber;
    private BigDecimal totalAmount;
    private LocalDateTime checkedOutAt;
}
