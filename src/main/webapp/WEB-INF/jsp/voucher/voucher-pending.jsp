<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-clipboard-check"></i> Pending Authorization</h3>
    <a href="${pageContext.request.contextPath}/vouchers" class="btn btn-secondary">
        <i class="bi bi-arrow-left"></i> Voucher Inquiry
    </a>
</div>

<div class="card shadow">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0">Vouchers Awaiting Authorization</h5>
        <span class="badge bg-warning text-dark">${not empty vouchers ? vouchers.size() : 0} Pending</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty vouchers}">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>Voucher ID</th>
                                <th>Batch Code</th>
                                <th>Scroll No</th>
                                <th>Debit A/C</th>
                                <th>Credit A/C</th>
                                <th>Amount</th>
                                <th>Business Date</th>
                                <th>Branch</th>
                                <th>Status</th>
                                <th>Actions</th>
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
                                <td><span class="cbs-voucher-status cbs-voucher-pending">PENDING</span></td>
                                <td>
                                    <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
                                    <form method="post" action="${pageContext.request.contextPath}/vouchers/${v.id}/authorize" class="d-inline">
                                        <button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Authorize this voucher?')">
                                            <i class="bi bi-check-lg"></i> Authorize
                                        </button>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/vouchers/${v.id}/reject" class="d-inline ms-1">
                                        <button type="submit" class="btn btn-sm btn-outline-danger" onclick="return confirm('Reject this voucher?')">
                                            <i class="bi bi-x-lg"></i> Reject
                                        </button>
                                    </form>
                                    </c:if>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-5 text-muted">
                    <i class="bi bi-inbox" style="font-size: 3rem;"></i>
                    <p class="mt-2">No vouchers pending authorization.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
