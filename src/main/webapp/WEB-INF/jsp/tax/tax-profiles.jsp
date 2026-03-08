<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-receipt"></i> Tax Profiles</h3>
</div>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<div class="card shadow">
    <div class="card-body">
        <c:choose>
            <c:when test="${not empty taxProfiles}">
                <div class="table-responsive">
                    <table class="table table-hover align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>ID</th>
                                <th>Customer</th>
                                <th>PAN</th>
                                <th>Aadhaar</th>
                                <th>GST</th>
                                <th>TDS</th>
                                <th>FATCA</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tp" items="${taxProfiles}">
                            <tr>
                                <td><c:out value="${tp.id}"/></td>
                                <td><c:out value="${tp.customerName}"/></td>
                                <td><code><c:out value="${tp.panNumber}"/></code></td>
                                <td><c:out value="${tp.aadhaarMasked}"/></td>
                                <td><c:out value="${tp.gstNumber}"/></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${tp.tdsApplicable}"><span class="badge bg-warning">Yes (<c:out value="${tp.tdsRate}"/>%)</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">No</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${tp.fatcaDeclaration}"><span class="badge bg-success">Declared</span></c:when>
                                        <c:otherwise><span class="badge bg-secondary">No</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <a href="${pageContext.request.contextPath}/tax-profiles/${tp.id}/edit" class="btn btn-sm btn-outline-primary"><i class="bi bi-pencil"></i></a>
                                </td>
                            </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted"><p>No tax profiles found.</p></div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
