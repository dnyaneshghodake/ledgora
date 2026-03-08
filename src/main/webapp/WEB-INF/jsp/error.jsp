<%@ page isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="layout/header.jsp" %>

<div class="container-fluid py-4">
    <div class="card shadow-sm border-0">
        <div class="card-body text-center py-5">
            <i class="bi bi-exclamation-octagon text-danger" style="font-size: 3rem;"></i>
            <h2 class="mt-3">Something went wrong</h2>
            <p class="text-muted mb-4">
                We couldn't process your request.
                <c:if test="${not empty status}">Status: <strong>${status}</strong>.</c:if>
            </p>

            <c:if test="${not empty error}">
                <div class="alert alert-danger d-inline-block text-start" role="alert">
                    <strong>Error:</strong> ${error}
                    <c:if test="${not empty message}"><br/><strong>Message:</strong> ${message}</c:if>
                </div>
            </c:if>

            <div class="mt-3">
                <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary me-2">
                    <i class="bi bi-house"></i> Back to Dashboard
                </a>
                <a href="javascript:history.back()" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left"></i> Go Back
                </a>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jsp" %>
