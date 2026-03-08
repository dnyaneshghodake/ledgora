<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<%-- Page Title --%>
<div class="d-flex justify-content-between align-items-center mb-3">
    <h3><i class="bi bi-receipt"></i> Tax Profiles</h3>
</div>

<%-- Operational Status Banner --%>
<%@ include file="../layout/status-banner.jsp" %>

<c:if test="${not empty message}">
    <div class="alert alert-success"><c:out value="${message}"/></div>
</c:if>

<%-- Search / Filter Section --%>
<div class="card shadow mb-4">
    <div class="card-header bg-light"><h6 class="mb-0"><i class="bi bi-funnel"></i> Search &amp; Filter</h6></div>
    <div class="card-body">
        <form method="get" action="${pageContext.request.contextPath}/tax-profiles" class="row g-2">
            <div class="col-md-4">
                <input type="text" name="search" class="form-control" placeholder="Search by Customer or PAN" value="<c:out value='${param.search}'/>"/>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-outline-primary w-100"><i class="bi bi-search"></i> Search</button>
            </div>
            <div class="col-md-2">
                <a href="${pageContext.request.contextPath}/tax-profiles" class="btn btn-outline-secondary w-100">Reset</a>
            </div>
        </form>
    </div>
</div>

<%-- Main Content Section --%>
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
                <%-- Pagination --%>
                <%@ include file="../layout/pagination.jsp" %>
            </c:when>
            <c:otherwise>
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-receipt" style="font-size: 3rem;"></i>
                    <p class="mt-2">No tax profiles found.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
