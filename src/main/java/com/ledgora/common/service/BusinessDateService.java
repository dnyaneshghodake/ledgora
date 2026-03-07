package com.ledgora.common.service;

import com.ledgora.common.entity.SystemDate;
import com.ledgora.common.enums.BusinessDateStatus;
import com.ledgora.common.repository.SystemDateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class BusinessDateService {

    private static final Logger log = LoggerFactory.getLogger(BusinessDateService.class);
    private final SystemDateRepository systemDateRepository;

    public BusinessDateService(SystemDateRepository systemDateRepository) {
        this.systemDateRepository = systemDateRepository;
    }

    public LocalDate getCurrentBusinessDate() {
        return systemDateRepository.findByStatus(BusinessDateStatus.OPEN)
                .map(SystemDate::getBusinessDate)
                .orElseGet(() -> {
                    log.warn("No open business date found, using system date");
                    return LocalDate.now();
                });
    }

    public SystemDate getCurrentSystemDate() {
        return systemDateRepository.findByStatus(BusinessDateStatus.OPEN)
                .orElseGet(() -> {
                    log.info("Initializing business date to today");
                    SystemDate sd = SystemDate.builder()
                            .businessDate(LocalDate.now())
                            .status(BusinessDateStatus.OPEN)
                            .build();
                    return systemDateRepository.save(sd);
                });
    }

    public boolean isBusinessDateOpen() {
        return systemDateRepository.findByStatus(BusinessDateStatus.OPEN).isPresent();
    }

    @Transactional
    public void startDayClosing() {
        SystemDate current = getCurrentSystemDate();
        current.setStatus(BusinessDateStatus.DAY_CLOSING);
        systemDateRepository.save(current);
        log.info("Business date {} set to DAY_CLOSING", current.getBusinessDate());
    }

    @Transactional
    public void closeDayAndAdvance() {
        SystemDate current = systemDateRepository.findByStatus(BusinessDateStatus.DAY_CLOSING)
                .orElseThrow(() -> new RuntimeException("No business date in DAY_CLOSING status"));
        current.setStatus(BusinessDateStatus.CLOSED);
        systemDateRepository.save(current);

        SystemDate nextDay = SystemDate.builder()
                .businessDate(current.getBusinessDate().plusDays(1))
                .status(BusinessDateStatus.OPEN)
                .build();
        systemDateRepository.save(nextDay);
        log.info("Business date advanced from {} to {}", current.getBusinessDate(), nextDay.getBusinessDate());
    }
}
