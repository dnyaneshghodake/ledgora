package com.ledgora.branch.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "branches")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Branch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "branch_code", length = 10, nullable = false, unique = true)
    private String branchCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
