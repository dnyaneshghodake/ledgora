package com.ledgora.loan.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Loan Module Configuration — Finacle/CBS Tier-1 Loan Management System.
 *
 * <p>Module scope per RBI Master Directions on Lending:
 *
 * <ul>
 *   <li>Loan Product Management (product master, interest rates, GL mappings)
 *   <li>Loan Origination (application → sanction → disbursement)
 *   <li>Repayment Processing (EMI, prepayment, FIFO installment matching)
 *   <li>Interest Accrual (daily, per RBI IRAC — performing loans only)
 *   <li>NPA Classification (DPD-based, per RBI Prudential Norms)
 *   <li>Provisioning (STANDARD/SUBSTANDARD/DOUBTFUL/LOSS tiers)
 *   <li>Write-Off (100% provisioned LOSS loans)
 *   <li>Restructuring (future — moratorium, tenure extension)
 * </ul>
 *
 * <p>All financial mutations flow through the voucher engine — no direct balance mutation.
 * Tenant-scoped, RBAC-enforced, audit-trail-linked.
 */
@Configuration
@ComponentScan(basePackages = "com.ledgora.loan")
public class LoanModuleConfig {
    // Module-level bean definitions can be added here as needed.
    // Current services use constructor injection via @Service annotation.
}
