<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-speedometer2"></i> Dashboard</h3>
        <hr>
    </div>
</div>

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

<div class="row">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-lightning"></i> Quick Actions</h5>
            </div>
            <div class="card-body">
                <div class="d-grid gap-2">
                    <a href="${pageContext.request.contextPath}/accounts/create" class="btn btn-outline-primary">
                        <i class="bi bi-plus-circle"></i> Create Account
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/deposit" class="btn btn-outline-success">
                        <i class="bi bi-arrow-down-circle"></i> Deposit
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/withdraw" class="btn btn-outline-danger">
                        <i class="bi bi-arrow-up-circle"></i> Withdraw
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/transfer" class="btn btn-outline-info">
                        <i class="bi bi-arrow-left-right"></i> Transfer
                    </a>
                    <a href="${pageContext.request.contextPath}/settlements/process" class="btn btn-outline-warning">
                        <i class="bi bi-check2-square"></i> Process Settlement
                    </a>
                </div>
            </div>
        </div>
    </div>
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
</div>

<%@ include file="../layout/footer.jsp" %>
