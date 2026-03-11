<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Finacle-grade Account Master — fully wired to AccountMasterController --%>

<%-- Operational Status Banner --%>
<c:if test="${freezeLevel != null && freezeLevel != 'NONE'}">
    <c:set var="freezeActive" value="${true}" scope="request"/>
    <c:set var="freezeLevel" value="${freezeLevel}" scope="request"/>
    <c:set var="freezeReason" value="${freezeReason}" scope="request"/>
</c:if>
<c:if test="${makerCheckerStatus == 'PENDING'}">
    <c:set var="approvalPending" value="${true}" scope="request"/>
    <c:set var="approvalPendingMessage" value="This account is pending maker-checker approval." scope="request"/>
</c:if>
<%@ include file="../layout/status-banner.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-wallet2"></i> Account Master <small class="text-muted">| <c:out value="${accountNumber}" default="--"/></small></h3>
    <div>
        <a href="${pageContext.request.contextPath}/accounts/${account.id}" class="btn btn-outline-secondary"><i class="bi bi-eye"></i> Standard View</a>
        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-primary"><i class="bi bi-arrow-left"></i> Back</a>
    </div>
</div>

<%-- Header Section: Account/Tenant/Branch/Maker-Checker --%>
<div class="card shadow fin-master-header">
    <div class="card-body">
        <div class="fin-header-grid">
            <div class="fin-header-field">
                <label class="fin-header-label">Account Number</label>
                <input type="text" class="form-control fin-readonly" value="<c:out value='${accountNumber}' default='--'/>" readonly />
            </div>
            <div class="fin-header-field">
                <label class="fin-header-label">Tenant</label>
                <input type="text" class="form-control fin-readonly" value="<c:out value='${tenantName}' default='--'/>" readonly />
            </div>
            <div class="fin-header-field">
                <label class="fin-header-label">Branch</label>
                <input type="text" class="form-control fin-readonly" value="<c:out value='${branchName}' default='--'/>" readonly />
            </div>
            <div class="fin-header-field fin-header-status">
                <label class="fin-header-label">Maker-Checker Status</label>
                <div class="fin-status-row">
                    <c:choose>
                        <c:when test="${makerCheckerStatus == 'APPROVED'}"><span class="fin-status-badge fin-status-approved">APPROVED</span></c:when>
                        <c:when test="${makerCheckerStatus == 'REJECTED'}"><span class="fin-status-badge fin-status-rejected">REJECTED</span></c:when>
                        <c:otherwise><span class="fin-status-badge fin-status-pending">PENDING</span></c:otherwise>
                    </c:choose>
                    <c:if test="${freezeLevel != null && freezeLevel != 'NONE'}">
                    <span class="fin-freeze-indicator" title="Freeze Active: ${freezeLevel}" aria-label="Freeze indicator">
                        <i class="bi bi-snow" aria-hidden="true"></i>
                    </span>
                    </c:if>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tabs (Vanilla JS, no Bootstrap tabs) --%>
<div class="fin-tabs" role="tablist" aria-label="Account master tabs">
    <button type="button" class="fin-tab active" id="tabBtnGeneral" data-tab="tabGeneral" onclick="switchTab('tabGeneral')">General Info</button>
    <button type="button" class="fin-tab" id="tabBtnContact" data-tab="tabContact" onclick="switchTab('tabContact')">Contact Info</button>
    <button type="button" class="fin-tab" id="tabBtnKyc" data-tab="tabKyc" onclick="switchTab('tabKyc')">KYC & Identity</button>
    <button type="button" class="fin-tab" id="tabBtnTax" data-tab="tabTax" onclick="switchTab('tabTax')">Tax Profile</button>
    <button type="button" class="fin-tab" id="tabBtnFreeze" data-tab="tabFreeze" onclick="switchTab('tabFreeze')">Freeze Control</button>
    <button type="button" class="fin-tab" id="tabBtnRelationships" data-tab="tabRelationships" onclick="switchTab('tabRelationships')">Relationships</button>
    <button type="button" class="fin-tab" id="tabBtnAudit" data-tab="tabAudit" onclick="switchTab('tabAudit')">Audit & Approval</button>
</div>

<%-- Tab: General Info + Contact Info (editable, single save form) --%>
<div class="tab-content active" id="tabGeneral" role="tabpanel" aria-labelledby="tabBtnGeneral">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-info-circle"></i> General Info</h5></div>
        <div class="card-body">
            <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/master/save" id="masterSaveForm">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label cbs-field-required">Account Name</label>
                        <input type="text" class="form-control" name="accountName" value="<c:out value='${accountName}'/>" required maxlength="100" />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Account Type</label>
                        <input type="text" class="form-control fin-readonly" value="<c:out value='${accountType}'/>" readonly />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Status</label>
                        <select class="form-select" name="status">
                            <c:forEach var="s" items="${accountStatuses}">
                                <option value="${s}" ${accountStatus == s.name() ? 'selected' : ''}><c:out value="${s}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Currency</label>
                        <input type="text" class="form-control" name="currency" value="<c:out value='${currency}' default='INR'/>" maxlength="3" />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">GL Account Code</label>
                        <input type="text" class="form-control" name="glAccountCode" value="<c:out value='${glAccountCode}'/>" maxlength="20" />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Home Branch</label>
                        <input type="text" class="form-control fin-readonly" value="<c:out value='${homeBranchDisplay}' default='--'/>" readonly />
                    </div>
                </div>
                <hr>
                <h6><i class="bi bi-person-lines-fill"></i> Contact Info</h6>
                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label">Customer Name</label>
                        <input type="text" class="form-control" name="customerName" value="<c:out value='${customerName}'/>" maxlength="100" />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Mobile</label>
                        <input type="text" class="form-control" name="customerPhone" value="<c:out value='${customerPhone}'/>" maxlength="20" />
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Email</label>
                        <input type="email" class="form-control" name="customerEmail" value="<c:out value='${customerEmail}'/>" maxlength="100" />
                    </div>
                </div>
                <hr>
                <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
                <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Save Account Master</button>
                </c:if>
            </form>
        </div>
    </div>
</div>

<%-- Tab: Contact Info (redirects to General since form is combined) --%>
<div class="tab-content" id="tabContact" role="tabpanel" aria-labelledby="tabBtnContact">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-telephone"></i> Contact Info</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>Customer Name:</strong> <c:out value="${customerName}" default="--"/></div>
                <div class="col-md-4"><strong>Mobile:</strong> <c:out value="${customerPhone}" default="--"/></div>
                <div class="col-md-4"><strong>Email:</strong> <c:out value="${customerEmail}" default="--"/></div>
            </div>
            <hr>
            <small class="text-muted"><i class="bi bi-info-circle"></i> Edit contact info in the <a href="#" onclick="switchTab('tabGeneral'); return false;">General Info</a> tab save form.</small>
        </div>
    </div>
</div>

<%-- Tab: KYC & Identity --%>
<div class="tab-content" id="tabKyc" role="tabpanel" aria-labelledby="tabBtnKyc">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-person-badge"></i> KYC & Identity</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>Customer Number:</strong> <code><c:out value="${customerNumber}" default="--"/></code></div>
                <div class="col-md-4"><strong>KYC Status:</strong>
                    <c:choose>
                        <c:when test="${account.customerMaster != null && account.customerMaster.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                        <c:when test="${account.customerMaster != null && account.customerMaster.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:when test="${account.customerMaster != null && account.customerMaster.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">--</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>National ID:</strong> <c:out value="${account.customerMaster != null ? account.customerMaster.nationalId : '--'}"/></div>
            </div>
            <hr>
            <small class="text-muted"><i class="bi bi-shield-lock"></i> KYC fields are managed via Customer Master. This tab is read-only at account level.</small>
        </div>
    </div>
</div>

<%-- Tab: Tax Profile --%>
<div class="tab-content" id="tabTax" role="tabpanel" aria-labelledby="tabBtnTax">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-receipt"></i> Tax Profile</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>PAN:</strong> <span class="text-muted">--</span></div>
                <div class="col-md-4"><strong>Aadhaar:</strong> <span class="text-muted">XXXX-XXXX-****</span></div>
                <div class="col-md-4"><strong>GST:</strong> <span class="text-muted">--</span></div>
            </div>
            <hr>
            <small class="text-muted"><i class="bi bi-shield-lock"></i> Tax profile is linked to Customer Master. Manage via Customer Tax Profile screen.</small>
        </div>
    </div>
</div>

<%-- Tab: Freeze Control --%>
<div class="tab-content" id="tabFreeze" role="tabpanel" aria-labelledby="tabBtnFreeze">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="bi bi-snow"></i> Freeze Control</h5>
            <c:if test="${freezeLevel != null && freezeLevel != 'NONE'}">
                <span class="badge bg-danger">FREEZE: <c:out value="${freezeLevel}"/></span>
            </c:if>
        </div>
        <div class="card-body">
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/master/freeze" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Freeze Level</label>
                    <select class="form-select" name="freezeLevel" required>
                        <c:forEach var="fl" items="${freezeLevels}">
                            <option value="${fl}" ${freezeLevel == fl.name() ? 'selected' : ''}><c:out value="${fl}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label">Freeze Reason</label>
                    <input type="text" class="form-control" name="freezeReason" value="<c:out value='${freezeReason}'/>" maxlength="255" />
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-warning"><i class="bi bi-snow"></i> Update Freeze</button>
                </div>
            </form>
            </c:if>

            <c:if test="${!(sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin)}">
                <div class="text-muted">You do not have permission to modify freeze controls.</div>
            </c:if>

            <hr>

            <h6><i class="bi bi-clock-history"></i> Freeze History</h6>
            <c:choose>
                <c:when test="${not empty freezeHistory}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Action</th><th>User</th><th>Details</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="fh" items="${freezeHistory}">
                                <tr>
                                    <td><small><c:out value="${fh.timestamp}"/></small></td>
                                    <td><span class="badge bg-info"><c:out value="${fh.action}"/></span></td>
                                    <td><c:out value="${fh.username}"/></td>
                                    <td><small><c:out value="${fh.details}"/></small></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-muted">No freeze history records found.</div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Tab: Relationships (Ownership) --%>
<div class="tab-content" id="tabRelationships" role="tabpanel" aria-labelledby="tabBtnRelationships">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="bi bi-people"></i> Relationships (Ownership)</h5>
            <a href="${pageContext.request.contextPath}/ownerships/account/${account.id}" class="btn btn-sm btn-outline-primary">
                <i class="bi bi-pencil"></i> Manage Ownership
            </a>
        </div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty ownerships}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm" id="relationshipsTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Type</th>
                                    <th>Customer No</th>
                                    <th>Name</th>
                                    <th>Status</th>
                                    <th class="text-end">Ownership %</th>
                                    <th class="text-end">Operational</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="o" items="${ownerships}">
                                <tr>
                                    <td><span class="badge bg-info"><c:out value="${o.ownershipType}"/></span></td>
                                    <td><code><c:out value="${o.customerNumber}"/></code></td>
                                    <td><c:out value="${o.customerName}"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${o.status == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                                            <c:when test="${o.status == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                                            <c:when test="${o.status == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary"><c:out value="${o.status}"/></span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end"><c:out value="${o.ownershipPercentage}"/>%</td>
                                    <td class="text-end">
                                        <c:choose>
                                            <c:when test="${o.isOperational == true}"><span class="badge bg-success">Yes</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary">No</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-muted">No ownership records.</div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Tab: Audit & Approval --%>
<div class="tab-content" id="tabAudit" role="tabpanel" aria-labelledby="tabBtnAudit">
    <div class="card shadow">
        <div class="card-header bg-white d-flex justify-content-between align-items-center">
            <h5 class="mb-0"><i class="bi bi-shield-check"></i> Audit & Approval</h5>
            <div class="d-flex gap-2">
                <c:if test="${makerCheckerStatus == 'PENDING' && (sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager)}">
                    <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/master/approve" class="m-0">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit" class="btn btn-sm btn-success" onclick="return confirm('Approve this account?')">
                            <i class="bi bi-check-circle"></i> Approve
                        </button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/accounts/${account.id}/master/reject" class="m-0">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit" class="btn btn-sm btn-danger" onclick="return confirm('Reject this account?')">
                            <i class="bi bi-x-circle"></i> Reject
                        </button>
                    </form>
                </c:if>
            </div>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-3"><strong>Maker:</strong> <c:out value="${createdByUsername}" default="--"/></div>
                <div class="col-md-3"><strong>Created At:</strong> <c:out value="${createdAt}" default="--"/></div>
                <div class="col-md-3"><strong>Checker:</strong> <c:out value="${approvedByUsername}" default="--"/></div>
                <div class="col-md-3"><strong>Last Updated:</strong> <c:out value="${updatedAt}" default="--"/></div>
            </div>

            <hr>

            <div class="row g-3">
                <div class="col-md-4">
                    <strong>Approval Status:</strong>
                    <c:choose>
                        <c:when test="${approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                        <c:when test="${approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-warning">PENDING</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4">
                    <strong>Freeze:</strong>
                    <c:choose>
                        <c:when test="${freezeLevel != null && freezeLevel != 'NONE'}"><span class="badge bg-danger"><c:out value="${freezeLevel}"/></span></c:when>
                        <c:otherwise><span class="badge bg-success">NONE</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4">
                    <strong>Account Status:</strong>
                    <span class="badge bg-info"><c:out value="${accountStatus}" default="--"/></span>
                </div>
            </div>

            <%-- Optional standardized audit fragment --%>
            <c:set var="auditCreatedBy" value="${createdByUsername}" scope="request"/>
            <c:set var="auditCreatedAt" value="${createdAt}" scope="request"/>
            <c:set var="auditApprovedBy" value="${approvedByUsername}" scope="request"/>
            <c:set var="auditApprovalStatus" value="${approvalStatus}" scope="request"/>
            <c:set var="auditUpdatedAt" value="${updatedAt}" scope="request"/>
            <c:set var="auditCurrentStatus" value="${accountStatus}" scope="request"/>
            <c:set var="auditEntityType" value="Account" scope="request"/>
            <c:set var="auditEntityId" value="${accountNumber}" scope="request"/>
            <%@ include file="../layout/audit-info.jsp" %>
        </div>
    </div>
</div>

<style>
/* Finacle-style tabs and master header (scoped to this page) */
.fin-master-header {
    border: 1px solid var(--bank-border);
    margin-bottom: 1rem;
}

.fin-header-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(220px, 1fr));
    gap: 0.75rem;
    align-items: end;
}

@media (max-width: 1200px) {
    .fin-header-grid {
        grid-template-columns: repeat(2, minmax(220px, 1fr));
    }
}

.fin-header-label {
    display: block;
    font-size: 0.78rem;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    color: #64748b;
    margin-bottom: 0.25rem;
}

.fin-readonly {
    background: #f8fafc;
    border-color: #e2e8f0;
}

.fin-header-status .fin-status-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
}

/* Tabs */
.fin-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.25rem;
    padding: 0.35rem;
    background: #ffffff;
    border: 1px solid var(--bank-border);
    border-radius: 6px;
    box-shadow: var(--ent-shadow-sm);
    margin-bottom: 0.75rem;
}

.fin-tab {
    border: 1px solid transparent;
    background: transparent;
    color: #475569;
    font-weight: 700;
    font-size: 0.82rem;
    padding: 0.45rem 0.75rem;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.12s ease, border-color 0.12s ease, color 0.12s ease;
}

.fin-tab:hover {
    background: rgba(0, 64, 128, 0.06);
    border-color: rgba(0, 64, 128, 0.12);
    color: var(--bank-primary);
}

.fin-tab.active {
    background: rgba(0, 64, 128, 0.10);
    border-color: rgba(0, 64, 128, 0.22);
    color: var(--bank-primary);
}

/* Required by task: each tab wrapped in .tab-content and hidden except active */
.tab-content {
    display: none;
}

.tab-content.active {
    display: block;
}

/* Status badge colors (Finacle-style) */
.fin-status-badge {
    display: inline-flex;
    align-items: center;
    padding: 0.25rem 0.7rem;
    border-radius: 999px;
    font-size: 0.75rem;
    font-weight: 800;
    letter-spacing: 0.04em;
    border: 1px solid rgba(0, 0, 0, 0.08);
}

.fin-status-pending {
    background: #fef3c7;
    color: #92400e;
    border-color: #fcd34d;
}

.fin-status-approved {
    background: #dcfce7;
    color: #166534;
    border-color: #86efac;
}

.fin-status-rejected {
    background: #fee2e2;
    color: #991b1b;
    border-color: #fca5a5;
}

/* Freeze indicator icon style (red highlight) */
.fin-freeze-indicator {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border-radius: 999px;
    border: 1px solid #fca5a5;
    background: #fee2e2;
    color: #991b1b;
}

.fin-freeze-indicator-inline {
    width: 22px;
    height: 22px;
    border-radius: 6px;
}

.fin-freeze-note {
    display: inline-flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.75rem;
    border: 1px solid #fca5a5;
    background: #fef2f2;
    border-radius: 6px;
}
</style>

<script>
function switchTab(tabId) {
    var tabs = document.querySelectorAll('.tab-content');
    tabs.forEach(function(t) {
        t.classList.remove('active');
    });

    var tabButtons = document.querySelectorAll('.fin-tab');
    tabButtons.forEach(function(b) {
        b.classList.remove('active');
    });

    var activeTab = document.getElementById(tabId);
    if (activeTab) {
        activeTab.classList.add('active');
    }

    var activeBtn = document.querySelector('.fin-tab[data-tab="' + tabId + '"]');
    if (activeBtn) {
        activeBtn.classList.add('active');
    }
}

document.addEventListener('DOMContentLoaded', function() {
    var hash = window.location.hash;
    if (hash && document.getElementById(hash.substring(1))) {
        switchTab(hash.substring(1));
    } else {
        switchTab('tabGeneral');
    }
});
</script>

<%@ include file="../layout/footer.jsp" %>
