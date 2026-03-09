<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%--
  CBS Global Error Page
  Displays structured error information with proper CBS card-based layout.
  Model attributes populated by GlobalExceptionHandler:
    errorTitle, errorMessage, errorCode, correlationId, timestamp, tenantName, branchCode, businessDate
--%>

<div class="cbs-error-page">
    <div class="cbs-error-card">
        <%-- Error Header with contextual icon and title --%>
        <div class="cbs-error-header">
            <c:choose>
                <c:when test="${errorCode == '403'}">
                    <i class="bi bi-shield-lock-fill"></i>
                    <h4>Access Denied</h4>
                </c:when>
                <c:when test="${errorCode == 'EOD_CLOSED'}">
                    <i class="bi bi-calendar-x-fill"></i>
                    <h4>Business Day Closed</h4>
                </c:when>
                <c:when test="${errorCode == 'TENANT_VIOLATION'}">
                    <i class="bi bi-building-slash"></i>
                    <h4>Tenant Isolation Violation</h4>
                </c:when>
                <c:when test="${errorCode == 'INSUFFICIENT_BALANCE'}">
                    <i class="bi bi-wallet2"></i>
                    <h4>Insufficient Balance</h4>
                </c:when>
                <c:when test="${errorCode == 'INVALID_AMOUNT'}">
                    <i class="bi bi-currency-exchange"></i>
                    <h4>Invalid Amount</h4>
                </c:when>
                <c:when test="${errorCode == 'SCRIPT_INJECTION'}">
                    <i class="bi bi-shield-exclamation"></i>
                    <h4>Invalid Input Detected</h4>
                </c:when>
                <c:when test="${errorCode == 'VALIDATION_ERROR'}">
                    <i class="bi bi-x-circle-fill"></i>
                    <h4>Validation Error</h4>
                </c:when>
                <c:when test="${errorCode == 'INTERNAL_ERROR'}">
                    <i class="bi bi-gear-wide-connected"></i>
                    <h4>System Error</h4>
                </c:when>
                <c:otherwise>
                    <i class="bi bi-exclamation-triangle-fill"></i>
                    <h4>
                        <c:choose>
                            <c:when test="${not empty errorTitle}"><c:out value="${errorTitle}"/></c:when>
                            <c:otherwise>Something Went Wrong</c:otherwise>
                        </c:choose>
                    </h4>
                </c:otherwise>
            </c:choose>
        </div>

        <%-- Error Body --%>
        <div class="cbs-error-body">
            <%-- Error Message --%>
            <p class="cbs-error-message">
                <c:choose>
                    <c:when test="${not empty errorMessage}"><c:out value="${errorMessage}"/></c:when>
                    <c:otherwise>An unexpected error occurred. Please try again or contact support.</c:otherwise>
                </c:choose>
            </p>

            <%-- Error Details --%>
            <div class="cbs-error-details">
                <dl class="row mb-0">
                    <dt class="col-sm-4">Error Code</dt>
                    <dd class="col-sm-8"><code><c:out value="${not empty errorCode ? errorCode : 'UNKNOWN'}"/></code></dd>

                    <dt class="col-sm-4">Correlation ID</dt>
                    <dd class="col-sm-8"><code><c:out value="${not empty correlationId ? correlationId : 'N/A'}"/></code></dd>

                    <dt class="col-sm-4">Timestamp</dt>
                    <dd class="col-sm-8"><c:out value="${not empty timestamp ? timestamp : 'N/A'}"/></dd>

                    <dt class="col-sm-4">Tenant</dt>
                    <dd class="col-sm-8"><c:out value="${not empty tenantName ? tenantName : 'N/A'}"/></dd>

                    <dt class="col-sm-4">Branch</dt>
                    <dd class="col-sm-8"><c:out value="${not empty branchCode ? branchCode : 'N/A'}"/></dd>

                    <dt class="col-sm-4">Business Date</dt>
                    <dd class="col-sm-8"><c:out value="${not empty businessDate ? businessDate : 'N/A'}"/></dd>
                </dl>
            </div>
        </div>

        <%-- Action Buttons --%>
        <div class="cbs-error-actions">
            <a href="javascript:history.back()" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left"></i> Go Back
            </a>
            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary ms-2">
                <i class="bi bi-house"></i> Return to Dashboard
            </a>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
