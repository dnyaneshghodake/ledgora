<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-cash-stack"></i> Cash Deposit</h3>
    <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Back to Transactions</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Holiday Warning --%>
<c:if test="${isHoliday}">
<div class="alert alert-danger d-flex align-items-center mb-3">
    <i class="bi bi-calendar-x-fill fs-4 me-2"></i>
    <div><strong>Bank Holiday.</strong> Manual (TELLER) transactions are blocked. ATM/Digital channels may be restricted.</div>
</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/transactions/deposit" id="depositForm"
      data-context-path="${pageContext.request.contextPath}"
      data-is-holiday="${isHoliday}" data-txn-type="DEPOSIT">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
    <input type="hidden" name="transactionType" value="DEPOSIT"/>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 1: TRANSACTION CONTEXT (Finacle-style header strip)       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card border-primary shadow-sm mb-3">
        <div class="card-body py-2">
            <div class="row align-items-center g-2">
                <div class="col-md-3">
                    <small class="text-muted d-block">Transaction Type</small>
                    <span class="badge bg-success fs-6">DEPOSIT</span>
                </div>
                <div class="col-md-2">
                    <small class="text-muted d-block">Business Date</small>
                    <strong><c:out value="${businessDate}" default="--"/></strong>
                </div>
                <div class="col-md-2">
                    <small class="text-muted d-block">Day Status</small>
                    <c:choose>
                        <c:when test="${dayStatus == 'OPEN'}"><span class="badge bg-success">OPEN</span></c:when>
                        <c:when test="${dayStatus == 'CLOSED'}"><span class="badge bg-danger">CLOSED</span></c:when>
                        <c:otherwise><span class="badge bg-warning text-dark"><c:out value="${dayStatus}" default="--"/></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="col-md-2">
                    <small class="text-muted d-block">Currency</small>
                    <strong><c:out value="${baseCurrency}" default="INR"/></strong>
                </div>
                <div class="col-md-3">
                    <small class="text-muted d-block">Maker</small>
                    <i class="bi bi-person-fill text-primary"></i> <strong><c:out value="${makerName}" default="--"/></strong>
                </div>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 2: ACCOUNT SELECTION                                       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-wallet2 me-2"></i>Account Selection</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-6">
                    <label for="accountNumber" class="form-label cbs-field-required">Destination Account</label>
                    <div class="input-group">
                        <input type="text" name="destinationAccountNumber" id="accountNumber" class="form-control" required readonly
                               value="<c:out value="${param.accountNumber}"/>" placeholder="Use lookup to select account"/>
                        <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('accountNumber','accountNameDisplay')" title="Search Account">
                            <i class="bi bi-search"></i>
                        </button>
                    </div>
                    <div id="acctInlineError" class="cbs-inline-error">Please select an account using the lookup.</div>
                </div>
                <div class="col-md-6">
                    <label for="accountNameDisplay" class="form-label">Account Name</label>
                    <input type="text" class="form-control" id="accountNameDisplay" disabled placeholder="Auto-filled on selection"/>
                </div>
            </div>

                <%-- Balance Display --%>
                <div class="col-12 cbs-txn-balance-row">
                    <div class="row g-2">
                        <div class="col-md-3">
                            <div class="card border-primary cbs-txn-balance-card">
                                <div class="card-body text-center p-2">
                                    <small class="text-primary">Ledger Balance</small>
                                    <h5 id="ledgerBalance" class="mb-0 text-primary">--</h5>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-success cbs-txn-balance-card">
                                <div class="card-body text-center p-2">
                                    <small class="text-success">Available Balance</small>
                                    <h5 id="availableBalance" class="mb-0 text-success">--</h5>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-warning cbs-txn-balance-card">
                                <div class="card-body text-center p-2">
                                    <small class="text-warning">Lien Amount</small>
                                    <h5 id="lienAmount" class="mb-0 text-warning">--</h5>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="card border-info cbs-txn-balance-card">
                                <div class="card-body text-center p-2">
                                    <small class="text-info">Business Date</small>
                                    <h5 id="businessDate" class="mb-0 text-info"><c:out value="${businessDate}" default="--"/></h5>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <%-- Freeze Warning --%>
                <div class="col-12 d-none" id="freezeWarning">
                    <div class="cbs-txn-freeze-warning">
                        <i class="bi bi-slash-circle"></i>
                        <strong>FREEZE ACTIVE</strong> &mdash; <span id="freezeMsg"></span>
                    </div>
                </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 3: TRANSACTION DETAILS                                     --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-cash-stack me-2"></i>Transaction Details</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label for="amountInput" class="form-label cbs-field-required">Amount (<c:out value="${baseCurrency}" default="INR"/>)</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01"
                           placeholder="Enter amount (must be > 0)" id="amountInput"/>
                    <div id="amtInlineError" class="cbs-inline-error">Amount must be greater than zero.</div>
                </div>
                <div class="col-md-4">
                    <label for="channelInput" class="form-label cbs-field-required">Channel</label>
                    <select name="channel" id="channelInput" class="form-select" required>
                        <option value="">-- Select Channel --</option>
                        <option value="TELLER" selected>TELLER</option>
                        <option value="ATM">ATM</option>
                        <option value="ONLINE">ONLINE (Internet Banking)</option>
                        <option value="MOBILE">MOBILE (Mobile Banking)</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="descriptionInput" class="form-label">Description</label>
                    <input type="text" name="description" id="descriptionInput" class="form-control" maxlength="255" placeholder="Transaction description"/>
                </div>
                <div class="col-12">
                    <label for="narrationInput" class="form-label">Narration</label>
                    <input type="text" name="narration" id="narrationInput" class="form-control" maxlength="500" placeholder="Narration (for audit trail and passbook)"/>
                </div>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SUBMIT                                                             --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="d-flex gap-2 align-items-center">
        <button type="submit" class="btn btn-success btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}>
            <i class="bi bi-cash-stack"></i> Submit Deposit
        </button>
        <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary">Cancel</a>
        <c:if test="${isHoliday}">
            <small class="text-danger ms-3"><i class="bi bi-info-circle"></i> Submissions disabled on holidays.</small>
        </c:if>
    </div>

</form>

<script src="${pageContext.request.contextPath}/resources/js/transaction.js"></script>
<%@ include file="../layout/footer.jsp" %>
