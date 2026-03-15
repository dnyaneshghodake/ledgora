<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>
<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-exclamation-triangle"></i> NPA Monitor — RBI IRAC</h3>
    <a href="${pageContext.request.contextPath}/loan/dashboard" class="btn btn-sm btn-outline-secondary"><i class="bi bi-arrow-left"></i> Dashboard</a>
</div>
<%@ include file="../layout/status-banner.jsp" %>

<%-- NPA Loans --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white"><h5 class="mb-0 text-danger"><i class="bi bi-x-octagon"></i> Non-Performing Assets (DPD > 90)</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty npaLoans}">
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr><th>Loan #</th><th class="text-end">Outstanding</th><th class="text-end">Penal</th><th class="text-end">Int. Reversed</th><th>DPD</th><th>Category</th><th>Provision %</th><th class="text-end">Provision Amt</th><th>NPA Date</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="loan" items="${npaLoans}">
                            <tr>
                                <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                                <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                                <td class="text-end ${loan.penalInterest > 0 ? 'text-danger' : ''}"><fmt:formatNumber value="${loan.penalInterest}" maxFractionDigits="0"/></td>
                                <td class="text-end ${loan.interestReversed > 0 ? 'text-warning' : ''}"><fmt:formatNumber value="${loan.interestReversed}" maxFractionDigits="0"/></td>
                                <td class="text-danger fw-bold"><c:out value="${loan.dpd}"/></td>
                                <td><c:choose>
                                    <c:when test="${loan.npaClassification == 'SUBSTANDARD'}"><span class="badge bg-warning text-dark">Substandard</span></c:when>
                                    <c:when test="${loan.npaClassification == 'DOUBTFUL'}"><span class="badge bg-danger">Doubtful</span></c:when>
                                    <c:when test="${loan.npaClassification == 'LOSS'}"><span class="badge" style="background-color:#8b0000;color:#fff;">Loss</span></c:when>
                                </c:choose></td>
                                <td><c:out value="${loan.npaClassification.provisionRate}"/>%</td>
                                <td class="text-end"><fmt:formatNumber value="${loan.provisionAmount}" maxFractionDigits="0"/></td>
                                <td><c:out value="${loan.npaDate}"/></td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-success"><i class="bi bi-check-circle" style="font-size:2rem;"></i><p>No NPA loans. All assets performing.</p></div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- At-Risk Loans (DPD > 0 but < 90) --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0 text-warning"><i class="bi bi-exclamation-triangle"></i> At-Risk Loans (DPD > 0, approaching NPA)</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty atRiskLoans}">
                <div class="table-responsive">
                    <table class="table table-sm table-hover">
                        <thead class="table-light">
                            <tr><th>Loan #</th><th>Borrower</th><th class="text-end">Outstanding</th><th class="text-end">Penal</th><th>DPD</th><th>SMA Category</th><th>Days to NPA</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="loan" items="${atRiskLoans}">
                            <tr>
                                <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                                <td><c:out value="${loan.borrowerName}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                                <td class="text-end ${loan.penalInterest > 0 ? 'text-danger' : ''}"><fmt:formatNumber value="${loan.penalInterest}" maxFractionDigits="0"/></td>
                                <td class="text-warning fw-bold"><c:out value="${loan.dpd}"/></td>
                                <td><c:choose>
                                    <c:when test="${loan.smaCategory == 'SMA_0'}"><span class="badge bg-info">SMA-0</span></c:when>
                                    <c:when test="${loan.smaCategory == 'SMA_1'}"><span class="badge bg-warning text-dark">SMA-1</span></c:when>
                                    <c:when test="${loan.smaCategory == 'SMA_2'}"><span class="badge bg-danger">SMA-2</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary">None</span></c:otherwise>
                                </c:choose></td>
                                <td class="${(90 - loan.dpd) <= 15 ? 'text-danger fw-bold' : ''}">${90 - loan.dpd} days</td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-3 text-muted">No at-risk loans.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Admin-Only NPA Operations Panel --%>
<c:if test="${sessionScope.isAdmin}">
<div class="card shadow mb-4 border-danger">
    <div class="card-header bg-danger bg-opacity-10"><h5 class="mb-0"><i class="bi bi-gear-wide-connected"></i> NPA Operations (Admin Only)</h5></div>
    <div class="card-body">
        <div class="row g-3">
            <div class="col-md-4">
                <form method="post" action="${pageContext.request.contextPath}/loan/npa/evaluate"
                      onsubmit="return confirm('Run DPD/NPA evaluation for all loans? This will update SMA categories, NPA classifications, and reverse interest on new NPAs.');">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <button type="submit" class="btn btn-outline-warning w-100"><i class="bi bi-arrow-repeat"></i> Evaluate DPD / NPA</button>
                    <small class="text-muted d-block mt-1">Updates DPD, SMA, NPA classification, interest reversal</small>
                </form>
            </div>
            <div class="col-md-4">
                <form method="post" action="${pageContext.request.contextPath}/loan/npa/provision"
                      onsubmit="return confirm('Recalculate provisions for all loans? This will update provision amounts based on current NPA classifications.');">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <button type="submit" class="btn btn-outline-danger w-100"><i class="bi bi-calculator"></i> Recalculate Provisions</button>
                    <small class="text-muted d-block mt-1">Standard 0.4%, Substandard 15%, Doubtful 25%, Loss 100%</small>
                </form>
            </div>
            <div class="col-md-4">
                <p class="text-muted mb-1"><strong>Write-Off:</strong> Select a LOSS-classified loan from the NPA table above, then use the loan detail page to initiate write-off.</p>
            </div>
        </div>
    </div>
</div>
</c:if>

<%-- NPA Loans Table: Add write-off action column for ADMIN on LOSS loans --%>
<c:if test="${sessionScope.isAdmin && not empty npaLoans}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-x-octagon"></i> Write-Off Eligible (LOSS + 100% Provisioned)</h5></div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm table-hover">
                <thead class="table-light">
                    <tr><th>Loan #</th><th class="text-end">Outstanding</th><th>Classification</th><th class="text-end">Provision</th><th>Action</th></tr>
                </thead>
                <tbody>
                    <c:forEach var="loan" items="${npaLoans}">
                    <c:if test="${loan.npaClassification == 'LOSS'}">
                    <tr>
                        <td><code><a href="${pageContext.request.contextPath}/loan/${loan.id}"><c:out value="${loan.loanAccountNumber}"/></a></code></td>
                        <td class="text-end"><fmt:formatNumber value="${loan.outstandingPrincipal}" maxFractionDigits="0"/></td>
                        <td><span class="badge" style="background-color:#8b0000;color:#fff;">Loss</span></td>
                        <td class="text-end"><fmt:formatNumber value="${loan.provisionAmount}" maxFractionDigits="0"/></td>
                        <td>
                            <c:if test="${loan.provisionAmount >= loan.outstandingPrincipal}">
                            <form method="post" action="${pageContext.request.contextPath}/loan/npa/${loan.id}/writeoff" class="d-inline"
                                  onsubmit="return confirm('WRITE OFF loan ${loan.loanAccountNumber}? This is irreversible. Outstanding ₹${loan.outstandingPrincipal} will be removed from the asset book.');">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-x-circle"></i> Write Off</button>
                            </form>
                            </c:if>
                            <c:if test="${loan.provisionAmount < loan.outstandingPrincipal}">
                            <span class="text-muted">Insufficient provision</span>
                            </c:if>
                        </td>
                    </tr>
                    </c:if>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>
</c:if>

<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i> NPA classification per RBI Prudential Norms (90-day DPD). SMA categories per RBI Early Warning Framework.
    Provisioning: Standard 0.4%, Substandard 15%, Doubtful 25%, Loss 100% — <strong>bidirectional</strong> (increases and decreases on NPA tier changes).
    Interest reversal and NPA upgrade posted via voucher engine. All data are derived caches — source of truth is immutable ledger entries.
</div>

<%@ include file="../layout/footer.jsp" %>
