<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-person-circle"></i> Customer Details</h3>
    <div class="d-flex gap-2">
        <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/360" class="btn btn-outline-dark btn-sm" title="Customer 360° View"><i class="bi bi-person-circle"></i> 360° View</a>
        <c:if test="${sessionScope.isMaker || sessionScope.isAdmin || sessionScope.isManager}">
        <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/edit" class="btn btn-outline-secondary btn-sm"><i class="bi bi-pencil"></i> Edit</a>
        </c:if>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-primary btn-sm"><i class="bi bi-arrow-left"></i> Back</a>
    </div>
</div>

<%-- Operational Status Banner --%>
<c:if test="${customer.kycStatus == 'PENDING'}">
    <c:set var="approvalPending" value="${true}" scope="request"/>
    <c:set var="approvalPendingMessage" value="This customer requires approval before accounts can be opened." scope="request"/>
</c:if>
<%@ include file="../layout/status-banner.jsp" %>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- SUMMARY BANNER (Finacle-style sticky CIF strip)                   --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="card border-primary mb-3 shadow-sm">
    <div class="card-body py-2">
        <div class="row align-items-center g-2">
            <div class="col-md-3 d-flex align-items-center">
                <i class="bi bi-person-circle fs-2 text-primary me-2"></i>
                <div>
                    <h6 class="mb-0"><c:out value="${customer.firstName}"/> <c:out value="${customer.lastName}"/></h6>
                    <small class="text-muted">CIF: <code><c:out value="${customer.customerId}"/></code></small>
                </div>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Customer Type</small>
                <strong><c:out value="${customer.customerType}" default="INDIVIDUAL"/></strong>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">KYC Status</small>
                <c:choose>
                    <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                    <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                    <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.kycStatus}" default="N/A"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Approval</small>
                <c:choose>
                    <c:when test="${customer.approvalStatus == 'APPROVED'}"><span class="badge cbs-badge-approved">APPROVED</span></c:when>
                    <c:when test="${customer.approvalStatus == 'PENDING'}"><span class="badge cbs-badge-pending">PENDING</span></c:when>
                    <c:when test="${customer.approvalStatus == 'REJECTED'}"><span class="badge cbs-badge-rejected">REJECTED</span></c:when>
                    <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.approvalStatus}" default="--"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-2">
                <small class="text-muted d-block">Risk</small>
                <c:choose>
                    <c:when test="${customer.riskCategory == 'HIGH'}"><span class="badge bg-danger">HIGH</span></c:when>
                    <c:when test="${customer.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark">MEDIUM</span></c:when>
                    <c:otherwise><span class="badge bg-success"><c:out value="${customer.riskCategory}" default="LOW"/></span></c:otherwise>
                </c:choose>
            </div>
            <div class="col-md-1 text-end">
                <c:if test="${customer.freezeLevel != null && customer.freezeLevel != 'NONE'}">
                    <span class="badge bg-danger" title="Freeze: ${customer.freezeLevel}"><i class="bi bi-snow"></i> <c:out value="${customer.freezeLevel}"/></span>
                </c:if>
            </div>
        </div>
    </div>
</div>

<%-- Flash alerts already rendered and cleared by header.jsp (PART 7) --%>

<ul class="nav nav-tabs cbs-360-tabs mb-0" id="customerTabs">
    <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#tab-basic"><i class="bi bi-person-badge"></i> Basic Details</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-tax"><i class="bi bi-receipt"></i> Tax Profile</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-freeze"><i class="bi bi-snow"></i> Freeze Control</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-accounts"><i class="bi bi-wallet2"></i> Linked Accounts</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-approval"><i class="bi bi-clipboard-check"></i> Approval</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-risk"><i class="bi bi-shield-exclamation"></i> Risk Flags</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-audit-preview"><i class="bi bi-clock-history"></i> Audit Trail</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-freeze-history"><i class="bi bi-clock-history"></i> Freeze History</a></li>
</ul>

<div class="tab-content cbs-360-tab-content">
<div class="tab-pane fade show active" id="tab-basic">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-person-badge"></i> Customer Information</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>Customer ID:</strong> <c:out value="${customer.customerId}"/></div>
                <div class="col-md-4"><strong>National ID:</strong> <c:out value="${customer.nationalId}"/></div>
                <div class="col-md-4"><strong>KYC Status:</strong>
                    <c:choose>
                        <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                        <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.kycStatus}"/></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>First Name:</strong> <c:out value="${customer.firstName}"/></div>
                <div class="col-md-4"><strong>Last Name:</strong> <c:out value="${customer.lastName}"/></div>
                <div class="col-md-4"><strong>Date of Birth:</strong> <c:out value="${customer.dob}"/></div>
                <div class="col-md-4"><strong>Mobile:</strong> <c:out value="${customer.phone}"/></div>
                <div class="col-md-4"><strong>Email:</strong> <c:out value="${customer.email}"/></div>
                <div class="col-md-4"><strong>Created:</strong> <c:out value="${customer.createdAt}"/></div>
                <div class="col-12"><strong>Address:</strong> <c:out value="${customer.address}"/></div>
            </div>
            <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
            <hr>
            <h6>Update KYC Status</h6>
            <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/kyc" class="row g-2">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-4">
                    <select name="kycStatus" class="form-select">
                        <option value="PENDING" ${customer.kycStatus == 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="UNDER_REVIEW" ${customer.kycStatus == 'UNDER_REVIEW' ? 'selected' : ''}>UNDER REVIEW</option>
                        <option value="VERIFIED" ${customer.kycStatus == 'VERIFIED' ? 'selected' : ''}>VERIFIED</option>
                        <option value="REJECTED" ${customer.kycStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    </select>
                </div>
                <div class="col-md-2"><button type="submit" class="btn btn-primary">Update KYC</button></div>
            </form>
            </c:if>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-tax">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-receipt"></i> Tax Profile</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>Customer Type:</strong>
                    <span class="badge bg-info"><c:out value="${customer.customerType}" default="INDIVIDUAL"/></span>
                </div>
                <div class="col-md-4"><strong>Risk Category:</strong>
                    <c:choose>
                        <c:when test="${customer.riskCategory == 'HIGH'}"><span class="badge bg-danger"><c:out value="${customer.riskCategory}"/></span></c:when>
                        <c:when test="${customer.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark"><c:out value="${customer.riskCategory}"/></span></c:when>
                        <c:otherwise><span class="badge bg-success"><c:out value="${customer.riskCategory}" default="LOW"/></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"></div>
                <div class="col-md-4"><strong>PAN:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.panNumber}"><code><c:out value="${customer.panNumber}"/></code></c:when>
                        <c:otherwise><span class="text-muted">Not provided</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>Aadhaar:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.aadhaarNumber}"><span class="text-muted">XXXX-XXXX-<c:out value="${fn:substring(customer.aadhaarNumber, 8, 12)}"/></span></c:when>
                        <c:otherwise><span class="text-muted">Not provided</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>GST:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.gstNumber}"><code><c:out value="${customer.gstNumber}"/></code></c:when>
                        <c:otherwise><span class="text-muted">Not provided</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <hr>
            <a href="${pageContext.request.contextPath}/tax-profiles/create?customerId=${customer.customerId}" class="btn btn-outline-primary btn-sm">
                <i class="bi bi-pencil"></i> Manage Tax Profile
            </a>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-freeze">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-snow"></i> Freeze Control</h5></div>
        <div class="card-body">
            <c:if test="${sessionScope.isAdmin || sessionScope.isManager || sessionScope.isBranchManager || sessionScope.isTenantAdmin || sessionScope.isSuperAdmin}">
            <h6>Update Freeze Level</h6>
            <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/freeze" class="row g-2">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-3">
                    <label class="form-label cbs-field-required">Freeze Level</label>
                    <select name="freezeLevel" class="form-select" required>
                        <c:forEach var="fl" items="${freezeLevels}">
                            <option value="${fl}"><c:out value="${fl}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-5">
                    <label class="form-label cbs-field-required">Freeze Reason</label>
                    <input type="text" name="freezeReason" class="form-control" required maxlength="255" placeholder="Reason for freeze/unfreeze"/>
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button type="submit" class="btn btn-warning"><i class="bi bi-snow"></i> Update Freeze</button>
                </div>
            </form>
            </c:if>
            <c:if test="${!sessionScope.isAdmin && !sessionScope.isManager && !sessionScope.isBranchManager && !sessionScope.isTenantAdmin && !sessionScope.isSuperAdmin}">
            <div class="text-center py-3 text-muted">
                <i class="bi bi-lock" style="font-size: 2rem;"></i>
                <p class="mt-2">You do not have permission to modify freeze controls.</p>
            </div>
            </c:if>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-accounts">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-wallet2"></i> Linked Accounts</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty linkedAccounts}">
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead class="table-light">
                                <tr><th>Account Number</th><th>Name</th><th>Type</th><th>Status</th><th>Balance</th><th>Actions</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="acct" items="${linkedAccounts}">
                                <tr>
                                    <td><code><c:out value="${acct.accountNumber}"/></code></td>
                                    <td><c:out value="${acct.accountName}"/></td>
                                    <td><span class="badge bg-info"><c:out value="${acct.accountType}"/></span></td>
                                    <td><span class="badge bg-success"><c:out value="${acct.status}"/></span></td>
                                    <td class="fw-bold"><c:out value="${acct.balance}"/></td>
                                    <td><a href="${pageContext.request.contextPath}/accounts/${acct.id}" class="btn btn-sm btn-outline-primary"><i class="bi bi-eye"></i></a></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise><div class="text-center py-4 text-muted"><i class="bi bi-wallet2" style="font-size: 2rem;"></i><p class="mt-2">No linked accounts.</p></div></c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-approval">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clipboard-check"></i> Approval</h5></div>
        <div class="card-body">
            <div class="row g-3 mb-3">
                <div class="col-md-4"><strong>Approval Status:</strong>
                    <c:choose>
                        <c:when test="${customer.approvalStatus == 'APPROVED'}"><span class="badge bg-success">APPROVED</span></c:when>
                        <c:when test="${customer.approvalStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING APPROVAL</span></c:when>
                        <c:when test="${customer.approvalStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.approvalStatus}"/></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-4"><strong>KYC Status:</strong>
                    <c:choose>
                        <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                        <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark">PENDING</span></c:when>
                        <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.kycStatus}"/></span></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <c:if test="${customer.approvalStatus == 'PENDING'}">
                <%-- KYC pre-flight check: service enforces KYC=VERIFIED before approval --%>
                <c:if test="${customer.kycStatus != 'VERIFIED'}">
                <div class="alert alert-warning d-flex align-items-start mb-3">
                    <i class="bi bi-exclamation-triangle-fill me-2 mt-1"></i>
                    <div>
                        <strong>KYC not verified.</strong>
                        This customer cannot be approved until KYC status is set to
                        <strong>VERIFIED</strong>. Current KYC: <strong><c:out value="${customer.kycStatus}"/></strong>.
                        Use the <em>Basic Details</em> tab to update KYC status first.
                    </div>
                </div>
                </c:if>
                <c:if test="${sessionScope.isChecker || sessionScope.isAdmin || sessionScope.isManager}">
                <div class="d-flex gap-2">
                    <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/approve">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit"
                                class="btn btn-success ${customer.kycStatus != 'VERIFIED' ? 'disabled' : ''}"
                                ${customer.kycStatus != 'VERIFIED' ? 'disabled' : ''}
                                onclick="return confirm('Approve this customer?')">
                            <i class="bi bi-check-circle"></i> Approve
                        </button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/reject">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit" class="btn btn-danger" onclick="return confirm('Reject this customer?')">
                            <i class="bi bi-x-circle"></i> Reject
                        </button>
                    </form>
                </div>
                </c:if>
            </c:if>
        </div>
    </div>
</div>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- RISK FLAGS TAB                                                    --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="tab-pane fade" id="tab-risk">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-shield-exclamation me-2"></i>Risk &amp; Governance Flags</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <%-- Risk Category --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Risk Category</small>
                            <c:choose>
                                <c:when test="${customer.riskCategory == 'HIGH'}"><span class="badge bg-danger fs-6">HIGH</span></c:when>
                                <c:when test="${customer.riskCategory == 'MEDIUM'}"><span class="badge bg-warning text-dark fs-6">MEDIUM</span></c:when>
                                <c:otherwise><span class="badge bg-success fs-6"><c:out value="${customer.riskCategory}" default="LOW"/></span></c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">RBI KYC risk-based classification.</div>
                        </div>
                    </div>
                </div>
                <%-- Freeze Level --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Freeze Level</small>
                            <c:choose>
                                <c:when test="${customer.freezeLevel == null || customer.freezeLevel == 'NONE'}">
                                    <span class="badge bg-success fs-6">NONE</span>
                                </c:when>
                                <c:when test="${customer.freezeLevel == 'FULL'}">
                                    <span class="badge bg-danger fs-6"><i class="bi bi-snow"></i> FULL FREEZE</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-warning text-dark fs-6"><i class="bi bi-snow"></i> <c:out value="${customer.freezeLevel}"/></span>
                                </c:otherwise>
                            </c:choose>
                            <c:if test="${not empty customer.freezeReason}">
                                <div class="text-muted small mt-2">Reason: <c:out value="${customer.freezeReason}"/></div>
                            </c:if>
                        </div>
                    </div>
                </div>
                <%-- KYC Status --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">KYC Status</small>
                            <c:choose>
                                <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success fs-6">VERIFIED</span></c:when>
                                <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning text-dark fs-6">PENDING</span></c:when>
                                <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger fs-6">REJECTED</span></c:when>
                                <c:otherwise><span class="badge bg-secondary fs-6"><c:out value="${customer.kycStatus}" default="N/A"/></span></c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">RBI KYC Master Direction compliance.</div>
                        </div>
                    </div>
                </div>
                <%-- Approval Status --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Approval Status</small>
                            <c:choose>
                                <c:when test="${customer.approvalStatus == 'APPROVED'}"><span class="badge cbs-badge-approved fs-6">APPROVED</span></c:when>
                                <c:when test="${customer.approvalStatus == 'PENDING'}"><span class="badge cbs-badge-pending fs-6">PENDING</span></c:when>
                                <c:when test="${customer.approvalStatus == 'REJECTED'}"><span class="badge cbs-badge-rejected fs-6">REJECTED</span></c:when>
                                <c:otherwise><span class="badge bg-secondary fs-6"><c:out value="${customer.approvalStatus}" default="--"/></span></c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">Maker-checker dual control status.</div>
                        </div>
                    </div>
                </div>
                <%-- PEP Indicator --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">PEP Indicator</small>
                            <c:choose>
                                <c:when test="${customer.isPep == true}">
                                    <span class="badge bg-danger fs-6"><i class="bi bi-exclamation-octagon-fill"></i> PEP — HIGH RISK</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="badge bg-success fs-6">Not PEP</span>
                                </c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">Politically Exposed Person — per RBI KYC Master Direction.</div>
                        </div>
                    </div>
                </div>
                <%-- Occupation --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Occupation</small>
                            <c:choose>
                                <c:when test="${not empty customer.occupation}">
                                    <strong><c:out value="${customer.occupation}"/></strong>
                                </c:when>
                                <c:otherwise><span class="text-muted">Not captured</span></c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">Used for risk derivation.</div>
                        </div>
                    </div>
                </div>
                <%-- Annual Income --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Annual Income (INR)</small>
                            <c:choose>
                                <c:when test="${not empty customer.annualIncome}">
                                    <strong>&#8377; <c:out value="${customer.annualIncome}"/></strong>
                                </c:when>
                                <c:otherwise><span class="text-muted">Not captured</span></c:otherwise>
                            </c:choose>
                            <div class="text-muted small mt-2">Used for risk derivation.</div>
                        </div>
                    </div>
                </div>
                <%-- Maker / Checker --%>
                <div class="col-md-4">
                    <div class="card border-0 bg-light h-100">
                        <div class="card-body">
                            <small class="text-muted d-block mb-1">Maker / Checker</small>
                            <div class="small">
                                <i class="bi bi-person-fill text-primary"></i>
                                <c:choose>
                                    <c:when test="${customer.createdBy != null}"><c:out value="${customer.createdBy.username}"/></c:when>
                                    <c:otherwise><span class="text-muted">System</span></c:otherwise>
                                </c:choose>
                                <c:if test="${customer.makerTimestamp != null}">
                                    <br/><small class="text-muted"><c:out value="${customer.makerTimestamp}"/></small>
                                </c:if>
                            </div>
                            <div class="small mt-1">
                                <i class="bi bi-person-check-fill text-success"></i>
                                <c:choose>
                                    <c:when test="${customer.approvedBy != null}"><c:out value="${customer.approvedBy.username}"/></c:when>
                                    <c:otherwise><span class="text-muted">Pending</span></c:otherwise>
                                </c:choose>
                                <c:if test="${customer.checkerTimestamp != null}">
                                    <br/><small class="text-muted"><c:out value="${customer.checkerTimestamp}"/></small>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- ═══════════════════════════════════════════════════════════════════ --%>
<%-- AUDIT TRAIL PREVIEW TAB (last 5 entries)                          --%>
<%-- ═══════════════════════════════════════════════════════════════════ --%>
<div class="tab-pane fade" id="tab-audit-preview">
    <div class="card shadow">
        <div class="card-header bg-white d-flex align-items-center">
            <h5 class="mb-0"><i class="bi bi-clock-history me-2"></i>Audit Trail Preview</h5>
            <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/360#pane-audit"
               class="btn btn-outline-secondary btn-sm ms-auto">Full Audit Trail in 360° View</a>
        </div>
        <div class="card-body">
            <div class="alert alert-light border mb-3">
                <i class="bi bi-info-circle"></i> Showing last <strong>5</strong> audit entries for this customer. Audit trail is <strong>read-only</strong> and immutable.
            </div>
            <c:choose>
                <c:when test="${not empty auditPreview}">
                <div class="cbs-audit-timeline">
                    <c:forEach var="al" items="${auditPreview}">
                    <div class="cbs-audit-entry">
                        <div class="cbs-audit-dot"></div>
                        <div class="cbs-audit-content">
                            <div class="d-flex justify-content-between align-items-start">
                                <div>
                                    <span class="badge bg-dark me-1"><c:out value="${al.action}"/></span>
                                    <c:if test="${al.username != null}">
                                        <span class="text-muted">by <strong><c:out value="${al.username}"/></strong></span>
                                    </c:if>
                                </div>
                                <small class="text-muted"><c:out value="${al.timestamp}"/></small>
                            </div>
                            <c:if test="${al.details != null}">
                                <p class="mb-1 mt-1 small"><c:out value="${al.details}"/></p>
                            </c:if>
                            <c:if test="${al.ipAddress != null}">
                                <small class="text-muted">IP: <c:out value="${al.ipAddress}"/></small>
                            </c:if>
                        </div>
                    </div>
                    </c:forEach>
                </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-clock-history" style="font-size: 2rem;"></i>
                        <p class="mt-2">No audit entries found for this customer.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Freeze History Tab --%>
<div class="tab-pane fade" id="tab-freeze-history">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clock-history"></i> Freeze History</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty freezeHistory}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm">
                            <thead class="table-light">
                                <tr><th>Date</th><th>Action</th><th>Maker</th><th>Checker</th><th>Status</th><th>Reason</th></tr>
                            </thead>
                            <tbody>
                                <c:forEach var="fh" items="${freezeHistory}">
                                <tr>
                                    <td><small><c:out value="${fh.timestamp}"/></small></td>
                                    <td><span class="badge bg-info"><c:out value="${fh.action}"/></span></td>
                                    <td><c:out value="${fh.username}" default="System"/></td>
                                    <td><c:out value="${fh.checker}" default="--"/></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${fh.action == 'FREEZE_APPLY'}"><span class="badge bg-danger">Frozen</span></c:when>
                                            <c:when test="${fh.action == 'FREEZE_RELEASE'}"><span class="badge bg-success">Released</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary"><c:out value="${fh.action}"/></span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><small><c:out value="${fh.details}"/></small></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-clock-history" style="font-size: 2rem;"></i>
                        <p class="mt-2">No freeze history records found.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>
</div>

<%-- Audit Info Section --%>
<c:set var="auditCreatedBy" value="${customer.createdBy != null ? customer.createdBy.username : ''}" scope="request"/>
<c:set var="auditCreatedAt" value="${customer.createdAt}" scope="request"/>
<c:set var="auditLastModifiedBy" value="${customer.createdBy != null ? customer.createdBy.username : ''}" scope="request"/>
<c:set var="auditUpdatedAt" value="${customer.updatedAt}" scope="request"/>
<c:set var="auditApprovedBy" value="${customer.approvedBy != null ? customer.approvedBy.username : ''}" scope="request"/>
<c:set var="auditApprovalStatus" value="${customer.approvalStatus}" scope="request"/>
<c:set var="auditCurrentStatus" value="${customer.approvalStatus}" scope="request"/>
<c:set var="auditEntityType" value="Customer" scope="request"/>
<c:set var="auditEntityId" value="${customer.customerId}" scope="request"/>
<%@ include file="../layout/audit-info.jsp" %>

<%@ include file="../layout/footer.jsp" %>
