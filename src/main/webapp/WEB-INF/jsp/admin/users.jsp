<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> User Management</h3>
</div>

<div class="card shadow">
    <div class="table-responsive">
        <table class="table table-hover mb-0">
            <thead class="table-light">
                <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Full Name</th>
                    <th>Email</th>
                    <th>Branch</th>
                    <th>Roles</th>
                    <th>Status</th>
                    <th>Last Login</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="user" items="${users}">
                    <tr>
                        <td>${user.id}</td>
                        <td><strong><c:out value="${user.username}"/></strong></td>
                        <td><c:out value="${user.fullName}"/></td>
                        <td><c:out value="${user.email}"/></td>
                        <td><c:out value="${user.branchCode}"/></td>
                        <td>
                            <c:forEach var="role" items="${user.roles}">
                                <span class="badge bg-primary"><c:out value="${role.name}"/></span>
                            </c:forEach>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${user.isActive}"><span class="badge bg-success">Active</span></c:when>
                                <c:otherwise><span class="badge bg-danger">Inactive</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td>${user.lastLogin}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
