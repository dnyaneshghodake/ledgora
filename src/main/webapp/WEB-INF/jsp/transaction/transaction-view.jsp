<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-receipt"></i> Transaction Details</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-secondary">Back</a>
</div>

<div class="row">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0">Transaction Information</h5></div>
            <div class="card-body">
                <table class="table table-borderless">
                    <tr><td class="text-muted" width="200">Reference</td><td><code class="fs-5"><c:out value="${transaction.transactionRef}"/></code></td></tr>
                    <tr><td class="text-muted">Type</td><td>
                        <c:choose>
                            <c:when test="${transaction.transactionType == 'DEPOSIT'}"><span class="badge bg-success fs-6">DEPOSIT</span></c:when>
                            <c:when test="${transaction.transactionType == 'WITHDRAWAL'}"><span class="badge bg-danger fs-6">WITHDRAWAL</span></c:when>
                            <c:when test="${transaction.transactionType == 'TRANSFER'}"><span class="badge bg-info fs-6">TRANSFER</span></c:when>
                            <c:otherwise><span class="badge bg-secondary fs-6"><c:out value="${transaction.transactionType}"/></span></c:otherwise>
                        </c:choose>
                    </td></tr>
                    <tr><td class="text-muted">Status</td><td><span class="badge bg-success">${transaction.status}</span></td></tr>
                    <tr><td class="text-muted">Amount</td><td><span class="fs-4 fw-bold text-primary">${transaction.amount} ${transaction.currency}</span></td></tr>
                    <tr><td class="text-muted">Source Account</td><td><c:if test="${transaction.sourceAccount != null}"><code><c:out value="${transaction.sourceAccount.accountNumber}"/></code> - <c:out value="${transaction.sourceAccount.customerName}"/></c:if><c:if test="${transaction.sourceAccount == null}">N/A</c:if></td></tr>
                    <tr><td class="text-muted">Destination Account</td><td><c:if test="${transaction.destinationAccount != null}"><code><c:out value="${transaction.destinationAccount.accountNumber}"/></code> - <c:out value="${transaction.destinationAccount.customerName}"/></c:if><c:if test="${transaction.destinationAccount == null}">N/A</c:if></td></tr>
                    <tr><td class="text-muted">Description</td><td><c:out value="${transaction.description}"/></td></tr>
                    <tr><td class="text-muted">Narration</td><td><c:out value="${transaction.narration}"/></td></tr>
                    <tr><td class="text-muted">Performed By</td><td><c:if test="${transaction.performedBy != null}"><c:out value="${transaction.performedBy.username}"/></c:if></td></tr>
                    <tr><td class="text-muted">Date</td><td>${transaction.createdAt}</td></tr>
                </table>
            </div>
        </div>

        <%-- RBI Audit Trail Panel (TASK 5) --%>
        <div class="card shadow mt-4">
            <div class="card-header bg-white">
                <h5 class="mb-0"><i class="bi bi-shield-lock"></i> RBI Audit Trail</h5>
            </div>
            <div class="card-body">
                <table class="table table-sm table-borderless">
                    <tr>
                        <td class="text-muted" width="200">Transaction ID</td>
                        <td><code><c:out value="${transaction.id}"/></code></td>
                    </tr>
                    <tr>
                        <td class="text-muted">Transaction Reference</td>
                        <td><code><c:out value="${transaction.transactionRef}"/></code></td>
                    </tr>
                    <tr>
                        <td class="text-muted">Idempotency Key</td>
                        <td>
                            <c:choose>
                                <c:when test="${transaction.clientReferenceId != null && !transaction.clientReferenceId.isEmpty()}">
                                    <code><c:out value="${transaction.clientReferenceId}"/></code>
                                </c:when>
                                <c:otherwise><span class="text-muted">N/A</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="text-muted">Channel</td>
                        <td>
                            <c:choose>
                                <c:when test="${transaction.channel != null}">
                                    <span class="badge bg-secondary"><c:out value="${transaction.channel}"/></span>
                                </c:when>
                                <c:otherwise><span class="text-muted">N/A</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="text-muted">Maker (Performed By)</td>
                        <td>
                            <c:choose>
                                <c:when test="${transaction.performedBy != null}">
                                    <i class="bi bi-person"></i> <c:out value="${transaction.performedBy.username}"/>
                                </c:when>
                                <c:otherwise><span class="text-muted">System</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    <tr>
                        <td class="text-muted">Checker (Approver)</td>
                        <td><span class="text-muted">See Approval History</span></td>
                    </tr>
                    <tr>
                        <td class="text-muted">Business Date</td>
                        <td>${transaction.businessDate}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Created At</td>
                        <td>${transaction.createdAt}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Updated At</td>
                        <td>${transaction.updatedAt}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Value Date</td>
                        <td>${transaction.valueDate}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Journal Entries</td>
                        <td>
                            <c:forEach var="entry" items="${ledgerEntries}">
                                <c:if test="${entry.journal != null}">
                                    <a href="${pageContext.request.contextPath}/ledger/explorer?businessDate=${entry.businessDate}">
                                        <code>J-${entry.journal.id}</code>
                                    </a>
                                </c:if>
                            </c:forEach>
                            <c:if test="${empty ledgerEntries}"><span class="text-muted">None</span></c:if>
                        </td>
                    </tr>
                    <tr>
                        <td class="text-muted">Audit Trail</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/admin/audit" class="btn btn-outline-secondary btn-sm">
                                <i class="bi bi-clock-history"></i> View Audit Trail
                            </a>
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card shadow">
            <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-journal-text"></i> Ledger Entries</h5></div>
            <div class="card-body">
                <c:forEach var="entry" items="${ledgerEntries}">
                    <div class="border rounded p-2 mb-2">
                        <div class="d-flex justify-content-between">
                            <span class="badge ${entry.entryType == 'DEBIT' ? 'bg-danger' : 'bg-success'}">${entry.entryType}</span>
                            <strong>${entry.amount}</strong>
                        </div>
                        <small class="text-muted">GL: <c:out value="${entry.glAccountCode}"/></small><br>
                        <small><c:out value="${entry.narration}"/></small><br>
                        <small class="text-muted">Balance After: ${entry.balanceAfter}</small>
                    </div>
                </c:forEach>
                <c:if test="${empty ledgerEntries}">
                    <p class="text-muted text-center">No ledger entries</p>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%-- Audit Disclaimer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All ledger entries are immutable. System of Record: LedgerEntries.
</div>

<%@ include file="../layout/footer.jsp" %>
