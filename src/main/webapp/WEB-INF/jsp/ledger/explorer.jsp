<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-journal-text"></i> Ledger Explorer <small class="text-muted">System of Record</small></h3>
</div>

<%-- Filter Panel --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white">
        <h6 class="mb-0"><i class="bi bi-funnel"></i> Filters</h6>
    </div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/ledger/explorer">
            <div class="row g-3">
                <div class="col-md-2">
                    <label class="form-label">GL Code</label>
                    <input type="text" class="form-control form-control-sm" name="glCode" value="${filterGlCode}" placeholder="e.g. 1000">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Account Number</label>
                    <input type="text" class="form-control form-control-sm" name="accountNumber" value="${filterAccountNumber}" placeholder="e.g. ACC-001">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Business Date</label>
                    <input type="date" class="form-control form-control-sm" name="businessDate" value="${filterBusinessDate}">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Date From</label>
                    <input type="date" class="form-control form-control-sm" name="dateFrom" value="${filterDateFrom}">
                </div>
                <div class="col-md-2">
                    <label class="form-label">Date To</label>
                    <input type="date" class="form-control form-control-sm" name="dateTo" value="${filterDateTo}">
                </div>
                <div class="col-md-2 d-flex align-items-end gap-2">
                    <button type="submit" class="btn btn-primary btn-sm"><i class="bi bi-search"></i> Search</button>
                    <a href="${pageContext.request.contextPath}/ledger/explorer" class="btn btn-outline-secondary btn-sm">Clear</a>
                </div>
            </div>
        </form>
    </div>
</div>

<%-- Summary Panel --%>
<div class="row mb-4">
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Total Entries</div>
                <div class="fs-4 fw-bold">${entryCount}</div>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Total Debits</div>
                <div class="fs-4 fw-bold ledger-amount-debit">${totalDebits}</div>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Total Credits</div>
                <div class="fs-4 fw-bold ledger-amount-credit">${totalCredits}</div>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Balance Status</div>
                <div class="fs-5">
                    <c:choose>
                        <c:when test="${isBalanced}">
                            <span class="badge ledger-balanced px-3 py-2"><i class="bi bi-check-circle"></i> BALANCED</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge ledger-unbalanced px-3 py-2"><i class="bi bi-exclamation-triangle"></i> UNBALANCED</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Ledger Entries Table --%>
<div class="card shadow">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h6 class="mb-0"><i class="bi bi-table"></i> Ledger Entries</h6>
        <span class="badge bg-secondary">${entryCount} records</span>
    </div>
    <div class="table-responsive">
        <table class="table table-hover table-sm mb-0">
            <thead class="table-light">
                <tr>
                    <th>Journal ID</th>
                    <th>Txn ID</th>
                    <th>Account</th>
                    <th>GL Code</th>
                    <th class="text-end">Debit</th>
                    <th class="text-end">Credit</th>
                    <th>Currency</th>
                    <th>Business Date</th>
                    <th>Posting Time</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="entry" items="${entries}">
                    <tr>
                        <td>
                            <c:if test="${entry.journal != null}">
                                <a href="javascript:void(0)" class="journal-link" onclick="showJournalModal(${entry.journal.id})">
                                    J-${entry.journal.id}
                                </a>
                            </c:if>
                            <c:if test="${entry.journal == null}">-</c:if>
                        </td>
                        <td>
                            <c:if test="${entry.transaction != null}">
                                <a href="${pageContext.request.contextPath}/transactions/${entry.transaction.id}">
                                    <code><c:out value="${entry.transaction.transactionRef}"/></code>
                                </a>
                            </c:if>
                        </td>
                        <td>
                            <c:if test="${entry.account != null}">
                                <code><c:out value="${entry.account.accountNumber}"/></code>
                            </c:if>
                        </td>
                        <td><code><c:out value="${entry.glAccountCode}"/></code></td>
                        <td class="${entry.entryType == 'DEBIT' ? 'ledger-amount-debit' : 'ledger-amount-zero'}">
                            <c:if test="${entry.entryType == 'DEBIT'}">${entry.amount}</c:if>
                            <c:if test="${entry.entryType != 'DEBIT'}">-</c:if>
                        </td>
                        <td class="${entry.entryType == 'CREDIT' ? 'ledger-amount-credit' : 'ledger-amount-zero'}">
                            <c:if test="${entry.entryType == 'CREDIT'}">${entry.amount}</c:if>
                            <c:if test="${entry.entryType != 'CREDIT'}">-</c:if>
                        </td>
                        <td>${entry.currency}</td>
                        <td>${entry.businessDate}</td>
                        <td><small>${entry.postingTime}</small></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty entries}">
                    <tr>
                        <td colspan="9" class="text-center text-muted py-4">
                            <i class="bi bi-journal-x fs-3 d-block mb-2"></i>
                            No ledger entries found for the selected filters
                        </td>
                    </tr>
                </c:if>
            </tbody>
        </table>
    </div>
</div>

<%-- Audit Disclaimer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    All ledger entries are immutable. System of Record: LedgerEntries. Data displayed is read-only from the ledger.
</div>

<%-- Journal Detail Modal --%>
<div class="modal fade" id="journalModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-journal-text"></i> Journal Entry Details</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="journalModalBody">
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
function escapeHtml(str) {
    if (str == null) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(String(str)));
    return div.innerHTML;
}
function showJournalModal(journalId) {
    var modal = new bootstrap.Modal(document.getElementById('journalModal'));
    var body = document.getElementById('journalModalBody');
    body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></div>';
    modal.show();

    fetch('${pageContext.request.contextPath}/ledger/journal/' + journalId + '/entries')
        .then(function(response) { return response.json(); })
        .then(function(data) {
            var html = '<div class="mb-3">';
            html += '<div class="row"><div class="col-md-4"><strong>Journal ID:</strong> J-' + data.journalId + '</div>';
            if (data.businessDate) {
                html += '<div class="col-md-4"><strong>Business Date:</strong> ' + data.businessDate + '</div>';
            }
            if (data.description) {
                html += '<div class="col-md-4"><strong>Description:</strong> ' + escapeHtml(data.description) + '</div>';
            }
            html += '</div></div>';

            html += '<table class="table table-sm table-bordered">';
            html += '<thead class="table-light"><tr><th>Type</th><th>Account</th><th>GL Code</th><th class="text-end">Amount</th><th>Narration</th></tr></thead>';
            html += '<tbody>';

            for (var i = 0; i < data.entries.length; i++) {
                var e = data.entries[i];
                var typeClass = e.entryType === 'DEBIT' ? 'text-danger' : 'text-success';
                html += '<tr>';
                html += '<td><span class="badge ' + (e.entryType === 'DEBIT' ? 'bg-danger' : 'bg-success') + '">' + e.entryType + '</span></td>';
                    html += '<td><code>' + escapeHtml(e.accountNumber || '-') + '</code></td>';
                    html += '<td><code>' + escapeHtml(e.glAccountCode || '-') + '</code></td>';
                    html += '<td class="text-end ' + typeClass + ' fw-bold">' + escapeHtml(String(e.amount)) + ' ' + escapeHtml(e.currency || '') + '</td>';
                    html += '<td><small>' + escapeHtml(e.narration || '-') + '</small></td>';
                html += '</tr>';
            }

            html += '</tbody>';
            html += '<tfoot class="table-light"><tr>';
            html += '<td colspan="3" class="fw-bold">Totals</td>';
            html += '<td class="text-end"><span class="text-danger">DR: ' + data.totalDebits + '</span> / <span class="text-success">CR: ' + data.totalCredits + '</span></td>';
            html += '<td>';
            if (data.isBalanced) {
                html += '<span class="badge bg-success">BALANCED</span>';
            } else {
                html += '<span class="badge bg-danger">UNBALANCED</span>';
            }
            html += '</td></tr></tfoot></table>';

            body.innerHTML = html;
        })
        .catch(function(err) {
            body.innerHTML = '<div class="alert alert-danger">Failed to load journal details: ' + escapeHtml(err.message) + '</div>';
        });
}
</script>

<%@ include file="../layout/footer.jsp" %>
