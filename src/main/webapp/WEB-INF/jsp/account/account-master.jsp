<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Finacle-style Account Master (UI skeleton only; no backend wiring) --%>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-wallet2"></i> Account Master</h3>
    <div>
        <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-primary">
            <i class="bi bi-arrow-left"></i> Back
        </a>
    </div>
</div>

<%-- Header Section: Account/Tenant/Branch/Maker-Checker --%>
<div class="card shadow fin-master-header">
    <div class="card-body">
        <div class="fin-header-grid">
            <div class="fin-header-field">
                <label class="fin-header-label">Account Number</label>
                <input type="text" class="form-control fin-readonly" id="accountNumber" value="--" readonly />
            </div>
            <div class="fin-header-field">
                <label class="fin-header-label">Tenant</label>
                <input type="text" class="form-control fin-readonly" id="tenantName" value="--" readonly />
            </div>
            <div class="fin-header-field">
                <label class="fin-header-label">Branch</label>
                <input type="text" class="form-control fin-readonly" id="branchName" value="--" readonly />
            </div>
            <div class="fin-header-field fin-header-status">
                <label class="fin-header-label">Maker-Checker Status</label>
                <div class="fin-status-row">
                    <span id="makerCheckerStatusBadge" class="fin-status-badge fin-status-pending">PENDING</span>
                    <span id="freezeIndicator" class="fin-freeze-indicator" title="Freeze Active" aria-label="Freeze indicator">
                        <i class="bi bi-snow" aria-hidden="true"></i>
                    </span>
                </div>
                <small class="text-muted">Status shown as placeholder only</small>
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

<%-- Tab: General Info --%>
<div class="tab-content active" id="tabGeneral" role="tabpanel" aria-labelledby="tabBtnGeneral">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">General Info</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Account Name</label>
                    <input type="text" class="form-control" id="accountName" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Account Type</label>
                    <select class="form-select" id="accountType">
                        <option value="">-- Select --</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Status</label>
                    <select class="form-select" id="accountStatus">
                        <option value="">-- Select --</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Currency</label>
                    <input type="text" class="form-control" id="currency" placeholder="INR" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">GL Account Code</label>
                    <input type="text" class="form-control" id="glAccountCode" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Home Branch</label>
                    <input type="text" class="form-control" id="homeBranch" placeholder="--" />
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Contact Info --%>
<div class="tab-content" id="tabContact" role="tabpanel" aria-labelledby="tabBtnContact">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">Contact Info</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Customer Name</label>
                    <input type="text" class="form-control" id="customerName" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Mobile</label>
                    <input type="text" class="form-control" id="customerPhone" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Email</label>
                    <input type="text" class="form-control" id="customerEmail" placeholder="--" />
                </div>
                <div class="col-12">
                    <label class="form-label">Address</label>
                    <textarea class="form-control" id="customerAddress" rows="2" placeholder="--"></textarea>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: KYC & Identity --%>
<div class="tab-content" id="tabKyc" role="tabpanel" aria-labelledby="tabBtnKyc">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">KYC & Identity</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Customer Number</label>
                    <input type="text" class="form-control" id="customerNumber" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">National ID</label>
                    <input type="text" class="form-control" id="nationalId" placeholder="--" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">KYC Status</label>
                    <select class="form-select" id="kycStatus">
                        <option value="">-- Select --</option>
                    </select>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Tax Profile --%>
<div class="tab-content" id="tabTax" role="tabpanel" aria-labelledby="tabBtnTax">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">Tax Profile</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">PAN</label>
                    <input type="text" class="form-control" id="panNumber" placeholder="ABCDE1234F" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">Aadhaar</label>
                    <input type="text" class="form-control" id="aadhaarNumber" placeholder="XXXXXXXXXXXX" />
                </div>
                <div class="col-md-4">
                    <label class="form-label">GST</label>
                    <input type="text" class="form-control" id="gstNumber" placeholder="--" />
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Freeze Control --%>
<div class="tab-content" id="tabFreeze" role="tabpanel" aria-labelledby="tabBtnFreeze">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">Freeze Control</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Freeze Level</label>
                    <select class="form-select" id="freezeLevel">
                        <option value="">-- Select --</option>
                    </select>
                </div>
                <div class="col-md-8">
                    <label class="form-label">Freeze Reason</label>
                    <input type="text" class="form-control" id="freezeReason" placeholder="--" />
                </div>
                <div class="col-12">
                    <div class="fin-freeze-note">
                        <span class="fin-freeze-indicator fin-freeze-indicator-inline" aria-hidden="true"><i class="bi bi-snow"></i></span>
                        <strong>Freeze Indicator</strong>
                        <span class="text-muted">(placeholder styling; enforcement not wired)</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Relationships --%>
<div class="tab-content" id="tabRelationships" role="tabpanel" aria-labelledby="tabBtnRelationships">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">Relationships</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="table-responsive">
                <table class="table table-hover table-sm" id="relationshipsTable">
                    <thead class="table-light">
                        <tr>
                            <th>Type</th>
                            <th>Ref No</th>
                            <th>Name</th>
                            <th>Status</th>
                            <th class="text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td colspan="5" class="text-center text-muted">No relationship records (UI skeleton)</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Audit & Approval --%>
<div class="tab-content" id="tabAudit" role="tabpanel" aria-labelledby="tabBtnAudit">
    <div class="card shadow">
        <div class="card-header bg-white">
            <h5 class="mb-0">Audit & Approval</h5>
            <small class="text-muted">UI skeleton only</small>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label">Maker</label>
                    <input type="text" class="form-control fin-readonly" id="makerUsername" value="--" readonly />
                </div>
                <div class="col-md-3">
                    <label class="form-label">Maker Timestamp</label>
                    <input type="text" class="form-control fin-readonly" id="makerTimestamp" value="--" readonly />
                </div>
                <div class="col-md-3">
                    <label class="form-label">Checker</label>
                    <input type="text" class="form-control fin-readonly" id="checkerUsername" value="--" readonly />
                </div>
                <div class="col-md-3">
                    <label class="form-label">Checker Timestamp</label>
                    <input type="text" class="form-control fin-readonly" id="checkerTimestamp" value="--" readonly />
                </div>
            </div>

            <hr>

            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Approval Status</label>
                    <div>
                        <span id="approvalStatusBadge" class="fin-status-badge fin-status-pending">PENDING</span>
                    </div>
                </div>
                <div class="col-md-8">
                    <label class="form-label">Remarks</label>
                    <textarea class="form-control" id="approvalRemarks" rows="2" placeholder="--"></textarea>
                </div>
            </div>
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
    switchTab('tabGeneral');
});
</script>

<%@ include file="../layout/footer.jsp" %>
