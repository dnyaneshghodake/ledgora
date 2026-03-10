# Ledgora — Role-Based Navigation Alignment & SoD Enforcement

**Version:** 1.0
**Baseline:** PR #43 — Feature Enhancement (sidebar commit `efb6682c`)
**Standard:** RBI IT Governance §4.2 (Segregation of Duties), IS Audit §5.2

## Purpose

Document how the sidebar navigation enforces CBS-grade role-based visibility and RBI-compliant Segregation of Duties (SoD). The sidebar is the primary UI surface for access control — links hidden from unauthorized roles prevent accidental or intentional access to restricted features.

**Important:** Sidebar visibility is a **UI convenience control**, not the security boundary. The authoritative access control is `@PreAuthorize` on each controller method. If a user manually types a URL, Spring Security blocks unauthorized access regardless of sidebar visibility.

## Navigation Visibility Matrix

| Sidebar Section | ADMIN | MANAGER | OPERATIONS | MAKER | CHECKER | AUDITOR | TELLER |
|---|---|---|---|---|---|---|---|
| **Dashboard** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Pending Approvals | ✅ | ✅ | — | — | ✅ | — | — |
| EOD Status | ✅ | ✅ | — | — | — | — | — |
| **Customer Management** | ✅ | ✅ | — | ✅ | ✅ | — | — |
| New Customer | ✅ | ✅ | — | ✅ | — | — | — |
| Customer Approval Queue | ✅ | ✅ | — | — | ✅ | — | — |
| **Account Management** | ✅ | ✅ | — | ✅ | — | — | ✅ |
| Open New Account | ✅ | ✅ | — | ✅ | — | — | — |
| **Transactions** | ✅ | ✅ | — | ✅ | — | — | ✅ |
| Deposit/Withdraw/Transfer | ✅ | ✅ | — | ✅ | — | — | ✅ |
| High-Value Approval Queue | ✅ | ✅ | — | — | ✅ | — | — |
| **Voucher Operations** | ✅ | ✅ | — | ✅ | ✅ | — | ✅ |
| Create Voucher | ✅ | ✅ | — | ✅ | — | — | ✅ |
| Pending Authorization | ✅ | ✅ | — | — | ✅ | — | — |
| **Inter-Branch Transfer** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| New IBT | ✅ | ✅ | — | ✅ | — | — | ✅ |
| IBT List | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| IBT Reconciliation | ✅ | ✅ | ✅ | — | — | ✅ | — |
| Clearing Engine | ✅ | ✅ | ✅ | — | — | — | — |
| **Suspense GL** | ✅ | ✅ | ✅ | — | — | ✅ | — |
| Suspense Dashboard | ✅ | ✅ | ✅ | — | — | ✅ | — |
| **Risk & Fraud** | ✅ | ✅ | ✅ | — | — | ✅ | — |
| Hard Ceiling Monitor | ✅ | ✅ | ✅ | — | — | ✅ | — |
| Velocity Fraud Monitor | ✅ | ✅ | ✅ | — | — | ✅ | — |
| **Ledger & GL** | ✅ | ✅ | ✅ | — | — | — | — |
| **Batch & EOD** | ✅ | ✅ | — | — | — | — | — |
| **Reports** | ✅ | ✅ | ✅ | — | — | — | — |
| **Audit & Governance** | ✅ | ✅ | — | — | — | ✅ | — |
| Audit Log Explorer | ✅ | — | — | — | — | ✅ | — |
| Approval Queue | ✅ | ✅ | — | — | ✅ | — | — |
| **Diagnostics** | ✅ | — | — | — | — | — | — |
| Query Plan Analyzer | ✅ | — | — | — | — | — | — |
| Concurrency Audit | ✅ | — | — | — | — | — | — |
| Enterprise Certification | ✅ | — | — | — | — | — | — |
| **Admin Settings** | ✅ | — | — | — | — | — | — |

## Segregation of Duties (SoD) Enforcement

### RBI Requirement: Maker ≠ Checker

| Rule | How sidebar enforces it |
|---|---|
| Maker must NOT see authorize/approve links | "Pending Authorization" hidden from MAKER. "Customer Approval Queue" hidden from MAKER. "High-Value Approval Queue" hidden from MAKER. |
| Checker must NOT see create/initiate links | "Create Voucher" hidden from CHECKER. "New IBT" hidden from CHECKER. "Deposit/Withdraw/Transfer" hidden from CHECKER. "Open New Account" hidden from CHECKER. |
| Auditor must NOT see POST/mutate links | All create/approve/execute links hidden from AUDITOR. Only view/list/dashboard links visible. |

### Risk Feature Isolation

| Rule | How sidebar enforces it |
|---|---|
| Risk dashboards hidden from MAKER & TELLER | Risk & Fraud section requires `isAdmin \|\| isManager \|\| isOperations \|\| isAuditor` |
| Diagnostics visible ONLY to ADMIN | Diagnostics section requires `isAdmin` only |
| Certification endpoint visible ONLY to ADMIN | Same as Diagnostics |
| EOD run button hidden from AUDITOR | Batch & EOD section requires `isAdmin \|\| isManager \|\| isBranchManager` — AUDITOR excluded |

### Clearing/Settlement Isolation

| Rule | How sidebar enforces it |
|---|---|
| Clearing Engine hidden from AUDITOR | Requires `isOperations \|\| isAdmin \|\| isManager` — AUDITOR excluded |
| IBT Reconciliation visible to AUDITOR (read-only) | AUDITOR can view reconciliation dashboard but cannot execute settlement |

## Implementation Pattern

The sidebar uses `sessionScope.isXxx` boolean flags set by `CustomAuthenticationSuccessHandler` on login. These flags map to Spring Security roles:

| Session Flag | Spring Security Role |
|---|---|
| `sessionScope.isAdmin` | `ROLE_ADMIN` |
| `sessionScope.isManager` | `ROLE_MANAGER` |
| `sessionScope.isOperations` | `ROLE_OPERATIONS` |
| `sessionScope.isMaker` | `ROLE_MAKER` |
| `sessionScope.isChecker` | `ROLE_CHECKER` |
| `sessionScope.isAuditor` | `ROLE_AUDITOR` |
| `sessionScope.isTeller` | `ROLE_TELLER` |

Each sidebar section wraps its visibility in `<c:if>` blocks:

```jsp
<c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isOperations}">
    <!-- Section visible only to these roles -->
</c:if>
```

Sub-items within a section can have more restrictive visibility:

```jsp
<%-- Section visible to many roles --%>
<c:if test="${sessionScope.isAdmin || ... || sessionScope.isTeller}">
    <%-- But create link only for makers --%>
    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
        <a href="/ibt/create">New IBT</a>
    </c:if>
</c:if>
```

## Defense in Depth

Sidebar visibility is **Layer 1** of a 3-layer access control:

| Layer | Mechanism | What it prevents |
|---|---|---|
| **Layer 1: Sidebar** | `<c:if>` on session flags | Unauthorized links not rendered — prevents accidental navigation |
| **Layer 2: Controller** | `@PreAuthorize("hasAnyRole(...)")` | Blocks HTTP request even if URL typed manually |
| **Layer 3: Service** | Business rule validation (maker ≠ checker, etc.) | Blocks execution even if controller check is bypassed |

**Layer 2 is authoritative.** If a sidebar `<c:if>` is misconfigured (e.g., shows a link to the wrong role), the `@PreAuthorize` on the controller will still block access. The sidebar is a convenience/UX layer, not a security boundary.

## New Sections Added (PR #43)

| Section | Commit | Sidebar Location |
|---|---|---|
| Inter-Branch Transfer | `efb6682c` | After Voucher Operations |
| Suspense GL | `efb6682c` | After IBT |
| Risk & Fraud | `efb6682c` | After Suspense GL |
| Audit Log Explorer | `efb6682c` | Inside Audit & Governance |
| Diagnostics | `efb6682c` | Before Admin section |

Version bumped from v2.0 to v2.7 in sidebar footer.
