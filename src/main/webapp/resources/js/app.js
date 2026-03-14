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

    // ── CBS Session Timeout Countdown ──
    // Must match server.servlet.session.timeout=15m in application.properties
    var sessionTimerEl = document.getElementById('cbsSessionTimer');
    var sessionCountdownEl = document.getElementById('cbsSessionCountdown');
    if (sessionTimerEl && sessionCountdownEl) {
        var SESSION_TIMEOUT = 15 * 60; // 15 minutes — synced with server
        var SESSION_WARN = 3 * 60;     // warn at 3 minutes
        var SESSION_CRITICAL = 1 * 60; // critical at 1 minute
        var sessionSecondsLeft = SESSION_TIMEOUT;
        var pad2 = function(n) { return String(n).padStart(2, '0'); };

        var updateSessionDisplay = function() {
            var mins = Math.floor(sessionSecondsLeft / 60);
            var secs = sessionSecondsLeft % 60;
            sessionCountdownEl.textContent = pad2(mins) + ':' + pad2(secs);
            sessionTimerEl.classList.remove('cbs-session-warning', 'cbs-session-critical');
            if (sessionSecondsLeft <= SESSION_CRITICAL) {
                sessionTimerEl.classList.add('cbs-session-critical');
            } else if (sessionSecondsLeft <= SESSION_WARN) {
                sessionTimerEl.classList.add('cbs-session-warning');
            }
        };

        var sessionTick = function() {
            if (sessionSecondsLeft > 0) {
                sessionSecondsLeft--;
                updateSessionDisplay();
            } else {
                var ctxPath = document.body.getAttribute('data-context-path') || '';
                window.location.href = ctxPath + '/login?expired=true';
            }
        };

        // Ping server to keep server-side session alive on user activity
        var pingServer = function() {
            var ctxPath = document.body.getAttribute('data-context-path') || '';
            fetch(ctxPath + '/actuator/health', { method: 'HEAD', credentials: 'same-origin' }).catch(function() {});
        };

        var resetSessionTimer = function() {
            sessionSecondsLeft = SESSION_TIMEOUT;
            updateSessionDisplay();
            pingServer();
        };

        updateSessionDisplay();
        setInterval(sessionTick, 1000);

        var lastResetTime = Date.now();
        ['click', 'keypress', 'scroll', 'mousemove'].forEach(function(evt) {
            document.addEventListener(evt, function() {
                var now = Date.now();
                if (now - lastResetTime > 5000) {
                    lastResetTime = now;
                    resetSessionTimer();
                }
            }, { passive: true });
        });
    }

    // ── CBS Keyboard Shortcuts (Finacle-style Alt+Key navigation) ──
    document.addEventListener('keydown', function(e) {
        // Only handle Alt+Key combinations
        if (!e.altKey || e.ctrlKey || e.metaKey) return;

        var ctx = (document.body.getAttribute('data-context-path') || '') + '/';
        var handled = false;

        switch (e.key.toLowerCase()) {
            case 'd': // Dashboard
                window.location.href = ctx + 'dashboard';
                handled = true;
                break;
            case 't': // New Transaction (deposit as default)
                window.location.href = ctx + 'transactions/deposit';
                handled = true;
                break;
            case 'c': // Customer list
                window.location.href = ctx + 'customers';
                handled = true;
                break;
            case 'a': // Account list
                window.location.href = ctx + 'accounts';
                handled = true;
                break;
            case 's': // Toggle sidebar
                var toggleBtn = document.getElementById('sidebarToggle');
                if (toggleBtn) toggleBtn.click();
                handled = true;
                break;
            case 'p': // Approvals
                window.location.href = ctx + 'approvals';
                handled = true;
                break;
        }

        if (handled) {
            e.preventDefault();
            e.stopPropagation();
        }
    });

    // ── Keyboard accessibility: Escape closes open dropdowns ──
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            var openDropdowns = document.querySelectorAll('.dropdown-menu.show');
            openDropdowns.forEach(function(menu) {
                var toggle = menu.previousElementSibling;
                if (toggle && typeof bootstrap !== 'undefined') {
                    var dd = bootstrap.Dropdown.getInstance(toggle);
                    if (dd) dd.hide();
                }
            });
        }
    });

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
    // XSS-safe: use textContent for message, createElement for close button (CWE-79)
    alertDiv.textContent = message;
    var closeBtn = document.createElement('button');
    closeBtn.type = 'button';
    closeBtn.className = 'btn-close';
    closeBtn.setAttribute('data-bs-dismiss', 'alert');
    alertDiv.appendChild(closeBtn);
    container.insertBefore(alertDiv, container.firstChild);
    setTimeout(function() {
        var bsAlert = new bootstrap.Alert(alertDiv);
        bsAlert.close();
    }, 5000);
}
