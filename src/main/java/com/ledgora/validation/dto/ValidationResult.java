package com.ledgora.validation.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/** PART 6: Validation result DTO for ledger integrity checks. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {
    public enum Status {
        HEALTHY,
        WARNING,
        CORRUPTED
    }

    @Builder.Default private Status status = Status.HEALTHY;

    @Builder.Default private List<String> warnings = new ArrayList<>();

    @Builder.Default private List<String> errors = new ArrayList<>();

    @Builder.Default private LocalDateTime validatedAt = LocalDateTime.now();

    private long transactionsChecked;
    private long accountsChecked;
    private long orphanEntriesFound;

    public void addWarning(String warning) {
        if (warnings == null) warnings = new ArrayList<>();
        warnings.add(warning);
        if (status == Status.HEALTHY) status = Status.WARNING;
    }

    public void addError(String error) {
        if (errors == null) errors = new ArrayList<>();
        errors.add(error);
        status = Status.CORRUPTED;
    }
}
