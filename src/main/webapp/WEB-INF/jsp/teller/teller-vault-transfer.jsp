<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-safe"></i> Vault Cash Transfer</h3>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> Teller Inquiry</a>
</div>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/teller/vault-transfer">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

    <div class="card shadow-sm mb-3">
        <div class="card-header bg-white"><h5 class="mb-0"><i class="bi bi-arrow-left-right me-2"></i>Transfer Details</h5></div>
        <div class="card-body">
            <div class="row g-3">
                <div class="col-md-4">
                    <label for="direction" class="form-label fw-bold">Direction</label>
                    <select name="direction" id="direction" class="form-select" required>
                        <option value="TELLER_TO_VAULT" selected>Teller &rarr; Vault</option>
                        <option value="VAULT_TO_TELLER">Vault &rarr; Teller</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="amount" class="form-label fw-bold">Amount</label>
                    <input type="number" name="amount" id="amount" class="form-control form-control-lg" step="0.01" min="0.01" required placeholder="Transfer amount"/>
                </div>
                <div class="col-md-4">
                    <label for="remarks" class="form-label">Remarks</label>
                    <input type="text" name="remarks" id="remarks" class="form-control" maxlength="500" placeholder="Optional remarks"/>
                </div>
            </div>
        </div>
    </div>

    <div class="alert alert-info"><i class="bi bi-info-circle"></i> Vault transfers require <strong>dual custody authorization</strong> per RBI guidelines. A second custodian must approve this transfer.</div>

    <button type="submit" class="btn btn-warning btn-lg"><i class="bi bi-safe"></i> Initiate Transfer</button>
    <a href="${pageContext.request.contextPath}/teller/inquiry" class="btn btn-outline-secondary">Cancel</a>
</form>

<%@ include file="../layout/footer.jsp" %>
