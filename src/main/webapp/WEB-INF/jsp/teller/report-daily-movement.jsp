<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-arrow-left-right"></i> Daily Cash Movement Report</h3>
    <a href="${pageContext.request.contextPath}/teller/reports" class="btn btn-outline-secondary btn-sm"><i class="bi bi-arrow-left"></i> All Reports</a>
</div>

<form method="get" action="${pageContext.request.contextPath}/teller/reports/daily-movement" class="mb-3">
    <div class="row g-2 align-items-end">
        <div class="col-md-3">
            <label for="date" class="form-label">Business Date</label>
            <input type="date" name="date" id="date" class="form-control" value="${reportDate}"/>
        </div>
        <div class="col-md-2"><button type="submit" class="btn btn-info"><i class="bi bi-search"></i> Filter</button></div>
    </div>
</form>

<div class="row g-3">
    <div class="col-md-3">
        <div class="card border-primary shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Active Sessions</small>
                <h3 class="text-primary"><c:out value="${report.sessionCount}" default="0"/></h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card border-success shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Total Credits (Deposits)</small>
                <h3 class="text-success"><c:out value="${report.totalCredits}" default="0.00"/></h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card border-danger shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Total Debits (Withdrawals)</small>
                <h3 class="text-danger"><c:out value="${report.totalDebits}" default="0.00"/></h3>
            </div>
        </div>
    </div>
    <div class="col-md-3">
        <div class="card border-info shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Net Movement</small>
                <h3 class="text-info"><c:out value="${report.netMovement}" default="0.00"/></h3>
            </div>
        </div>
    </div>
</div>

<div class="row g-3 mt-2">
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Total Opening Balance</small>
                <h4><c:out value="${report.totalOpening}" default="0.00"/></h4>
            </div>
        </div>
    </div>
    <div class="col-md-6">
        <div class="card shadow-sm">
            <div class="card-body text-center">
                <small class="text-muted">Total Current Balance</small>
                <h4><c:out value="${report.totalCurrent}" default="0.00"/></h4>
            </div>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
