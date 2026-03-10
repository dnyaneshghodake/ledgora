<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-building"></i> Inter-Branch Transfers</h3>
    <a href="${pageContext.request.contextPath}/ibt/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> New IBT</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<%-- Filter Section --%>
<div class="card shadow mb-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-funnel"></i> Filter by Status</h6></div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/ibt" class="row g-2">
            <div class="col-md-4">
                <select name="status" class="form-select">
                    <option value="">All Statuses</option>
                    <c:forEach var="s" items="${statuses}">
                        <option value="${s}" ${s.name() == selectedStatus ? 'selected' : ''}><c:out value="${s}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Filter</button>
            </div>
            <c:if test="${not empty selectedStatus}">
                <div class="col-md-2">
                    <a href="${pageContext.request.contextPath}/ibt" class="btn btn-outline-secondary w-100"><i class="bi bi-x-circle"></i> Clear</a>
                </div>
            </c:if>
        </form>
    </div>
</div>

<%-- IBT Transfer Table --%>
<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty transfers}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Business Date</th>
                                <th>From Branch</th>
                                <th>To Branch</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Created By</th>
                                <th>Created At</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="ibt" items="${transfers}">
                            <tr>
                                <td><c:out value="${ibt.id}"/></td>
                                <td><small><c:out value="${ibt.businessDate}"/></small></td>
                                <td>
                                    <c:if test="${ibt.fromBranch != null}">
                                        <span class="badge bg-outline-secondary border"><c:out value="${ibt.fromBranch.branchCode}"/></span>
                                    </c:if>
                                </td>
                                <td>
                                    <c:if test="${ibt.toBranch != null}">
                                        <span class="badge bg-outline-secondary border"><c:out value="${ibt.toBranch.branchCode}"/></span>
                                    </c:if>
                                </td>
                                <td class="fw-bold"><c:out value="${ibt.amount}"/> <small class="text-muted"><c:out value="${ibt.currency}"/></small></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ibt.status == 'INITIATED'}"><span class="badge bg-secondary">INITIATED</span></c:when>
                                        <c:when test="${ibt.status == 'SENT'}"><span class="badge bg-info">SENT</span></c:when>
                                        <c:when test="${ibt.status == 'RECEIVED'}"><span class="badge bg-primary">RECEIVED</span></c:when>
                                        <c:when test="${ibt.status == 'SETTLED'}"><span class="badge bg-success">SETTLED</span></c:when>
                                        <c:when test="${ibt.status == 'FAILED'}"><span class="badge bg-danger">FAILED</span></c:when>
                                        <c:otherwise><span class="badge bg-light text-dark"><c:out value="${ibt.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${ibt.createdBy != null}"><small><c:out value="${ibt.createdBy.username}"/></small></c:if>
                                </td>
                                <td><small><c:out value="${ibt.createdAt}"/></small></td>
                                <td>
                                    <c:if test="${ibt.referenceTransaction != null}">
                                        <a href="${pageContext.request.contextPath}/ibt/${ibt.referenceTransaction.id}" class="btn btn-sm btn-outline-primary" title="View Details">
                                            <i class="bi bi-eye"></i>
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <div class="text-muted mt-2">
                    <small><i class="bi bi-info-circle"></i> Showing <c:out value="${transfers.size()}"/> inter-branch transfer(s).</small>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-building" style="font-size: 3rem;"></i>
                    <p class="mt-2">No inter-branch transfers found.</p>
                    <a href="${pageContext.request.contextPath}/ibt/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> Create New IBT</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- IBT Lifecycle Legend --%>
<div class="card shadow mt-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-diagram-3"></i> IBT Lifecycle</h6></div>
    <div class="card-body">
        <div class="d-flex align-items-center flex-wrap gap-2">
            <span class="badge bg-secondary">INITIATED</span>
            <i class="bi bi-arrow-right"></i>
            <span class="badge bg-info">SENT</span>
            <i class="bi bi-arrow-right"></i>
            <span class="badge bg-primary">RECEIVED</span>
            <i class="bi bi-arrow-right"></i>
            <span class="badge bg-success">SETTLED</span>
            <span class="mx-3 text-muted">|</span>
            <span class="badge bg-danger">FAILED</span>
            <small class="text-muted ms-2">(requires investigation)</small>
        </div>
        <small class="text-muted mt-2 d-block">
            <i class="bi bi-shield-lock"></i>
            EOD blocks if any transfers are not SETTLED or FAILED. Clearing GL must net to zero.
        </small>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
