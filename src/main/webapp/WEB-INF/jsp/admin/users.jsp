<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-people"></i> User Management</h3>
</div>

<%-- PART 8: Edit User Modal --%>
<c:if test="${not empty editUser}">
<div class="card shadow mb-4">
    <div class="card-header bg-white"><h6 class="mb-0"><i class="bi bi-pencil"></i> Edit User: <c:out value="${editUser.username}"/></h6></div>
    <div class="card-body">
        <form method="post" action="${pageContext.request.contextPath}/admin/users/${editUser.id}/edit">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="row g-3">
                <div class="col-md-4">
                    <label class="form-label">Full Name</label>
                    <input type="text" class="form-control" name="fullName" value="${fn:escapeXml(editUser.fullName)}" required/>
                </div>
                <div class="col-md-4">
                    <label class="form-label">Email</label>
                    <input type="email" class="form-control" name="email" value="${fn:escapeXml(editUser.email)}"/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">Branch Code</label>
                    <input type="text" class="form-control" name="branchCode" value="${fn:escapeXml(editUser.branchCode)}"/>
                </div>
                <div class="col-md-2">
                    <label class="form-label">New Password <small class="text-muted">(optional)</small></label>
                    <input type="password" class="form-control" name="password" placeholder="Leave blank to keep"/>
                </div>
            </div>
            <div class="mt-3">
                <button type="submit" class="btn btn-primary btn-sm"><i class="bi bi-check"></i> Save</button>
                <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary btn-sm">Cancel</a>
            </div>
        </form>
    </div>
</div>
</c:if>

<div class="card shadow">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h6 class="mb-0">Users <span class="badge bg-secondary">${users.size()} total</span></h6>
    </div>
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
                    <th>Actions</th>
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
                        <td>
                            <c:choose>
                                <c:when test="${not empty user.lastLogin}">
                                    <small><c:out value="${user.lastLogin}"/></small>
                                </c:when>
                                <c:otherwise><small class="text-muted">Never</small></c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <div class="btn-group btn-group-sm">
                                <a href="${pageContext.request.contextPath}/admin/users/${user.id}/edit"
                                   class="btn btn-outline-primary" title="Edit"><i class="bi bi-pencil"></i></a>
                                <form method="post" action="${pageContext.request.contextPath}/admin/users/${user.id}/toggle" style="display:inline;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <button type="submit" class="btn btn-outline-warning" title="Toggle Active"
                                            data-confirm="Toggle active status for ${fn:escapeXml(user.username)}?">
                                        <c:choose>
                                            <c:when test="${user.isActive}"><i class="bi bi-pause-circle"></i></c:when>
                                            <c:otherwise><i class="bi bi-play-circle"></i></c:otherwise>
                                        </c:choose>
                                    </button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/admin/users/${user.id}/delete" style="display:inline;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <button type="submit" class="btn btn-outline-danger" title="Deactivate"
                                            data-confirm="Deactivate user ${fn:escapeXml(user.username)}? The user will be unable to log in.">
                                        <i class="bi bi-person-x"></i>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<%-- PART 8: Role Management Section --%>
<div class="mt-4">
    <div class="d-flex justify-content-between align-items-center mb-3">
        <h4><i class="bi bi-shield-lock"></i> Role Management</h4>
    </div>
    <div class="card shadow">
        <div class="table-responsive">
            <table class="table table-hover mb-0">
                <thead class="table-light">
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Description</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="role" items="${roles}">
                        <tr>
                            <td>${role.id}</td>
                            <td><span class="badge bg-primary"><c:out value="${role.name}"/></span></td>
                            <td>
                                <form method="post" action="${pageContext.request.contextPath}/admin/roles/${role.id}/edit" class="d-flex gap-2">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="text" class="form-control form-control-sm" name="description"
                                           value="${fn:escapeXml(role.description)}" placeholder="Add description" style="max-width:300px;"/>
                                    <button type="submit" class="btn btn-outline-primary btn-sm"><i class="bi bi-check"></i></button>
                                </form>
                            </td>
                            <td>
                                <form method="post" action="${pageContext.request.contextPath}/admin/roles/${role.id}/delete" style="display:inline;">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <button type="submit" class="btn btn-outline-danger btn-sm"
                                            data-confirm="Delete role ${fn:escapeXml(role.name)}? Users with this role may lose access.">
                                        <i class="bi bi-trash"></i> Delete
                                    </button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
