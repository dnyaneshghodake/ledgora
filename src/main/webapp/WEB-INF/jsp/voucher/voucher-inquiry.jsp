<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-search"></i> Voucher Inquiry</h3>
    <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager || sessionScope.isTeller}">
    <a href="${pageContext.request.contextPath}/vouchers/create" class="btn btn-primary cbs-lockable">
        <i class="bi bi-plus-square"></i> Create Voucher
    </a>
    </c:if>
</div>

<%-- Search Filters --%>
<div class="card shadow mb-4">
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/vouchers" class="row g-3">
            <div class="col-md-3">
                <label class="form-label">Batch Code</label>
                <input type="text" name="batchCode" class="form-control" value="${param.batchCode}" placeholder="Batch Code" />
            </div>
            <div class="col-md-3">
                <label class="form-label">Scroll No</label>
                <input type="text" name="scrollNo" class="form-control" value="${param.scrollNo}" placeholder="Scroll No" />
            </div>
            <div class="col-md-3">
                <label class="form-label">Business Date</label>
                <input type="date" name="businessDate" class="form-control" value="${param.businessDate}" />
            </div>
            <div class="col-md-3">
                <label class="form-label">Status</label>
                <select name="status" class="form-select">
                    <option value="">All</option>
                    <option value="PENDING" ${param.status == 'PENDING' ? 'selected' : ''}>Pending</option>
                    <option value="AUTHORIZED" ${param.status == 'AUTHORIZED' ? 'selected' : ''}>Authorized</option>
                    <option value="POSTED" ${param.status == 'POSTED' ? 'selected' : ''}>Posted</option>
                    <option value="CANCELLED" ${param.status == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                </select>
            </div>
            <div class="col-12">
                <button type="submit" class="btn btn-primary"><i class="bi bi-search"></i> Search</button>
                <a href="${pageContext.request.contextPath}/vouchers" class="btn btn-outline-secondary ms-2">Clear</a>
            </div>
        </form>
    </div>
</div>

<%-- Results --%>
<div class="card shadow">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0">Voucher Results</h5>
        <span class="badge bg-secondary">${not empty vouchers ? vouchers.size() : 0} records</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty vouchers}">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Batch Code</th>
                                <th>Scroll No</th>
                                <th>Debit A/C</th>
                                <th>Credit A/C</th>
                                <th>Amount</th>
                                <th>Business Date</th>
                                <th>Branch</th>
                                <th>Tenant</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="v" items="${vouchers}">
                            <tr>
                                <td><code>${v.id}</code></td>
                                <td>${v.batchCode}</td>
                                <td>${v.scrollNo}</td>
                                <td>${v.debitAccountNumber}</td>
                                <td>${v.creditAccountNumber}</td>
                                <td class="fw-bold">${v.amount} ${v.currency}</td>
                                <td>${v.businessDate}</td>
                                <td>${v.branchCode}</td>
                                <td>${v.tenantCode}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${v.status == 'PENDING'}"><span class="cbs-voucher-status cbs-voucher-pending">PENDING</span></c:when>
                                        <c:when test="${v.status == 'AUTHORIZED'}"><span class="cbs-voucher-status cbs-voucher-authorized">AUTHORIZED</span></c:when>
                                        <c:when test="${v.status == 'POSTED'}"><span class="cbs-voucher-status cbs-voucher-posted">POSTED</span></c:when>
                                        <c:when test="${v.status == 'CANCELLED'}"><span class="cbs-voucher-status cbs-voucher-cancelled">CANCELLED</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">${v.status}</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-5 text-muted">
                    <i class="bi bi-search" style="font-size: 3rem;"></i>
                    <p class="mt-2">Use the filters above to search vouchers.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
