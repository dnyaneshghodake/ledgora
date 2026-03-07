<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ include file="layout/header.jsp" %>

<div class="row mb-4">
    <div class="col-12">
        <h3><i class="bi bi-pencil"></i> Edit GL Account</h3>
        <hr>
    </div>
</div>

<div class="row justify-content-center">
    <div class="col-md-8">
        <div class="card shadow">
            <div class="card-body p-4">
                <form:form action="${pageContext.request.contextPath}/gl/${glDTO.id}/edit" method="post" modelAttribute="glDTO">
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <label class="form-label">GL Code</label>
                            <input type="text" class="form-control" value="${glDTO.glCode}" disabled/>
                        </div>
                        <div class="col-md-8">
                            <label for="glName" class="form-label">GL Name *</label>
                            <form:input path="glName" cssClass="form-control" id="glName" required="true"/>
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label for="normalBalance" class="form-label">Normal Balance</label>
                            <form:select path="normalBalance" cssClass="form-select" id="normalBalance">
                                <option value="DEBIT" ${glDTO.normalBalance == 'DEBIT' ? 'selected' : ''}>DEBIT</option>
                                <option value="CREDIT" ${glDTO.normalBalance == 'CREDIT' ? 'selected' : ''}>CREDIT</option>
                            </form:select>
                        </div>
                        <div class="col-md-6">
                            <label for="isActive" class="form-label">Status</label>
                            <form:select path="isActive" cssClass="form-select" id="isActive">
                                <option value="true" ${glDTO.isActive ? 'selected' : ''}>Active</option>
                                <option value="false" ${!glDTO.isActive ? 'selected' : ''}>Inactive</option>
                            </form:select>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label for="description" class="form-label">Description</label>
                        <form:textarea path="description" cssClass="form-control" id="description" rows="2"/>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-circle"></i> Update</button>
                        <a href="${pageContext.request.contextPath}/gl/${glDTO.id}" class="btn btn-secondary">Cancel</a>
                    </div>
                </form:form>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
