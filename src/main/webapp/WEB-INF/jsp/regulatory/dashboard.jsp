<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ include file="../layout/header.jsp" %>

<link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/regulatory.css"/>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-bank2"></i> Regulatory Dashboard</h3>
    <div>
        <span class="badge bg-dark me-2"><i class="bi bi-calendar3"></i> Business Date: <c:out value="${businessDate}"/></span>
        <span class="badge bg-secondary"><c:out value="${tenantName}"/></span>
    </div>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Flash Messages --%>
<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<%-- ═══ SECTION A: Capital Adequacy (CRAR) Panel ═══ --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-shield-check"></i> Capital Adequacy (CRAR) — Basel III</h5>
        <a href="${pageContext.request.contextPath}/regulatory/crar" class="btn btn-sm btn-outline-primary">View Full Report</a>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${crarAvailable}">
                <div class="row g-3">
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">CRAR %</small>
                            <h2 class="mb-0 ${crarData.meetsMinimumCrar ? 'text-success' : 'text-danger'}">
                                <c:out value="${crarData.crarPercent}"/>%
                            </h2>
                            <c:choose>
                                <c:when test="${crarData.meetsMinimumCrar}">
                                    <span class="badge bg-success"><i class="bi bi-check-circle-fill"></i> SAFE</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-danger"><i class="bi bi-exclamation-triangle-fill"></i> BREACH</span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Tier 1 Capital</small>
                            <h4 class="mb-0"><fmt:formatNumber value="${crarData.tier1Capital}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Total Capital</small>
                            <h4 class="mb-0"><fmt:formatNumber value="${crarData.totalCapital}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Risk Weighted Assets</small>
                            <h4 class="mb-0"><fmt:formatNumber value="${crarData.totalRwa}" type="currency" currencySymbol="&#8377;" maxFractionDigits="0"/></h4>
                        </div>
                    </div>
                </div>
                <div class="mt-2">
                    <small class="text-muted">RBI Minimum: 9% | Status: <c:out value="${crarData.complianceStatus}"/></small>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-warning mb-0"><i class="bi bi-exclamation-triangle"></i> CRAR snapshot not available for <c:out value="${businessDate}"/>. Run EOD to generate.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- ═══ SECTION B: ALM Liquidity Panel ═══ --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-bar-chart-steps"></i> ALM — Structural Liquidity</h5>
        <a href="${pageContext.request.contextPath}/regulatory/alm" class="btn btn-sm btn-outline-primary">View Full Report</a>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${almAvailable}">
                <c:if test="${almData.hasStructuralLiquidityRisk}">
                    <div class="alert alert-danger"><i class="bi bi-exclamation-octagon-fill"></i> <strong>Liquidity Risk Alert:</strong> <c:out value="${almData.riskAssessment}"/></div>
                </c:if>
                <div class="table-responsive">
                    <table class="table table-sm table-hover reg-table">
                        <thead class="table-light">
                            <tr><th>Bucket</th><th class="text-end">Assets</th><th class="text-end">Liabilities</th><th class="text-end">Gap</th><th class="text-end">Cumulative Gap</th><th class="text-end">Gap Ratio %</th></tr>
                        </thead>
                        <tbody>
                            <c:forEach var="bucket" items="${almData.buckets}">
                            <tr>
                                <td><c:out value="${bucket.bucketName}"/></td>
                                <td class="text-end"><fmt:formatNumber value="${bucket.assets}" maxFractionDigits="0"/></td>
                                <td class="text-end"><fmt:formatNumber value="${bucket.liabilities}" maxFractionDigits="0"/></td>
                                <td class="text-end ${bucket.gap < 0 ? 'text-danger' : ''}"><fmt:formatNumber value="${bucket.gap}" maxFractionDigits="0"/></td>
                                <td class="text-end ${bucket.cumulativeGap < 0 ? 'text-danger fw-bold' : ''}"><fmt:formatNumber value="${bucket.cumulativeGap}" maxFractionDigits="0"/></td>
                                <td class="text-end"><c:out value="${bucket.gapRatioPercent}"/>%</td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-warning mb-0"><i class="bi bi-exclamation-triangle"></i> ALM snapshot not available for <c:out value="${businessDate}"/>.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- ═══ SECTION C: Trial Balance Summary ═══ --%>
<div class="card shadow mb-4 reg-card">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0"><i class="bi bi-calculator"></i> Trial Balance</h5>
        <a href="${pageContext.request.contextPath}/regulatory/trial-balance" class="btn btn-sm btn-outline-primary">View Full Trial Balance</a>
    </div>
    <div class="card-body">
        <c:choose>
            <c:when test="${tbAvailable}">
                <div class="row g-3">
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Total Debit</small>
                            <h4 class="mb-0"><fmt:formatNumber value="${tbData.totalDebits}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h4>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Total Credit</small>
                            <h4 class="mb-0"><fmt:formatNumber value="${tbData.totalCredits}" type="currency" currencySymbol="&#8377;" maxFractionDigits="2"/></h4>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Difference</small>
                            <h4 class="mb-0">${tbData.balanced ? '0.00' : 'IMBALANCE'}</h4>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="reg-kpi-card text-center">
                            <small class="text-muted d-block">Status</small>
                            <c:choose>
                                <c:when test="${tbData.balanced}"><span class="badge bg-success fs-6"><i class="bi bi-check-circle-fill"></i> Balanced</span></c:when>
                                <c:otherwise><span class="badge bg-danger fs-6"><i class="bi bi-x-circle-fill"></i> Imbalance</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-warning mb-0"><i class="bi bi-exclamation-triangle"></i> Trial Balance snapshot not available for <c:out value="${businessDate}"/>.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%-- ═══ SECTION D: Snapshot Metadata ═══ --%>
<div class="card shadow mb-4">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-info-circle"></i> Snapshot Metadata</h5>
    </div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-sm">
                <thead class="table-light">
                    <tr><th>Report</th><th>Status</th><th>Generated At</th><th>Generated By</th><th>Hash (masked)</th></tr>
                </thead>
                <tbody>
                    <c:if test="${crarSnapshot != null}">
                    <tr>
                        <td>CRAR</td>
                        <td><span class="badge bg-success">FINAL</span></td>
                        <td><c:out value="${crarSnapshot.generatedAt}"/></td>
                        <td><c:out value="${crarSnapshot.generatedBy}"/></td>
                        <td><code>${crarSnapshot.hashChecksum.substring(0, 16)}...</code></td>
                    </tr>
                    </c:if>
                    <c:if test="${almSnapshot != null}">
                    <tr>
                        <td>ALM</td>
                        <td><span class="badge bg-success">FINAL</span></td>
                        <td><c:out value="${almSnapshot.generatedAt}"/></td>
                        <td><c:out value="${almSnapshot.generatedBy}"/></td>
                        <td><code>${almSnapshot.hashChecksum.substring(0, 16)}...</code></td>
                    </tr>
                    </c:if>
                    <c:if test="${tbSnapshot != null}">
                    <tr>
                        <td>Trial Balance</td>
                        <td><span class="badge bg-success">FINAL</span></td>
                        <td><c:out value="${tbSnapshot.generatedAt}"/></td>
                        <td><c:out value="${tbSnapshot.generatedBy}"/></td>
                        <td><code>${tbSnapshot.hashChecksum.substring(0, 16)}...</code></td>
                    </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%-- ═══ Admin Regeneration Control ═══ --%>
<c:if test="${sessionScope.isAdmin}">
<div class="card shadow mb-4 border-warning">
    <div class="card-header bg-warning bg-opacity-25">
        <h6 class="mb-0"><i class="bi bi-gear-wide-connected"></i> Admin: Regenerate Snapshots</h6>
    </div>
    <div class="card-body">
        <p class="text-muted mb-2">Regenerate all regulatory snapshots for the current business date. This action is audit-logged.</p>
        <form method="post" action="${pageContext.request.contextPath}/regulatory/regenerate" onsubmit="return confirm('Are you sure you want to regenerate all regulatory snapshots? This action will be audit-logged.');">
            <input type="hidden" name="type" value="ALL"/>
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <button type="submit" class="btn btn-warning"><i class="bi bi-arrow-clockwise"></i> Regenerate All Snapshots</button>
        </form>
    </div>
</div>
</c:if>

<%-- Governance Footer --%>
<div class="audit-disclaimer mt-3">
    <i class="bi bi-shield-lock"></i>
    Regulatory snapshots are generated during EOD and are immutable after FINAL status.
    All data derives from immutable ledger entries only. SHA-256 checksums ensure tamper detection.
    This dashboard is read-only — no recomputation occurs in the UI layer.
</div>

<%@ include file="../layout/footer.jsp" %>
