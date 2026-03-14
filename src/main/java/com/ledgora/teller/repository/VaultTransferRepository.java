package com.ledgora.teller.repository;

import com.ledgora.common.enums.VaultTransferStatus;
import com.ledgora.teller.entity.VaultTransfer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VaultTransferRepository extends JpaRepository<VaultTransfer, Long> {

    List<VaultTransfer> findByTellerSessionId(Long sessionId);

    List<VaultTransfer> findByVaultIdAndStatus(Long vaultId, VaultTransferStatus status);

    List<VaultTransfer> findByTenantIdAndStatus(Long tenantId, VaultTransferStatus status);
}
