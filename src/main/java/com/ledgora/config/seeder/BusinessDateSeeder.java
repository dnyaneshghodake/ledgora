package com.ledgora.config.seeder;

import com.ledgora.common.entity.SystemDate;
import com.ledgora.common.enums.BusinessDateStatus;
import com.ledgora.common.repository.SystemDateRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CBS DataSeeder: Module 5 — Business Date seeding.
 * Seeds current business date with OPEN status.
 */
@Component
public class BusinessDateSeeder {

    private static final Logger log = LoggerFactory.getLogger(BusinessDateSeeder.class);
    private final SystemDateRepository systemDateRepository;

    public BusinessDateSeeder(SystemDateRepository systemDateRepository) {
        this.systemDateRepository = systemDateRepository;
    }

    public void seed() {
        if (systemDateRepository.count() == 0) {
            SystemDate sd =
                    SystemDate.builder()
                            .businessDate(LocalDate.now())
                            .status(BusinessDateStatus.OPEN)
                            .build();
            systemDateRepository.save(sd);
            log.info("  [SystemDate] Initialized business date: {} (OPEN)", sd.getBusinessDate());
        }
    }
}
