<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-play-circle"></i> Run End of Day</h3>
    <a href="${pageContext.request.contextPath}/eod/validate" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back to Validation
    </a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-header bg-white"><h5 class="mb-0">EOD Confirmation</h5></div>
    <div class="card-body">
        <c:if test="${not empty error}">
            <div class="alert alert-danger"><c:out value="${error}"/></div>
        </c:if>
        <c:if test="${not empty message}">
            <div class="alert alert-success"><c:out value="${message}"/></div>
        </c:if>

        <div class="row mb-4">
            <div class="col-md-4">
                <div class="card bg-light">
                    <div class="card-body text-center">
                        <div class="text-muted small">Business Date</div>
                        <div class="fs-5 fw-bold">
                            <c:choose>
                                <c:when test="${not empty businessDate}"><c:out value="${businessDate}"/></c:when>
                                <c:otherwise><%= new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()) %></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card bg-light">
                    <div class="card-body text-center">
                        <div class="text-muted small">Current Status</div>
                        <div class="fs-5 fw-bold">
                            <c:choose>
                                <c:when test="${businessDateStatus == 'OPEN'}"><span class="text-success">OPEN</span></c:when>
                                <c:when test="${businessDateStatus == 'DAY_CLOSING'}"><span class="text-warning">DAY_CLOSING</span></c:when>
                                <c:when test="${businessDateStatus == 'CLOSED'}"><span class="text-danger">CLOSED</span></c:when>
                                <c:otherwise><span class="text-secondary">UNKNOWN</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-md-4">
                <div class="card bg-light">
                    <div class="card-body text-center">
                        <div class="text-muted small">Branch</div>
                        <div class="fs-5 fw-bold"><c:out value="${not empty branchCode ? branchCode : 'HQ'}"/></div>
                    </div>
                </div>
            </div>
        </div>

        <div class="alert alert-warning">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <strong>Warning:</strong> Running EOD will close the current business day. No further transactions will be allowed for this date.
        </div>

        <c:if test="${businessDateStatus != 'CLOSED'}">
        <form method="post" action="${pageContext.request.contextPath}/eod/run" class="text-center mt-4">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <button type="submit" class="btn btn-danger btn-lg" data-confirm="Are you sure you want to run EOD? This action cannot be undone.">
                <i class="bi bi-play-circle-fill"></i> Run End of Day
            </button>
        </form>
        </c:if>
        <c:if test="${businessDateStatus == 'CLOSED'}">
        <div class="alert alert-info text-center mt-3">
            <i class="bi bi-info-circle"></i> Business day is already closed.
        </div>
        </c:if>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
