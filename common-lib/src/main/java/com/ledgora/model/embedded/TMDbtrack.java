package com.ledgora.model.embedded;

import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Database Track Information - Embedded Object
 *
 * <p>Used to track audit information including creation and modification details. This is embedded
 * in various entity classes for audit trail purposes.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TMDbtrack {

    /** Created By - User ID who created the record */
    private String createdBy;

    /** Created Date and Time */
    private LocalDateTime createdDt;

    /** Modified By - User ID who last modified the record */
    private String modifiedBy;

    /** Modified Date and Time */
    private LocalDateTime modifiedDt;

    @Override
    public String toString() {
        return "TMDbtrack{"
                + "createdBy='"
                + createdBy
                + '\''
                + ", createdDt="
                + createdDt
                + ", modifiedBy='"
                + modifiedBy
                + '\''
                + ", modifiedDt="
                + modifiedDt
                + '}';
    }
}
