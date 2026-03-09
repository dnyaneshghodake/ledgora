<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Reusable Lookup Modal Fragment
  Provides standardized search popups for Customer, Account, Branch, GL Parent, and Pincode.

  Usage: <%@ include file="../layout/lookup-modal.jsp" %>

  This fragment creates modals:
    1. #customerLookupModal - Search and select a customer
    2. #accountLookupModal  - Search and select an account
    3. #branchLookupModal   - Search and select a branch
    4. #glParentLookupModal - Search and select a GL parent account
    5. #pincodeLookupModal  - Search and select a pincode

  JavaScript API:
    openCustomerLookup(targetFieldId, nameFieldId, kycFieldId)
    openAccountLookup(targetFieldId, nameFieldId)
    openBranchLookup(targetFieldId, nameFieldId)
    openGlParentLookup(targetFieldId, nameFieldId)
    openPincodeLookup(targetFieldId, cityFieldId, stateFieldId)
--%>

<%-- Customer Lookup Modal --%>
<div class="modal fade" id="customerLookupModal" tabindex="-1" aria-labelledby="customerLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="customerLookupLabel"><i class="bi bi-people"></i> Customer Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsCustomerSearchInput" class="form-control" placeholder="Search by name, ID, or national ID..."
                           onkeydown="if(event.key==='Enter'){event.preventDefault();cbsSearchCustomers(0);}"/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchCustomers(0)"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsCustomerSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
                <div id="cbsCustomerPagination" class="cbs-lookup-pagination"></div>
            </div>
        </div>
    </div>
</div>

<%-- Account Lookup Modal --%>
<div class="modal fade" id="accountLookupModal" tabindex="-1" aria-labelledby="accountLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="accountLookupLabel"><i class="bi bi-wallet2"></i> Account Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsAccountSearchInput" class="form-control" placeholder="Search by account number or name..."
                           onkeydown="if(event.key==='Enter'){event.preventDefault();cbsSearchAccounts(0);}"/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchAccounts(0)"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsAccountSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
                <div id="cbsAccountPagination" class="cbs-lookup-pagination"></div>
            </div>
        </div>
    </div>
</div>

<%-- Branch Lookup Modal --%>
<div class="modal fade" id="branchLookupModal" tabindex="-1" aria-labelledby="branchLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="branchLookupLabel"><i class="bi bi-geo-alt"></i> Branch Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsBranchSearchInput" class="form-control" placeholder="Search by branch code or name..."
                           onkeydown="if(event.key==='Enter'){event.preventDefault();cbsSearchBranches(0);}"/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchBranches(0)"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsBranchSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
                <div id="cbsBranchPagination" class="cbs-lookup-pagination"></div>
            </div>
        </div>
    </div>
</div>

<%-- GL Parent Lookup Modal --%>
<div class="modal fade" id="glParentLookupModal" tabindex="-1" aria-labelledby="glParentLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="glParentLookupLabel"><i class="bi bi-diagram-3"></i> GL Parent Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsGlParentSearchInput" class="form-control" placeholder="Search by GL code or name..."
                           onkeydown="if(event.key==='Enter'){event.preventDefault();cbsSearchGlParents(0);}"/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchGlParents(0)"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsGlParentSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
                <div id="cbsGlParentPagination" class="cbs-lookup-pagination"></div>
            </div>
        </div>
    </div>
</div>

<%-- Pincode Lookup Modal --%>
<div class="modal fade" id="pincodeLookupModal" tabindex="-1" aria-labelledby="pincodeLookupLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <div class="modal-header bg-light">
                <h5 class="modal-title" id="pincodeLookupLabel"><i class="bi bi-mailbox"></i> Pincode Lookup</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="input-group mb-3">
                    <input type="text" id="cbsPincodeSearchInput" class="form-control" placeholder="Search by pincode, city, or area..."
                           onkeydown="if(event.key==='Enter'){event.preventDefault();cbsSearchPincodes(0);}"/>
                    <button class="btn btn-primary" type="button" onclick="cbsSearchPincodes(0)"><i class="bi bi-search"></i> Search</button>
                </div>
                <div id="cbsPincodeSearchResults" class="table-responsive" style="max-height: 400px; overflow-y: auto;"></div>
                <div id="cbsPincodePagination" class="cbs-lookup-pagination"></div>
            </div>
        </div>
    </div>
</div>

<script>
(function() {
    'use strict';

    var CBS_PAGE_SIZE = 10;
    var _custTargetField, _custNameField, _custKycField;
    var _acctTargetField, _acctNameField;
    var _branchTargetField, _branchNameField;
    var _glParentTargetField, _glParentNameField;
    var _pincodeTargetField, _pincodeCityField, _pincodeStateField;

    // ── Pagination helper ──
    function renderPagination(containerId, currentPage, totalItems, searchFn) {
        var totalPages = Math.ceil(totalItems / CBS_PAGE_SIZE);
        var container = document.getElementById(containerId);
        if (!container || totalPages <= 1) {
            if (container) container.innerHTML = '';
            return;
        }
        var html = '<button ' + (currentPage <= 0 ? 'disabled' : '') + ' onclick="' + searchFn + '(' + (currentPage - 1) + ')">&laquo; Prev</button>';
        html += '<span class="cbs-page-info">Page ' + (currentPage + 1) + ' of ' + totalPages + '</span>';
        html += '<button ' + (currentPage >= totalPages - 1 ? 'disabled' : '') + ' onclick="' + searchFn + '(' + (currentPage + 1) + ')">Next &raquo;</button>';
        container.innerHTML = html;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }

    // ── Customer Lookup ──
    window.openCustomerLookup = function(targetFieldId, nameFieldId, kycFieldId) {
        _custTargetField = targetFieldId;
        _custNameField = nameFieldId;
        _custKycField = kycFieldId;
        document.getElementById('cbsCustomerSearchInput').value = '';
        document.getElementById('cbsCustomerSearchResults').innerHTML = '';
        document.getElementById('cbsCustomerPagination').innerHTML = '';
        var modal = new bootstrap.Modal(document.getElementById('customerLookupModal'));
        modal.show();
        setTimeout(function() { document.getElementById('cbsCustomerSearchInput').focus(); }, 300);
    };

    window.cbsSearchCustomers = function(page) {
        var query = document.getElementById('cbsCustomerSearchInput').value;
        if (!query || query.length < 2) return;
        fetch('${pageContext.request.contextPath}/customers/api/search?q=' + encodeURIComponent(query) + '&page=' + page + '&size=' + CBS_PAGE_SIZE)
            .then(function(r) { return r.json(); })
            .then(function(response) {
                var data = response.content || response;
                var total = response.totalElements || (Array.isArray(data) ? data.length : 0);
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>ID</th><th>Name</th><th>National ID</th><th>KYC</th><th></th></tr></thead><tbody>';
                if (data && (Array.isArray(data) ? data.length > 0 : true)) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(c) {
                        var fullName = escapeHtml((c.firstName || '') + ' ' + (c.lastName || ''));
                        html += '<tr><td>' + escapeHtml(c.customerId) + '</td>';
                        html += '<td>' + fullName + '</td>';
                        html += '<td>' + escapeHtml(c.nationalId) + '</td>';
                        html += '<td>' + escapeHtml(c.kycStatus) + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectCustomer(' + c.customerId + ',\'' + fullName.replace(/'/g, "\\'") + '\',\'' + escapeHtml(c.kycStatus) + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No customers found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsCustomerSearchResults').innerHTML = html;
                renderPagination('cbsCustomerPagination', page, total, 'cbsSearchCustomers');
            })
            .catch(function(err) { console.error('Customer lookup failed:', err); });
    };

    window.cbsSelectCustomer = function(id, name, kyc) {
        if (_custTargetField) document.getElementById(_custTargetField).value = id;
        if (_custNameField) document.getElementById(_custNameField).value = name;
        if (_custKycField) document.getElementById(_custKycField).value = kyc;
        bootstrap.Modal.getInstance(document.getElementById('customerLookupModal')).hide();
    };

    // ── Account Lookup ──
    window.openAccountLookup = function(targetFieldId, nameFieldId) {
        _acctTargetField = targetFieldId;
        _acctNameField = nameFieldId;
        document.getElementById('cbsAccountSearchInput').value = '';
        document.getElementById('cbsAccountSearchResults').innerHTML = '';
        document.getElementById('cbsAccountPagination').innerHTML = '';
        var modal = new bootstrap.Modal(document.getElementById('accountLookupModal'));
        modal.show();
        setTimeout(function() { document.getElementById('cbsAccountSearchInput').focus(); }, 300);
    };

    window.cbsSearchAccounts = function(page) {
        var query = document.getElementById('cbsAccountSearchInput').value;
        if (!query || query.length < 2) return;
        fetch('${pageContext.request.contextPath}/accounts/api/search?q=' + encodeURIComponent(query) + '&page=' + page + '&size=' + CBS_PAGE_SIZE)
            .then(function(r) { return r.json(); })
            .then(function(response) {
                var data = response.content || response;
                var total = response.totalElements || (Array.isArray(data) ? data.length : 0);
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>Account No</th><th>Name</th><th>Type</th><th>Status</th><th></th></tr></thead><tbody>';
                if (data) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(a) {
                        html += '<tr><td><code>' + escapeHtml(a.accountNumber) + '</code></td>';
                        html += '<td>' + escapeHtml(a.accountName) + '</td>';
                        html += '<td>' + escapeHtml(a.accountType) + '</td>';
                        html += '<td>' + escapeHtml(a.status) + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectAccount(\'' + escapeHtml(a.accountNumber) + '\',\'' + escapeHtml(a.accountName).replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No accounts found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsAccountSearchResults').innerHTML = html;
                renderPagination('cbsAccountPagination', page, total, 'cbsSearchAccounts');
            })
            .catch(function(err) { console.error('Account lookup failed:', err); });
    };

    window.cbsSelectAccount = function(number, name) {
        if (_acctTargetField) document.getElementById(_acctTargetField).value = number;
        if (_acctNameField) document.getElementById(_acctNameField).value = name;
        bootstrap.Modal.getInstance(document.getElementById('accountLookupModal')).hide();
        // Trigger change event so balance lookups can fire
        var el = _acctTargetField ? document.getElementById(_acctTargetField) : null;
        if (el) el.dispatchEvent(new Event('change', { bubbles: true }));
    };

    // ── Branch Lookup ──
    window.openBranchLookup = function(targetFieldId, nameFieldId) {
        _branchTargetField = targetFieldId;
        _branchNameField = nameFieldId;
        document.getElementById('cbsBranchSearchInput').value = '';
        document.getElementById('cbsBranchSearchResults').innerHTML = '';
        document.getElementById('cbsBranchPagination').innerHTML = '';
        var modal = new bootstrap.Modal(document.getElementById('branchLookupModal'));
        modal.show();
        setTimeout(function() { document.getElementById('cbsBranchSearchInput').focus(); }, 300);
    };

    window.cbsSearchBranches = function(page) {
        var query = document.getElementById('cbsBranchSearchInput').value;
        if (!query || query.length < 1) return;
        fetch('${pageContext.request.contextPath}/admin/branches/api/search?q=' + encodeURIComponent(query) + '&page=' + page + '&size=' + CBS_PAGE_SIZE)
            .then(function(r) { return r.json(); })
            .then(function(response) {
                var data = response.content || response;
                var total = response.totalElements || (Array.isArray(data) ? data.length : 0);
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>Code</th><th>Name</th><th>City</th><th>Status</th><th></th></tr></thead><tbody>';
                if (data && (Array.isArray(data) ? data.length > 0 : true)) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(b) {
                        html += '<tr><td><code>' + escapeHtml(b.branchCode || b.code) + '</code></td>';
                        html += '<td>' + escapeHtml(b.branchName || b.name) + '</td>';
                        html += '<td>' + escapeHtml(b.city) + '</td>';
                        html += '<td>' + escapeHtml(b.status || 'ACTIVE') + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectBranch(\'' + escapeHtml(b.branchCode || b.code) + '\',\'' + escapeHtml(b.branchName || b.name).replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No branches found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsBranchSearchResults').innerHTML = html;
                renderPagination('cbsBranchPagination', page, total, 'cbsSearchBranches');
            })
            .catch(function(err) { console.error('Branch lookup failed:', err); });
    };

    window.cbsSelectBranch = function(code, name) {
        if (_branchTargetField) document.getElementById(_branchTargetField).value = code;
        if (_branchNameField) document.getElementById(_branchNameField).value = name;
        bootstrap.Modal.getInstance(document.getElementById('branchLookupModal')).hide();
    };

    // ── GL Parent Lookup ──
    window.openGlParentLookup = function(targetFieldId, nameFieldId) {
        _glParentTargetField = targetFieldId;
        _glParentNameField = nameFieldId;
        document.getElementById('cbsGlParentSearchInput').value = '';
        document.getElementById('cbsGlParentSearchResults').innerHTML = '';
        document.getElementById('cbsGlParentPagination').innerHTML = '';
        var modal = new bootstrap.Modal(document.getElementById('glParentLookupModal'));
        modal.show();
        setTimeout(function() { document.getElementById('cbsGlParentSearchInput').focus(); }, 300);
    };

    window.cbsSearchGlParents = function(page) {
        var query = document.getElementById('cbsGlParentSearchInput').value;
        if (!query || query.length < 1) return;
        fetch('${pageContext.request.contextPath}/gl/api/search?q=' + encodeURIComponent(query) + '&parentOnly=true&page=' + page + '&size=' + CBS_PAGE_SIZE)
            .then(function(r) { return r.json(); })
            .then(function(response) {
                var data = response.content || response;
                var total = response.totalElements || (Array.isArray(data) ? data.length : 0);
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>GL Code</th><th>Name</th><th>Type</th><th>Category</th><th></th></tr></thead><tbody>';
                if (data && (Array.isArray(data) ? data.length > 0 : true)) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(g) {
                        html += '<tr><td><code>' + escapeHtml(g.accountCode || g.glCode) + '</code></td>';
                        html += '<td>' + escapeHtml(g.accountName || g.name) + '</td>';
                        html += '<td>' + escapeHtml(g.accountType || g.type) + '</td>';
                        html += '<td>' + escapeHtml(g.category) + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectGlParent(\'' + escapeHtml(g.accountCode || g.glCode) + '\',\'' + escapeHtml(g.accountName || g.name).replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No GL accounts found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsGlParentSearchResults').innerHTML = html;
                renderPagination('cbsGlParentPagination', page, total, 'cbsSearchGlParents');
            })
            .catch(function(err) { console.error('GL Parent lookup failed:', err); });
    };

    window.cbsSelectGlParent = function(code, name) {
        if (_glParentTargetField) document.getElementById(_glParentTargetField).value = code;
        if (_glParentNameField) document.getElementById(_glParentNameField).value = name;
        bootstrap.Modal.getInstance(document.getElementById('glParentLookupModal')).hide();
    };

    // ── Pincode Lookup ──
    window.openPincodeLookup = function(targetFieldId, cityFieldId, stateFieldId) {
        _pincodeTargetField = targetFieldId;
        _pincodeCityField = cityFieldId;
        _pincodeStateField = stateFieldId;
        document.getElementById('cbsPincodeSearchInput').value = '';
        document.getElementById('cbsPincodeSearchResults').innerHTML = '';
        document.getElementById('cbsPincodePagination').innerHTML = '';
        var modal = new bootstrap.Modal(document.getElementById('pincodeLookupModal'));
        modal.show();
        setTimeout(function() { document.getElementById('cbsPincodeSearchInput').focus(); }, 300);
    };

    window.cbsSearchPincodes = function(page) {
        var query = document.getElementById('cbsPincodeSearchInput').value;
        if (!query || query.length < 2) return;
        fetch('${pageContext.request.contextPath}/api/pincodes/search?q=' + encodeURIComponent(query) + '&page=' + page + '&size=' + CBS_PAGE_SIZE)
            .then(function(r) { return r.json(); })
            .then(function(response) {
                var data = response.content || response;
                var total = response.totalElements || (Array.isArray(data) ? data.length : 0);
                var html = '<table class="table table-hover table-sm"><thead class="table-light"><tr><th>Pincode</th><th>Area</th><th>City</th><th>State</th><th></th></tr></thead><tbody>';
                if (data && (Array.isArray(data) ? data.length > 0 : true)) {
                    var items = Array.isArray(data) ? data : [data];
                    items.forEach(function(p) {
                        html += '<tr><td><code>' + escapeHtml(p.pincode || p.code) + '</code></td>';
                        html += '<td>' + escapeHtml(p.area || p.locality) + '</td>';
                        html += '<td>' + escapeHtml(p.city || p.district) + '</td>';
                        html += '<td>' + escapeHtml(p.state) + '</td>';
                        html += '<td><button class="btn btn-sm btn-primary" onclick="cbsSelectPincode(\'' + escapeHtml(p.pincode || p.code) + '\',\'' + escapeHtml(p.city || p.district).replace(/'/g, "\\'") + '\',\'' + escapeHtml(p.state).replace(/'/g, "\\'") + '\')">Select</button></td></tr>';
                    });
                } else {
                    html += '<tr><td colspan="5" class="text-center text-muted">No pincodes found</td></tr>';
                }
                html += '</tbody></table>';
                document.getElementById('cbsPincodeSearchResults').innerHTML = html;
                renderPagination('cbsPincodePagination', page, total, 'cbsSearchPincodes');
            })
            .catch(function(err) { console.error('Pincode lookup failed:', err); });
    };

    window.cbsSelectPincode = function(pincode, city, state) {
        if (_pincodeTargetField) document.getElementById(_pincodeTargetField).value = pincode;
        if (_pincodeCityField) document.getElementById(_pincodeCityField).value = city;
        if (_pincodeStateField) document.getElementById(_pincodeStateField).value = state;
        bootstrap.Modal.getInstance(document.getElementById('pincodeLookupModal')).hide();
    };
})();
</script>
