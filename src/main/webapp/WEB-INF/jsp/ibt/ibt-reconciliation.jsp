<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-clipboard2-data"></i> IBT Reconciliation Dashboard</h3>
    <div>
        <a href="${pageContext.request.contextPath}/ibt" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> IBT List</a>
        <a href="${pageContext.request.contextPath}/ibt/create" class="btn btn-primary"><i class="bi bi-plus-circle"></i> New IBT</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Section A: Governance Banner --%>
<div class="alert alert-dark border-0 mb-4">
    <div class="d-flex align-items-center">
        <i class="bi bi-shield-lock-fill fs-4 me-3"></i>
        <div>
            <strong>RBI CBS Control:</strong> All inter-branch transfers must settle and clearing GL must net to zero before EOD.
            Unsettled transfers beyond T+2 require escalation to Operations Head.
        </div>
    </div>
</div>

<%-- Section A: KPI Cards --%>
<div class="row g-3 mb-4">
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${totalUnsettled > 0 ? 'border-warning' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Unsettled Transfers</small>
                <h2 class="mb-0 ${totalUnsettled > 0 ? 'text-warning' : 'text-success'}">
                    <c:out value="${totalUnsettled}"/>
                </h2>
                <small class="text-muted">INITIATED + SENT + RECEIVED</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${failedCount > 0 ? 'border-danger' : 'border-success'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Failed Transfers</small>
                <h2 class="mb-0 ${failedCount > 0 ? 'text-danger' : 'text-success'}">
                    <c:out value="${failedCount}"/>
                </h2>
                <small class="text-muted">Requires investigation</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${clearingBalanced ? 'border-success' : 'border-danger'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Clearing GL Net</small>
                <h2 class="mb-0 ${clearingBalanced ? 'text-success' : 'text-danger'}">
                    <c:out value="${clearingNet}"/>
                </h2>
                <small class="text-muted">CLEARING_ACCOUNT sum</small>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow border-start border-4 ${clearingBalanced ? 'border-success' : 'border-danger'}">
            <div class="card-body text-center">
                <small class="text-muted d-block">Clearing Status</small>
                <c:choose>
                    <c:when test="${clearingBalanced}">
                        <h4 class="mb-0"><span class="badge bg-success"><i class="bi bi-check-circle-fill"></i> BALANCED</span></h4>
                    </c:when>
                    <c:otherwise>
                        <h4 class="mb-0"><span class="badge bg-danger"><i class="bi bi-x-octagon-fill"></i> IMBALANCE</span></h4>
                    </c:otherwise>
                </c:choose>
                <small class="text-muted">EOD gate check</small>
            </div>
        </div>
    </div>
</div>

<%-- Section B: Clearing Net Status Alert --%>
<c:choose>
    <c:when test="${!clearingBalanced}">
        <div class="alert alert-danger border-0 shadow-sm">
            <div class="d-flex align-items-center">
                <i class="bi bi-exclamation-octagon-fill fs-3 me-3"></i>
                <div>
                    <strong>CLEARING GL NET IS NOT ZERO</strong>
                    <p class="mb-0 mt-1">
                        Net clearing balance is <strong><c:out value="${clearingNet}"/></strong>.
                        EOD will block until all inter-branch clearing accounts net to zero.
                        Investigate unsettled transfers and ensure both legs are posted.
                    </p>
                </div>
            </div>
        </div>
    </c:when>
    <c:otherwise>
        <div class="alert alert-success border-0 shadow-sm">
            <div class="d-flex align-items-center">
                <i class="bi bi-shield-fill-check fs-3 me-3"></i>
                <div>
                    <strong>Clearing GL Balanced</strong>
                    <p class="mb-0 mt-1">All inter-branch clearing accounts net to zero. EOD clearing gate: PASS.</p>
                </div>
            </div>
        </div>
    </c:otherwise>
</c:choose>

<%-- Section C: Aging Table — Oldest Unsettled IBTs --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Oldest Unsettled IBTs (Top 5)</h5>
        <span class="badge bg-secondary"><c:out value="${totalUnsettled}"/> total unsettled</span>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty oldestUnsettled}">
                <div class="table-responsive">
                    <table class="table table-hover table-sm">
                        <thead class="table-light">
                            <tr>
                                <th>IBT ID</th>
                                <th>Source Branch</th>
                                <th>Dest Branch</th>
                                <th>Amount</th>
                                <th>Status</th>
                                <th>Business Date</th>
                                <th>Age (days)</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="ibt" items="${oldestUnsettled}">
                            <%-- Compute age in days using JSP scriptlet for businessDate difference --%>
                            <c:set var="ibtBizDate" value="${ibt.businessDate}"/>
                            <jsp:useBean id="now" class="java.util.Date"/>
                            <%
                                // Age calculation: current business date - IBT business date
                                java.time.LocalDate bizDate = (java.time.LocalDate) pageContext.getAttribute("ibtBizDate");
                                java.time.LocalDate refDate = (java.time.LocalDate) request.getAttribute("businessDate");
                                long ageDays = 0;
                                if (bizDate != null && refDate != null) {
                                    ageDays = java.time.temporal.ChronoUnit.DAYS.between(bizDate, refDate);
                                }
                                pageContext.setAttribute("ageDays", ageDays);
                            %>
                            <tr class="${ageDays >= 3 ? 'table-danger' : (ageDays >= 2 ? 'table-warning' : (ageDays >= 1 ? 'table-info' : ''))}">
                                <td><code><c:out value="${ibt.id}"/></code></td>
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
                                        <c:when test="${ibt.status == 'RECEIVED'}"><span class="badge" style="background-color:#6f42c1;">RECEIVED</span></c:when>
                                        <c:otherwise><span class="badge bg-light text-dark"><c:out value="${ibt.status}"/></span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td><c:out value="${ibt.businessDate}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${ageDays >= 3}">
                                            <span class="badge bg-danger"><c:out value="${ageDays}"/>d</span>
                                            <small class="text-danger ms-1">CRITICAL</small>
                                        </c:when>
                                        <c:when test="${ageDays >= 2}">
                                            <span class="badge bg-warning text-dark"><c:out value="${ageDays}"/>d</span>
                                            <small class="text-warning ms-1">ESCALATE</small>
                                        </c:when>
                                        <c:when test="${ageDays >= 1}">
                                            <span class="badge bg-info"><c:out value="${ageDays}"/>d</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary"><c:out value="${ageDays}"/>d</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:if test="${ibt.referenceTransaction != null}">
                                        <a href="${pageContext.request.contextPath}/ibt/${ibt.id}" class="btn btn-sm btn-outline-primary" title="View IBT Detail">
                                            <i class="bi bi-eye"></i>
                                        </a>
                                    </c:if>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
                <%-- Aging Legend --%>
                <div class="d-flex gap-3 mt-2">
                    <small><span class="badge bg-secondary">T+0</span> Same day</small>
                    <small><span class="badge bg-info">T+1</span> Next day</small>
                    <small><span class="badge bg-warning text-dark">T+2</span> Escalate</small>
                    <small><span class="badge bg-danger">T+3+</span> Critical</small>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-check-circle" style="font-size: 3rem;"></i>
                    <p class="mt-2">No unsettled inter-branch transfers. All clear.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Per-Branch Clearing Account Balances --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-diagram-3"></i> Per-Branch Clearing Account Balances</h5></div>
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty clearingAccounts}">
                <div class="table-responsive">
                    <table class="table table-sm table-hover">
                        <thead class="table-light">
                            <tr>
                                <th>Account Number</th>
                                <th>Account Name</th>
                                <th>Balance</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="a" items="${clearingAccounts}">
                            <tr class="${a.balance.signum() != 0 ? 'table-warning' : ''}">
                                <td><code><c:out value="${a.accountNumber}"/></code></td>
                                <td><c:out value="${a.accountName}"/></td>
                                <td class="fw-bold"><c:out value="${a.balance}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${a.balance.signum() == 0}"><span class="badge bg-success">ZERO</span></c:when>
                                        <c:otherwise><span class="badge bg-warning text-dark">NON-ZERO</span></c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-muted text-center py-3">No clearing accounts found for tenant.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- Audit Disclaimer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    This dashboard is read-only. All data sourced from CLEARING_ACCOUNT balances and InterBranchTransfer lifecycle states.
    No voucher derivation. No record mutation.
</div>

<%@ include file="../layout/footer.jsp" %>
