<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-building"></i> IBT Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/ibt" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
        <a href="${pageContext.request.contextPath}/ibt/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> New IBT</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${error}"/></div>
</c:if>

<c:if test="${transaction != null}">

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Transfer Header</h5></div>
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="220">Transaction Ref</td><td><code class="fs-6"><c:out value="${transaction.transactionRef}"/></code></td></tr>
                    <tr><td class="text-muted">Transaction ID</td><td><code><c:out value="${transaction.id}"/></code></td></tr>
                    <tr><td class="text-muted">Type</td><td><span class="badge bg-info">TRANSFER</span></td></tr>
                    <tr><td class="text-muted">Status</td><td>
                        <c:choose>
                            <c:when test="${transaction.status == 'COMPLETED'}"><span class="badge bg-success">COMPLETED</span></c:when>
                            <c:when test="${transaction.status == 'PENDING_APPROVAL'}"><span class="badge bg-warning text-dark">PENDING APPROVAL</span></c:when>
                            <c:when test="${transaction.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                            <c:otherwise><span class="badge bg-light text-dark"><c:out value="${transaction.status}"/></span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Business Date</td><td><c:out value="${transaction.businessDate}"/></td></tr>
                    <tr><td class="text-muted">Amount</td><td><span class="fs-5 fw-bold text-primary"><c:out value="${transaction.amount}"/> <c:out value="${transaction.currency}"/></span></td></tr>
                    <tr><td class="text-muted">Source Account</td><td>
                        <c:if test="${transaction.sourceAccount != null}">
                            <code><c:out value="${transaction.sourceAccount.accountNumber}"/></code>
                            <c:if test="${transaction.sourceAccount.branch != null}">
                                <span class="badge bg-secondary ms-2"><c:out value="${transaction.sourceAccount.branch.branchCode}"/></span>
                            </c:if>
                        </c:if>
                    </td></tr>
                    <tr><td class="text-muted">Destination Account</td><td>
                        <c:if test="${transaction.destinationAccount != null}">
                            <code><c:out value="${transaction.destinationAccount.accountNumber}"/></code>
                            <c:if test="${transaction.destinationAccount.branch != null}">
                                <span class="badge bg-secondary ms-2"><c:out value="${transaction.destinationAccount.branch.branchCode}"/></span>
                            </c:if>
                        </c:if>
                    </td></tr>
                    <tr><td class="text-muted">Narration</td><td><c:out value="${transaction.narration}"/></td></tr>
                    <tr><td class="text-muted">Maker</td><td>
                        <c:choose>
                            <c:when test="${transaction.maker != null}"><c:out value="${transaction.maker.username}"/></c:when>
                            <c:when test="${transaction.performedBy != null}"><c:out value="${transaction.performedBy.username}"/></c:when>
                            <c:otherwise><span class="text-muted">System</span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Checker</td><td>
                        <c:choose>
                            <c:when test="${transaction.checker != null}"><c:out value="${transaction.checker.username}"/></c:when>
                            <c:when test="${transaction.status == 'PENDING_APPROVAL'}"><span class="badge bg-warning text-dark">Awaiting checker</span></c:when>
                            <c:otherwise><span class="text-muted">Auto-authorized / N/A</span></c:otherwise>
                        </c:choose>
                    </td></tr>
                </table>

                <div class="mt-3">
                    <a href="${pageContext.request.contextPath}/transactions/${transaction.id}" class="btn btn-outline-secondary btn-sm">
                        <i class="bi bi-receipt"></i> View Transaction Screen
                    </a>
                    <a href="${pageContext.request.contextPath}/transactions/${transaction.id}/view" class="btn btn-outline-dark btn-sm">
                        <i class="bi bi-eye-fill"></i> Transaction 360° View
                    </a>
                </div>
            </div>
        </div>

        <div class="card shadow mt-4">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h5 class="mb-0">Vouchers (Expected: 4 for IBT)</h5>
                <span class="badge ${voucherCount == 4 ? 'bg-success' : 'bg-warning text-dark'}">
                    Count: <c:out value="${voucherCount}"/>
                </span>
            </div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${not empty vouchers}">
                        <div class="table-responsive">
                            <table class="table table-sm table-hover">
                                <thead class="table-light">
                                    <tr>
                                        <th>ID</th>
                                        <th>Voucher No</th>
                                        <th>Branch</th>
                                        <th>DR/CR</th>
                                        <th>Account</th>
                                        <th>Amount</th>
                                        <th>Auth</th>
                                        <th>Post</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="v" items="${vouchers}">
                                    <tr>
                                        <td><c:out value="${v.id}"/></td>
                                        <td><code><c:out value="${v.voucherNumber}"/></code></td>
                                        <td><c:out value="${v.branch != null ? v.branch.branchCode : ''}"/></td>
                                        <td>
                                            <span class="badge ${v.drCr == 'DR' ? 'bg-danger' : 'bg-success'}"><c:out value="${v.drCr}"/></span>
                                        </td>
                                        <td><code><c:out value="${v.account != null ? v.account.accountNumber : ''}"/></code></td>
                                        <td><c:out value="${v.transactionAmount}"/></td>
                                        <td><span class="badge ${v.authFlag == 'Y' ? 'bg-success' : 'bg-secondary'}"><c:out value="${v.authFlag}"/></span></td>
                                        <td><span class="badge ${v.postFlag == 'Y' ? 'bg-success' : 'bg-secondary'}"><c:out value="${v.postFlag}"/></span></td>
                                        <td>
                                            <a href="${pageContext.request.contextPath}/vouchers/${v.id}" class="btn btn-sm btn-outline-primary" title="View Voucher">
                                                <i class="bi bi-eye"></i>
                                            </a>
                                        </td>
                                    </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="text-center py-3 text-muted">No vouchers found for this transaction.</div>
                    </c:otherwise>
                </c:choose>

                <c:if test="${voucherCount != 4}">
                    <div class="alert alert-warning mt-3">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>
                        This transaction does not currently have 4 vouchers. It may be pending approval,
                        failed, or not an inter-branch transfer.
                    </div>
                </c:if>
            </div>
        </div>

        <div class="card shadow mt-4">
            <div class="card-header bg-white"><h5 class="mb-0">Inter-Branch Transfer Record</h5></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${ibtRecord != null}">
                        <table class="table table-borderless table-sm">
                            <tr><td class="text-muted" width="220">IBT ID</td><td><code><c:out value="${ibtRecord.id}"/></code></td></tr>
                            <tr><td class="text-muted">Status</td><td><span class="badge bg-secondary"><c:out value="${ibtRecord.status}"/></span></td></tr>
                            <tr><td class="text-muted">From Branch</td><td><c:out value="${ibtRecord.fromBranch != null ? ibtRecord.fromBranch.branchCode : ''}"/></td></tr>
                            <tr><td class="text-muted">To Branch</td><td><c:out value="${ibtRecord.toBranch != null ? ibtRecord.toBranch.branchCode : ''}"/></td></tr>
                            <tr><td class="text-muted">Amount</td><td><c:out value="${ibtRecord.amount}"/> <c:out value="${ibtRecord.currency}"/></td></tr>
                            <tr><td class="text-muted">Created By</td><td><c:out value="${ibtRecord.createdBy != null ? ibtRecord.createdBy.username : ''}"/></td></tr>
                            <tr><td class="text-muted">Approved By</td><td><c:out value="${ibtRecord.approvedBy != null ? ibtRecord.approvedBy.username : ''}"/></td></tr>
                            <tr><td class="text-muted">Failure Reason</td><td><c:out value="${ibtRecord.failureReason}"/></td></tr>
                            <tr><td class="text-muted">Created At</td><td><c:out value="${ibtRecord.createdAt}"/></td></tr>
                            <tr><td class="text-muted">Updated At</td><td><c:out value="${ibtRecord.updatedAt}"/></td></tr>
                        </table>
                    </c:when>
                    <c:otherwise>
                        <div class="text-muted">No InterBranchTransfer record found. This may be a same-branch transfer or the transaction is pending approval.</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

    </div>

    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-diagram-3"></i> Clearing GL (IBC Accounts)</h5></div>
            <div class="card-body">
                <c:choose>
                    <c:when test="${not empty clearingAccounts}">
                        <div class="table-responsive">
                            <table class="table table-sm">
                                <thead class="table-light">
                                    <tr>
                                        <th>Account</th>
                                        <th>Balance</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="a" items="${clearingAccounts}">
                                    <tr>
                                        <td><code><c:out value="${a.accountNumber}"/></code></td>
                                        <td><c:out value="${a.balance}"/></td>
                                    </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                        <small class="text-muted">
                            <i class="bi bi-shield-lock"></i> Clearing GL must net to zero before EOD.
                        </small>
                    </c:when>
                    <c:otherwise>
                        <div class="text-muted">No clearing accounts found for tenant.</div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <%-- Audit Info Section --%>
        <c:set var="auditCreatedBy" value="${transaction.performedBy != null ? transaction.performedBy.username : (transaction.maker != null ? transaction.maker.username : 'System')}" scope="request"/>
        <c:set var="auditCreatedAt" value="${transaction.createdAt}" scope="request"/>
        <c:set var="auditUpdatedAt" value="${transaction.updatedAt}" scope="request"/>
        <c:set var="auditApprovalStatus" value="${transaction.status != null ? transaction.status.name() : ''}" scope="request"/>
        <c:set var="auditCurrentStatus" value="${transaction.status != null ? transaction.status.name() : ''}" scope="request"/>
        <c:set var="auditEntityType" value="IBT (Transfer)" scope="request"/>
        <c:set var="auditEntityId" value="${transaction.transactionRef}" scope="request"/>
        <%@ include file="../layout/audit-info.jsp" %>
    </div>
</div>

</c:if>

<%@ include file="../layout/footer.jsp" %>
