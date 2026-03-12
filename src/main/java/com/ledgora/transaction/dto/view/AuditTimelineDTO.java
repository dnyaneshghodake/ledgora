package com.ledgora.transaction.dto.view;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Audit trail timeline entry for Transaction 360° View. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTimelineDTO {
    private Long id;
    private String action;
    private LocalDateTime timestamp;
    private String username;
    private String ipAddress;
    private String oldValue;
    private String newValue;
    private String details;
}
