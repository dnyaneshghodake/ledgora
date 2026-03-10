package com.ledgora.common.entity;

import com.ledgora.common.enums.BusinessDateStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "system_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private BusinessDateStatus status = BusinessDateStatus.OPEN;
}
