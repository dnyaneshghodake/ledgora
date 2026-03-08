<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-play-circle"></i> Process EOD Settlement</h3>
        <hr>
    </div>
</div>

<div class="row justify-content-center">
    <div class="col-md-6">
        <div class="card shadow">
            <div class="card-body p-4">
                <div class="alert alert-info">
                    <i class="bi bi-info-circle"></i> This will process all completed transactions for the selected date and generate a settlement report with netting calculations.
                </div>
                <form action="${pageContext.request.contextPath}/settlements/process" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <div class="mb-3">
                        <label for="settlementDate" class="form-label">Settlement Date *</label>
                        <input type="date" name="settlementDate" id="settlementDate" class="form-control" value="${settlementDate}" required/>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary" onclick="return confirm('Process settlement for this date?')">
                            <i class="bi bi-play-circle"></i> Process Settlement
                        </button>
                        <a href="${pageContext.request.contextPath}/settlements" class="btn btn-secondary">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
