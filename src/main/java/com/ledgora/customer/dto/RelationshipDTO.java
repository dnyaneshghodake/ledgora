package com.ledgora.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.*;

/** DTO for CustomerRelationship — used in Tab 6 of Customer Master screen. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipDTO {

    private Long id;

    @NotNull(message = "Related customer ID is required")
    private Long relatedCustomerId;

    private String relatedCustomerNumber;
    private String relatedCustomerName;

    @NotBlank(message = "Relationship type is required")
    private String relationshipType; // JOINT_HOLDER, NOMINEE, GUARANTOR, GUARDIAN, etc.

    private Boolean isActive;
    private String remarks;

    // Audit (read-only)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
