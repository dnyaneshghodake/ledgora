package com.ledgora;

import com.ledgora.batch.entity.TransactionBatch;
import com.ledgora.batch.repository.TransactionBatchRepository;
import com.ledgora.batch.service.BatchService;
import com.ledgora.common.enums.*;
import com.ledgora.common.exception.BusinessDayClosedException;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.repository.GeneralLedgerRepository;
import com.ledgora.gl.service.GlBalanceService;
import com.ledgora.tenant.context.TenantContextHolder;
import com.ledgora.tenant.entity.Tenant;
import com.ledgora.tenant.repository.TenantRepository;
import com.ledgora.tenant.service.TenantService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive integration tests for Ledgora enhancement features:
 * - PART 1: GL Balance updates and parent rollup
 * - PART 2: Transaction batching
 * - PART 3: Multi-tenant isolation
 * - PART 4: Strict EOD enforcement
 * - PART 5: Per-tenant settlement and business date advancement
 */
@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgoraEnhancementIntegrationTest {

    @Autowired
    private GlBalanceService glBalanceService;

    @Autowired
    private GeneralLedgerRepository glRepository;

    @Autowired
    private BatchService batchService;

    @Autowired
    private TransactionBatchRepository batchRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    // ════════════════════════════════════════════════════════════════════════
    // PART 1: GL BALANCE UPDATES AND PARENT ROLLUP
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @Transactional
    @DisplayName("PART 1: GL balance updates correctly for Asset account (debit increases)")
    void testGlBalanceUpdate_AssetAccount_DebitIncreases() {
        // Create a standalone Asset GL account for testing
        GeneralLedger assetGL = GeneralLedger.builder()
                .glCode("TEST-ASSET-001")
                .glName("Test Asset Account")
                .description("Test")
                .accountType(GLAccountType.ASSET)
                .level(0)
                .isActive(true)
                .normalBalance("DEBIT")
                .balance(BigDecimal.ZERO)
                .build();
        assetGL = glRepository.save(assetGL);

        // Debit 1000 to an asset account should increase balance
        glBalanceService.updateGlBalance(assetGL, new BigDecimal("1000.0000"), BigDecimal.ZERO);

        GeneralLedger updated = glRepository.findById(assetGL.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1000.0000").compareTo(updated.getBalance()),
                "Asset account balance should increase by debit amount");
    }

    @Test
    @Order(2)
    @Transactional
    @DisplayName("PART 1: GL balance updates correctly for Liability account (credit increases)")
    void testGlBalanceUpdate_LiabilityAccount_CreditIncreases() {
        GeneralLedger liabilityGL = GeneralLedger.builder()
                .glCode("TEST-LIAB-001")
                .glName("Test Liability Account")
                .description("Test")
                .accountType(GLAccountType.LIABILITY)
                .level(0)
                .isActive(true)
                .normalBalance("CREDIT")
                .balance(BigDecimal.ZERO)
                .build();
        liabilityGL = glRepository.save(liabilityGL);

        // Credit 2000 to a liability account should increase balance
        glBalanceService.updateGlBalance(liabilityGL, BigDecimal.ZERO, new BigDecimal("2000.0000"));

        GeneralLedger updated = glRepository.findById(liabilityGL.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("2000.0000").compareTo(updated.getBalance()),
                "Liability account balance should increase by credit amount");
    }

    @Test
    @Order(3)
    @Transactional
    @DisplayName("PART 1: Parent GL rollup propagates correctly from child to parent")
    void testGlBalanceUpdate_ParentRollup() {
        // Create parent (root) Asset GL
        GeneralLedger parentAsset = GeneralLedger.builder()
                .glCode("TEST-PAR-001")
                .glName("Parent Asset")
                .description("Parent test asset")
                .accountType(GLAccountType.ASSET)
                .level(0)
                .isActive(true)
                .normalBalance("DEBIT")
                .balance(BigDecimal.ZERO)
                .build();
        parentAsset = glRepository.save(parentAsset);

        // Create child Asset GL under parent
        GeneralLedger childAsset = GeneralLedger.builder()
                .glCode("TEST-CHD-001")
                .glName("Child Asset")
                .description("Child test asset")
                .accountType(GLAccountType.ASSET)
                .parent(parentAsset)
                .level(1)
                .isActive(true)
                .normalBalance("DEBIT")
                .balance(BigDecimal.ZERO)
                .build();
        childAsset = glRepository.save(childAsset);

        // Update child GL balance - should propagate to parent
        glBalanceService.updateGlBalance(childAsset, new BigDecimal("5000.0000"), BigDecimal.ZERO);

        GeneralLedger updatedChild = glRepository.findById(childAsset.getId()).orElseThrow();
        GeneralLedger updatedParent = glRepository.findById(parentAsset.getId()).orElseThrow();

        assertEquals(0, new BigDecimal("5000.0000").compareTo(updatedChild.getBalance()),
                "Child asset balance should be updated");
        assertEquals(0, new BigDecimal("5000.0000").compareTo(updatedParent.getBalance()),
                "Parent asset balance should be updated via rollup");
    }

    @Test
    @Order(4)
    @Transactional
    @DisplayName("PART 1: Expense account debit increases, credit decreases balance")
    void testGlBalanceUpdate_ExpenseAccount_SignConventions() {
        GeneralLedger expenseGL = GeneralLedger.builder()
                .glCode("TEST-EXP-001")
                .glName("Test Expense Account")
                .description("Test")
                .accountType(GLAccountType.EXPENSE)
                .level(0)
                .isActive(true)
                .normalBalance("DEBIT")
                .balance(new BigDecimal("1000.0000"))
                .build();
        expenseGL = glRepository.save(expenseGL);

        // Credit should decrease expense balance
        glBalanceService.updateGlBalance(expenseGL, BigDecimal.ZERO, new BigDecimal("300.0000"));

        GeneralLedger updated = glRepository.findById(expenseGL.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("700.0000").compareTo(updated.getBalance()),
                "Expense account balance should decrease by credit amount");
    }

    @Test
    @Order(5)
    @Transactional
    @DisplayName("PART 1: Revenue account credit increases, debit decreases balance")
    void testGlBalanceUpdate_RevenueAccount_SignConventions() {
        GeneralLedger revenueGL = GeneralLedger.builder()
                .glCode("TEST-REV-001")
                .glName("Test Revenue Account")
                .description("Test")
                .accountType(GLAccountType.REVENUE)
                .level(0)
                .isActive(true)
                .normalBalance("CREDIT")
                .balance(new BigDecimal("5000.0000"))
                .build();
        revenueGL = glRepository.save(revenueGL);

        // Debit should decrease revenue balance
        glBalanceService.updateGlBalance(revenueGL, new BigDecimal("1000.0000"), BigDecimal.ZERO);

        GeneralLedger updated = glRepository.findById(revenueGL.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("4000.0000").compareTo(updated.getBalance()),
                "Revenue account balance should decrease by debit amount");
    }

    // ════════════════════════════════════════════════════════════════════════
    // PART 2: TRANSACTION BATCHING
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(10)
    @Transactional
    @DisplayName("PART 2: Batch is created for tenant+channel+date combination")
    void testBatchCreation() {
        Tenant tenant = createTestTenant("BATCH-TEST-01", "Batch Test Bank 1");

        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ATM, LocalDate.now());

        assertNotNull(batch);
        assertNotNull(batch.getId());
        assertEquals(BatchStatus.OPEN, batch.getStatus());
        assertEquals(BatchType.ATM, batch.getBatchType());
        assertEquals(0, BigDecimal.ZERO.compareTo(batch.getTotalDebit()));
        assertEquals(0, BigDecimal.ZERO.compareTo(batch.getTotalCredit()));
        assertEquals(0, batch.getTransactionCount());
    }

    @Test
    @Order(11)
    @Transactional
    @DisplayName("PART 2: Same batch returned for same tenant+channel+date")
    void testBatchReuse() {
        Tenant tenant = createTestTenant("BATCH-TEST-02", "Batch Test Bank 2");
        LocalDate today = LocalDate.now();

        TransactionBatch batch1 = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ONLINE, today);
        TransactionBatch batch2 = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ONLINE, today);

        assertEquals(batch1.getId(), batch2.getId(),
                "Same batch should be returned for same tenant+channel+date");
    }

    @Test
    @Order(12)
    @Transactional
    @DisplayName("PART 2: Batch totals update correctly")
    void testBatchTotalsUpdate() {
        Tenant tenant = createTestTenant("BATCH-TEST-03", "Batch Test Bank 3");

        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.TELLER, LocalDate.now());

        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("1000.0000"), new BigDecimal("1000.0000"));
        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("500.0000"), new BigDecimal("500.0000"));

        TransactionBatch updated = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1500.0000").compareTo(updated.getTotalDebit()));
        assertEquals(0, new BigDecimal("1500.0000").compareTo(updated.getTotalCredit()));
        assertEquals(2, updated.getTransactionCount());
    }

    @Test
    @Order(13)
    @Transactional
    @DisplayName("PART 2: Batch close and settle works correctly")
    void testBatchCloseAndSettle() {
        Tenant tenant = createTestTenant("BATCH-TEST-04", "Batch Test Bank 4");
        LocalDate today = LocalDate.now();

        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ATM, today);

        // Add balanced totals
        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("2000.0000"), new BigDecimal("2000.0000"));

        // Close batches
        batchService.closeAllBatches(tenant.getId(), today);
        TransactionBatch closed = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(BatchStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());

        // Settle batches
        batchService.settleAllBatches(tenant.getId(), today);
        TransactionBatch settled = batchRepository.findById(batch.getId()).orElseThrow();
        assertEquals(BatchStatus.SETTLED, settled.getStatus());
    }

    @Test
    @Order(14)
    @Transactional
    @DisplayName("PART 2: Unbalanced batch settlement fails")
    void testUnbalancedBatchSettlementFails() {
        Tenant tenant = createTestTenant("BATCH-TEST-05", "Batch Test Bank 5");
        LocalDate today = LocalDate.now();

        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ONLINE, today);

        // Add unbalanced totals (debit != credit)
        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("3000.0000"), new BigDecimal("2000.0000"));

        // Close should fail for unbalanced batch (batch close now validates balance)
        assertThrows(RuntimeException.class, () ->
                batchService.closeAllBatches(tenant.getId(), today),
                "Unbalanced batch close should throw exception");
    }

    @Test
    @Order(15)
    @Transactional
    @DisplayName("PART 2: Cannot update closed batch")
    void testCannotUpdateClosedBatch() {
        Tenant tenant = createTestTenant("BATCH-TEST-06", "Batch Test Bank 6");
        LocalDate today = LocalDate.now();

        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.TELLER, today);

        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("100.0000"), new BigDecimal("100.0000"));

        batchService.closeAllBatches(tenant.getId(), today);

        // Attempting to update a closed batch should fail
        assertThrows(RuntimeException.class, () ->
                batchService.updateBatchTotals(batch.getId(),
                        new BigDecimal("200.0000"), new BigDecimal("200.0000")),
                "Should not be able to update closed batch");
    }

    // ════════════════════════════════════════════════════════════════════════
    // PART 3: MULTI-TENANT ISOLATION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(20)
    @Transactional
    @DisplayName("PART 3: Tenant creation and retrieval works")
    void testTenantCreationAndRetrieval() {
        Tenant tenant = createTestTenant("MT-TEST-01", "Multi-Tenant Test Bank");

        Tenant retrieved = tenantService.getTenantById(tenant.getId());
        assertNotNull(retrieved);
        assertEquals("MT-TEST-01", retrieved.getTenantCode());
        assertEquals("Multi-Tenant Test Bank", retrieved.getTenantName());
        assertEquals(DayStatus.OPEN, retrieved.getDayStatus());
        assertEquals(LocalDate.now(), retrieved.getCurrentBusinessDate());
    }

    @Test
    @Order(21)
    @Transactional
    @DisplayName("PART 3: Each tenant has independent batches")
    void testTenantBatchIsolation() {
        Tenant tenant1 = createTestTenant("MT-TEST-02A", "Tenant A");
        Tenant tenant2 = createTestTenant("MT-TEST-02B", "Tenant B");
        LocalDate today = LocalDate.now();

        // Create batches for each tenant
        TransactionBatch batch1 = batchService.getOrCreateOpenBatch(
                tenant1.getId(), TransactionChannel.ATM, today);
        TransactionBatch batch2 = batchService.getOrCreateOpenBatch(
                tenant2.getId(), TransactionChannel.ATM, today);

        // Batches should be different
        assertNotEquals(batch1.getId(), batch2.getId(),
                "Different tenants should have separate batches");

        // Update batch for tenant1 only
        batchService.updateBatchTotals(batch1.getId(),
                new BigDecimal("1000.0000"), new BigDecimal("1000.0000"));

        // Verify tenant2's batch is unaffected
        TransactionBatch tenant2Batch = batchRepository.findById(batch2.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(tenant2Batch.getTotalDebit()),
                "Tenant 2 batch should be unaffected by Tenant 1 operations");
        assertEquals(0, tenant2Batch.getTransactionCount(),
                "Tenant 2 batch transaction count should be 0");
    }

    @Test
    @Order(22)
    @Transactional
    @DisplayName("PART 3: Tenant context holder works correctly")
    void testTenantContextHolder() {
        // Set tenant context
        TenantContextHolder.setTenantId(42L);
        assertEquals(42L, TenantContextHolder.getTenantId());

        // Clear tenant context
        TenantContextHolder.clear();
        assertNull(TenantContextHolder.getTenantId(),
                "Tenant context should be null after clear");
    }

    @Test
    @Order(23)
    @Transactional
    @DisplayName("PART 3: Duplicate tenant code rejected")
    void testDuplicateTenantCodeRejected() {
        createTestTenant("MT-TEST-DUP", "First Tenant");

        assertThrows(RuntimeException.class, () ->
                tenantService.createTenant("MT-TEST-DUP", "Duplicate Tenant", LocalDate.now()),
                "Duplicate tenant code should be rejected");
    }

    @Test
    @Order(24)
    @Transactional
    @DisplayName("PART 3: Each tenant has independent business date")
    void testTenantIndependentBusinessDate() {
        Tenant tenant1 = createTestTenant("MT-TEST-BD1", "Bank Alpha");
        Tenant tenant2 = createTestTenant("MT-TEST-BD2", "Bank Beta");

        // Both start with today's date
        assertEquals(LocalDate.now(), tenantService.getCurrentBusinessDate(tenant1.getId()));
        assertEquals(LocalDate.now(), tenantService.getCurrentBusinessDate(tenant2.getId()));

        // Start day closing and advance tenant1
        tenantService.startDayClosing(tenant1.getId());
        tenantService.closeDayAndAdvance(tenant1.getId());

        // Tenant1 should have advanced, tenant2 unchanged
        assertEquals(LocalDate.now().plusDays(1), tenantService.getCurrentBusinessDate(tenant1.getId()));
        assertEquals(LocalDate.now(), tenantService.getCurrentBusinessDate(tenant2.getId()));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PART 4: STRICT DAY-END CONTROL
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(30)
    @Transactional
    @DisplayName("PART 4: BusinessDayClosedException thrown when day is not OPEN")
    void testBusinessDayClosedException() {
        Tenant tenant = createTestTenant("EOD-TEST-01", "EOD Test Bank 1");

        // Start day closing
        tenantService.startDayClosing(tenant.getId());

        // Attempting to validate should throw exception
        assertThrows(BusinessDayClosedException.class, () ->
                tenantService.validateBusinessDayOpen(tenant.getId()),
                "Should throw BusinessDayClosedException when day is in DAY_CLOSING");
    }

    @Test
    @Order(31)
    @Transactional
    @DisplayName("PART 4: Day status transitions OPEN -> DAY_CLOSING -> OPEN (after advance)")
    void testDayStatusTransitions() {
        Tenant tenant = createTestTenant("EOD-TEST-02", "EOD Test Bank 2");

        // Initially OPEN
        Tenant initial = tenantService.getTenantById(tenant.getId());
        assertEquals(DayStatus.OPEN, initial.getDayStatus());

        // Transition to DAY_CLOSING
        tenantService.startDayClosing(tenant.getId());
        Tenant dayClosing = tenantService.getTenantById(tenant.getId());
        assertEquals(DayStatus.DAY_CLOSING, dayClosing.getDayStatus());

        // Close and advance - should be CLOSED (requires Day Begin to re-open)
        tenantService.closeDayAndAdvance(tenant.getId());
        Tenant advanced = tenantService.getTenantById(tenant.getId());
        assertEquals(DayStatus.CLOSED, advanced.getDayStatus(),
                "After closeDayAndAdvance, status must be CLOSED (requires Day Begin to open)");
        assertEquals(LocalDate.now().plusDays(1), advanced.getCurrentBusinessDate());
    }

    @Test
    @Order(32)
    @Transactional
    @DisplayName("PART 4: Cannot start day closing when not OPEN")
    void testCannotStartDayClosingWhenNotOpen() {
        Tenant tenant = createTestTenant("EOD-TEST-03", "EOD Test Bank 3");

        // Start day closing first time
        tenantService.startDayClosing(tenant.getId());

        // Attempting to start day closing again should fail
        assertThrows(RuntimeException.class, () ->
                tenantService.startDayClosing(tenant.getId()),
                "Should not be able to start day closing when already in DAY_CLOSING");
    }

    @Test
    @Order(33)
    @Transactional
    @DisplayName("PART 4: Cannot close day when not in DAY_CLOSING")
    void testCannotCloseDayWhenNotDayClosing() {
        Tenant tenant = createTestTenant("EOD-TEST-04", "EOD Test Bank 4");

        // Attempting to close day when OPEN should fail
        assertThrows(RuntimeException.class, () ->
                tenantService.closeDayAndAdvance(tenant.getId()),
                "Should not be able to close day when status is OPEN");
    }

    @Test
    @Order(34)
    @Transactional
    @DisplayName("PART 4: validate business day open passes for OPEN tenant")
    void testValidateBusinessDayOpenPasses() {
        Tenant tenant = createTestTenant("EOD-TEST-05", "EOD Test Bank 5");

        // Should not throw when day is OPEN
        assertDoesNotThrow(() ->
                tenantService.validateBusinessDayOpen(tenant.getId()),
                "Should not throw when day status is OPEN");
    }

    // ════════════════════════════════════════════════════════════════════════
    // PART 5: PER-TENANT SETTLEMENT AND BUSINESS DATE ADVANCEMENT
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @Transactional
    @DisplayName("PART 5: Settlement advances only target tenant's business date")
    void testSettlementAdvancesOnlyTargetTenant() {
        Tenant tenantA = createTestTenant("SET-TEST-01A", "Settlement Bank A");
        Tenant tenantB = createTestTenant("SET-TEST-01B", "Settlement Bank B");
        LocalDate today = LocalDate.now();

        // Create and close batches for tenantA
        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenantA.getId(), TransactionChannel.ATM, today);
        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("1000.0000"), new BigDecimal("1000.0000"));

        // Perform settlement-like operations for tenantA only
        tenantService.startDayClosing(tenantA.getId());
        batchService.closeAllBatches(tenantA.getId(), today);
        batchService.settleAllBatches(tenantA.getId(), today);
        tenantService.closeDayAndAdvance(tenantA.getId());

        // TenantA should have advanced
        assertEquals(today.plusDays(1), tenantService.getCurrentBusinessDate(tenantA.getId()),
                "Settled tenant should have advanced business date");

        // TenantB should be unchanged
        assertEquals(today, tenantService.getCurrentBusinessDate(tenantB.getId()),
                "Non-settled tenant business date should be unchanged");
        assertEquals(DayStatus.OPEN, tenantService.getTenantById(tenantB.getId()).getDayStatus(),
                "Non-settled tenant day status should still be OPEN");
    }

    @Test
    @Order(41)
    @Transactional
    @DisplayName("PART 5: Batch close only affects target tenant")
    void testBatchCloseOnlyAffectsTargetTenant() {
        Tenant tenantA = createTestTenant("SET-TEST-02A", "Close Bank A");
        Tenant tenantB = createTestTenant("SET-TEST-02B", "Close Bank B");
        LocalDate today = LocalDate.now();

        // Create batches for both tenants
        TransactionBatch batchA = batchService.getOrCreateOpenBatch(
                tenantA.getId(), TransactionChannel.ONLINE, today);
        TransactionBatch batchB = batchService.getOrCreateOpenBatch(
                tenantB.getId(), TransactionChannel.ONLINE, today);

        batchService.updateBatchTotals(batchA.getId(),
                new BigDecimal("500.0000"), new BigDecimal("500.0000"));
        batchService.updateBatchTotals(batchB.getId(),
                new BigDecimal("700.0000"), new BigDecimal("700.0000"));

        // Close only tenantA batches
        batchService.closeAllBatches(tenantA.getId(), today);

        // TenantA's batch should be closed
        TransactionBatch closedA = batchRepository.findById(batchA.getId()).orElseThrow();
        assertEquals(BatchStatus.CLOSED, closedA.getStatus());

        // TenantB's batch should still be OPEN
        TransactionBatch stillOpenB = batchRepository.findById(batchB.getId()).orElseThrow();
        assertEquals(BatchStatus.OPEN, stillOpenB.getStatus(),
                "Tenant B batch should still be OPEN");
    }

    @Test
    @Order(42)
    @Transactional
    @DisplayName("PART 5: Are all batches closed check works")
    void testAreAllBatchesClosed() {
        Tenant tenant = createTestTenant("SET-TEST-03", "Check Bank");
        LocalDate today = LocalDate.now();

        // Create a batch
        TransactionBatch batch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.TELLER, today);
        batchService.updateBatchTotals(batch.getId(),
                new BigDecimal("100.0000"), new BigDecimal("100.0000"));

        // Not all closed yet
        assertFalse(batchService.areAllBatchesClosed(tenant.getId(), today),
                "Should return false when there are open batches");

        // Close batches
        batchService.closeAllBatches(tenant.getId(), today);

        // Now all should be closed
        assertTrue(batchService.areAllBatchesClosed(tenant.getId(), today),
                "Should return true when all batches are closed");
    }

    @Test
    @Order(43)
    @Transactional
    @DisplayName("PART 5: Multiple channels create separate batches per tenant")
    void testMultipleChannelsBatches() {
        Tenant tenant = createTestTenant("SET-TEST-04", "Multi-Channel Bank");
        LocalDate today = LocalDate.now();

        TransactionBatch atmBatch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ATM, today);
        TransactionBatch onlineBatch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.ONLINE, today);
        TransactionBatch tellerBatch = batchService.getOrCreateOpenBatch(
                tenant.getId(), TransactionChannel.TELLER, today);

        // All should be different batches
        assertNotEquals(atmBatch.getId(), onlineBatch.getId());
        assertNotEquals(onlineBatch.getId(), tellerBatch.getId());
        assertNotEquals(atmBatch.getId(), tellerBatch.getId());

        // Verify batch types
        assertEquals(BatchType.ATM, atmBatch.getBatchType());
        assertEquals(BatchType.ONLINE, onlineBatch.getBatchType());
        assertEquals(BatchType.BRANCH_CASH, tellerBatch.getBatchType());

        // All batches should belong to the tenant
        List<TransactionBatch> tenantBatches = batchService.getAllBatchesByTenant(tenant.getId());
        assertEquals(3, tenantBatches.size(), "Should have 3 batches for different channels");
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════════════════

    private Tenant createTestTenant(String code, String name) {
        return tenantService.createTenant(code, name, LocalDate.now());
    }
}
