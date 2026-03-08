<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-check-circle"></i> EOD Validation</h3>
    <a href="${pageContext.request.contextPath}/eod/status" class="btn btn-outline-secondary">
        <i class="bi bi-calendar-check"></i> Business Date Status
    </a>
</div>

<div class="card shadow mb-4">
    <div class="card-header bg-white">
        <h5 class="mb-0">Pre-EOD Validation Checklist</h5>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${allVouchersPosted}">
                <div class="cbs-eod-check cbs-eod-check-pass">
                    <div class="cbs-eod-check-icon"><i class="bi bi-check-lg"></i></div>
                    <div class="cbs-eod-check-label">All vouchers posted</div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="cbs-eod-check cbs-eod-check-fail">
                    <div class="cbs-eod-check-icon"><i class="bi bi-x-lg"></i></div>
                    <div class="cbs-eod-check-label">All vouchers posted</div>
                </div>
            </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${branchBalanced}">
                <div class="cbs-eod-check cbs-eod-check-pass">
                    <div class="cbs-eod-check-icon"><i class="bi bi-check-lg"></i></div>
                    <div class="cbs-eod-check-label">Branch balanced</div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="cbs-eod-check cbs-eod-check-fail">
                    <div class="cbs-eod-check-icon"><i class="bi bi-x-lg"></i></div>
                    <div class="cbs-eod-check-label">Branch balanced</div>
                </div>
            </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${clearingGlZero}">
                <div class="cbs-eod-check cbs-eod-check-pass">
                    <div class="cbs-eod-check-icon"><i class="bi bi-check-lg"></i></div>
                    <div class="cbs-eod-check-label">Clearing GL zero</div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="cbs-eod-check cbs-eod-check-fail">
                    <div class="cbs-eod-check-icon"><i class="bi bi-x-lg"></i></div>
                    <div class="cbs-eod-check-label">Clearing GL zero</div>
                </div>
            </c:otherwise>
        </c:choose>

        <c:choose>
            <c:when test="${noPendingAuth}">
                <div class="cbs-eod-check cbs-eod-check-pass">
                    <div class="cbs-eod-check-icon"><i class="bi bi-check-lg"></i></div>
                    <div class="cbs-eod-check-label">No pending authorization</div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="cbs-eod-check cbs-eod-check-fail">
                    <div class="cbs-eod-check-icon"><i class="bi bi-x-lg"></i></div>
                    <div class="cbs-eod-check-label">No pending authorization</div>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<c:if test="${allVouchersPosted && branchBalanced && clearingGlZero && noPendingAuth}">
<div class="text-center">
    <a href="${pageContext.request.contextPath}/eod/run" class="btn btn-primary btn-lg">
        <i class="bi bi-play-circle"></i> Proceed to Run EOD
    </a>
</div>
</c:if>
<c:if test="${!allVouchersPosted || !branchBalanced || !clearingGlZero || !noPendingAuth}">
<div class="alert alert-warning mt-3">
    <i class="bi bi-exclamation-triangle"></i> One or more validations failed. Please resolve the issues before proceeding with EOD.
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
