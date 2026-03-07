package com.ledgora.common.repository;

import com.ledgora.common.entity.SystemDate;
import com.ledgora.common.enums.BusinessDateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SystemDateRepository extends JpaRepository<SystemDate, Long> {
    Optional<SystemDate> findByStatus(BusinessDateStatus status);

    @Query("SELECT sd FROM SystemDate sd ORDER BY sd.id DESC LIMIT 1")
    Optional<SystemDate> findCurrentDate();
}
