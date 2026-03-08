# RBI UI Compliance Report — Ledgora

## Overall Risk Level: **HIGH**

This audit is restricted to current UI/controller behavior (no refactoring performed). Findings are evidence-based from JSP and mapped controller/security code.

---

## 1. Ledger Integrity

**Status:** **Partially Compliant**

**Issues:**
- Ledger explorer is read-only and exposes filter/query + viewing only (no edit/delete controls in ledger UI).  
- Controller for ledger explorer only provides GET endpoints (view + JSON read), with no write endpoint in that controller.  
- Transaction details page explicitly states ledger immutability and treats ledger entries as system of record.  

**Risk Level:** **LOW**

**Recommendations:**
- Keep ledger explorer read-only.
- Continue enforcing reversal via voucher lifecycle screens.

---

## 2. Maker-Checker

**Status:** **Partially Compliant**

**Issues:**
- Voucher nav separates create vs pending-authorization visibility, but `ADMIN`/`MANAGER` can see both create and pending authorization paths from the same UI menu.
- Pending voucher actions are role-gated in UI (`Checker/Admin/Manager`) and use POST forms for authorize/reject.
- Voucher service enforces maker-checker separation at business-rule level by rejecting same maker/checker user.
- Approval screens show status badges and decision actions.

**Risk Level:** **HIGH**

**Recommendations:**
- Tighten UI role matrix so create and authorize actions are mutually exclusive for operational roles where RBI policy requires strict segregation.
- Keep service-level maker-checker enforcement as defense in depth.

---

## 3. Business-Day Lock Visibility

**Status:** **Mostly Compliant**

**Issues:**
- Header shows business date and date status (OPEN/DAY_CLOSING/CLOSED).
- Closed-day banner is rendered when status is CLOSED.
- Client-side lock disables `.cbs-lockable` elements when EOD banner is present.
- Server-side transaction service enforces `validateBusinessDayOpen(...)` before posting and routes business-day-closed exceptions to friendly error page.

**Risk Level:** **LOW**

**Recommendations:**
- Keep server-side gating as authoritative control (already present).
- Expand `.cbs-lockable` coverage to all relevant financial action forms/pages over time.

---

## 4. Batch Lifecycle Visibility

**Status:** **Compliant**

**Issues:**
- Batch dashboard clearly displays OPEN/CLOSED/SETTLED statuses, debit/credit totals, balanced indicator, and transaction counts.
- Batch code and scroll inquiry entry points are visible in navigation.
- Service layer prevents updating non-OPEN batches and enforces debit/credit equality before settlement.

**Risk Level:** **LOW**

**Recommendations:**
- Optionally expose explicit “posting allowed only in OPEN” helper text on additional financial forms for operator clarity.

---

## 5. Tenant Isolation UI Safety

**Status:** **Partially Compliant**

**Issues:**
- Header shows tenant context and tenant switch is displayed only for `tenantScope == MULTI`.
- Tenant switch endpoint is a GET route that mutates session tenant context.
- Security config does not have explicit authorization rule for `/tenant/**` beyond authenticated users (`anyRequest().authenticated()`), and controller lacks role annotations.
- Voucher inquiry table includes a tenant column, which improves visibility but does not itself guarantee backend filtering.

**Risk Level:** **CRITICAL**

**Recommendations:**
- Restrict tenant-switch route by role/scope at controller/security level.
- Convert state-changing tenant switch to POST + CSRF protection.
- Add explicit backend authorization check that requested tenant is within allowed `availableTenants` for the authenticated user.

---

## 6. Shadow vs Actual Balance Clarity

**Status:** **Compliant**

**Issues:**
- Account view clearly separates Actual Total, Actual Cleared, Shadow, Lien, Charge Hold, and Available balance semantics.
- UI shows these as informational cards; no direct balance-edit control found in account detail screen.

**Risk Level:** **LOW**

**Recommendations:**
- Keep this explicit nomenclature and tooltip pattern across all account and statement screens.

---

## 7. Error Handling Discipline

**Status:** **Compliant**

**Issues:**
- Centralized global exception handler maps business, access, insufficient-balance, business-day-closed, and tenant-isolation errors to a friendly error JSP.
- Error page displays sanitized fields (error code, correlation ID, tenant/branch/business date context) and no stack trace.

**Risk Level:** **LOW**

**Recommendations:**
- Continue avoiding raw exception leakage in JSP/model attributes.

---

## 8. Security UI Hygiene

**Status:** **Non-Compliant**

**Issues:**
- CSRF is globally disabled in security config.
- Many sensitive POST forms (voucher authorize/reject, approval approve/reject, EOD run, account status toggle) have no CSRF token fields.
- Tenant switch is state-changing via GET.

**Risk Level:** **HIGH**

**Recommendations:**
- Enable CSRF and add token support to all JSP forms.
- Keep financial mutations strictly POST/PUT/PATCH with CSRF.
- Remove state-changing GET endpoints.

---

## 9. Audit Trail Visibility

**Status:** **Partially Compliant**

**Issues:**
- Transaction detail includes maker, business date, value date, timestamps, journal references, and audit trail navigation link.
- Approval pages show requester, approver, and created/approved timestamps.
- Voucher posted/pending/cancelled lists do not consistently show maker/checker columns directly in those tables.

**Risk Level:** **MEDIUM**

**Recommendations:**
- Add maker/checker/posting timestamp columns in voucher lifecycle screens for first-page auditability.

---

## 10. Operational Usability (Banking Standard)

**Status:** **Mostly Compliant**

**Issues:**
- Breadcrumb framework exists (rendered when breadcrumb model is provided).
- Scroll number and batch code are visible in voucher screens.
- Business date is globally visible in header.
- GL inquiry is separated from account inquiry in sidebar.
- Reports are available from dedicated menu/dashboard.
- `Run EOD` POST form exists in JSP, but `EodController` only defines GET mappings (`/run` GET and no POST handler in this controller), indicating a possible UI-action mismatch depending on other route handling.

**Risk Level:** **MEDIUM**

**Recommendations:**
- Ensure EOD run UI action maps to an actual POST handler for predictable operator workflow.
- Keep breadcrumb model consistently populated from all major controllers.

---

## Critical Violations Summary

1. **Tenant isolation control gap in tenant switching:** state change via GET and no explicit role/scope check in route/controller-level authorization.

---

## Immediate Fix Recommendations (Top 5)

1. Enforce explicit authorization on `/tenant/switch/{tenantId}` and validate requested tenant against user’s allowed tenant set.
2. Replace tenant switching GET mutation with POST.
3. Re-enable CSRF protection and add token fields to all mutating forms.
4. Enforce stricter UI segregation for maker vs checker capabilities where policy requires hard split.
5. Verify/implement POST handler wiring for EOD run action for deterministic control-room flow.

---

## Long-Term Hardening Recommendations

- Add UI compliance regression checklist for each release (CSRF, mutating verbs, maker-checker visibility, tenant switch authorization).
- Add automated UI/security tests for role-based menu/action visibility and forbidden-path attempts.
- Standardize audit metadata columns across voucher, settlement, and transaction list pages.
- Add explicit compliance banners/tooltips for “read-only ledger” and “open-batch-only posting” across all financial operation forms.

