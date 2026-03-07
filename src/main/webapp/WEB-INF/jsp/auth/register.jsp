<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ledgora - Register</title>
    <link href="${pageContext.request.contextPath}/resources/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/resources/css/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/resources/css/style.css" rel="stylesheet">
</head>
<body class="bg-light">
<div class="container">
    <div class="row justify-content-center mt-5">
        <div class="col-md-6">
            <div class="card shadow">
                <div class="card-body p-5">
                    <div class="text-center mb-4">
                        <h2 class="text-primary"><i class="bi bi-bank2"></i> Ledgora</h2>
                        <p class="text-muted">Create New Account</p>
                    </div>
                    <c:if test="${not empty error}">
                        <div class="alert alert-danger">${error}</div>
                    </c:if>
                    <form:form action="${pageContext.request.contextPath}/register" method="post" modelAttribute="registerRequest">
                        <div class="mb-3">
                            <label for="username" class="form-label">Username</label>
                            <form:input path="username" cssClass="form-control" id="username" placeholder="Enter username" required="true"/>
                        </div>
                        <div class="mb-3">
                            <label for="password" class="form-label">Password</label>
                            <form:password path="password" cssClass="form-control" id="password" placeholder="Enter password" required="true"/>
                        </div>
                        <div class="mb-3">
                            <label for="fullName" class="form-label">Full Name</label>
                            <form:input path="fullName" cssClass="form-control" id="fullName" placeholder="Enter full name" required="true"/>
                        </div>
                        <div class="mb-3">
                            <label for="email" class="form-label">Email</label>
                            <form:input path="email" type="email" cssClass="form-control" id="email" placeholder="Enter email"/>
                        </div>
                        <div class="mb-3">
                            <label for="phone" class="form-label">Phone</label>
                            <form:input path="phone" cssClass="form-control" id="phone" placeholder="Enter phone number"/>
                        </div>
                        <div class="mb-3">
                            <label for="branchCode" class="form-label">Branch Code</label>
                            <form:input path="branchCode" cssClass="form-control" id="branchCode" placeholder="Enter branch code"/>
                        </div>
                        <button type="submit" class="btn btn-primary w-100 mb-3">
                            <i class="bi bi-person-plus"></i> Register
                        </button>
                    </form:form>
                    <div class="text-center">
                        <a href="${pageContext.request.contextPath}/login" class="text-decoration-none">
                            Already have an account? Sign In
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<script src="${pageContext.request.contextPath}/resources/js/bootstrap.bundle.min.js"></script>
</body>
</html>
