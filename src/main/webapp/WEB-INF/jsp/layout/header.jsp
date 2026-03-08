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
    <meta name="_csrf" content="${_csrf.token}"/>
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
</head>
<body>
<c:if test="${not empty sessionScope.username}">
<%--
  CBS Layout: Fixed Header + Fixed Sidebar + Scrollable Main Content
  Role flags set by CustomAuthenticationSuccessHandler on login:
    sessionScope.isAdmin, sessionScope.isManager, sessionScope.isTeller, sessionScope.isCustomer,
    sessionScope.isFinance, sessionScope.isMaker, sessionScope.isChecker,
    sessionScope.isBranchManager, sessionScope.isTenantAdmin, sessionScope.isSuperAdmin
--%>

<%-- CBS Top Header Bar --%>
<header class="cbs-header" id="cbsHeader">
    <div class="cbs-header-left">
        <button class="cbs-sidebar-toggle" id="sidebarToggle" type="button" title="Toggle Sidebar">
            <i class="bi bi-list"></i>
        </button>
        <a href="${pageContext.request.contextPath}/dashboard" class="cbs-header-brand">
            <i class="bi bi-bank2"></i>
            <span class="cbs-header-brand-text">LEDGORA</span>
        </a>
        <span class="cbs-header-separator"></span>
        <span class="cbs-header-subtitle">Core Banking System</span>
    </div>
    <div class="cbs-header-center">
        <%-- Environment Badge --%>
        <span class="cbs-env-badge cbs-env-dev">
            <c:choose>
                <c:when test="${not empty sessionScope.environment}">${sessionScope.environment}</c:when>
                <c:otherwise>DEV</c:otherwise>
            </c:choose>
        </span>
        <span class="cbs-header-separator"></span>
        <%-- Business Date --%>
        <div class="cbs-business-date">
            <i class="bi bi-calendar3"></i>
            <span>Business Date:</span>
            <strong>
                <c:choose>
                    <c:when test="${not empty sessionScope.businessDate}">${sessionScope.businessDate}</c:when>
                    <c:otherwise><%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %></c:otherwise>
                </c:choose>
            </strong>
        </div>
        <span class="cbs-header-separator"></span>
        <%-- Business Date Status --%>
        <div class="cbs-date-status">
            <c:choose>
                <c:when test="${sessionScope.businessDateStatus == 'OPEN'}">
                    <span class="cbs-status-indicator cbs-status-open"><i class="bi bi-circle-fill"></i> OPEN</span>
                </c:when>
                <c:when test="${sessionScope.businessDateStatus == 'DAY_CLOSING'}">
                    <span class="cbs-status-indicator cbs-status-closing"><i class="bi bi-circle-fill"></i> DAY_CLOSING</span>
                </c:when>
                <c:when test="${sessionScope.businessDateStatus == 'CLOSED'}">
                    <span class="cbs-status-indicator cbs-status-closed"><i class="bi bi-circle-fill"></i> CLOSED</span>
                </c:when>
                <c:otherwise>
                    <span class="cbs-status-indicator cbs-status-open"><i class="bi bi-circle-fill"></i> OPEN</span>
                </c:otherwise>
            </c:choose>
        </div>
        <span class="cbs-header-separator"></span>
        <%-- Ledger Health Indicator --%>
        <div class="cbs-ledger-health">
            <c:choose>
                <c:when test="${sessionScope.ledgerHealth == 'WARNING'}">
                    <span class="cbs-health-badge cbs-health-warning" title="Ledger Health: WARNING">
                        <i class="bi bi-exclamation-triangle-fill"></i> WARNING
                    </span>
                </c:when>
                <c:when test="${sessionScope.ledgerHealth == 'CORRUPTED'}">
                    <span class="cbs-health-badge cbs-health-corrupted" title="Ledger Health: CORRUPTED">
                        <i class="bi bi-x-octagon-fill"></i> CORRUPTED
                    </span>
                </c:when>
                <c:otherwise>
                    <span class="cbs-health-badge cbs-health-healthy" title="Ledger Health: HEALTHY">
                        <i class="bi bi-shield-check"></i> HEALTHY
                    </span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="cbs-header-right">
        <%-- Tenant Context Display --%>
        <div class="cbs-tenant-info" style="display: flex; align-items: center; gap: 8px; margin-right: 12px; padding: 4px 10px; background: rgba(255,255,255,0.1); border-radius: 6px;">
            <i class="bi bi-building" style="color: #ffc107;"></i>
            <c:choose>
                <c:when test="${not empty sessionScope.tenantName}">
                    <span style="font-size: 0.85rem; color: #e0e0e0;"><c:out value="${sessionScope.tenantName}"/></span>
                </c:when>
                <c:otherwise>
                    <span style="font-size: 0.85rem; color: #e0e0e0;">Default Tenant</span>
                </c:otherwise>
            </c:choose>
            <span class="cbs-header-separator"></span>
            <%-- Tenant Switch Dropdown (for MULTI tenant users) --%>
            <c:if test="${sessionScope.tenantScope == 'MULTI'}">
                <div class="dropdown" style="display: inline-block;">
                    <button class="btn btn-sm btn-outline-light dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false" style="font-size: 0.8rem; padding: 2px 8px;">
                        Switch Tenant
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end">
                        <c:forEach var="t" items="${sessionScope.availableTenants}">
                            <li>
                                <form method="post" action="${pageContext.request.contextPath}/tenant/switch" class="m-0">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="tenantId" value="${t.id}">
                                    <button type="submit" class="dropdown-item"><c:out value="${t.tenantName}"/> (<c:out value="${t.tenantCode}"/>)</button>
                                </form>
                            </li>
                        </c:forEach>
                    </ul>
                </div>
            </c:if>
        </div>
        <div class="cbs-user-info">
            <div class="cbs-user-avatar">
                <i class="bi bi-person-circle"></i>
            </div>
            <div class="cbs-user-details">
                <span class="cbs-user-name"><c:out value="${sessionScope.username}"/></span>
                <span class="cbs-user-role">
                    <c:if test="${sessionScope.isSuperAdmin}"><span class="badge bg-dark">Super Admin</span></c:if>
                    <c:if test="${sessionScope.isTenantAdmin}"><span class="badge bg-purple" style="background:#7c3aed!important;">Tenant Admin</span></c:if>
                    <c:if test="${sessionScope.isAdmin}"><span class="badge bg-danger">Admin</span></c:if>
                    <c:if test="${sessionScope.isBranchManager}"><span class="badge bg-indigo" style="background:#4f46e5!important;">Branch Mgr</span></c:if>
                    <c:if test="${sessionScope.isManager}"><span class="badge bg-warning text-dark">Manager</span></c:if>
                    <c:if test="${sessionScope.isFinance}"><span class="badge bg-info">Finance</span></c:if>
                    <c:if test="${sessionScope.isMaker}"><span class="badge bg-success">Maker</span></c:if>
                    <c:if test="${sessionScope.isChecker}"><span class="badge bg-primary">Checker</span></c:if>
                    <c:if test="${sessionScope.isTeller}"><span class="badge bg-info">Teller</span></c:if>
                    <c:if test="${sessionScope.isCustomer && !sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isTeller}"><span class="badge bg-secondary">Customer</span></c:if>
                </span>
            </div>
        </div>
        <a href="${pageContext.request.contextPath}/logout" class="cbs-logout-btn" title="Sign Out">
            <i class="bi bi-box-arrow-right"></i>
            <span>Logout</span>
        </a>
    </div>
</header>

<%-- CBS Sidebar --%>
<%@ include file="sidebar.jsp" %>

<%-- Business Day Closed Banner --%>
<c:if test="${sessionScope.businessDateStatus == 'CLOSED'}">
<div class="cbs-eod-banner" id="eodBanner">
    <div class="cbs-eod-banner-content">
        <i class="bi bi-exclamation-triangle-fill"></i>
        <span>Business Day Closed &mdash; No transactions allowed until next business day is opened.</span>
    </div>
</div>
</c:if>

<%-- CBS Main Content Area --%>
<main class="cbs-main" id="cbsMain">
    <%-- Breadcrumb Navigation --%>
    <c:if test="${not empty breadcrumb}">
    <nav aria-label="breadcrumb" class="cbs-breadcrumb-nav">
        <ol class="breadcrumb cbs-breadcrumb">
            <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/dashboard">Dashboard</a></li>
            <c:forEach var="crumb" items="${breadcrumb}">
                <c:choose>
                    <c:when test="${crumb.active}">
                        <li class="breadcrumb-item active" aria-current="page">${crumb.label}</li>
                    </c:when>
                    <c:otherwise>
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}${crumb.url}">${crumb.label}</a></li>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
        </ol>
    </nav>
    </c:if>
    <div class="cbs-content">
        <c:if test="${not empty message}">
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <c:out value="${message}"/>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
        <c:if test="${not empty error}">
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <c:out value="${error}"/>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>
</c:if>
<c:if test="${empty sessionScope.username}">
<%-- Unauthenticated pages render without CBS layout --%>
</c:if>
