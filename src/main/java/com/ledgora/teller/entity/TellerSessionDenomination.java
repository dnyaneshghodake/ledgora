package com.ledgora.teller.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * CBS-grade denomination capture for teller session lifecycle events. Stores row-wise denomination
 * breakdown for OPENING and CLOSING events. Immutable after authorization.
 *
 * <p>Opening denominations persisted on OPEN_REQUESTED -> OPEN transition. Closing denominations
 * persisted on CLOSING_REQUESTED -> CLOSED transition.
 */
@Entity
@Table(
        name = "teller_session_denominations",
        indexes = {
            @Index(name = "idx_tsd_session", columnList = "session_id"),
            @Index(name = "idx_tsd_session_type", columnList = "session_id, event_type")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TellerSessionDenomination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TellerSession session;

    /** OPENING or CLOSING — identifies which lifecycle event this denomination belongs to. */
    @Column(name = "event_type", length = 10, nullable = false)
    private String eventType;

    /** Face value of the denomination (e.g., 2000, 500). */
    @Column(name = "denomination_value", precision = 19, scale = 4, nullable = false)
    private BigDecimal denominationValue;

    /** Number of notes/coins of this denomination. */
    @Column(name = "count", nullable = false)
    private Integer count;

    /** Computed: denominationValue x count. Stored for query performance. */
    @Column(name = "total_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (denominationValue != null && count != null) {
            totalAmount = denominationValue.multiply(new BigDecimal(count));
        }
    }
}
