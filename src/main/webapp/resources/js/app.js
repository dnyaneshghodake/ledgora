// Ledgora Core Banking Platform - Client-side JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Form validation for amount fields
    const amountInputs = document.querySelectorAll('input[type="number"][step="0.01"]');
    amountInputs.forEach(function(input) {
        input.addEventListener('input', function() {
            if (parseFloat(this.value) <= 0) {
                this.setCustomValidity('Amount must be greater than zero');
            } else {
                this.setCustomValidity('');
            }
        });
    });

    // Transfer form - prevent same account selection
    const transferForm = document.getElementById('transferForm');
    if (transferForm) {
        transferForm.addEventListener('submit', function(e) {
            const source = document.getElementById('sourceAccountNumber').value;
            const dest = document.getElementById('destinationAccountNumber').value;
            if (source && dest && source === dest) {
                e.preventDefault();
                showAlert('Source and destination accounts cannot be the same', 'danger');
            }
        });
    }

    // Highlight active nav item
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-nav .nav-link');
    navLinks.forEach(function(link) {
        const href = link.getAttribute('href');
        if (href && currentPath.startsWith(href) && href !== '/') {
            link.classList.add('active');
            const dropdown = link.closest('.dropdown');
            if (dropdown) {
                dropdown.querySelector('.nav-link.dropdown-toggle').classList.add('active');
            }
        }
    });

    // Confirm dialogs for destructive actions
    const confirmButtons = document.querySelectorAll('[data-confirm]');
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
    const container = document.querySelector('.container-fluid');
    const alertDiv = document.createElement('div');
    alertDiv.className = 'alert alert-' + type + ' alert-dismissible fade show';
    alertDiv.setAttribute('role', 'alert');
    alertDiv.innerHTML = message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
    container.insertBefore(alertDiv, container.firstChild);
    setTimeout(function() {
        const bsAlert = new bootstrap.Alert(alertDiv);
        bsAlert.close();
    }, 5000);
}
