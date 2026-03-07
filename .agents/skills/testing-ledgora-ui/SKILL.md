# Testing Ledgora CBS UI

## Local Development Setup

1. Ensure Java 17 is installed
2. Install Maven if not available: `sudo apt-get install -y maven`
3. Start the app: `mvn spring-boot:run` from the repo root
4. App runs on `http://localhost:8080`
5. Uses H2 in-memory database (dev profile) - data resets on restart

## Seed Data & Login Credentials

DataInitializer runs on startup and creates:

| Username | Password | Role | Branch |
|----------|----------|------|--------|
| admin | admin123 | ROLE_ADMIN | HQ001 |
| manager | manager123 | ROLE_MANAGER | HQ001 |
| teller1 | teller123 | ROLE_TELLER | BR001 |
| teller2 | teller123 | ROLE_TELLER | BR002 |
| customer1 | cust123 | ROLE_CUSTOMER | BR001 |
| customer2 | cust123 | ROLE_CUSTOMER | BR001 |
| customer3 | cust123 | ROLE_CUSTOMER | BR002 |
| customer4 | cust123 | ROLE_CUSTOMER | BR002 |

Seed data also includes: 4 sample transactions, GL hierarchy (29 accounts), 4 customers, 15 accounts, exchange rates, and idempotency keys.

## Authentication Flow

- Login page: `/login` (form-based login)
- Login processing: POST `/perform_login`
- Success redirects to `/dashboard`
- CSRF is **disabled** in SecurityConfig
- JWT token is stored in session for API calls
- `CustomAuthenticationSuccessHandler` sets session attributes: `username`, `userRoles`, `isAdmin`, `isManager`, `isTeller`, `isCustomer`

## Session Attributes for Header

The header JSP reads these session attributes:
- `sessionScope.username` - set by auth handler
- `sessionScope.isAdmin`, `isManager`, `isTeller`, `isCustomer` - set by auth handler
- `sessionScope.environment` - NOT set by auth handler (defaults to empty/DEV in JSP)
- `sessionScope.businessDateStatus` - NOT set by auth handler (may need a filter/interceptor)
- `sessionScope.ledgerHealth` - NOT set by auth handler (may need a filter/interceptor)
- `sessionScope.isFinance` - NOT set (no ROLE_FINANCE exists in seed data)

If header indicators don't show dynamic values, check whether a servlet filter or Spring interceptor has been added to populate these session attributes.

## Key URLs for Testing

| Page | URL | Required Role |
|------|-----|---------------|
| Login | `/login` | Public |
| Dashboard | `/dashboard` | Any authenticated |
| Transactions | `/transactions` | Any authenticated |
| Transaction Detail | `/transactions/{id}` | Any authenticated |
| Ledger Explorer | `/ledger/explorer` | Any authenticated |
| Settlement Dashboard | `/settlement/dashboard` | Any authenticated |
| Settlement History | `/settlements` | Any authenticated |
| Chart of Accounts | `/gl` | Any authenticated |
| Customers | `/customers` | Any authenticated |
| Admin Audit Logs | `/admin/audit-logs` | ROLE_ADMIN |
| H2 Console | `/h2-console` | Public |

## Role-Based Sidebar Visibility

Test with different users to verify sidebar sections:
- **Admin**: All 12 sections visible
- **Manager**: Dashboard, Customer Mgmt, Accounts, Transactions, Payments, Ledger, Settlement, Approvals, Reports
- **Teller**: Dashboard, Accounts, Transactions (limited), Payments
- **Customer**: Dashboard, Accounts (own), Transaction History
- **Finance**: Ledger, Reports, Validation (requires ROLE_FINANCE which doesn't exist in seed data)

## Known Behaviors

- Business date initializes to current date with OPEN status on each app restart
- Ledger entries are immutable - the system enforces SUM(debits) = SUM(credits)
- Settlement process is a 7-step workflow that changes business date status: OPEN -> DAY_CLOSING -> CLOSED -> next day OPEN
- The Ledger Explorer AJAX endpoint (`/ledger/journal/{id}/entries`) returns JSON and may encounter LazyInitializationException if JPA session is closed. If this happens, add `@Transactional(readOnly = true)` to the controller method.

## Devin Secrets Needed

No secrets required - all credentials are seeded by DataInitializer in the H2 dev database.
