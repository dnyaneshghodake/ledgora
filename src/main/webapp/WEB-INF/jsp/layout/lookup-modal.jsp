<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Reusable Lookup Modal Fragment
  Provides a standardized customer/account search popup.

  Usage: <%@ include file="../layout/lookup-modal.jsp" %>

  This fragment creates two modals:
    1. #customerLookupModal - Search and select a customer
    2. #accountLookupModal  - Search and select an account

  JavaScript API:
    openCustomerLookup(targetFieldId, nameFieldId, kycFieldId)
    openAccountLookup(targetFieldId, nameFieldId)
--%>

<%-- Customer Lookup Modal --%>
<div class="modal fade" id="customerLookupModal" tabindex="-1" aria-labelledby="customerLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="customerLookupLabel"><i class="bi bi-search"></i> Customer Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsCustomerSearchInput" class="form-control" placeholder="Search by name, ID, or national ID..."/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchCustomers()"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsCustomerSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
            </div>
        </div>
    </div>
</div>

<%-- Account Lookup Modal --%>
<div class="modal fade" id="accountLookupModal" tabindex="-1" aria-labelledby="accountLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="accountLookupLabel"><i class="bi bi-search"></i> Account Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsAccountSearchInput" class="form-control" placeholder="Search by account number or name..."/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchAccounts()"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsAccountSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
            </div>
        </div>
    </div>
</div>

<script>
(function() {
    'use strict';

    var _custTargetField, _custNameField, _custKycField;
    var _acctTargetField, _acctNameField;

    window.openCustomerLookup = function(targetFieldId, nameFieldId, kycFieldId) {
        _custTargetField = targetFieldId;
        _custNameField = nameFieldId;
        _custKycField = kycFieldId;
        var modal = new bootstrap.Modal(document.getElementById('customerLookupModal'));
        modal.show();
    };

    window.openAccountLookup = function(targetFieldId, nameFieldId) {
        _acctTargetField = targetFieldId;
        _acctNameField = nameFieldId;
        var modal = new bootstrap.Modal(document.getElementById('accountLookupModal'));
        modal.show();
    };

    window.cbsSearchCustomers = function() {
        var query = document.getElementById('cbsCustomerSearchInput').value;
        if (!query || query.length < 2) return;
        fetch('${pageContext.request.contextPath}/customers/api/search?q=' + encodeURIComponent(query))
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>ID</th><th>Name</th><th>National ID</th><th>KYC</th><th></th></tr></thead><tbody>';
                if (data && data.length > 0) {
                    data.forEach(function(c) {
                        html += '<tr><td>' + c.customerId + '</td>';
                        html += '<td>' + c.firstName + ' ' + c.lastName + '</td>';
                        html += '<td>' + (c.nationalId || '') + '</td>';
                        html += '<td>' + (c.kycStatus || '') + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectCustomer(' + c.customerId + ',\'' + (c.firstName + ' ' + c.lastName).replace(/'/g, "\\'") + '\',\'' + (c.kycStatus || '') + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No customers found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsCustomerSearchResults').innerHTML = html;
            })
            .catch(function(err) { console.error('Customer lookup failed:', err); });
    };

    window.cbsSelectCustomer = function(id, name, kyc) {
        if (_custTargetField) document.getElementById(_custTargetField).value = id;
        if (_custNameField) document.getElementById(_custNameField).value = name;
        if (_custKycField) document.getElementById(_custKycField).value = kyc;
        bootstrap.Modal.getInstance(document.getElementById('customerLookupModal')).hide();
    };

    window.cbsSearchAccounts = function() {
        var query = document.getElementById('cbsAccountSearchInput').value;
        if (!query || query.length < 2) return;
        fetch('${pageContext.request.contextPath}/accounts/api/lookup?accountNumber=' + encodeURIComponent(query))
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>Account No</th><th>Name</th><th>Type</th><th>Status</th><th></th></tr></thead><tbody>';
                if (data) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(a) {
                        html += '<tr><td><code>' + (a.accountNumber || '') + '</code></td>';
                        html += '<td>' + (a.accountName || '') + '</td>';
                        html += '<td>' + (a.accountType || '') + '</td>';
                        html += '<td>' + (a.status || '') + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectAccount(\'' + (a.accountNumber || '') + '\',\'' + (a.accountName || '').replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No accounts found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsAccountSearchResults').innerHTML = html;
            })
            .catch(function(err) { console.error('Account lookup failed:', err); });
    };

    window.cbsSelectAccount = function(number, name) {
        if (_acctTargetField) document.getElementById(_acctTargetField).value = number;
        if (_acctNameField) document.getElementById(_acctNameField).value = name;
        bootstrap.Modal.getInstance(document.getElementById('accountLookupModal')).hide();
    };
})();
</script>
