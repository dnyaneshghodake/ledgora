<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-clock-history"></i> Rate History — Product #<c:out value="${productId}"/></h3>
    <a href="${pageContext.request.contextPath}/loan/rates" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Rates</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Rate History Table --%>
<c:if test="${not empty rates}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-list-ul"></i> Rate Versions</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th class="text-end">Rate (%)</th>
                        <th>Type</th>
                        <th>Benchmark</th>
                        <th class="text-end">Spread</th>
                        <th>Effective From</th>
                        <th>End Date</th>
                        <th>Active</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="rate" items="${rates}">
                    <tr class="${rate.isActive ? 'table-success' : ''}">
                        <td class="text-end fw-bold"><fmt:formatNumber value="${rate.effectiveRate}" maxFractionDigits="4"/>%</td>
                        <td><c:out value="${rate.interestType}"/></td>
                        <td><c:out value="${rate.benchmarkName != null ? rate.benchmarkName : '-'}"/></td>
                        <td class="text-end"><c:if test="${rate.spread != null}"><fmt:formatNumber value="${rate.spread}" maxFractionDigits="4"/>%</c:if></td>
                        <td><c:out value="${rate.effectiveDate}"/></td>
                        <td><c:out value="${rate.endDate != null ? rate.endDate : 'Open'}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${rate.isActive}"><span class="badge bg-success">Active</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">Closed</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<%-- Change History (Immutable Audit Trail) --%>
<c:if test="${not empty history}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-shield-lock"></i> Change Audit Trail</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th>Timestamp</th>
                        <th>Effective Date</th>
                        <th>Loan</th>
                        <th class="text-end">Old Rate</th>
                        <th class="text-end">New Rate</th>
                        <th class="text-end">Old EMI</th>
                        <th class="text-end">New EMI</th>
                        <th>Reason</th>
                        <th>Changed By</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="h" items="${history}">
                    <tr>
                        <td><c:out value="${h.createdAt}"/></td>
                        <td><c:out value="${h.effectiveDate}"/></td>
                        <td><c:out value="${h.loanAccount != null ? h.loanAccount.loanAccountNumber : 'Product-level'}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${h.oldRate}" maxFractionDigits="4"/>%</td>
                        <td class="text-end"><fmt:formatNumber value="${h.newRate}" maxFractionDigits="4"/>%</td>
                        <td class="text-end"><c:if test="${h.oldEmi != null}"><fmt:formatNumber value="${h.oldEmi}" maxFractionDigits="2"/></c:if></td>
                        <td class="text-end"><c:if test="${h.newEmi != null}"><fmt:formatNumber value="${h.newEmi}" maxFractionDigits="2"/></c:if></td>
                        <td><span class="badge bg-info"><c:out value="${h.changeReason}"/></span></td>
                        <td><c:out value="${h.changedBy}"/></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<c:if test="${empty rates && empty history}">
    <div class="alert alert-info">No rate history found for this product.</div>
</c:if>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Rate change history is immutable per RBI audit requirements. Each entry records old/new rate, EMI impact, and initiator.
</div>

<%@ include file="../layout/footer.jsp" %>
