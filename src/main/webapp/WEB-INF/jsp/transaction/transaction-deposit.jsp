<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="../layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-arrow-down-circle text-success"></i> Cash Deposit</h3>
        <hr>
    </div>
</div>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-body p-4">
                <form:form action="${pageContext.request.contextPath}/transactions/deposit" method="post" modelAttribute="transactionDTO" id="depositForm">
                    <form:hidden path="transactionType" value="DEPOSIT"/>
                    <div class="mb-3">
                        <label for="destinationAccountNumber" class="form-label">Account *</label>
                        <form:select path="destinationAccountNumber" cssClass="form-select" id="destinationAccountNumber" required="true">
                            <option value="">Select Account</option>
                            <c:forEach var="acc" items="${accounts}">
                                <option value="${acc.accountNumber}" ${param.account == acc.accountNumber ? 'selected' : ''}>${acc.accountNumber} - ${acc.customerName} (${acc.balance} ${acc.currency})</option>
                            </c:forEach>
                        </form:select>
                    </div>
                    <div class="mb-3">
                        <label for="amount" class="form-label">Amount *</label>
                        <div class="input-group">
                            <span class="input-group-text">INR</span>
                            <form:input path="amount" type="number" step="0.01" min="0.01" cssClass="form-control" id="amount" required="true" placeholder="0.00"/>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="description" class="form-label">Description</label>
                        <form:input path="description" cssClass="form-control" id="description" placeholder="Cash Deposit"/>
                    </div>
                    <div class="mb-3">
                        <label for="narration" class="form-label">Narration</label>
                        <form:textarea path="narration" cssClass="form-control" id="narration" rows="2"/>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-success"><i class="bi bi-check-circle"></i> Deposit</button>
                        <a href="${pageContext.request.contextPath}/transactions" class="btn btn-secondary">Cancel</a>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
