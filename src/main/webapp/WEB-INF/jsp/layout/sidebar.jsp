<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Sidebar Navigation - Domain-Aligned Collapsible Menu Structure
  Role flags set by CustomAuthenticationSuccessHandler:
    sessionScope.isAdmin, sessionScope.isManager, sessionScope.isTeller, sessionScope.isCustomer,
    sessionScope.isOperations, sessionScope.isMaker, sessionScope.isChecker,
    sessionScope.isBranchManager, sessionScope.isTenantAdmin, sessionScope.isSuperAdmin, sessionScope.isAuditor

  CBS Role Mapping:
    MAKER          -> Create Voucher, Customer Create
    CHECKER        -> Authorization screens
    BRANCH_MANAGER -> Batch, GL, EOD
    TENANT_ADMIN   -> All branch screens
    SUPER_ADMIN    -> All tenants
--%>
<aside class="cbs-sidebar" id="cbsSidebar" role="navigation" aria-label="Main sidebar navigation">
    <nav class="cbs-sidebar-nav">
        <ul class="cbs-nav-list">

            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="dashboard">
                    <i class="bi bi-speedometer2"></i>
                    <span>Dashboard</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-dashboard">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/dashboard" class="cbs-nav-link" data-page="dashboard">
                            <i class="bi bi-bar-chart-line"></i>
                            <span>Operational Summary</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isChecker}">
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
                    <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/eod/status" class="cbs-nav-link" data-page="eod/status">
                            <i class="bi bi-calendar-check"></i>
                            <span>EOD Status</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker || sessionScope.isChecker || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isBranchManager}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="customer">
                    <i class="bi bi-people"></i>
                    <span>Customer Management</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-customer">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/customers" class="cbs-nav-link" data-page="customers">
                            <i class="bi bi-person-lines-fill"></i>
                            <span>Customer List</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/customers" class="cbs-nav-link" data-page="customers-master">
                            <i class="bi bi-person-badge"></i>
                            <span>Customer Master Inquiry</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/customers?tab=freeze" class="cbs-nav-link" data-page="customers-freeze">
                            <i class="bi bi-snow"></i>
                            <span>Customer Freeze</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/customers?tab=tax" class="cbs-nav-link" data-page="customers-tax">
                            <i class="bi bi-receipt"></i>
                            <span>Tax Profile</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/approvals?type=CUSTOMER" class="cbs-nav-link" data-page="approvals-customer">
                            <i class="bi bi-person-check"></i>
                            <span>Customer Approval Queue</span>
                        </a>
                    </li>
                    </c:if>
                    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/customers/create" class="cbs-nav-link" data-page="customers/create">
                            <i class="bi bi-person-plus"></i>
                            <span>New Customer</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller || sessionScope.isMaker || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="account">
                    <i class="bi bi-wallet2"></i>
                    <span>Account Management</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-account">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                            <i class="bi bi-search"></i>
                            <span>Account List</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts-master">
                            <i class="bi bi-wallet2"></i>
                            <span>Account Master Inquiry</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts?view=ownership" class="cbs-nav-link" data-page="accounts-ownership">
                            <i class="bi bi-diagram-2"></i>
                            <span>Account Ownership</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts?view=freeze" class="cbs-nav-link" data-page="accounts-freeze">
                            <i class="bi bi-snow"></i>
                            <span>Account Freeze</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts?view=lien" class="cbs-nav-link" data-page="accounts-lien">
                            <i class="bi bi-lock"></i>
                            <span>Account Lien</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/gl" class="cbs-nav-link" data-page="gl">
                            <i class="bi bi-diagram-3"></i>
                            <span>Chart of Accounts</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts/create" class="cbs-nav-link cbs-lockable" data-page="accounts/create">
                            <i class="bi bi-plus-circle"></i>
                            <span>Open New Account</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <%-- Customer-only account view --%>
            <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller && !sessionScope.isMaker}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="mybanking">
                    <i class="bi bi-wallet2"></i>
                    <span>My Banking</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-mybanking">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/accounts" class="cbs-nav-link" data-page="accounts">
                            <i class="bi bi-wallet2"></i>
                            <span>My Accounts</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/transactions" class="cbs-nav-link" data-page="transactions">
                            <i class="bi bi-clock-history"></i>
                            <span>Transaction History</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller || sessionScope.isMaker}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="transactions">
                    <i class="bi bi-cash-stack"></i>
                    <span>Transactions</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-transactions">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/transactions/deposit" class="cbs-nav-link cbs-lockable" data-page="transactions/deposit">
                            <i class="bi bi-arrow-down-circle"></i>
                            <span>Deposit</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/transactions/withdraw" class="cbs-nav-link cbs-lockable" data-page="transactions/withdraw">
                            <i class="bi bi-arrow-up-circle"></i>
                            <span>Withdrawal</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/transactions/transfer" class="cbs-nav-link cbs-lockable" data-page="transactions/transfer">
                            <i class="bi bi-send"></i>
                            <span>Transfer</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/transactions" class="cbs-nav-link" data-page="transactions">
                            <i class="bi bi-arrow-left-right"></i>
                            <span>All Transactions</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/approvals?type=TRANSACTION" class="cbs-nav-link" data-page="approvals-transaction">
                            <i class="bi bi-shield-exclamation"></i>
                            <span>High-Value Approval Queue</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller || sessionScope.isMaker || sessionScope.isChecker || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="vouchers">
                    <i class="bi bi-journal-text"></i>
                    <span>Voucher Operations</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-vouchers">
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
                </ul>
            </li>
            </c:if>

            <%-- ═══ Inter-Branch Transfer (IBT) ═══ --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker || sessionScope.isChecker || sessionScope.isOperations || sessionScope.isAuditor || sessionScope.isTeller}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="ibt">
                    <i class="bi bi-building"></i>
                    <span>Inter-Branch Transfer</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-ibt">
                    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/ibt/create" class="cbs-nav-link cbs-lockable" data-page="ibt/create">
                            <i class="bi bi-plus-circle"></i>
                            <span>New IBT</span>
                        </a>
                    </li>
                    </c:if>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/ibt" class="cbs-nav-link" data-page="ibt">
                            <i class="bi bi-list-ul"></i>
                            <span>IBT List</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isOperations || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isAuditor}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/ibt/reconciliation" class="cbs-nav-link" data-page="ibt/reconciliation">
                            <i class="bi bi-clipboard2-data"></i>
                            <span>IBT Reconciliation</span>
                        </a>
                    </li>
                    </c:if>
                    <c:if test="${sessionScope.isOperations || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/clearing/engine" class="cbs-nav-link" data-page="clearing/engine">
                            <i class="bi bi-gear-wide-connected"></i>
                            <span>Clearing Engine</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <%-- ═══ Suspense GL Governance ═══ --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isOperations || sessionScope.isAuditor}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="suspense">
                    <i class="bi bi-exclamation-diamond"></i>
                    <span>Suspense GL</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-suspense">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/suspense/dashboard" class="cbs-nav-link" data-page="suspense/dashboard">
                            <i class="bi bi-clipboard2-data"></i>
                            <span>Suspense Dashboard</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <%-- ═══ Risk & Fraud Monitoring ═══ --%>
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isOperations || sessionScope.isAuditor}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="risk">
                    <i class="bi bi-shield-exclamation"></i>
                    <span>Risk & Fraud</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-risk">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/risk/hard-ceiling" class="cbs-nav-link" data-page="risk/hard-ceiling">
                            <i class="bi bi-shield-exclamation"></i>
                            <span>Hard Ceiling Monitor</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/risk/velocity" class="cbs-nav-link" data-page="risk/velocity">
                            <i class="bi bi-speedometer2"></i>
                            <span>Velocity Fraud Monitor</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isOperations}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="ledger">
                    <i class="bi bi-book"></i>
                    <span>Ledger & GL</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-ledger">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/ledger/explorer" class="cbs-nav-link" data-page="ledger/explorer">
                            <i class="bi bi-journal-text"></i>
                            <span>Ledger Explorer</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isTenantAdmin || sessionScope.isSuperAdmin || sessionScope.isAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/ledger/explorer?scope=tenant" class="cbs-nav-link" data-page="ledger/tenant">
                            <i class="bi bi-building"></i>
                            <span>GL Summary</span>
                        </a>
                    </li>
                    </c:if>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/reports/trial-balance" class="cbs-nav-link" data-page="reports/trial-balance">
                            <i class="bi bi-calculator"></i>
                            <span>Trial Balance</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/settlement/dashboard" class="cbs-nav-link" data-page="settlement/dashboard">
                            <i class="bi bi-gear-wide-connected"></i>
                            <span>Settlement</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/settlements" class="cbs-nav-link" data-page="settlements">
                            <i class="bi bi-check2-square"></i>
                            <span>Settlement History</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="batch">
                    <i class="bi bi-collection"></i>
                    <span>Batch & EOD</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-batch">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/eod/day-begin" class="cbs-nav-link" data-page="eod/day-begin">
                            <i class="bi bi-sunrise"></i>
                            <span>Day Begin</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/batches" class="cbs-nav-link" data-page="batches">
                            <i class="bi bi-collection"></i>
                            <span>Batch Management</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/eod/validate" class="cbs-nav-link" data-page="eod/validate">
                            <i class="bi bi-check-circle"></i>
                            <span>EOD Validation</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/eod/run" class="cbs-nav-link" data-page="eod/run">
                            <i class="bi bi-play-circle"></i>
                            <span>Execute EOD</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/eod/status" class="cbs-nav-link" data-page="eod/status">
                            <i class="bi bi-calendar-check"></i>
                            <span>Day Status</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isOperations || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="reports">
                    <i class="bi bi-file-earmark-bar-graph"></i>
                    <span>Reports</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-reports">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/reports" class="cbs-nav-link" data-page="reports">
                            <i class="bi bi-file-earmark-bar-graph"></i>
                            <span>Financial Reports</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isAdmin || sessionScope.isOperations}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/admin/ledger/view/validate" class="cbs-nav-link" data-page="admin/ledger">
                            <i class="bi bi-shield-check"></i>
                            <span>Ledger Validation</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isMaker || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="calendar">
                    <i class="bi bi-calendar3"></i>
                    <span>Banking Calendar</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-calendar">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/calendar" class="cbs-nav-link" data-page="calendar">
                            <i class="bi bi-calendar-event"></i>
                            <span>Calendar Maintenance</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/calendar/create" class="cbs-nav-link" data-page="calendar/create">
                            <i class="bi bi-calendar-plus"></i>
                            <span>Add Calendar Entry</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <%-- ═══ Audit & Governance Section ═══ --%>
            <c:if test="${sessionScope.isAuditor || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isSuperAdmin || sessionScope.isChecker}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="audit-governance">
                    <i class="bi bi-shield-check"></i>
                    <span>Audit & Governance</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-audit-governance">
                    <c:if test="${sessionScope.isAuditor || sessionScope.isAdmin || sessionScope.isSuperAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/audit/validation" class="cbs-nav-link" data-page="audit/validation">
                            <i class="bi bi-clipboard-data"></i>
                            <span>Audit Dashboard</span>
                        </a>
                    </li>
                    </c:if>
                    <c:if test="${sessionScope.isAuditor || sessionScope.isAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/audit/explorer" class="cbs-nav-link" data-page="audit/explorer">
                            <i class="bi bi-clock-history"></i>
                            <span>Audit Log Explorer</span>
                        </a>
                    </li>
                    </c:if>
                    <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/approvals" class="cbs-nav-link" data-page="approvals-queue">
                            <i class="bi bi-clipboard-check"></i>
                            <span>Approval Queue</span>
                            <c:if test="${sessionScope.pendingApprovals != null && sessionScope.pendingApprovals > 0}">
                                <span class="cbs-badge">${sessionScope.pendingApprovals}</span>
                            </c:if>
                        </a>
                    </li>
                    </c:if>
                    <%-- Config Governance (CBS Tier-1: maker-checker on parameter changes) --%>
                    <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/governance" class="cbs-nav-link" data-page="governance">
                            <i class="bi bi-sliders"></i>
                            <span>Config Governance</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

            <%-- ═══ Diagnostics (ADMIN only — stress profile features) ═══ --%>
            <c:if test="${sessionScope.isAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="diagnostics">
                    <i class="bi bi-wrench-adjustable"></i>
                    <span>Diagnostics</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-diagnostics">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/diagnostics/query-plans" class="cbs-nav-link" data-page="diagnostics/query-plans">
                            <i class="bi bi-diagram-3"></i>
                            <span>Query Plan Analyzer</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/diagnostics/concurrency-audit" class="cbs-nav-link" data-page="diagnostics/concurrency-audit">
                            <i class="bi bi-check2-all"></i>
                            <span>Concurrency Audit</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="#" class="cbs-nav-link" data-page="diagnostics/certification" title="POST /diagnostics/certification/run — use API client">
                            <i class="bi bi-award"></i>
                            <span>Enterprise Certification</span>
                        </a>
                    </li>
                </ul>
            </li>
            </c:if>

            <c:if test="${sessionScope.isAdmin || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <li class="cbs-nav-group">
                <a href="#" class="cbs-nav-group-toggle" data-group="admin">
                    <i class="bi bi-gear"></i>
                    <span>Admin</span>
                    <i class="bi bi-chevron-down cbs-nav-arrow"></i>
                </a>
                <ul class="cbs-nav-submenu" id="group-admin">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/admin/users" class="cbs-nav-link" data-page="admin/users">
                            <i class="bi bi-person-gear"></i>
                            <span>User Management</span>
                        </a>
                    </li>
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/admin/users?tab=roles" class="cbs-nav-link" data-page="admin/users-roles">
                            <i class="bi bi-shield-lock"></i>
                            <span>Role Management</span>
                        </a>
                    </li>
                    <c:if test="${sessionScope.isSuperAdmin || sessionScope.isAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/admin/tenants" class="cbs-nav-link" data-page="admin/tenants">
                            <i class="bi bi-building"></i>
                            <span>Tenant Config</span>
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
                    <c:if test="${sessionScope.isAdmin || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
                    <li class="cbs-nav-item">
                        <a href="${pageContext.request.contextPath}/gl/create" class="cbs-nav-link" data-page="gl/create">
                            <i class="bi bi-plus-square"></i>
                            <span>New GL Account</span>
                        </a>
                    </li>
                    </c:if>
                </ul>
            </li>
            </c:if>

        </ul>
    </nav>

    <div class="cbs-sidebar-footer">
        <div class="cbs-sidebar-version">
            <small>Ledgora CBS v2.7</small>
        </div>
    </div>
</aside>
