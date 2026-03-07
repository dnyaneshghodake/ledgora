<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="cbs-error-page">
    <div class="cbs-error-icon">
        <c:choose>
            <c:when test="${errorCode == '403'}"><i class="bi bi-shield-lock-fill"></i></c:when>
            <c:when test="${errorCode == 'EOD_CLOSED'}"><i class="bi bi-calendar-x-fill"></i></c:when>
            <c:when test="${errorCode == 'TENANT_VIOLATION'}"><i class="bi bi-building-slash"></i></c:when>
            <c:when test="${errorCode == 'INSUFFICIENT_BALANCE'}"><i class="bi bi-wallet2"></i></c:when>
            <c:otherwise><i class="bi bi-exclamation-triangle-fill"></i></c:otherwise>
        </c:choose>
    </div>
    <h2 class="cbs-error-title">
        <c:choose>
            <c:when test="${not empty errorTitle}">${errorTitle}</c:when>
            <c:otherwise>Something Went Wrong</c:otherwise>
        </c:choose>
    </h2>
    <p class="cbs-error-message">
        <c:choose>
            <c:when test="${not empty errorMessage}">${errorMessage}</c:when>
            <c:otherwise>An unexpected error occurred. Please try again or contact support.</c:otherwise>
        </c:choose>
    </p>

    <div class="cbs-error-details">
        <table class="table table-sm table-borderless">
            <tr>
                <td class="text-muted" width="140">Error Code</td>
                <td><code>${not empty errorCode ? errorCode : 'UNKNOWN'}</code></td>
            </tr>
            <tr>
                <td class="text-muted">Correlation ID</td>
                <td><code>${not empty correlationId ? correlationId : 'N/A'}</code></td>
            </tr>
            <tr>
                <td class="text-muted">Timestamp</td>
                <td>${not empty timestamp ? timestamp : 'N/A'}</td>
            </tr>
            <tr>
                <td class="text-muted">Tenant</td>
                <td>${not empty tenantName ? tenantName : 'N/A'}</td>
            </tr>
            <tr>
                <td class="text-muted">Branch</td>
                <td>${not empty branchCode ? branchCode : 'N/A'}</td>
            </tr>
            <tr>
                <td class="text-muted">Business Date</td>
                <td>${not empty businessDate ? businessDate : 'N/A'}</td>
            </tr>
        </table>
    </div>

    <div class="cbs-error-actions">
        <a href="javascript:history.back()" class="btn btn-outline-secondary">
            <i class="bi bi-arrow-left"></i> Go Back
        </a>
        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary ms-2">
            <i class="bi bi-house"></i> Return to Dashboard
        </a>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
