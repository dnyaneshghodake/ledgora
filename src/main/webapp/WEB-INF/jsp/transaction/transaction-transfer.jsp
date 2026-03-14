<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-arrow-left-right"></i> Fund Transfer</h3>
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

<form method="post" action="${pageContext.request.contextPath}/transactions/transfer" id="transferForm"
      data-context-path="${pageContext.request.contextPath}"
      data-is-holiday="${isHoliday}" data-txn-type="TRANSFER">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
    <input type="hidden" name="transactionType" value="TRANSFER"/>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 1: TRANSACTION CONTEXT (Finacle-style header strip)       --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card border-primary shadow-sm mb-3">
        <div class="card-body py-2">
            <div class="row align-items-center g-2">
                <div class="col-md-3">
                    <small class="text-muted d-block">Transaction Type</small>
                    <span class="badge bg-info fs-6">TRANSFER</span>
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
    <%-- SECTION 2: ACCOUNT SELECTION (Dual Panel)                          --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-arrow-left-right me-2"></i>Account Selection</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-up-circle text-danger"></i> From Account (Debit)</strong></div>
                        <div class="card-body">
                            <label class="form-label cbs-field-required">Account Number</label>
                            <div class="input-group">
                                <input type="text" name="sourceAccountNumber" id="fromAccount" class="form-control" required readonly
                                       placeholder="Use lookup to select"/>
                                <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('fromAccount','fromAccountName')" title="Search Account">
                                    <i class="bi bi-search"></i>
                                </button>
                            </div>
                            <input type="hidden" id="fromAccountName"/>
                            <div class="mt-2" id="fromInfo"></div>
                            <%-- From Account Balance Cards --%>
                            <div class="row g-2 mt-2" id="fromBalanceRow" style="display:none;">
                                <div class="col-6">
                                    <div class="card border-primary cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-primary">Available</small>
                                            <h6 id="fromAvailBalance" class="mb-0 text-primary">--</h6>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="card border-warning cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-warning">Lien</small>
                                            <h6 id="fromLien" class="mb-0 text-warning">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="fromFreezeWarning" class="cbs-txn-freeze-warning mt-2" style="display:none;">
                                <i class="bi bi-slash-circle"></i> <span id="fromFreezeMsg"></span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card bg-light">
                        <div class="card-header"><strong><i class="bi bi-arrow-down-circle text-success"></i> To Account (Credit)</strong></div>
                        <div class="card-body">
                            <label class="form-label cbs-field-required">Account Number</label>
                            <div class="input-group">
                                <input type="text" name="destinationAccountNumber" id="toAccount" class="form-control" required readonly
                                       placeholder="Use lookup to select"/>
                                <button type="button" class="btn btn-outline-primary" onclick="openAccountLookup('toAccount','toAccountName')" title="Search Account">
                                    <i class="bi bi-search"></i>
                                </button>
                            </div>
                            <input type="hidden" id="toAccountName"/>
                            <div class="mt-2" id="toInfo"></div>
                            <%-- To Account Balance Cards --%>
                            <div class="row g-2 mt-2" id="toBalanceRow" style="display:none;">
                                <div class="col-6">
                                    <div class="card border-success cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-success">Available</small>
                                            <h6 id="toAvailBalance" class="mb-0 text-success">--</h6>
                                        </div>
                                    </div>
                                </div>
                                <div class="col-6">
                                    <div class="card border-warning cbs-txn-balance-card">
                                        <div class="card-body p-2 text-center">
                                            <small class="text-warning">Lien</small>
                                            <h6 id="toLien" class="mb-0 text-warning">--</h6>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div id="toFreezeWarning" class="cbs-txn-freeze-warning mt-2" style="display:none;">
                                <i class="bi bi-slash-circle"></i> <span id="toFreezeMsg"></span>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SECTION 3: TRANSACTION DETAILS                                     --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white">
            <h5 class="mb-0"><i class="bi bi-arrow-left-right me-2"></i>Transaction Details</h5>
        </div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-3">
                    <label class="form-label cbs-field-required">Amount</label>
                    <input type="number" name="amount" class="form-control" required step="0.01" min="0.01" id="amountInput"
                           placeholder="Enter transfer amount"/>
                    <div id="amtInlineError" class="cbs-inline-error">Amount must be greater than zero.</div>
                    <div id="balanceExceedError" class="cbs-inline-error">Amount exceeds source available balance.</div>
                </div>
                <div class="col-md-2">
                    <label for="currencyInput" class="form-label cbs-field-required">Currency</label>
                    <select name="currency" id="currencyInput" class="form-select" required>
                        <option value="INR" selected>INR</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                    </select>
                </div>
                <div class="col-md-3">
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
                    <label class="form-label">Description</label>
                    <input type="text" name="description" class="form-control" maxlength="255" placeholder="Transfer description"/>
                </div>
                <div class="col-12">
                    <label for="narrationInput" class="form-label">Narration</label>
                    <input type="text" name="narration" id="narrationInput" class="form-control" maxlength="500" placeholder="Narration (for audit trail and passbook)"/>
                </div>
                <%-- FX Info --%>
                <div class="col-12 d-none" id="fxInfoRow">
                    <div class="alert alert-info d-flex align-items-center mb-0 py-2">
                        <i class="bi bi-currency-exchange me-2 fs-5"></i>
                        <span>Cross-currency transfer detected. FX conversion will be applied at the business-date rate on submission.</span>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <%-- SUBMIT                                                             --%>
    <%-- ═══════════════════════════════════════════════════════════════════ --%>
    <div class="d-flex gap-2 align-items-center">
        <button type="submit" class="btn btn-primary btn-lg" id="submitBtn" ${isHoliday ? 'disabled' : ''}>
            <i class="bi bi-arrow-left-right"></i> Execute Transfer
        </button>
        <a href="${pageContext.request.contextPath}/transactions" class="btn btn-outline-secondary">Cancel</a>
        <c:if test="${isHoliday}">
            <small class="text-danger ms-3"><i class="bi bi-info-circle"></i> Submissions disabled on holidays.</small>
        </c:if>
    </div>

</form>

<script src="${pageContext.request.contextPath}/resources/js/transaction.js"></script>
<%@ include file="../layout/footer.jsp" %>
