<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-percent"></i> Loan Rate Management</h3>
    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}"><div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>
<c:if test="${not empty error}"><div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div></c:if>

<%-- Create New Rate --%>
<c:if test="${sessionScope.isAdmin || sessionScope.isManager}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-plus-circle"></i> Create New Rate</h5></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/loan/rates/create"
              onsubmit="return confirm('Confirm rate creation? This will update the product rate and create an audit trail entry.');">
            <div class="row g-3 mb-3">
                <div class="col-md-3">
                    <label class="form-label fw-bold">Product ID <span class="text-danger">*</span></label>
                    <input type="number" name="productId" class="form-control" required placeholder="e.g. 1"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold">Effective Rate (%) <span class="text-danger">*</span></label>
                    <input type="number" name="effectiveRate" step="0.0001" class="form-control" placeholder="e.g. 9.5000"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold">Effective Date <span class="text-danger">*</span></label>
                    <input type="date" name="effectiveDate" class="form-control" required/>
                </div>
                <div class="col-md-3">
                    <label class="form-label fw-bold">Change Reason</label>
                    <select name="changeReason" class="form-select">
                        <option value="PRODUCT_REVISION">Product Revision</option>
                        <option value="BENCHMARK_RESET">Benchmark Reset</option>
                        <option value="RBI_DIRECTIVE">RBI Directive</option>
                        <option value="MANUAL_OVERRIDE">Manual Override</option>
                    </select>
                </div>
            </div>
            <div class="row g-3 mb-3">
                <div class="col-md-3">
                    <label class="form-label">Benchmark Name</label>
                    <input type="text" name="benchmarkName" class="form-control" placeholder="e.g. REPO, MCLR_1Y"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Benchmark Rate (%)</label>
                    <input type="number" name="benchmarkRate" step="0.0001" class="form-control" placeholder="e.g. 6.5000"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Spread (%)</label>
                    <input type="number" name="spread" step="0.0001" class="form-control" placeholder="e.g. 2.5000"/>
                </div>
                <div class="col-md-3">
                    <label class="form-label">Remarks</label>
                    <input type="text" name="remarks" class="form-control" placeholder="Optional remarks"/>
                </div>
            </div>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Create Rate</button>
        </form>
    </div>
</div>
</c:if>

<%-- Current Rates --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-list-ul"></i> Current Rates</h5></div>
    <div class="card-body">
        <c:if test="${empty rates}">
            <div class="alert alert-info">No rates configured for this tenant.</div>
        </c:if>
        <c:if test="${not empty rates}">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th>Product</th>
                        <th>Type</th>
                        <th class="text-end">Rate (%)</th>
                        <th>Benchmark</th>
                        <th class="text-end">Spread</th>
                        <th>Effective From</th>
                        <th>End Date</th>
                        <th>Active</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="rate" items="${rates}">
                    <tr class="${rate.isActive ? '' : 'table-secondary'}">
                        <td><c:out value="${rate.loanProduct.productCode}"/></td>
                        <td><c:out value="${rate.interestType}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${rate.effectiveRate}" maxFractionDigits="4"/>%</td>
                        <td><c:out value="${rate.benchmarkName != null ? rate.benchmarkName : '-'}"/></td>
                        <td class="text-end"><c:if test="${rate.spread != null}"><fmt:formatNumber value="${rate.spread}" maxFractionDigits="4"/>%</c:if></td>
                        <td><c:out value="${rate.effectiveDate}"/></td>
                        <td><c:out value="${rate.endDate != null ? rate.endDate : 'Open'}"/></td>
                        <td>
                            <c:choose>
                                <c:when test="${rate.isActive}"><span class="badge bg-success">Active</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <a href="${pageContext.request.contextPath}/loan/rates/history/${rate.loanProduct.id}" class="btn btn-sm btn-outline-info"><i class="bi bi-clock-history"></i></a>
                            <c:if test="${sessionScope.isAdmin && rate.interestType == 'FLOATING' && rate.isActive}">
                            <form method="post" action="${pageContext.request.contextPath}/loan/rates/propagate" class="d-inline"
                                  onsubmit="return confirm('Propagate this rate to all active FLOATING loans of this product?');">
                                <input type="hidden" name="productId" value="${rate.loanProduct.id}"/>
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-outline-warning" title="Propagate to active loans"><i class="bi bi-broadcast"></i></button>
                            </form>
                            </c:if>
                        </td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
        </c:if>
    </div>
</div>

<%-- Rate Change History --%>
<c:if test="${not empty history}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clock-history"></i> Rate Change History</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr>
                        <th>Date</th>
                        <th>Product</th>
                        <th>Loan</th>
                        <th class="text-end">Old Rate</th>
                        <th class="text-end">New Rate</th>
                        <th>Reason</th>
                        <th>Changed By</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="h" items="${history}">
                    <tr>
                        <td><c:out value="${h.effectiveDate}"/></td>
                        <td><c:out value="${h.loanProduct.productCode}"/></td>
                        <td><c:out value="${h.loanAccount != null ? h.loanAccount.loanAccountNumber : 'Product-level'}"/></td>
                        <td class="text-end"><fmt:formatNumber value="${h.oldRate}" maxFractionDigits="4"/>%</td>
                        <td class="text-end"><fmt:formatNumber value="${h.newRate}" maxFractionDigits="4"/>%</td>
                        <td><c:out value="${h.changeReason}"/></td>
                        <td><c:out value="${h.changedBy}"/></td>
                    </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Rate changes are immutable and audit-logged per RBI Fair Practices Code.
    Floating rate propagation updates loan-level rates and recalculates EMI.
</div>

<%@ include file="../layout/footer.jsp" %>
