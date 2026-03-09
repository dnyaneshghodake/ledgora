<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<%--
  Role-Aware Dashboard
  Panels and quick actions are shown/hidden based on the user's role.
  Role flags are set by DashboardController (model attrs) and CustomAuthenticationSuccessHandler (session attrs).
  - ADMIN:    Full metrics — accounts, users, transactions, settlements, daily flows, system info
  - MANAGER:  Account overview, transaction approvals, settlements, reports
  - TELLER:   Branch stats, today's transactions, customer account view
  - CUSTOMER: Own account summary, transaction history
--%>

<div class="row mb-4">
    <div class="col-12">
        <h3>
            <i class="bi bi-speedometer2"></i> Dashboard
            <c:if test="${isAdmin}"><small class="text-muted fs-6">&mdash; Administrator View</small></c:if>
            <c:if test="${isManager}"><small class="text-muted fs-6">&mdash; Manager View</small></c:if>
            <c:if test="${isTeller}"><small class="text-muted fs-6">&mdash; Teller View</small></c:if>
            <c:if test="${isCustomer && !isAdmin && !isManager && !isTeller}"><small class="text-muted fs-6">&mdash; My Banking</small></c:if>
        </h3>
        <hr>
    </div>
</div>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- ADMIN & MANAGER: Full summary metric cards                            --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<c:if test="${isAdmin || isManager}">
<div class="row mb-4">
    <div class="col-md-3">
        <div class="card bg-primary text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Total Accounts</h6>
                        <h2>${dashboard.totalAccounts}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-wallet2"></i></div>
                </div>
                <small>Active: ${dashboard.activeAccounts}</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card bg-success text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Transactions Today</h6>
                        <h2>${dashboard.todayTransactions}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-arrow-left-right"></i></div>
                </div>
                <small>Total: ${dashboard.totalTransactions}</small>
            </div>
        </div>
    </div>
    <c:if test="${isAdmin}">
    <div class="col-md-3">
        <div class="card bg-info text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Total Users</h6>
                        <h2>${dashboard.totalUsers}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-people"></i></div>
                </div>
            </div>
        </div>
    </div>
    </c:if>
    <div class="col-md-3">
        <div class="card bg-warning text-dark shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Settlements</h6>
                        <h2>${dashboard.completedSettlements}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-check2-square"></i></div>
                </div>
                <small>Pending: ${dashboard.pendingSettlements}</small>
            </div>
        </div>
    </div>
</div>

<%-- Pending Approvals Alert Bar --%>
<c:if test="${dashboard.pendingApprovals > 0}">
<div class="row mb-4">
    <div class="col-12">
        <div class="alert alert-warning d-flex align-items-center shadow-sm mb-0">
            <i class="bi bi-clipboard-check fs-4 me-3"></i>
            <div class="flex-grow-1">
                <strong>${dashboard.pendingApprovals} pending approval(s)</strong>
                <c:if test="${dashboard.pendingTransactionApprovals > 0}">
                    &mdash; including <strong>${dashboard.pendingTransactionApprovals} transaction(s)</strong> awaiting checker
                </c:if>
            </div>
            <a href="${pageContext.request.contextPath}/approvals" class="btn btn-warning btn-sm ms-3">
                <i class="bi bi-arrow-right"></i> Review Queue
            </a>
        </div>
    </div>
</div>
</c:if>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- TELLER: Branch-focused summary cards                                  --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<c:if test="${isTeller && !isAdmin && !isManager}">
<div class="row mb-4">
    <div class="col-md-4">
        <div class="card bg-success text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Today's Transactions</h6>
                        <h2>${dashboard.todayTransactions}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-arrow-left-right"></i></div>
                </div>
                <small>Total: ${dashboard.totalTransactions}</small>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card bg-primary text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Accounts</h6>
                        <h2>${dashboard.totalAccounts}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-wallet2"></i></div>
                </div>
                <small>Active: ${dashboard.activeAccounts}</small>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card bg-info text-white shadow">
            <div class="card-body text-center">
                <h6 class="card-title">Branch Operations</h6>
                <h4 class="mt-2"><i class="bi bi-building"></i> Teller Counter</h4>
            </div>
        </div>
    </div>
</div>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- CUSTOMER: Own account summary                                         --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<c:if test="${isCustomer && !isAdmin && !isManager && !isTeller}">
<div class="row mb-4">
    <div class="col-md-6">
        <div class="card bg-primary text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">My Accounts</h6>
                        <h2>${dashboard.totalAccounts}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-wallet2"></i></div>
                </div>
                <small>Active: ${dashboard.activeAccounts}</small>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card bg-success text-white shadow">
            <div class="card-body">
                <div class="d-flex justify-content-between">
                    <div>
                        <h6 class="card-title">Recent Transactions</h6>
                        <h2>${dashboard.todayTransactions}</h2>
                    </div>
                    <div class="fs-1 opacity-50"><i class="bi bi-clock-history"></i></div>
                </div>
                <small>Today</small>
            </div>
        </div>
    </div>
</div>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- ADMIN & MANAGER & TELLER: Daily flow summary                          --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<c:if test="${isAdmin || isManager || isTeller}">
<div class="row mb-4">
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-body text-center">
                <h6 class="text-muted">Today's Deposits</h6>
                <h3 class="text-success"><i class="bi bi-arrow-down-circle"></i> ${dashboard.totalDeposits} INR</h3>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-body text-center">
                <h6 class="text-muted">Today's Withdrawals</h6>
                <h3 class="text-danger"><i class="bi bi-arrow-up-circle"></i> ${dashboard.totalWithdrawals} INR</h3>
            </div>
        </div>
    </div>
    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-body text-center">
                <h6 class="text-muted">Today's Transfers</h6>
                <h3 class="text-primary"><i class="bi bi-arrow-left-right"></i> ${dashboard.totalTransfers} INR</h3>
            </div>
        </div>
    </div>
</div>
</c:if>

<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<%-- Quick Actions + System Info (role-aware)                              --%>
<%-- ═══════════════════════════════════════════════════════════════════════ --%>
<div class="row">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-lightning"></i> Quick Actions</h5>
            </div>
            <div class="card-body">
                <div class="d-grid gap-2">

                    <%-- ADMIN & MANAGER: Create Account --%>
                    <c:if test="${isAdmin || isManager}">
                    <a href="${pageContext.request.contextPath}/accounts/create" class="btn btn-outline-primary">
                        <i class="bi bi-plus-circle"></i> Create Account
                    </a>
                    </c:if>

                    <%-- ADMIN, MANAGER, TELLER: Deposit / Withdraw / Transfer --%>
                    <c:if test="${isAdmin || isManager || isTeller}">
                    <a href="${pageContext.request.contextPath}/transactions/deposit" class="btn btn-outline-success">
                        <i class="bi bi-arrow-down-circle"></i> Deposit
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/withdraw" class="btn btn-outline-danger">
                        <i class="bi bi-arrow-up-circle"></i> Withdraw
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/transfer" class="btn btn-outline-info">
                        <i class="bi bi-arrow-left-right"></i> Transfer
                    </a>
                    </c:if>

                    <%-- ADMIN & MANAGER: Approval Queue --%>
                    <c:if test="${isAdmin || isManager}">
                    <a href="${pageContext.request.contextPath}/approvals" class="btn btn-outline-warning">
                        <i class="bi bi-clipboard-check"></i> Approval Queue
                        <c:if test="${dashboard.pendingApprovals > 0}">
                            <span class="badge bg-danger ms-1">${dashboard.pendingApprovals}</span>
                        </c:if>
                    </a>
                    </c:if>

                    <%-- ADMIN & MANAGER: Day Begin / EOD --%>
                    <c:if test="${isAdmin || isManager}">
                    <a href="${pageContext.request.contextPath}/eod/status" class="btn btn-outline-secondary">
                        <i class="bi bi-calendar-check"></i> Day Status / EOD
                    </a>
                    </c:if>

                    <%-- ADMIN & MANAGER: Process Settlement --%>
                    <c:if test="${isAdmin || isManager}">
                    <a href="${pageContext.request.contextPath}/settlements/process" class="btn btn-outline-warning">
                        <i class="bi bi-check2-square"></i> Process Settlement
                    </a>
                    </c:if>

                    <%-- CUSTOMER: View Accounts & Transaction History --%>
                    <c:if test="${isCustomer && !isAdmin && !isManager && !isTeller}">
                    <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-primary">
                        <i class="bi bi-wallet2"></i> View My Accounts
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-success">
                        <i class="bi bi-clock-history"></i> Transaction History
                    </a>
                    </c:if>

                </div>
            </div>
        </div>
    </div>

    <%-- System Info — ADMIN and MANAGER only --%>
    <c:if test="${isAdmin || isManager}">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-info-circle"></i> System Info</h5>
            </div>
            <div class="card-body">
                <table class="table table-borderless mb-0">
                    <tr><td class="text-muted">Platform</td><td>Ledgora Core Banking</td></tr>
                    <tr><td class="text-muted">Version</td><td>1.0.0</td></tr>
                    <tr><td class="text-muted">Architecture</td><td>Monolithic (Spring Boot)</td></tr>
                    <tr><td class="text-muted">Database</td><td>H2 (Dev) / SQL Server (Prod)</td></tr>
                    <tr><td class="text-muted">Accounting</td><td>Double-Entry Ledger</td></tr>
                    <tr><td class="text-muted">Authentication</td><td>JWT + Session</td></tr>
                </table>
            </div>
        </div>
    </div>
    </c:if>

    <%-- CUSTOMER: Account help panel instead of System Info --%>
    <c:if test="${isCustomer && !isAdmin && !isManager && !isTeller}">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-question-circle"></i> Help & Support</h5>
            </div>
            <div class="card-body">
                <table class="table table-borderless mb-0">
                    <tr><td class="text-muted">Need Help?</td><td>Contact your branch manager</td></tr>
                    <tr><td class="text-muted">Account Issues</td><td>Visit your nearest branch</td></tr>
                    <tr><td class="text-muted">Phone Banking</td><td>1800-LEDGORA (toll-free)</td></tr>
                    <tr><td class="text-muted">Email</td><td>support@ledgora.com</td></tr>
                </table>
            </div>
        </div>
    </div>
    </c:if>

    <%-- TELLER: Branch operations panel --%>
    <c:if test="${isTeller && !isAdmin && !isManager}">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-building"></i> Teller Operations</h5>
            </div>
            <div class="card-body">
                <table class="table table-borderless mb-0">
                    <tr><td class="text-muted">Role</td><td>Branch Teller</td></tr>
                    <tr><td class="text-muted">Capabilities</td><td>Deposits, Withdrawals, Transfers</td></tr>
                    <tr><td class="text-muted">Customer Lookup</td><td>Account search by name or number</td></tr>
                    <tr><td class="text-muted">Transaction Limit</td><td>Per-transaction limits apply</td></tr>
                </table>
            </div>
        </div>
    </div>
    </c:if>
</div>

<%@ include file="../layout/footer.jsp" %>
