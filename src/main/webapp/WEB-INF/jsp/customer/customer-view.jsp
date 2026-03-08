<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success alert-dismissible fade show"><c:out value="${message}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>
<c:if test="${not empty error}">
    <div class="alert alert-danger alert-dismissible fade show"><c:out value="${error}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
</c:if>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-person"></i> Customer Details</h3>
    <div>
        <a href="${pageContext.request.contextPath}/customers/${customer.customerId}/edit" class="btn btn-outline-secondary"><i class="bi bi-pencil"></i> Edit</a>
        <a href="${pageContext.request.contextPath}/customers" class="btn btn-outline-primary"><i class="bi bi-arrow-left"></i> Back</a>
    </div>
</div>

<c:if test="${customer.kycStatus == 'PENDING'}">
<div class="alert alert-warning mb-3"><i class="bi bi-hourglass-split"></i> <strong>PENDING APPROVAL</strong> - This customer requires approval before accounts can be opened.</div>
</c:if>

<ul class="nav nav-tabs mb-4" id="customerTabs">
    <li class="nav-item"><a class="nav-link active" data-bs-toggle="tab" href="#tab-basic">Basic Details</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-tax">Tax Profile</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-freeze">Freeze Control</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-accounts">Linked Accounts</a></li>
    <li class="nav-item"><a class="nav-link" data-bs-toggle="tab" href="#tab-approval">Approval</a></li>
</ul>

<div class="tab-content">
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
            <hr>
            <h6>Update KYC Status</h6>
            <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/kyc" class="row g-2">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-4">
                    <select name="kycStatus" class="form-select">
                        <option value="PENDING" ${customer.kycStatus == 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="VERIFIED" ${customer.kycStatus == 'VERIFIED' ? 'selected' : ''}>VERIFIED</option>
                        <option value="REJECTED" ${customer.kycStatus == 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    </select>
                </div>
                <div class="col-md-2"><button type="submit" class="btn btn-primary">Update KYC</button></div>
            </form>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-tax">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-receipt"></i> Tax Profile</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4"><strong>PAN:</strong> <c:out value="${customer.nationalId}" default="Not provided"/></div>
                <div class="col-md-4"><strong>Aadhaar:</strong> <span class="text-muted">XXXX-XXXX-****</span></div>
                <div class="col-md-4"><strong>GST:</strong> <span class="text-muted">Not provided</span></div>
            </div>
            <hr>
            <a href="${pageContext.request.contextPath}/tax-profiles/create?customerId=${customer.customerId}" class="btn btn-outline-primary btn-sm"><i class="bi bi-pencil"></i> Manage Tax Profile</a>
        </div>
    </div>
</div>

<div class="tab-pane fade" id="tab-freeze">
    <div class="card shadow">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-snow"></i> Freeze Control</h5></div>
        <div class="card-body">
            <h6>Update Freeze Level</h6>
            <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/freeze" class="row g-2">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="col-md-3">
                    <label class="form-label">Freeze Level *</label>
                    <select name="freezeLevel" class="form-select" required>
                        <c:forEach var="fl" items="${freezeLevels}">
                            <option value="${fl}"><c:out value="${fl}"/></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-5">
                    <label class="form-label">Freeze Reason *</label>
                    <input type="text" name="freezeReason" class="form-control" required maxlength="255" placeholder="Reason for freeze/unfreeze"/>
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button type="submit" class="btn btn-warning"><i class="bi bi-snow"></i> Update Freeze</button>
                </div>
            </form>
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
                <div class="col-md-4"><strong>Status:</strong>
                    <c:choose>
                        <c:when test="${customer.kycStatus == 'VERIFIED'}"><span class="badge bg-success">APPROVED</span></c:when>
                        <c:when test="${customer.kycStatus == 'PENDING'}"><span class="badge bg-warning">PENDING</span></c:when>
                        <c:otherwise><span class="badge bg-secondary"><c:out value="${customer.kycStatus}"/></span></c:otherwise>
                    </c:choose>
                </div>
            </div>
            <c:if test="${customer.kycStatus == 'PENDING'}">
                <div class="d-flex gap-2">
                    <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/approve">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit" class="btn btn-success" onclick="return confirm('Approve this customer?')"><i class="bi bi-check-circle"></i> Approve</button>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/customers/${customer.customerId}/reject">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                        <button type="submit" class="btn btn-danger" onclick="return confirm('Reject this customer?')"><i class="bi bi-x-circle"></i> Reject</button>
                    </form>
                </div>
            </c:if>
        </div>
    </div>
</div>
</div>

<%@ include file="../layout/footer.jsp" %>
