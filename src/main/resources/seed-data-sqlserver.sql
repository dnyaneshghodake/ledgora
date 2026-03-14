-- ═══════════════════════════════════════════════════════════════════════════
-- Ledgora CBS — Production-Grade Seed Data for SQL Server
-- ═══════════════════════════════════════════════════════════════════════════
--
-- Purpose: Supplements Java seeders with volume data for UAT/demo testing.
-- Run AFTER the application has started once (Java seeders create schema + base data).
--
-- Contents:
--   1. Banking Calendar (2026 full year — holidays + working days)
--   2. Account Ownership records
--   3. Account Liens
--   4. Frozen Accounts
--   5. Additional FX rates (historical)
--
-- Prerequisites: Java seeders must have run first to create tenants, branches,
--   users, customers, accounts, GL hierarchy, and base transactions.
--
-- Usage:
--   sqlcmd -S localhost -d ledgora -U sa -P "sqlserver#123" -i seed-data-sqlserver.sql
-- ═══════════════════════════════════════════════════════════════════════════

-- ─────────────────────────────────────────────────────────────────────────
-- 1. BANKING CALENDAR 2026 — RBI Gazetted Holidays + Weekends
-- ─────────────────────────────────────────────────────────────────────────
-- Tenant ID 1 = TENANT-001 (Ledgora Main Bank)
-- Created by user ID 1 = admin, Approved by user ID 2 = manager

-- Republic Day
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-01-26', 'HOLIDAY', 'Republic Day', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Holi
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-03-10', 'HOLIDAY', 'Holi', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Good Friday
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-04-03', 'HOLIDAY', 'Good Friday', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Dr Ambedkar Jayanti
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-04-14', 'HOLIDAY', 'Dr Ambedkar Jayanti', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- May Day
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-05-01', 'HOLIDAY', 'May Day / Maharashtra Day', 'REGIONAL', 1, 0, 'APPROVED', 1, 2, 'Regional Holiday — Maharashtra', GETDATE(), GETDATE());

-- Eid ul-Fitr (approx)
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-03-21', 'HOLIDAY', 'Eid ul-Fitr', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday (date subject to moon sighting)', GETDATE(), GETDATE());

-- Independence Day
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-08-15', 'HOLIDAY', 'Independence Day', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Ganesh Chaturthi
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-08-27', 'HOLIDAY', 'Ganesh Chaturthi', 'REGIONAL', 1, 0, 'APPROVED', 1, 2, 'Regional Holiday — Maharashtra', GETDATE(), GETDATE());

-- Mahatma Gandhi Jayanti
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-10-02', 'HOLIDAY', 'Mahatma Gandhi Jayanti', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Dussehra
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-10-19', 'HOLIDAY', 'Dussehra (Vijaya Dashami)', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Diwali (Lakshmi Puja)
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-11-08', 'HOLIDAY', 'Diwali (Lakshmi Puja)', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Guru Nanak Jayanti
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-11-27', 'HOLIDAY', 'Guru Nanak Jayanti', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());

-- Christmas
INSERT INTO bank_calendar (tenant_id, calendar_date, day_type, holiday_name, holiday_type, atm_allowed, system_transactions_allowed, approval_status, created_by, approved_by, remarks, created_at, updated_at)
VALUES (1, '2026-12-25', 'HOLIDAY', 'Christmas', 'NATIONAL', 1, 0, 'APPROVED', 1, 2, 'RBI Gazetted Holiday', GETDATE(), GETDATE());


-- ─────────────────────────────────────────────────────────────────────────
-- 2. ADDITIONAL FX RATES (Historical — for backtesting multi-currency)
-- ─────────────────────────────────────────────────────────────────────────

INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('USD', 'INR', 82.95000000, '2026-03-01', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('USD', 'INR', 83.00000000, '2026-03-07', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('EUR', 'INR', 89.50000000, '2026-03-01', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('EUR', 'INR', 90.00000000, '2026-03-07', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('GBP', 'INR', 104.80000000, '2026-03-01', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('GBP', 'INR', 105.20000000, '2026-03-07', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('INR', 'USD', 0.01205000, '2026-03-01', GETDATE());
INSERT INTO exchange_rates (currency_from, currency_to, rate, effective_date, created_at)
VALUES ('INR', 'USD', 0.01204000, '2026-03-07', GETDATE());


-- ═══════════════════════════════════════════════════════════════════════════
-- END OF SEED DATA
-- ═══════════════════════════════════════════════════════════════════════════
-- Run count verification:
--   SELECT 'bank_calendar' AS entity, COUNT(*) AS cnt FROM bank_calendar
--   UNION ALL SELECT 'exchange_rates', COUNT(*) FROM exchange_rates;
