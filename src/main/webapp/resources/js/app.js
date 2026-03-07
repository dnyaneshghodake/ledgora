// Ledgora Core Banking Platform - Client-side JavaScript

document.addEventListener('DOMContentLoaded', function() {

    // ── CBS Sidebar Toggle ──
    var sidebarToggle = document.getElementById('sidebarToggle');
    var sidebarOverlay = document.getElementById('sidebarOverlay');
    var isMobile = window.innerWidth < 992;

    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', function() {
            if (isMobile) {
                document.body.classList.toggle('cbs-sidebar-open');
            } else {
                document.body.classList.toggle('cbs-sidebar-collapsed');
                // Persist sidebar state
                if (document.body.classList.contains('cbs-sidebar-collapsed')) {
                    localStorage.setItem('cbs-sidebar', 'collapsed');
                } else {
                    localStorage.setItem('cbs-sidebar', 'expanded');
                }
            }
        });
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', function() {
            document.body.classList.remove('cbs-sidebar-open');
        });
    }

    // Restore sidebar state from localStorage
    if (!isMobile && localStorage.getItem('cbs-sidebar') === 'collapsed') {
        document.body.classList.add('cbs-sidebar-collapsed');
    }

    // Update mobile flag on resize
    window.addEventListener('resize', function() {
        isMobile = window.innerWidth < 992;
        if (!isMobile) {
            document.body.classList.remove('cbs-sidebar-open');
        }
    });

    // ── CBS Sidebar Active Link ──
    var currentPath = window.location.pathname;
    var sidebarLinks = document.querySelectorAll('.cbs-nav-link');
    var bestMatch = null;
    var bestMatchLen = 0;

    sidebarLinks.forEach(function(link) {
        var href = link.getAttribute('href');
        if (href && currentPath.indexOf(href) === 0 && href.length > bestMatchLen && href !== '/') {
            bestMatch = link;
            bestMatchLen = href.length;
        }
    });

    if (bestMatch) {
        bestMatch.classList.add('active');
    }

    // ── Auto-dismiss alerts after 5 seconds ──
    var alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            var bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // ── Form validation for amount fields ──
    var amountInputs = document.querySelectorAll('input[type="number"][step="0.01"]');
    amountInputs.forEach(function(input) {
        input.addEventListener('input', function() {
            if (parseFloat(this.value) <= 0) {
                this.setCustomValidity('Amount must be greater than zero');
            } else {
                this.setCustomValidity('');
            }
        });
    });

    // ── Transfer form - prevent same account selection ──
    var transferForm = document.getElementById('transferForm');
    if (transferForm) {
        transferForm.addEventListener('submit', function(e) {
            var source = document.getElementById('sourceAccountNumber').value;
            var dest = document.getElementById('destinationAccountNumber').value;
            if (source && dest && source === dest) {
                e.preventDefault();
                showAlert('Source and destination accounts cannot be the same', 'danger');
            }
        });
    }

    // ── Confirm dialogs for destructive actions ──
    var confirmButtons = document.querySelectorAll('[data-confirm]');
    confirmButtons.forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            if (!confirm(this.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });
});

// Helper function to show alerts
function showAlert(message, type) {
    var container = document.querySelector('.cbs-content') || document.querySelector('.container-fluid') || document.querySelector('.container');
    if (!container) return;
    var alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-' + type + ' alert-dismissible fade show';
    alertDiv.setAttribute('role', 'alert');
    alertDiv.innerHTML = message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
    container.insertBefore(alertDiv, container.firstChild);
    setTimeout(function() {
        var bsAlert = new bootstrap.Alert(alertDiv);
        bsAlert.close();
    }, 5000);
}
