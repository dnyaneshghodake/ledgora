<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-calendar-plus"></i> Add Calendar Entry</h3>
    <a href="${pageContext.request.contextPath}/calendar" class="btn btn-outline-secondary">
        <i class="bi bi-arrow-left"></i> Back to Calendar
    </a>
</div>

<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/calendar/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Date</label>
                    <input type="date" name="calendarDate" class="form-control" required/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Day Type</label>
                    <select name="dayType" class="form-select" required>
                        <option value="WORKING_DAY">Working Day</option>
                        <option value="HOLIDAY">Holiday</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Holiday Type</label>
                    <select name="holidayType" class="form-select">
                        <option value="">Select...</option>
                        <option value="NATIONAL">National Holiday</option>
                        <option value="REGIONAL">Regional Holiday</option>
                        <option value="BANK_SPECIFIC">Bank Specific</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label">Holiday Name</label>
                    <input type="text" name="holidayName" class="form-control" maxlength="100"/>
                </div>
                <div class="col-md-3">
                    <div class="form-check mt-4">
                        <input class="form-check-input" type="checkbox" name="atmAllowed" value="true" id="atmAllowed">
                        <label class="form-check-label" for="atmAllowed">ATM Transactions Allowed</label>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="form-check mt-4">
                        <input class="form-check-input" type="checkbox" name="systemTxnAllowed" value="true" id="systemTxnAllowed">
                        <label class="form-check-label" for="systemTxnAllowed">System Transactions Allowed</label>
                    </div>
                </div>
                <div class="col-12">
                    <label class="form-label">Remarks</label>
                    <textarea name="remarks" class="form-control" rows="2" maxlength="500"></textarea>
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-primary"><i class="bi bi-send"></i> Submit for Approval</button>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
