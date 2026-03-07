<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <div class="row mb-4">
        <div class="col-12">
            <h2><i class="bi bi-collection"></i> Batch Dashboard</h2>
            <p class="text-muted">Transaction batch management - view open, closed, and settled batches by channel.</p>
        </div>
    </div>

    <%-- Summary Cards --%>
    <div class="row mb-4">
        <div class="col-md-4">
            <div class="card border-primary">
                <div class="card-body text-center">
                    <h5 class="card-title text-primary"><i class="bi bi-unlock"></i> Open Batches</h5>
                    <h2 class="display-6">${openCount}</h2>
                    <p class="text-muted">Currently accepting transactions</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-warning">
                <div class="card-body text-center">
                    <h5 class="card-title text-warning"><i class="bi bi-lock"></i> Closed Batches</h5>
                    <h2 class="display-6">${closedCount}</h2>
                    <p class="text-muted">No longer accepting transactions</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-success">
                <div class="card-body text-center">
                    <h5 class="card-title text-success"><i class="bi bi-check-circle"></i> Settled Batches</h5>
                    <h2 class="display-6">${settledCount}</h2>
                    <p class="text-muted">Fully reconciled and finalized</p>
                </div>
            </div>
        </div>
    </div>

    <%-- Open Batches Table --%>
    <div class="card mb-4">
        <div class="card-header bg-primary text-white">
            <h5 class="mb-0"><i class="bi bi-unlock"></i> Open Batches</h5>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty openBatches}">
                    <div class="table-responsive">
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Batch Code</th>
                                    <th>Batch Type</th>
                                    <th>Branch</th>
                                    <th>Business Date</th>
                                    <th>Transactions</th>
                                    <th>Total Debit</th>
                                    <th>Total Credit</th>
                                    <th>Balanced?</th>
                                    <th>Status</th>
                                    <th>Created</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="batch" items="${openBatches}">
                                    <tr>
                                        <td><code>${not empty batch.batchCode ? batch.batchCode : batch.id}</code></td>
                                        <td><span class="badge bg-info">${batch.batchType}</span></td>
                                        <td>${not empty batch.branchCode ? batch.branchCode : '-'}</td>
                                        <td>${batch.businessDate}</td>
                                        <td>${batch.transactionCount}</td>
                                        <td class="text-end">${batch.totalDebit}</td>
                                        <td class="text-end">${batch.totalCredit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${batch.totalDebit == batch.totalCredit}"><span class="badge bg-success">Y</span></c:when>
                                                <c:otherwise><span class="badge bg-danger">N</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><span class="badge bg-primary">OPEN</span></td>
                                        <td>${batch.createdAt}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="text-muted text-center">No open batches.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Closed Batches Table --%>
    <div class="card mb-4">
        <div class="card-header bg-warning text-dark">
            <h5 class="mb-0"><i class="bi bi-lock"></i> Closed Batches</h5>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty closedBatches}">
                    <div class="table-responsive">
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Batch Code</th>
                                    <th>Batch Type</th>
                                    <th>Branch</th>
                                    <th>Business Date</th>
                                    <th>Transactions</th>
                                    <th>Total Debit</th>
                                    <th>Total Credit</th>
                                    <th>Balanced?</th>
                                    <th>Status</th>
                                    <th>Closed At</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="batch" items="${closedBatches}">
                                    <tr>
                                        <td><code>${not empty batch.batchCode ? batch.batchCode : batch.id}</code></td>
                                        <td><span class="badge bg-warning text-dark">${batch.batchType}</span></td>
                                        <td>${not empty batch.branchCode ? batch.branchCode : '-'}</td>
                                        <td>${batch.businessDate}</td>
                                        <td>${batch.transactionCount}</td>
                                        <td class="text-end">${batch.totalDebit}</td>
                                        <td class="text-end">${batch.totalCredit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${batch.totalDebit == batch.totalCredit}"><span class="badge bg-success">Y</span></c:when>
                                                <c:otherwise><span class="badge bg-danger">N</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><span class="badge bg-warning text-dark">CLOSED</span></td>
                                        <td>${batch.closedAt}</td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="text-muted text-center">No closed batches.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <%-- Settled Batches Table --%>
    <div class="card mb-4">
        <div class="card-header bg-success text-white">
            <h5 class="mb-0"><i class="bi bi-check-circle"></i> Settled Batches</h5>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty settledBatches}">
                    <div class="table-responsive">
                        <table class="table table-striped table-hover">
                            <thead>
                                <tr>
                                    <th>Batch Code</th>
                                    <th>Batch Type</th>
                                    <th>Branch</th>
                                    <th>Business Date</th>
                                    <th>Transactions</th>
                                    <th>Total Debit</th>
                                    <th>Total Credit</th>
                                    <th>Balanced?</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="batch" items="${settledBatches}">
                                    <tr>
                                        <td><code>${not empty batch.batchCode ? batch.batchCode : batch.id}</code></td>
                                        <td><span class="badge bg-success">${batch.batchType}</span></td>
                                        <td>${not empty batch.branchCode ? batch.branchCode : '-'}</td>
                                        <td>${batch.businessDate}</td>
                                        <td>${batch.transactionCount}</td>
                                        <td class="text-end">${batch.totalDebit}</td>
                                        <td class="text-end">${batch.totalCredit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${batch.totalDebit == batch.totalCredit}">
                                                    <span class="badge bg-success">Y</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-danger">N</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><span class="badge bg-success">SETTLED</span></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="text-muted text-center">No settled batches.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
