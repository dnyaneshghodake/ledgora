<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ledgora - Core Banking Platform</title>
    <link href="${pageContext.request.contextPath}/resources/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/resources/css/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/resources/css/style.css" rel="stylesheet">
</head>
<body>
<%--
  Role-Aware Navigation Bar
  Menu items are shown/hidden based on the user's role stored in session.
  Roles are set by CustomAuthenticationSuccessHandler on login:
    sessionScope.isAdmin, sessionScope.isManager, sessionScope.isTeller, sessionScope.isCustomer
--%>
<c:if test="${not empty sessionScope.username}">
<nav class="navbar navbar-expand-lg navbar-dark bg-primary">
    <div class="container-fluid">
        <a class="navbar-brand d-flex align-items-center" href="${pageContext.request.contextPath}/dashboard">
            <i class="bi bi-bank2 me-1"></i> Ledgora
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav"
                aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
            <%-- Left-aligned nav items --%>
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <%-- Dashboard — visible to ALL authenticated users --%>
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/dashboard"><i class="bi bi-speedometer2"></i> Dashboard</a>
                </li>
                <%-- Accounts — ADMIN, MANAGER, TELLER can manage; CUSTOMER sees own accounts --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="bi bi-wallet2"></i> Accounts</a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/accounts">All Accounts</a></li>
                            <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/accounts/create">Create Account</a></li>
                            </c:if>
                        </ul>
                    </li>
                </c:if>
                <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/accounts"><i class="bi bi-wallet2"></i> My Accounts</a>
                    </li>
                </c:if>
                <%-- Transactions — ADMIN, MANAGER, TELLER get full access; CUSTOMER sees history --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="bi bi-arrow-left-right"></i> Transactions</a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/transactions">All Transactions</a></li>
                            <li><hr class="dropdown-divider"></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/transactions/deposit">Deposit</a></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/transactions/withdraw">Withdraw</a></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/transactions/transfer">Transfer</a></li>
                        </ul>
                    </li>
                </c:if>
                <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/transactions"><i class="bi bi-arrow-left-right"></i> Transaction History</a>
                    </li>
                </c:if>
                <%-- General Ledger — ADMIN and MANAGER only --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/gl"><i class="bi bi-diagram-3"></i> General Ledger</a>
                    </li>
                </c:if>
                <%-- Settlement — ADMIN and MANAGER only --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="nav-item dropdown">
                        <a class="nav-link dropdown-toggle" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false"><i class="bi bi-check2-square"></i> Settlement</a>
                        <ul class="dropdown-menu">
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/settlements">All Settlements</a></li>
                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/settlements/process">Process Settlement</a></li>
                        </ul>
                    </li>
                </c:if>
                <%-- Customers — ADMIN and MANAGER only --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/customers"><i class="bi bi-people"></i> Customers</a>
                    </li>
                </c:if>
                <%-- FX Rates — ADMIN only --%>
                <c:if test="${sessionScope.isAdmin}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/fx-rates"><i class="bi bi-currency-exchange"></i> FX Rates</a>
                    </li>
                </c:if>
                <%-- Reports — ADMIN and MANAGER --%>
                <c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/reports"><i class="bi bi-file-earmark-bar-graph"></i> Reports</a>
                    </li>
                </c:if>
                <%-- User Management — ADMIN only --%>
                <c:if test="${sessionScope.isAdmin}">
                    <li class="nav-item">
                        <a class="nav-link" href="${pageContext.request.contextPath}/admin/users"><i class="bi bi-person-gear"></i> Users</a>
                    </li>
                </c:if>
            </ul>
            <%-- Right-aligned user info & logout --%>
            <ul class="navbar-nav ms-auto mb-2 mb-lg-0 d-flex align-items-center">
                <li class="nav-item">
                    <span class="nav-link text-light d-flex align-items-center">
                        <i class="bi bi-person-circle me-1"></i> ${sessionScope.username}
                        <c:if test="${sessionScope.isAdmin}"><span class="badge bg-danger ms-2">Admin</span></c:if>
                        <c:if test="${sessionScope.isManager}"><span class="badge bg-warning text-dark ms-2">Manager</span></c:if>
                        <c:if test="${sessionScope.isTeller}"><span class="badge bg-info ms-2">Teller</span></c:if>
                        <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}"><span class="badge bg-secondary ms-2">Customer</span></c:if>
                    </span>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="${pageContext.request.contextPath}/logout"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </li>
            </ul>
        </div>
    </div>
</nav>
</c:if>
<div class="container-fluid mt-3">
    <c:if test="${not empty message}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            ${error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>
