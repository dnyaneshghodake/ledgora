<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-shield-check"></i> Ledger Validation Status</h3>
    <a href="${pageContext.request.contextPath}/admin/ledger/view/validate" class="btn btn-warning" id="runValidation">
        <i class="bi bi-play-circle"></i> Run Full Validation
    </a>
</div>

<c:if test="${result != null}">
<div class="row g-4 mb-4">
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Status</h6>
                <c:choose>
                    <c:when test="${result.status == 'HEALTHY'}"><h3 class="text-success">HEALTHY</h3></c:when>
                    <c:when test="${result.status == 'WARNING'}"><h3 class="text-warning">WARNING</h3></c:when>
                    <c:otherwise><h3 class="text-danger">CORRUPTED</h3></c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Transactions Checked</h6>
                <h3>${result.transactionsChecked}</h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Accounts Checked</h6>
                <h3>${result.accountsChecked}</h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card shadow text-center">
            <div class="card-body">
                <h6>Orphan Entries</h6>
                <h3>${result.orphanEntriesFound}</h3>
            </div>
        </div>
    </div>
</div>

<c:if test="${not empty result.warnings}">
<div class="card shadow mb-4 border-warning">
    <div class="card-header bg-warning text-dark"><strong>Warnings</strong></div>
    <ul class="list-group list-group-flush">
        <c:forEach var="w" items="${result.warnings}">
            <li class="list-group-item"><i class="bi bi-exclamation-triangle text-warning"></i> ${w}</li>
        </c:forEach>
    </ul>
</div>
</c:if>

<c:if test="${not empty result.errors}">
<div class="card shadow mb-4 border-danger">
    <div class="card-header bg-danger text-white"><strong>Errors</strong></div>
    <ul class="list-group list-group-flush">
        <c:forEach var="e" items="${result.errors}">
            <li class="list-group-item"><i class="bi bi-x-circle text-danger"></i> ${e}</li>
        </c:forEach>
    </ul>
</div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <p class="mb-0"><strong>Validated At:</strong> ${result.validatedAt}</p>
    </div>
</div>
</c:if>

<%@ include file="../layout/footer.jsp" %>
