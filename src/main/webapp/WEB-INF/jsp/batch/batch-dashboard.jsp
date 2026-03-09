<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="container-fluid">
    <%-- Page Title --%>
    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h2><i class="bi bi-collection"></i> Batch Dashboard</h2>
            <p class="text-muted mb-0">Transaction batch management - view open, closed, and settled batches by channel.</p>
        </div>
    </div>

    <%-- Operational Status Banner --%>
    <%@ include file="../layout/status-banner.jsp" %>

    <c:if test="${not empty message}">
        <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
    </c:if>
    <c:if test="${not empty error}">
        <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
    </c:if>

    <%-- Batch Actions Bar --%>
    <div class="card shadow mb-4">
        <div class="card-body">
            <div class="d-flex align-items-center flex-wrap gap-3">
                <strong><i class="bi bi-gear"></i> Batch Operations:</strong>

                <%-- Open New Batch --%>
                <form method="post" action="${pageContext.request.contextPath}/batches/open" class="d-inline-flex align-items-center gap-2">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <select name="channel" class="form-select form-select-sm" style="width: auto;" required>
                        <option value="TELLER">TELLER</option>
                        <option value="ATM">ATM</option>
                        <option value="ONLINE">ONLINE</option>
                        <option value="MOBILE">MOBILE</option>
                        <option value="BATCH">BATCH</option>
                    </select>
                    <button type="submit" class="btn btn-primary btn-sm">
                        <i class="bi bi-plus-circle"></i> Open New Batch
                    </button>
                </form>

                <span class="text-muted">|</span>

                <%-- Close All --%>
                <form method="post" action="${pageContext.request.contextPath}/batches/close-all" class="d-inline">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <button type="submit" class="btn btn-warning btn-sm" onclick="return confirm('Close ALL open batches for today? This validates balance on each batch.')">
                        <i class="bi bi-lock"></i> Close All Open
                    </button>
                </form>

                <%-- Settle All --%>
                <form method="post" action="${pageContext.request.contextPath}/batches/settle-all" class="d-inline">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <button type="submit" class="btn btn-success btn-sm" onclick="return confirm('Settle ALL closed batches? Each batch must be balanced (debit = credit).')">
                        <i class="bi bi-check-circle"></i> Settle All Closed
                    </button>
                </form>
            </div>
            <small class="text-muted mt-2 d-block">
                <i class="bi bi-info-circle"></i> CBS Rule: Closed batches cannot be reopened. Use "Open New Batch" to create a fresh batch for the current business date.
            </small>
        </div>
    </div>

    <%-- Summary Cards --%>
    <div class="row mb-4">
        <div class="col-md-4">
            <div class="card border-primary">
                <div class="card-body text-center">
                    <h5 class="card-title text-primary"><i class="bi bi-unlock"></i> Open Batches</h5>
                    <h2 class="display-6"><c:out value="${openCount}"/></h2>
                    <p class="text-muted">Currently accepting transactions</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-warning">
                <div class="card-body text-center">
                    <h5 class="card-title text-warning"><i class="bi bi-lock"></i> Closed Batches</h5>
                    <h2 class="display-6"><c:out value="${closedCount}"/></h2>
                    <p class="text-muted">No longer accepting transactions</p>
                </div>
            </div>
        </div>
        <div class="col-md-4">
            <div class="card border-success">
                <div class="card-body text-center">
                    <h5 class="card-title text-success"><i class="bi bi-check-circle"></i> Settled Batches</h5>
                    <h2 class="display-6"><c:out value="${settledCount}"/></h2>
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
                                    <th>Tenant</th>
                                    <th>Business Date</th>
                                    <th>Transactions</th>
                                    <th>Total Debit</th>
                                    <th>Total Credit</th>
                                    <th>Balanced?</th>
                                    <th>Status</th>
                                    <th>Created</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="batch" items="${openBatches}">
                                    <tr>
                                        <td><code><c:out value="${not empty batch.batchCode ? batch.batchCode : batch.id}"/></code></td>
                                        <td><span class="badge bg-info"><c:out value="${batch.batchType}"/></span></td>
                                        <td><c:out value="${batch.tenant != null ? batch.tenant.tenantName : '-'}"/></td>
                                        <td><c:out value="${batch.businessDate}"/></td>
                                        <td><c:out value="${batch.transactionCount}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalDebit}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalCredit}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${batch.totalDebit == batch.totalCredit}"><span class="badge bg-success">Y</span></c:when>
                                                <c:otherwise><span class="badge bg-danger">N</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><span class="badge bg-primary">OPEN</span></td>
                                        <td><c:out value="${batch.createdAt}"/></td>
                                        <td>
                                            <form method="post" action="${pageContext.request.contextPath}/batches/${batch.id}/close" class="d-inline">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                                <button type="submit" class="btn btn-warning btn-sm"
                                                        onclick="return confirm('Close batch ${batch.batchCode}? Batch must be balanced.')">
                                                    <i class="bi bi-lock"></i> Close
                                                </button>
                                            </form>
                                        </td>
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
                                    <th>Tenant</th>
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
                                        <td><code><c:out value="${not empty batch.batchCode ? batch.batchCode : batch.id}"/></code></td>
                                        <td><span class="badge bg-warning text-dark"><c:out value="${batch.batchType}"/></span></td>
                                        <td><c:out value="${batch.tenant != null ? batch.tenant.tenantName : '-'}"/></td>
                                        <td><c:out value="${batch.businessDate}"/></td>
                                        <td><c:out value="${batch.transactionCount}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalDebit}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalCredit}"/></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${batch.totalDebit == batch.totalCredit}"><span class="badge bg-success">Y</span></c:when>
                                                <c:otherwise><span class="badge bg-danger">N</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><span class="badge bg-warning text-dark">CLOSED</span></td>
                                        <td><c:out value="${batch.closedAt}"/></td>
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
                                    <th>Tenant</th>
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
                                        <td><code><c:out value="${not empty batch.batchCode ? batch.batchCode : batch.id}"/></code></td>
                                        <td><span class="badge bg-success"><c:out value="${batch.batchType}"/></span></td>
                                        <td><c:out value="${batch.tenant != null ? batch.tenant.tenantName : '-'}"/></td>
                                        <td><c:out value="${batch.businessDate}"/></td>
                                        <td><c:out value="${batch.transactionCount}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalDebit}"/></td>
                                        <td class="text-end"><c:out value="${batch.totalCredit}"/></td>
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
