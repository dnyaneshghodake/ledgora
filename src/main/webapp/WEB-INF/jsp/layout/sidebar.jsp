<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Sidebar Navigation - Domain-Aligned Enterprise Structure
  Role flags set by CustomAuthenticationSuccessHandler:
    sessionScope.isAdmin, sessionScope.isManager, sessionScope.isTeller, sessionScope.isCustomer, sessionScope.isFinance
  
  Teller     -> Dashboard, Accounts, Transactions, Payments (limited)
  Manager    -> + Customer Management, Approvals, Settlement, Reports
  Finance    -> + Ledger, Reports, Validation
  Admin      -> Full access to all sections
--%>
<aside class="cbs-sidebar" id="cbsSidebar">
    <div class="cbs-sidebar-header">
        <a href="${pageContext.request.contextPath}/dashboard" class="cbs-sidebar-brand">
            <i class="bi bi-bank2"></i>
            <span>LEDGORA</span>
        </a>
    </div>

    <nav class="cbs-sidebar-nav">
        <ul class="cbs-nav-list">

            <%-- Dashboard - ALL authenticated users --%>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/dashboard" class="cbs-nav-link" data-page="dashboard">
                    <i class="bi bi-speedometer2"></i>
                    <span>Dashboard</span>
                </a>
            </li>

            <%-- Customer Management - ADMIN, MANAGER --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-section">CUSTOMER MANAGEMENT</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers" class="cbs-nav-link" data-page="customers">
                    <i class="bi bi-people"></i>
                    <span>Customers</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers/create" class="cbs-nav-link" data-page="customers/create">
                    <i class="bi bi-person-plus"></i>
                    <span>New Customer</span>
                </a>
            </li>
            </c:if>

            <%-- Accounts - ADMIN, MANAGER, TELLER (full); CUSTOMER (own) --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
            <li class="cbs-nav-section">ACCOUNTS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                    <i class="bi bi-wallet2"></i>
                    <span>All Accounts</span>
                </a>
            </li>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts/create" class="cbs-nav-link" data-page="accounts/create">
                    <i class="bi bi-plus-circle"></i>
                    <span>Open Account</span>
                </a>
            </li>
            </c:if>
            </c:if>
            <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}">
            <li class="cbs-nav-section">MY BANKING</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                    <i class="bi bi-wallet2"></i>
                    <span>My Accounts</span>
                </a>
            </li>
            </c:if>

            <%-- Transactions - ADMIN, MANAGER, TELLER (full); CUSTOMER (history) --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
            <li class="cbs-nav-section">TRANSACTIONS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions" class="cbs-nav-link" data-page="transactions">
                    <i class="bi bi-arrow-left-right"></i>
                    <span>All Transactions</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/deposit" class="cbs-nav-link" data-page="transactions/deposit">
                    <i class="bi bi-arrow-down-circle"></i>
                    <span>Deposit</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/withdraw" class="cbs-nav-link" data-page="transactions/withdraw">
                    <i class="bi bi-arrow-up-circle"></i>
                    <span>Withdraw</span>
                </a>
            </li>
            </c:if>
            <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}">
            <li class="cbs-nav-section">TRANSACTIONS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions" class="cbs-nav-link" data-page="transactions">
                    <i class="bi bi-clock-history"></i>
                    <span>Transaction History</span>
                </a>
            </li>
            </c:if>

            <%-- Payments - ADMIN, MANAGER, TELLER --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
            <li class="cbs-nav-section">PAYMENTS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/transfer" class="cbs-nav-link" data-page="payments">
                    <i class="bi bi-send"></i>
                    <span>Fund Transfer</span>
                </a>
            </li>
            </c:if>

            <%-- Ledger (System of Record) - ADMIN, MANAGER, FINANCE --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isFinance}">
            <li class="cbs-nav-section">LEDGER</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/gl" class="cbs-nav-link" data-page="gl">
                    <i class="bi bi-diagram-3"></i>
                    <span>Chart of Accounts</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/ledger/explorer" class="cbs-nav-link" data-page="ledger/explorer">
                    <i class="bi bi-journal-text"></i>
                    <span>Ledger Explorer</span>
                </a>
            </li>
            </c:if>

            <%-- Batch Management - ADMIN, MANAGER, FINANCE --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isFinance}">
            <li class="cbs-nav-section">BATCH MANAGEMENT</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/batches" class="cbs-nav-link" data-page="batches">
                    <i class="bi bi-collection"></i>
                    <span>Batch Dashboard</span>
                </a>
            </li>
            </c:if>

            <%-- Settlement & Business Date - ADMIN, MANAGER --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-section">SETTLEMENT & BUSINESS DATE</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/settlement/dashboard" class="cbs-nav-link" data-page="settlement/dashboard">
                    <i class="bi bi-gear-wide-connected"></i>
                    <span>Settlement Control</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/settlements" class="cbs-nav-link" data-page="settlements">
                    <i class="bi bi-check2-square"></i>
                    <span>Settlement History</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/settlements/process" class="cbs-nav-link" data-page="settlements/process">
                    <i class="bi bi-play-circle"></i>
                    <span>Run Settlement</span>
                </a>
            </li>
            </c:if>

            <%-- Approvals (Maker-Checker) - ADMIN, MANAGER --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-section">APPROVALS (MAKER-CHECKER)</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/approvals" class="cbs-nav-link" data-page="approvals">
                    <i class="bi bi-clipboard-check"></i>
                    <span>Pending Approvals</span>
                    <c:if test="${sessionScope.pendingApprovals != null && sessionScope.pendingApprovals > 0}">
                        <span class="cbs-badge">${sessionScope.pendingApprovals}</span>
                    </c:if>
                </a>
            </li>
            </c:if>

            <%-- Reports - ADMIN, MANAGER, FINANCE --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isFinance}">
            <li class="cbs-nav-section">REPORTS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/reports" class="cbs-nav-link" data-page="reports">
                    <i class="bi bi-file-earmark-bar-graph"></i>
                    <span>Financial Reports</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/reports/trial-balance" class="cbs-nav-link" data-page="reports/trial-balance">
                    <i class="bi bi-calculator"></i>
                    <span>Trial Balance</span>
                </a>
            </li>
            </c:if>

            <%-- Validation & Integrity - ADMIN, FINANCE --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isFinance}">
            <li class="cbs-nav-section">VALIDATION & INTEGRITY</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/ledger/view/validate" class="cbs-nav-link" data-page="admin/ledger">
                    <i class="bi bi-shield-check"></i>
                    <span>Ledger Validation</span>
                </a>
            </li>
            </c:if>

            <%-- Observability - ADMIN --%>
            <c:if test="${sessionScope.isAdmin}">
            <li class="cbs-nav-section">OBSERVABILITY</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/actuator/health" class="cbs-nav-link" data-page="observability">
                    <i class="bi bi-activity"></i>
                    <span>Health & Metrics</span>
                </a>
            </li>
            </c:if>

            <%-- Administration - ADMIN only --%>
            <c:if test="${sessionScope.isAdmin}">
            <li class="cbs-nav-section">ADMINISTRATION</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/users" class="cbs-nav-link" data-page="admin/users">
                    <i class="bi bi-person-gear"></i>
                    <span>User Management</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/fx-rates" class="cbs-nav-link" data-page="fx-rates">
                    <i class="bi bi-currency-exchange"></i>
                    <span>FX Rates</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/gl/create" class="cbs-nav-link" data-page="gl/create">
                    <i class="bi bi-plus-square"></i>
                    <span>New GL Account</span>
                </a>
            </li>
            </c:if>

        </ul>
    </nav>

    <div class="cbs-sidebar-footer">
        <div class="cbs-sidebar-version">
            <small>Ledgora CBS v1.0</small>
        </div>
    </div>
</aside>
