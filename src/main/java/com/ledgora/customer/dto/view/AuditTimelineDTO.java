package com.ledgora.customer.dto.view;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Audit trail entry for Customer 360° View — Audit Trail tab. Populated from AuditLog entity with
 * tenant isolation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditTimelineDTO {

    private Long id;
    private String action;
    private String entity;
    private Long entityId;
    private String details;
    private String username;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String oldValue;
    private String newValue;
}
