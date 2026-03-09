/**
 * Ledgora CBS - Transaction Module
 * Handles balance refresh, freeze validation, amount validation, and form submission
 * for deposit, withdrawal, and transfer screens.
 *
 * Architecture:
 *   - No inline EL expressions. Server data passed via data-* attributes on the form.
 *   - CSP-compatible: no eval(), no inline event handlers in this file.
 *   - All DOM queries cached where possible.
 *
 * Required data attributes on the <form> element:
 *   data-context-path   = server context path (e.g., "" or "/ledgora")
 *   data-is-holiday     = "true" or "false"
 *   data-txn-type       = "DEPOSIT", "WITHDRAWAL", or "TRANSFER"
 *
 * Required element IDs (see individual page JSPs for which are present):
 *   accountNumber / fromAccount / toAccount — account input(s)
 *   accountNameDisplay — auto-filled name (deposit/withdraw)
 *   ledgerBalance, availableBalance, lienAmount — balance display
 *   freezeWarning, freezeMsg — freeze alert
 *   amountInput, amtInlineError, balanceExceedError — amount validation
 *   submitBtn — submit button
 */
(function () {
    'use strict';

    // ── Utility ──
    function formatCurrency(val) {
        var num = parseFloat(val);
        return isNaN(num) ? '0.00' : num.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
    }

    function show(el) { if (el) el.classList.remove('d-none'); }
    function hide(el) { if (el) el.classList.add('d-none'); }

    // ── Init on DOMContentLoaded ──
    document.addEventListener('DOMContentLoaded', function () {

        // Find the transaction form — exit if not on a transaction page
        var form = document.getElementById('depositForm')
            || document.getElementById('withdrawForm')
            || document.getElementById('transferForm');
        if (!form) return;

        var contextPath = form.getAttribute('data-context-path') || '';
        var isHoliday = form.getAttribute('data-is-holiday') === 'true';
        var txnType = form.getAttribute('data-txn-type') || '';

        var submitBtn = document.getElementById('submitBtn');
        var amountInput = document.getElementById('amountInput');
        var amtErr = document.getElementById('amtInlineError');
        var balErr = document.getElementById('balanceExceedError');

        // ── Single-account pages (deposit / withdrawal) ──
        if (txnType === 'DEPOSIT' || txnType === 'WITHDRAWAL') {
            var acctInput = document.getElementById('accountNumber');
            var acctName = document.getElementById('accountNameDisplay');
            var ledgerBal = document.getElementById('ledgerBalance');
            var availBal = document.getElementById('availableBalance');
            var lienAmt = document.getElementById('lienAmount');
            var freezeWarn = document.getElementById('freezeWarning');
            var freezeMsg = document.getElementById('freezeMsg');
            var acctErr = document.getElementById('acctInlineError');
            var _availBal = null;

            var freezeBlockDirection = txnType === 'DEPOSIT' ? 'CREDIT' : 'DEBIT';

            function refreshSingleAccount() {
                var num = acctInput ? acctInput.value : '';
                if (!num) return;

                hide(freezeWarn);
                if (submitBtn && !isHoliday) submitBtn.disabled = false;

                fetch(contextPath + '/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
                    .then(function (r) { return r.json(); })
                    .then(function (data) {
                        if (!data) return;
                        if (acctName) acctName.value = data.accountName || '';
                        if (ledgerBal) ledgerBal.textContent = formatCurrency(data.balance);
                        _availBal = parseFloat(data.availableBalance || data.balance) || 0;
                        if (availBal) availBal.textContent = formatCurrency(_availBal);
                        if (lienAmt) lienAmt.textContent = formatCurrency(data.totalLien);

                        // Freeze check
                        if (data.freezeLevel && data.freezeLevel !== 'NONE') {
                            show(freezeWarn);
                            if (freezeMsg) freezeMsg.textContent = 'Account freeze level: ' + data.freezeLevel;
                            var blocked = data.freezeLevel === 'FULL'
                                || (freezeBlockDirection === 'CREDIT' && data.freezeLevel === 'CREDIT_ONLY')
                                || (freezeBlockDirection === 'DEBIT' && data.freezeLevel === 'DEBIT_ONLY');
                            if (blocked && submitBtn) submitBtn.disabled = true;
                        } else {
                            hide(freezeWarn);
                        }

                        if (isHoliday && submitBtn) submitBtn.disabled = true;
                    })
                    .catch(function (e) { console.error('Balance lookup failed:', e); });
            }

            if (acctInput) {
                acctInput.addEventListener('change', refreshSingleAccount);
                if (acctInput.value) refreshSingleAccount();
            }

            // Amount validation (withdrawal: check against available balance)
            if (amountInput) {
                amountInput.addEventListener('input', function () {
                    var amt = parseFloat(this.value);
                    if (this.value && (isNaN(amt) || amt <= 0)) {
                        if (amtErr) amtErr.classList.add('visible');
                        if (balErr) balErr.classList.remove('visible');
                    } else if (txnType === 'WITHDRAWAL' && _availBal !== null && amt > _availBal) {
                        if (amtErr) amtErr.classList.remove('visible');
                        if (balErr) balErr.classList.add('visible');
                    } else {
                        if (amtErr) amtErr.classList.remove('visible');
                        if (balErr) balErr.classList.remove('visible');
                    }
                });
            }

            // Form submit validation
            form.addEventListener('submit', function (e) {
                if (acctInput && !acctInput.value) {
                    e.preventDefault();
                    if (acctErr) acctErr.classList.add('visible');
                    return;
                }
                var amt = amountInput ? parseFloat(amountInput.value) : 0;
                if (isNaN(amt) || amt <= 0) {
                    e.preventDefault();
                    if (amtErr) amtErr.classList.add('visible');
                    return;
                }
                if (txnType === 'WITHDRAWAL' && _availBal !== null && amt > _availBal) {
                    e.preventDefault();
                    if (balErr) balErr.classList.add('visible');
                }
            });
        }

        // ── Transfer page ──
        if (txnType === 'TRANSFER') {
            var fromAcct = document.getElementById('fromAccount');
            var toAcct = document.getElementById('toAccount');
            var _fromAvailBal = null;
            var _fromFrozen = false;
            var _toFrozen = false;

            function refreshTransferBalance(inputId, infoId, balRowId, availId, lienId, freezeWarnId, freezeMsgId, side) {
                var num = document.getElementById(inputId).value;
                if (!num) return;

                fetch(contextPath + '/accounts/api/lookup?accountNumber=' + encodeURIComponent(num))
                    .then(function (r) { return r.json(); })
                    .then(function (d) {
                        if (!d) return;
                        var infoEl = document.getElementById(infoId);
                        if (infoEl) {
                            infoEl.innerHTML = '<small><strong>' + (d.accountName || '') + '</strong> | Type: ' + (d.accountType || '') + ' | Status: ' + (d.status || '') + '</small>';
                        }

                        var balRow = document.getElementById(balRowId);
                        if (balRow) balRow.style.display = 'flex';

                        var avail = parseFloat(d.availableBalance || d.balance) || 0;
                        var availEl = document.getElementById(availId);
                        if (availEl) availEl.textContent = formatCurrency(avail);
                        var lienEl = document.getElementById(lienId);
                        if (lienEl) lienEl.textContent = formatCurrency(d.totalLien);

                        if (side === 'from') _fromAvailBal = avail;

                        var freezeEl = document.getElementById(freezeWarnId);
                        if (d.freezeLevel && d.freezeLevel !== 'NONE') {
                            if (freezeEl) freezeEl.style.display = 'flex';
                            var fmEl = document.getElementById(freezeMsgId);
                            if (fmEl) fmEl.textContent = 'Freeze: ' + d.freezeLevel;
                            if (side === 'from' && (d.freezeLevel === 'DEBIT_ONLY' || d.freezeLevel === 'FULL')) _fromFrozen = true;
                            if (side === 'to' && (d.freezeLevel === 'CREDIT_ONLY' || d.freezeLevel === 'FULL')) _toFrozen = true;
                        } else {
                            if (freezeEl) freezeEl.style.display = 'none';
                            if (side === 'from') _fromFrozen = false;
                            if (side === 'to') _toFrozen = false;
                        }
                        updateTransferSubmit();
                    })
                    .catch(function (e) { console.error('Balance lookup failed:', e); });
            }

            function updateTransferSubmit() {
                if (!submitBtn) return;
                if (isHoliday) { submitBtn.disabled = true; return; }
                submitBtn.disabled = _fromFrozen || _toFrozen;
            }

            if (fromAcct) {
                fromAcct.addEventListener('change', function () {
                    refreshTransferBalance('fromAccount', 'fromInfo', 'fromBalanceRow', 'fromAvailBalance', 'fromLien', 'fromFreezeWarning', 'fromFreezeMsg', 'from');
                });
            }
            if (toAcct) {
                toAcct.addEventListener('change', function () {
                    refreshTransferBalance('toAccount', 'toInfo', 'toBalanceRow', 'toAvailBalance', 'toLien', 'toFreezeWarning', 'toFreezeMsg', 'to');
                });
            }

            if (amountInput) {
                amountInput.addEventListener('input', function () {
                    var amt = parseFloat(this.value);
                    if (this.value && (isNaN(amt) || amt <= 0)) {
                        if (amtErr) amtErr.classList.add('visible');
                        if (balErr) balErr.classList.remove('visible');
                    } else if (_fromAvailBal !== null && amt > _fromAvailBal) {
                        if (amtErr) amtErr.classList.remove('visible');
                        if (balErr) balErr.classList.add('visible');
                    } else {
                        if (amtErr) amtErr.classList.remove('visible');
                        if (balErr) balErr.classList.remove('visible');
                    }
                });
            }

            form.addEventListener('submit', function (e) {
                var source = fromAcct ? fromAcct.value : '';
                var dest = toAcct ? toAcct.value : '';
                if (!source || !dest) {
                    e.preventDefault();
                    if (typeof showAlert === 'function') showAlert('Please select both source and destination accounts.', 'danger');
                    return;
                }
                if (source === dest) {
                    e.preventDefault();
                    if (typeof showAlert === 'function') showAlert('Source and destination accounts cannot be the same.', 'danger');
                    return;
                }
                var amt = amountInput ? parseFloat(amountInput.value) : 0;
                if (isNaN(amt) || amt <= 0) {
                    e.preventDefault();
                    if (amtErr) amtErr.classList.add('visible');
                    return;
                }
                if (_fromAvailBal !== null && amt > _fromAvailBal) {
                    e.preventDefault();
                    if (balErr) balErr.classList.add('visible');
                    return;
                }
                if (_fromFrozen || _toFrozen) {
                    e.preventDefault();
                    if (typeof showAlert === 'function') showAlert('Transfer blocked due to account freeze.', 'danger');
                }
            });
        }
    });
})();
