<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-wallet-fill"></i> Open Account</h3>
    <a href="${pageContext.request.contextPath}/accounts" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-header bg-white">
        <h5 class="mb-0"><i class="bi bi-wallet-fill"></i> Account Information</h5>
        <small class="text-muted">Fields marked with * are required. Account will be created in PENDING approval status.</small>
    </div>
    <div class="card-body">
        <form:form method="post" action="${pageContext.request.contextPath}/accounts/create" modelAttribute="accountDTO">

            <%-- Customer Lookup Section (PART 5) --%>
            <div class="card bg-light mb-4">
                <div class="card-body">
                    <h6><i class="bi bi-person-badge"></i> Customer Selection</h6>
                    <div class="row g-2">
                        <div class="col-md-4">
                            <label class="form-label cbs-field-required">Customer ID</label>
                            <div class="input-group">
                                <form:input path="customerId" cssClass="form-control" required="required" id="customerIdInput" type="number" readonly="true"
                                            placeholder="Use lookup to select"/>
                                <button type="button" class="btn btn-outline-primary" id="btnLookupCustomer" onclick="openCustomerLookup('customerIdInput','customerNameDisplay','customerKycDisplay')">
                                    <i class="bi bi-search"></i> Lookup
                                </button>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Customer Name</label>
                            <input type="text" class="form-control" id="customerNameDisplay" disabled placeholder="Auto-filled on lookup"/>
                            <form:hidden path="customerName" id="customerNameHidden"/>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">KYC Status</label>
                            <input type="text" class="form-control" id="customerKycDisplay" disabled placeholder="Auto-filled"/>
                        </div>
                    </div>
                    <div id="customerWarning" class="alert alert-warning mt-2" style="display:none;"></div>
                </div>
            </div>

            <div class="row g-3">
                <%-- Account Name --%>
                <div class="col-md-6">
                    <label class="form-label cbs-field-required">Account Name</label>
                    <form:input path="accountName" cssClass="form-control" required="required" maxlength="100"/>
                    <form:errors path="accountName" cssClass="text-danger small" />
                </div>

                <%-- Account Type --%>
                <div class="col-md-6">
                    <label class="form-label cbs-field-required">Account Type</label>
                    <form:select path="accountType" cssClass="form-select" required="required">
                        <option value="">Select Account Type</option>
                        <c:forEach var="at" items="${accountTypes}">
                            <option value="${at}" ${accountDTO.accountType == at ? 'selected' : ''}><c:out value="${at}"/></option>
                        </c:forEach>
                    </form:select>
                    <form:errors path="accountType" cssClass="text-danger small" />
                </div>

                <%-- Currency --%>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Currency</label>
                    <form:select path="currency" cssClass="form-select" required="required">
                        <option value="INR" ${accountDTO.currency == 'INR' ? 'selected' : ''}>INR</option>
                        <option value="USD" ${accountDTO.currency == 'USD' ? 'selected' : ''}>USD</option>
                        <option value="EUR" ${accountDTO.currency == 'EUR' ? 'selected' : ''}>EUR</option>
                    </form:select>
                </div>

                <%-- Interest Rate --%>
                <div class="col-md-4">
                    <label class="form-label">Interest Rate (%)</label>
                    <form:input path="interestRate" type="number" cssClass="form-control" step="0.01" min="0" max="100"/>
                    <form:errors path="interestRate" cssClass="text-danger small" />
                </div>

                <%-- Overdraft Limit --%>
                <div class="col-md-4">
                    <label class="form-label">Overdraft Limit</label>
                    <form:input path="overdraftLimit" type="number" cssClass="form-control" step="0.01" min="0"/>
                    <form:errors path="overdraftLimit" cssClass="text-danger small" />
                </div>

                <%-- GL Account Code --%>
                <div class="col-md-6">
                    <label class="form-label">GL Account Code</label>
                    <div class="input-group">
                        <form:input path="glAccountCode" cssClass="form-control" maxlength="20" id="glAccountCode" readonly="true"
                                    placeholder="Use lookup to select"/>
                        <button type="button" class="btn btn-outline-primary" onclick="openGlParentLookup('glAccountCode','glAccountCodeName')" title="Search GL Account">
                            <i class="bi bi-search"></i>
                        </button>
                    </div>
                    <input type="hidden" id="glAccountCodeName"/>
                </div>

                <%-- Parent Account (lookup) --%>
                <div class="col-md-6">
                    <label class="form-label">Parent Account ID</label>
                    <form:input path="parentAccountId" type="number" cssClass="form-control"/>
                    <small class="text-muted">For GL hierarchy</small>
                </div>

                <div class="col-12">
                    <hr>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Open Account (Pending Approval)</button>
                    <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary ms-2">Cancel</a>
                </div>
            </div>
        </form:form>
    </div>
</div>

<%-- Customer Lookup Modal (PART 5) --%>
<div class="modal fade" id="customerLookupModal" tabindex="-1">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-search"></i> Customer Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="customerSearchInput" class="form-control" placeholder="Search by name..."/>
                    <button class="btn btn-primary" onclick="searchCustomers()"><i class="bi bi-search"></i></button>
                </div>
                <div id="customerSearchResults" class="table-responsive"></div>
            </div>
        </div>
    </div>
</div>

<script>
function lookupCustomer() {
    var customerId = document.getElementById('customerIdInput').value;
    if (!customerId) {
        var modal = new bootstrap.Modal(document.getElementById('customerLookupModal'));
        modal.show();
        return;
    }
    fetch('${pageContext.request.contextPath}/customers/api/search?q=' + encodeURIComponent(customerId))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            if (data && data.length > 0) {
                var c = data[0];
                var fullName = c.firstName + ' ' + c.lastName;
                document.getElementById('customerNameDisplay').value = fullName;
                document.getElementById('customerNameHidden').value = fullName;
                document.getElementById('customerKycDisplay').value = c.kycStatus || 'N/A';
                if (c.kycStatus !== 'VERIFIED') {
                    var w = document.getElementById('customerWarning');
                    w.style.display = 'block';
                    w.textContent = 'Warning: Customer KYC is not VERIFIED. Account opening may be restricted.';
                }
            }
        })
        .catch(function(err) { console.error('Lookup failed:', err); });
}

function escapeHtml(str) {
    if (!str) return '';
    var div = document.createElement('div');
    div.appendChild(document.createTextNode(str));
    return div.innerHTML;
}

function searchCustomers() {
    var query = document.getElementById('customerSearchInput').value;
    if (!query || query.length < 2) return;
    fetch('${pageContext.request.contextPath}/customers/api/search?q=' + encodeURIComponent(query))
        .then(function(r) { return r.json(); })
        .then(function(data) {
            var html = '<table class="table table-hover table-sm"><thead><tr><th>ID</th><th>Name</th><th>KYC</th><th></th></tr></thead><tbody>';
            if (data && data.length > 0) {
                data.forEach(function(c) {
                    var safeName = escapeHtml(c.firstName + ' ' + c.lastName);
                    var safeKyc = escapeHtml(c.kycStatus || '');
                    var safeId = parseInt(c.customerId, 10);
                    html += '<tr><td>' + safeId + '</td><td>' + safeName + '</td><td>' + safeKyc + '</td>';
                    html += '<td><button class="btn btn-sm btn-primary" onclick="selectCustomer(' + safeId + ',\'' + safeName.replace(/'/g, "\\'") + '\',\'' + safeKyc.replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                });
            } else {
                html += '<tr><td colspan="4" class="text-center">No customers found</td></tr>';
            }
            html += '</tbody></table>';
            document.getElementById('customerSearchResults').innerHTML = html;
        });
}

function selectCustomer(id, name, kyc) {
    document.getElementById('customerIdInput').value = id;
    document.getElementById('customerNameDisplay').value = name;
    document.getElementById('customerNameHidden').value = name;
    document.getElementById('customerKycDisplay').value = kyc;
    bootstrap.Modal.getInstance(document.getElementById('customerLookupModal')).hide();
    if (kyc !== 'VERIFIED') {
        var w = document.getElementById('customerWarning');
        w.style.display = 'block';
        w.textContent = 'Warning: Customer KYC is not VERIFIED.';
    }
}
</script>

<%@ include file="../layout/footer.jsp" %>
