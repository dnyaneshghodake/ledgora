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

    if (!isMobile && localStorage.getItem('cbs-sidebar') === 'collapsed') {
        document.body.classList.add('cbs-sidebar-collapsed');
    }

    window.addEventListener('resize', function() {
        isMobile = window.innerWidth < 992;
        if (!isMobile) {
            document.body.classList.remove('cbs-sidebar-open');
        }
    });

    // ── CBS Sidebar Active Link ──
    var currentPath = window.location.pathname;
    var currentParams = new URLSearchParams(window.location.search);
    var sidebarLinks = document.querySelectorAll('.cbs-nav-link');
    var bestMatch = null;
    var bestMatchScore = -1;

    sidebarLinks.forEach(function(link) {
        var href = link.getAttribute('href');
        if (!href || href === '/') {
            return;
        }

        var linkUrl;
        try {
            linkUrl = new URL(href, window.location.origin);
        } catch (e) {
            return;
        }

        if (currentPath.indexOf(linkUrl.pathname) !== 0) {
            return;
        }

        var score = linkUrl.pathname.length * 10;
        var linkParams = new URLSearchParams(linkUrl.search);
        var matchedParams = 0;
        linkParams.forEach(function(value, key) {
            if (currentParams.get(key) === value) {
                matchedParams += 1;
            }
        });
        score += matchedParams;

        if (score > bestMatchScore) {
            bestMatch = link;
            bestMatchScore = score;
        }
    });

    if (bestMatch) {
        bestMatch.classList.add('active');
        // Auto-expand the parent nav group of the active link
        var parentGroup = bestMatch.closest('.cbs-nav-group');
        if (parentGroup) {
            parentGroup.classList.add('open');
            var groupToggle = parentGroup.querySelector('.cbs-nav-group-toggle');
            if (groupToggle) {
                var groupId = groupToggle.getAttribute('data-group');
                if (groupId) localStorage.setItem('cbs-nav-group-' + groupId, 'open');
            }
        }
    }

    // ── CBS Sidebar Collapsible Nav Groups ──
    var navGroupToggles = document.querySelectorAll('.cbs-nav-group-toggle');
    navGroupToggles.forEach(function(toggle) {
        var groupId = toggle.getAttribute('data-group');
        var parentGroup = toggle.closest('.cbs-nav-group');
        if (!parentGroup || !groupId) return;

        // Restore saved state from localStorage
        var savedState = localStorage.getItem('cbs-nav-group-' + groupId);
        if (savedState === 'open') {
            parentGroup.classList.add('open');
        }

        toggle.addEventListener('click', function(e) {
            e.preventDefault();
            parentGroup.classList.toggle('open');
            if (parentGroup.classList.contains('open')) {
                localStorage.setItem('cbs-nav-group-' + groupId, 'open');
            } else {
                localStorage.setItem('cbs-nav-group-' + groupId, 'closed');
            }
        });
    });

    // ── Header real-time clock (optional) ──
    var clockEl = document.getElementById('cbsClock');
    if (clockEl) {
        var pad2 = function(n) { return String(n).padStart(2, '0'); };
        var tick = function() {
            var now = new Date();
            var hh = pad2(now.getHours());
            var mm = pad2(now.getMinutes());
            var ss = pad2(now.getSeconds());
            clockEl.textContent = hh + ':' + mm + ':' + ss;
            clockEl.setAttribute('datetime', now.toISOString());
        };
        tick();
        setInterval(tick, 1000);
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
            var sourceEl = document.getElementById('fromAccount');
            var destEl = document.getElementById('toAccount');
            var source = sourceEl ? sourceEl.value : '';
            var dest = destEl ? destEl.value : '';
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

    // ── CBS Business Day Lock ──
    var eodBanner = document.getElementById('eodBanner');
    if (eodBanner) {
        document.body.classList.add('cbs-eod-active');
        var lockableElements = document.querySelectorAll('.cbs-lockable');
        lockableElements.forEach(function(el) {
            el.classList.add('cbs-locked');
            el.setAttribute('title', 'Business Day Closed - Transactions are locked');
            var inputs = el.querySelectorAll('input, select, textarea, button');
            inputs.forEach(function(input) {
                input.disabled = true;
            });
        });
    }

    // ── CBS Tab Navigation ──
    var tabLinks = document.querySelectorAll('.cbs-tabs .nav-link');
    tabLinks.forEach(function(tab) {
        tab.addEventListener('click', function(e) {
            e.preventDefault();
            tabLinks.forEach(function(t) { t.classList.remove('active'); });
            this.classList.add('active');

            var targetId = this.getAttribute('data-tab');
            var tabPanes = document.querySelectorAll('.cbs-tab-pane');
            tabPanes.forEach(function(pane) { pane.style.display = 'none'; });
            var targetPane = document.getElementById(targetId);
            if (targetPane) {
                targetPane.style.display = 'block';
            }
        });
    });

    // ── URL Tab Parameter Support ──
    var urlParams = new URLSearchParams(window.location.search);
    var activeTab = urlParams.get('tab');
    if (activeTab) {
        var tabTrigger = document.querySelector('.cbs-tabs .nav-link[data-tab="tab-' + activeTab + '"]');
        if (tabTrigger) {
            tabTrigger.click();
        }
    }
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
