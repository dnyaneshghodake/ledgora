<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2><i class="bi bi-shield-check"></i> Audit Logs</h2>
    </div>

    <div class="card mb-4">
        <div class="card-header">
            <h5 class="mb-0">Filter</h5>
        </div>
        <div class="card-body">
            <form method="get" action="${pageContext.request.contextPath}/admin/audit" class="row g-3">
                <div class="col-md-4">
                    <label for="entity" class="form-label">Entity Type</label>
                    <select class="form-select" id="entity" name="entity">
                        <option value="">All</option>
                        <option value="TRANSACTION" ${filterEntity == 'TRANSACTION' ? 'selected' : ''}>Transaction</option>
                        <option value="ACCOUNT" ${filterEntity == 'ACCOUNT' ? 'selected' : ''}>Account</option>
                        <option value="VOUCHER" ${filterEntity == 'VOUCHER' ? 'selected' : ''}>Voucher</option>
                        <option value="USER" ${filterEntity == 'USER' ? 'selected' : ''}>User</option>
                        <option value="SETTLEMENT" ${filterEntity == 'SETTLEMENT' ? 'selected' : ''}>Settlement</option>
                        <option value="APPROVAL" ${filterEntity == 'APPROVAL' ? 'selected' : ''}>Approval</option>
                    </select>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-filter"></i> Filter</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card">
        <div class="card-header"><h5 class="mb-0">Audit Trail <span class="badge bg-secondary">${auditLogs.size()} entries</span></h5></div>
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-striped table-hover table-sm">
                    <thead class="table-dark">
                        <tr>
                            <th>ID</th>
                            <th>Timestamp</th>
                            <th>Action</th>
                            <th>Entity</th>
                            <th>Entity ID</th>
                            <th>User ID</th>
                            <th>Details</th>
                            <th>IP Address</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="log" items="${auditLogs}">
                            <tr>
                                <td>${log.id}</td>
                                <td><small>${log.timestamp}</small></td>
                                <td><span class="badge bg-info">${log.action}</span></td>
                                <td><code>${log.entity}</code></td>
                                <td>${log.entityId}</td>
                                <td>${log.userId}</td>
                                <td><small>${log.details}</small></td>
                                <td><small>${log.ipAddress}</small></td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
