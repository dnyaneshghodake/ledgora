<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-calendar-plus"></i> Add Calendar Entry</h3>
    <a href="${pageContext.request.contextPath}/calendar" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty error}">
    <div class="alert alert-danger"><c:out value="${error}"/></div>
</c:if>

<%-- Main Content Section --%>
<div class="card shadow">
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/calendar/create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Date</label>
                    <input type="date" name="calendarDate" class="form-control" required/>
                </div>
                <div class="col-md-4">
                    <label class="form-label cbs-field-required">Day Type</label>
                    <select name="dayType" class="form-select" required>
                        <option value="WORKING_DAY">WORKING_DAY</option>
                        <option value="HOLIDAY">HOLIDAY</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Description</label>
                    <input type="text" name="description" class="form-control" maxlength="255"/>
                </div>
                <div class="col-12"><hr>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Create (Pending Approval)</button>
                </div>
            </div>
        </form>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
