<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-gear-wide-connected"></i> Settlement Control <small class="text-muted">Business Date Lifecycle</small></h3>
    <a href="${pageContext.request.contextPath}/settlements/process" class="btn btn-primary btn-sm">
        <i class="bi bi-play-circle"></i> Run Settlement
    </a>
</div>

<%-- Status Panel --%>
<div class="row mb-4">
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Business Date</div>
                <div class="fs-5 fw-bold">${businessDate}</div>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Status</div>
                <div class="fs-6">
                    <c:choose>
                        <c:when test="${businessDateStatus == 'OPEN'}">
                            <span class="badge bg-success px-3 py-2">OPEN</span>
                        </c:when>
                        <c:when test="${businessDateStatus == 'DAY_CLOSING'}">
                            <span class="badge bg-warning text-dark px-3 py-2">DAY_CLOSING</span>
                        </c:when>
                        <c:when test="${businessDateStatus == 'CLOSED'}">
                            <span class="badge bg-danger px-3 py-2">CLOSED</span>
                        </c:when>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Ledger Health</div>
                <div class="fs-6">
                    <c:choose>
                        <c:when test="${ledgerHealth == 'HEALTHY'}">
                            <span class="badge bg-success px-3 py-2"><i class="bi bi-shield-check"></i> HEALTHY</span>
                        </c:when>
                        <c:when test="${ledgerHealth == 'WARNING'}">
                            <span class="badge bg-warning text-dark px-3 py-2"><i class="bi bi-exclamation-triangle"></i> WARNING</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge bg-danger px-3 py-2"><i class="bi bi-x-octagon"></i> CORRUPTED</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
    <div class="col-md-2">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Pending</div>
                <div class="fs-5 fw-bold">${pendingCount + inProgressCount}</div>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow">
            <div class="card-body text-center">
                <div class="text-muted small text-uppercase">Last Settlement</div>
                <div class="fs-6">
                    <c:choose>
                        <c:when test="${lastSettlementTime != null}">
                            <small>${lastSettlementTime}</small>
                        </c:when>
                        <c:otherwise>
                            <span class="text-muted">Never</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="row">
    <%-- 7-Step Settlement Process --%>
    <div class="col-md-7">
        <div class="card shadow">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-list-ol"></i> Settlement Process - 7 Steps</h6>
            </div>
            <div class="card-body">
                <div class="settlement-step settlement-step-pending" id="step1">
                    <span class="settlement-step-number">1</span>
                    <div>
                        <div class="settlement-step-label">Stop Intake</div>
                        <div class="settlement-step-desc">Set business date to DAY_CLOSING, prevent new transactions</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step2">
                    <span class="settlement-step-number">2</span>
                    <div>
                        <div class="settlement-step-label">Flush Pending</div>
                        <div class="settlement-step-desc">Complete all pending transactions for the business date</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step3">
                    <span class="settlement-step-number">3</span>
                    <div>
                        <div class="settlement-step-label">Validate Ledger</div>
                        <div class="settlement-step-desc">Verify SUM(Debits) = SUM(Credits) - system invariant check</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step4">
                    <span class="settlement-step-number">4</span>
                    <div>
                        <div class="settlement-step-label">Trial Balance</div>
                        <div class="settlement-step-desc">Generate and verify trial balance report</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step5">
                    <span class="settlement-step-number">5</span>
                    <div>
                        <div class="settlement-step-label">Accruals</div>
                        <div class="settlement-step-desc">Post interest accruals and periodic fees</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step6">
                    <span class="settlement-step-number">6</span>
                    <div>
                        <div class="settlement-step-label">Reports</div>
                        <div class="settlement-step-desc">Generate settlement entries per account</div>
                    </div>
                </div>
                <div class="settlement-step settlement-step-pending" id="step7">
                    <span class="settlement-step-number">7</span>
                    <div>
                        <div class="settlement-step-label">Advance Business Date</div>
                        <div class="settlement-step-desc">Close current day and open next business date</div>
                    </div>
                </div>

                <div class="mt-3 text-center">
                    <button type="button" class="btn btn-warning" id="btnAdvanceDate"
                            data-bs-toggle="modal" data-bs-target="#advanceModal"
                            ${businessDateStatus != 'OPEN' ? 'disabled' : ''}>
                        <i class="bi bi-skip-forward-fill"></i> Initiate Settlement
                    </button>
                    <c:if test="${businessDateStatus == 'DAY_CLOSING'}">
                        <span class="text-warning ms-2"><i class="bi bi-exclamation-triangle"></i> Settlement already in progress</span>
                    </c:if>
                    <c:if test="${businessDateStatus == 'CLOSED'}">
                        <span class="text-danger ms-2"><i class="bi bi-lock"></i> Business date is closed</span>
                    </c:if>
                </div>
            </div>
        </div>
    </div>

    <%-- Ledger Integrity & Recent Settlements --%>
    <div class="col-md-5">
        <div class="card shadow mb-4">
            <div class="card-header bg-white">
                <h6 class="mb-0"><i class="bi bi-shield-check"></i> Ledger Integrity</h6>
            </div>
            <div class="card-body">
                <table class="table table-sm table-borderless mb-0">
                    <tr>
                        <td class="text-muted">Total Debits (Today)</td>
                        <td class="text-end fw-bold text-danger">${totalDebits}</td>
                    </tr>
                    <tr>
                        <td class="text-muted">Total Credits (Today)</td>
                        <td class="text-end fw-bold text-success">${totalCredits}</td>
                    </tr>
                    <tr class="border-top">
                        <td class="text-muted fw-bold">Balance Check</td>
                        <td class="text-end">
                            <c:choose>
                                <c:when test="${ledgerHealth == 'HEALTHY'}">
                                    <span class="badge bg-success">BALANCED</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-danger">IMBALANCED</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </table>
            </div>
        </div>

        <div class="card shadow">
            <div class="card-header bg-white d-flex justify-content-between align-items-center">
                <h6 class="mb-0"><i class="bi bi-clock-history"></i> Recent Settlements</h6>
                <a href="${pageContext.request.contextPath}/settlements" class="btn btn-outline-primary btn-sm">View All</a>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-sm table-hover mb-0">
                        <thead class="table-light">
                            <tr>
                                <th>Ref</th>
                                <th>Date</th>
                                <th>Status</th>
                                <th>Txns</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${recentSettlements}" end="4">
                                <tr>
                                    <td><a href="${pageContext.request.contextPath}/settlements/${s.id}"><code>${s.settlementRef}</code></a></td>
                                    <td>${s.businessDate}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${s.status == 'COMPLETED'}"><span class="badge bg-success">COMPLETED</span></c:when>
                                            <c:when test="${s.status == 'IN_PROGRESS'}"><span class="badge bg-info">IN_PROGRESS</span></c:when>
                                            <c:when test="${s.status == 'FAILED'}"><span class="badge bg-danger">FAILED</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary">${s.status}</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>${s.transactionCount}</td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty recentSettlements}">
                                <tr><td colspan="4" class="text-center text-muted py-3">No settlements yet</td></tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Advance Business Date Confirmation Modal --%>
<div class="modal fade" id="advanceModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header bg-warning">
                <h5 class="modal-title"><i class="bi bi-exclamation-triangle"></i> Confirm Settlement</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <p class="mb-3">You are about to initiate the settlement process for business date <strong>${businessDate}</strong>.</p>
                <p>This will:</p>
                <ul>
                    <li>Stop transaction intake</li>
                    <li>Flush all pending transactions</li>
                    <li>Validate ledger integrity</li>
                    <li>Generate trial balance and reports</li>
                    <li>Advance to next business date</li>
                </ul>
                <div class="alert alert-danger">
                    <strong>This action cannot be undone.</strong>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-bold">Type <code>ADVANCE</code> to confirm:</label>
                    <input type="text" class="form-control" id="confirmInput" placeholder="Type ADVANCE here">
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <form method="post" action="${pageContext.request.contextPath}/settlements/process" id="settlementForm" style="display:inline">
                    <input type="hidden" name="settlementDate" value="${businessDate}">
                    <button type="submit" class="btn btn-danger" id="btnConfirmAdvance" disabled>
                        <i class="bi bi-skip-forward-fill"></i> Proceed with Settlement
                    </button>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', function() {
    var confirmInput = document.getElementById('confirmInput');
    var btnConfirm = document.getElementById('btnConfirmAdvance');

    if (confirmInput && btnConfirm) {
        confirmInput.addEventListener('input', function() {
            btnConfirm.disabled = (this.value.trim() !== 'ADVANCE');
        });
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
