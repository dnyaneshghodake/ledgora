<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Sidebar Navigation - Full CBS Menu Structure
  Role flags set by CustomAuthenticationSuccessHandler:
    sessionScope.isAdmin, sessionScope.isManager, sessionScope.isTeller, sessionScope.isCustomer,
    sessionScope.isFinance, sessionScope.isMaker, sessionScope.isChecker,
    sessionScope.isBranchManager, sessionScope.isTenantAdmin, sessionScope.isSuperAdmin

  CBS Role Mapping:
    MAKER          -> Create Voucher, Customer Create
    CHECKER        -> Authorization screens
    BRANCH_MANAGER -> Batch, GL, EOD
    TENANT_ADMIN   -> All branch screens
    SUPER_ADMIN    -> All tenants
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

            <%-- DASHBOARD - ALL authenticated users --%>
            <li class="cbs-nav-section">DASHBOARD</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/dashboard" class="cbs-nav-link" data-page="dashboard">
                    <i class="bi bi-speedometer2"></i>
                    <span>Home</span>
                </a>
            </li>

            <%-- CUSTOMER MANAGEMENT --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker || sessionScope.isChecker || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isBranchManager}">
            <li class="cbs-nav-section">CUSTOMER MANAGEMENT</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers" class="cbs-nav-link" data-page="customers">
                    <i class="bi bi-people"></i>
                    <span>Customer Master</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers?tab=tax" class="cbs-nav-link" data-page="customers-tax">
                    <i class="bi bi-receipt"></i>
                    <span>Customer Tax Profile</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers?tab=freeze" class="cbs-nav-link" data-page="customers-freeze">
                    <i class="bi bi-snow"></i>
                    <span>Freeze Control</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers?tab=relationship" class="cbs-nav-link" data-page="customers-linked">
                    <i class="bi bi-diagram-2"></i>
                    <span>Linked Customers</span>
                </a>
            </li>
            <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/customers/create" class="cbs-nav-link" data-page="customers/create">
                    <i class="bi bi-person-plus"></i>
                    <span>New Customer</span>
                </a>
            </li>
            </c:if>
            </c:if>

            <%-- ACCOUNT MANAGEMENT --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller || sessionScope.isMaker || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-section">ACCOUNT MANAGEMENT</li>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts/create" class="cbs-nav-link cbs-lockable" data-page="accounts/create">
                    <i class="bi bi-plus-circle"></i>
                    <span>Account Opening</span>
                </a>
            </li>
            </c:if>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                    <i class="bi bi-search"></i>
                    <span>Account Inquiry</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts?view=balance" class="cbs-nav-link" data-page="accounts-balance">
                    <i class="bi bi-wallet2"></i>
                    <span>Balance View (CBS)</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts?view=lien" class="cbs-nav-link" data-page="accounts-lien">
                    <i class="bi bi-lock"></i>
                    <span>Lien Management</span>
                </a>
            </li>
            </c:if>
            <%-- Customer-only account view --%>
            <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller && !sessionScope.isMaker}">
            <li class="cbs-nav-section">MY BANKING</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                    <i class="bi bi-wallet2"></i>
                    <span>My Accounts</span>
                </a>
            </li>
            </c:if>

            <%-- VOUCHER OPERATIONS --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller || sessionScope.isMaker || sessionScope.isChecker || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-section">VOUCHER OPERATIONS</li>
            <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers/create" class="cbs-nav-link cbs-lockable" data-page="vouchers/create">
                    <i class="bi bi-plus-square"></i>
                    <span>Create Voucher</span>
                </a>
            </li>
            </c:if>
            <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers/pending" class="cbs-nav-link" data-page="vouchers/pending">
                    <i class="bi bi-clipboard-check"></i>
                    <span>Pending Authorization</span>
                </a>
            </li>
            </c:if>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers/posted" class="cbs-nav-link" data-page="vouchers/posted">
                    <i class="bi bi-check2-square"></i>
                    <span>Posted Vouchers</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers/cancelled" class="cbs-nav-link" data-page="vouchers/cancelled">
                    <i class="bi bi-x-circle"></i>
                    <span>Cancel / Reversal</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers" class="cbs-nav-link" data-page="vouchers">
                    <i class="bi bi-search"></i>
                    <span>Voucher Inquiry</span>
                </a>
            </li>
            </c:if>

            <%-- BATCH MANAGEMENT --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isFinance}">
            <li class="cbs-nav-section">BATCH MANAGEMENT</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/batches" class="cbs-nav-link" data-page="batches">
                    <i class="bi bi-collection"></i>
                    <span>Batch Monitor</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/vouchers?view=scroll" class="cbs-nav-link" data-page="vouchers-scroll">
                    <i class="bi bi-list-ol"></i>
                    <span>Scroll Inquiry</span>
                </a>
            </li>
            </c:if>

            <%-- GL MANAGEMENT --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isFinance}">
            <li class="cbs-nav-section">GL MANAGEMENT</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/gl" class="cbs-nav-link" data-page="gl">
                    <i class="bi bi-diagram-3"></i>
                    <span>GL Inquiry</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/ledger/explorer" class="cbs-nav-link" data-page="ledger/explorer">
                    <i class="bi bi-journal-text"></i>
                    <span>Branch GL Position</span>
                </a>
            </li>
            <c:if test="${sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isAdmin}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/ledger/explorer?scope=tenant" class="cbs-nav-link" data-page="ledger/tenant">
                    <i class="bi bi-building"></i>
                    <span>Tenant Consolidated GL</span>
                </a>
            </li>
            </c:if>
            </c:if>

            <%-- END OF DAY --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-section">END OF DAY</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/eod/validate" class="cbs-nav-link" data-page="eod/validate">
                    <i class="bi bi-check-circle"></i>
                    <span>EOD Validation</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/eod/run" class="cbs-nav-link" data-page="eod/run">
                    <i class="bi bi-play-circle"></i>
                    <span>Run EOD</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/eod/status" class="cbs-nav-link" data-page="eod/status">
                    <i class="bi bi-calendar-check"></i>
                    <span>Business Date Status</span>
                </a>
            </li>
            </c:if>

            <%-- TRANSACTIONS (Legacy) --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
            <li class="cbs-nav-section">TRANSACTIONS</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions" class="cbs-nav-link" data-page="transactions">
                    <i class="bi bi-arrow-left-right"></i>
                    <span>All Transactions</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/deposit" class="cbs-nav-link cbs-lockable" data-page="transactions/deposit">
                    <i class="bi bi-arrow-down-circle"></i>
                    <span>Deposit</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/withdraw" class="cbs-nav-link cbs-lockable" data-page="transactions/withdraw">
                    <i class="bi bi-arrow-up-circle"></i>
                    <span>Withdraw</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/transactions/transfer" class="cbs-nav-link cbs-lockable" data-page="transactions/transfer">
                    <i class="bi bi-send"></i>
                    <span>Fund Transfer</span>
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

            <%-- SETTLEMENT (Legacy) --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
            <li class="cbs-nav-section">SETTLEMENT</li>
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
            </c:if>

            <%-- APPROVALS --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isChecker}">
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

            <%-- REPORTS --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isFinance || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
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

            <%-- VALIDATION & INTEGRITY --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isFinance}">
            <li class="cbs-nav-section">VALIDATION & INTEGRITY</li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/ledger/view/validate" class="cbs-nav-link" data-page="admin/ledger">
                    <i class="bi bi-shield-check"></i>
                    <span>Ledger Validation</span>
                </a>
            </li>
            </c:if>

            <%-- ADMIN --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-section">ADMIN</li>
            <c:if test="${sessionScope.isSuperAdmin || sessionScope.isAdmin}">
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/tenants" class="cbs-nav-link" data-page="admin/tenants">
                    <i class="bi bi-building"></i>
                    <span>Tenant Management</span>
                </a>
            </li>
            </c:if>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/branches" class="cbs-nav-link" data-page="admin/branches">
                    <i class="bi bi-geo-alt"></i>
                    <span>Branch Management</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/users" class="cbs-nav-link" data-page="admin/users">
                    <i class="bi bi-person-gear"></i>
                    <span>User & Role</span>
                </a>
            </li>
            <li class="cbs-nav-item">
                <a href="${pageContext.request.contextPath}/admin/audit" class="cbs-nav-link" data-page="admin/audit">
                    <i class="bi bi-shield-check"></i>
                    <span>Audit Logs</span>
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
            <small>Ledgora CBS v2.0</small>
        </div>
    </div>
</aside>
