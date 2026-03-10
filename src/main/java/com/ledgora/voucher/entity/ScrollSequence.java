package com.ledgora.voucher.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

/**
 * Scroll number sequence tracker per (tenant_id, branch_id, posting_date). Auto-increments scroll
 * numbers for voucher batching.
 */
@Entity
@Table(
        name = "scroll_sequences",
        indexes = {
            @Index(
                    name = "idx_scroll_seq_composite",
                    columnList = "tenant_id, branch_id, posting_date",
                    unique = true)
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrollSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Column(name = "last_scroll_no", nullable = false)
    @Builder.Default
    private Long lastScrollNo = 0L;
}
