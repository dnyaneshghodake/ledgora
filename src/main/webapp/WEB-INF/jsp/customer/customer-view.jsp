<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Customer Freeze Banner --%>
<c:if test="${customer.frozen || customer.debitFrozen || customer.creditFrozen}">
<div class="cbs-freeze-banner">
    <i class="bi bi-slash-circle-fill"></i>
    <span>CUSTOMER FROZEN &mdash; Transactions Blocked
        <c:if test="${customer.debitFrozen && !customer.creditFrozen}"> (Debit Freeze)</c:if>
        <c:if test="${customer.creditFrozen && !customer.debitFrozen}"> (Credit Freeze)</c:if>
        <c:if test="${customer.debitFrozen && customer.creditFrozen}"> (Full Freeze)</c:if>
    </span>
</div>
</c:if>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-person"></i> Customer Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/edit" class="btn btn-outline-secondary">
            <i class="bi bi-pencil"></i> Edit
        </a>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-primary">
            <i class="bi bi-arrow-left"></i> Back
        </a>
    </div>
</div>

<%-- CBS Tabbed Navigation --%>
<ul class="nav cbs-tabs mb-4">
    <li class="nav-item">
        <a class="nav-link active" href="#" data-tab="tab-basic">Basic Details</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="#" data-tab="tab-tax">Tax Profile</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="#" data-tab="tab-freeze">Freeze Control</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="#" data-tab="tab-relationship">Relationship</a>
    </li>
    <li class="nav-item">
        <a class="nav-link" href="#" data-tab="tab-audit">Audit Trail</a>
    </li>
</ul>

<%-- Tab: Basic Details --%>
<div class="cbs-tab-pane" id="tab-basic" style="display: block;">
    <div class="card shadow">
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-6"><strong>Customer ID:</strong> <c:out value="${customer.customerId}"/></div>
                <div class="col-md-6"><strong>Name:</strong> <c:out value="${customer.firstName}"/> <c:out value="${customer.lastName}"/></div>
                <div class="col-md-6"><strong>National ID:</strong> <code><c:out value="${customer.nationalId}"/></code></div>
                <div class="col-md-6"><strong>Date of Birth:</strong> ${customer.dob}</div>
                <div class="col-md-6"><strong>Email:</strong> <c:out value="${customer.email}"/></div>
                <div class="col-md-6"><strong>Phone:</strong> <c:out value="${customer.phone}"/></div>
                <div class="col-12"><strong>Address:</strong> <c:out value="${customer.address}"/></div>
                <div class="col-md-6">
                    <strong>KYC Status:</strong>
                    <c:choose>
                        <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">VERIFIED</span></c:when>
                        <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:when test="${customer.kycStatus == 'REJECTED'}"><span class="badge bg-danger">REJECTED</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">${customer.kycStatus}</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-6"><strong>Created:</strong> ${customer.createdAt}</div>
            </div>
            <hr>
            <h5>Update KYC Status</h5>
            <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/kyc" class="row g-2">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-4">
                    <select name="kycStatus" class="form-select">
                        <option value="PENDING">PENDING</option>
                        <option value="VERIFIED">VERIFIED</option>
                        <option value="REJECTED">REJECTED</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary">Update</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Tab: Tax Profile --%>
<div class="cbs-tab-pane" id="tab-tax" style="display: none;">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-receipt"></i> Tax Profile</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-6">
                    <strong>PAN / Tax ID:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.taxId}"><c:out value="${customer.taxId}"/></c:when>
                        <c:otherwise><span class="text-muted">Not provided</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-6">
                    <strong>Tax Residency:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.taxResidency}"><c:out value="${customer.taxResidency}"/></c:when>
                        <c:otherwise><span class="text-muted">Not set</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-6">
                    <strong>TDS Applicable:</strong>
                    <c:choose>
                        <c:when test="${customer.tdsApplicable}"><span class="badge bg-warning">Yes</span></c:when>
                        <c:otherwise><span class="badge bg-secondary">No</span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-6">
                    <strong>Tax Category:</strong>
                    <c:choose>
                        <c:when test="${not empty customer.taxCategory}"><c:out value="${customer.taxCategory}"/></c:when>
                        <c:otherwise><span class="text-muted">Default</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</div>

<%-- Tab: Freeze Control --%>
<div class="cbs-tab-pane" id="tab-freeze" style="display: none;">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-snow"></i> Freeze Control</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="card ${customer.debitFrozen ? 'border-danger' : 'border-success'}">
                        <div class="card-body text-center">
                            <i class="bi ${customer.debitFrozen ? 'bi-lock-fill text-danger' : 'bi-unlock-fill text-success'}" style="font-size: 2rem;"></i>
                            <h6 class="mt-2">Debit Freeze</h6>
                            <span class="badge ${customer.debitFrozen ? 'bg-danger' : 'bg-success'}">${customer.debitFrozen ? 'FROZEN' : 'ACTIVE'}</span>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card ${customer.creditFrozen ? 'border-danger' : 'border-success'}">
                        <div class="card-body text-center">
                            <i class="bi ${customer.creditFrozen ? 'bi-lock-fill text-danger' : 'bi-unlock-fill text-success'}" style="font-size: 2rem;"></i>
                            <h6 class="mt-2">Credit Freeze</h6>
                            <span class="badge ${customer.creditFrozen ? 'bg-danger' : 'bg-success'}">${customer.creditFrozen ? 'FROZEN' : 'ACTIVE'}</span>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card ${customer.frozen ? 'border-danger' : 'border-success'}">
                        <div class="card-body text-center">
                            <i class="bi ${customer.frozen ? 'bi-shield-lock-fill text-danger' : 'bi-shield-check text-success'}" style="font-size: 2rem;"></i>
                            <h6 class="mt-2">Full Freeze</h6>
                            <span class="badge ${customer.frozen ? 'bg-danger' : 'bg-success'}">${customer.frozen ? 'FROZEN' : 'ACTIVE'}</span>
                        </div>
                    </div>
                </div>
            </div>
            <c:if test="${not empty customer.freezeReason}">
            <div class="alert alert-warning mt-3">
                <strong>Freeze Reason:</strong> <c:out value="${customer.freezeReason}"/>
            </div>
            </c:if>
        </div>
    </div>
</div>

<%-- Tab: Relationship --%>
<div class="cbs-tab-pane" id="tab-relationship" style="display: none;">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-people"></i> Linked Accounts & Relationships</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty linkedAccounts}">
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th>Account Number</th>
                                    <th>Account Name</th>
                                    <th>Type</th>
                                    <th>Status</th>
                                    <th>Balance</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="acct" items="${linkedAccounts}">
                                <tr>
                                    <td><code><c:out value="${acct.accountNumber}"/></code></td>
                                    <td><c:out value="${acct.accountName}"/></td>
                                    <td><span class="badge bg-info">${acct.accountType}</span></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${acct.status == 'ACTIVE'}"><span class="badge bg-success">ACTIVE</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary">${acct.status}</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>${acct.balance} ${acct.currency}</td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-wallet2" style="font-size: 2rem;"></i>
                        <p class="mt-2">No linked accounts found for this customer.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%-- Tab: Audit Trail --%>
<div class="cbs-tab-pane" id="tab-audit" style="display: none;">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-clock-history"></i> Audit Trail</h5></div>
        <div class="card-body">
            <c:choose>
                <c:when test="${not empty auditTrail}">
                    <div class="table-responsive">
                        <table class="table table-hover table-sm">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>Action</th>
                                    <th>Performed By</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="audit" items="${auditTrail}">
                                <tr>
                                    <td><small>${audit.timestamp}</small></td>
                                    <td><span class="badge bg-secondary">${audit.action}</span></td>
                                    <td><c:out value="${audit.performedBy}"/></td>
                                    <td><small><c:out value="${audit.details}"/></small></td>
                                </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="text-center py-4 text-muted">
                        <i class="bi bi-clock-history" style="font-size: 2rem;"></i>
                        <p class="mt-2">No audit records available.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
