<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty sessionScope.username}">
    </div><!-- /.cbs-content -->
</main><!-- /.cbs-main -->

<%-- Sidebar overlay for mobile --%>
<div class="cbs-sidebar-overlay" id="sidebarOverlay"></div>

<footer class="cbs-footer" id="cbsFooter">
    <div class="cbs-footer-content">
        <span class="cbs-footer-disclaimer">All ledger entries are immutable. System of Record: LedgerEntries.</span>
        <span class="cbs-footer-separator">|</span>
        <span>Ledgora Core Banking Platform &copy; 2026</span>
        <span class="cbs-footer-separator">|</span>
        <span>Powered by Spring Boot</span>
    </div>
</footer>
</c:if>

<c:if test="${empty sessionScope.username}">
<footer class="mt-5 py-3 bg-light text-center text-muted border-top">
    <div class="container">
        <p class="mb-0">Ledgora Core Banking Platform &copy; 2026 | Powered by Spring Boot</p>
    </div>
</footer>
</c:if>

<script src="${pageContext.request.contextPath}/resources/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/resources/js/app.js"></script>

<%-- Security: Anti-tampering protection for financial forms --%>
<script>
(function() {
    'use strict';
    // Protect financial forms from hidden field manipulation and DevTools tampering
    var financialSelectors = 'form[action*="/transactions/"], form[action*="/vouchers/create"], form[action*="/settlements/process"]';
    var financialForms = document.querySelectorAll(financialSelectors);
    financialForms.forEach(function(form) {
        // Store original hidden field values on page load
        var hiddenFields = form.querySelectorAll('input[type="hidden"]');
        var originalValues = {};
        hiddenFields.forEach(function(field) {
            if (field.name && field.name !== '_csrf' && field.name.indexOf('csrf') === -1) {
                originalValues[field.name] = field.value;
            }
        });
        // On submit, verify hidden fields haven't been tampered with
        form.addEventListener('submit', function(e) {
            hiddenFields.forEach(function(field) {
                if (field.name && field.name !== '_csrf' && field.name.indexOf('csrf') === -1) {
                    if (originalValues.hasOwnProperty(field.name) && field.value !== originalValues[field.name]) {
                        e.preventDefault();
                        alert('Security violation: Form field tampering detected. This action has been blocked.');
                        return false;
                    }
                }
            });
            // Validate amount fields are numeric and positive
            var amountField = form.querySelector('input[name="amount"]');
            if (amountField && amountField.value) {
                var amount = parseFloat(amountField.value);
                if (isNaN(amount) || amount <= 0 || amount > 999999999999.99) {
                    e.preventDefault();
                    alert('Invalid amount value. Please enter a valid positive amount.');
                    return false;
                }
            }
        });
    });
    // Add CSRF token to all AJAX requests
    var csrfToken = document.querySelector('meta[name="_csrf"]');
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    if (csrfToken && csrfHeader) {
        var origFetch = window.fetch;
        window.fetch = function(url, options) {
            options = options || {};
            options.headers = options.headers || {};
            if (!(options.headers instanceof Headers)) {
                options.headers[csrfHeader.content] = csrfToken.content;
            }
            return origFetch.call(this, url, options);
        };
    }
})();
</script>

<%-- Phase 6: Day-closed lockout - disable financial operations when business day is not OPEN --%>
<c:if test="${not empty sessionScope.username}">
<script>
(function() {
    var dayStatus = '${sessionScope.businessDateStatus}';
    if (dayStatus && dayStatus !== 'OPEN') {
        // Disable all financial operation forms and buttons when day is not OPEN
        var financialForms = document.querySelectorAll(
            'form[action*="/transactions/"], form[action*="/vouchers/create"], ' +
            'form[action*="/settlements/process"], form[action*="/eod/run"]'
        );
        financialForms.forEach(function(form) {
            var buttons = form.querySelectorAll('button[type="submit"], input[type="submit"]');
            buttons.forEach(function(btn) {
                btn.disabled = true;
                btn.title = 'Business day is ' + dayStatus + '. Financial operations are locked.';
                btn.classList.add('btn-secondary');
                btn.classList.remove('btn-primary', 'btn-success', 'btn-danger');
            });
        });
        // Show a banner warning
        var banner = document.createElement('div');
        banner.className = 'alert alert-warning text-center mb-0';
        banner.setAttribute('role', 'alert');
        banner.innerHTML = '<i class="bi bi-lock-fill"></i> <strong>Business Day ' + dayStatus +
            '</strong> - Financial operations are locked until the business day is OPEN.';
        var main = document.querySelector('.cbs-content') || document.querySelector('.container-fluid');
        if (main) { main.insertBefore(banner, main.firstChild); }
    }
})();
</script>
</c:if>

</body>
</html>
