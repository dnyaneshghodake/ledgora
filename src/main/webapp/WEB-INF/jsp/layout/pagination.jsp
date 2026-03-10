<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Pagination Fragment
  Provides a standardized pagination control for list pages.

  Usage: <%@ include file="../layout/pagination.jsp" %>

  Expected model attributes:
    currentPage   - int, current page number (0-based)
    totalPages    - int, total number of pages
    pageSize      - int, items per page
    totalElements - long, total number of records
    baseUrl       - String, base URL for pagination links (e.g., "/customers")
    queryString   - String, existing query parameters to preserve (e.g., "&search=foo&status=ACTIVE")
--%>

<c:if test="${not empty totalPages && totalPages > 1}">
<nav aria-label="Page navigation" class="mt-4 cbs-pagination">
    <div class="d-flex justify-content-between align-items-center">
        <small class="text-muted">
            <c:set var="pgStart" value="${currentPage * pageSize + 1}"/>
            <c:set var="pgEnd" value="${(currentPage + 1) * pageSize}"/>
            <c:if test="${not empty totalElements && pgEnd > totalElements}"><c:set var="pgEnd" value="${totalElements}"/></c:if>
            <c:choose>
                <c:when test="${not empty totalElements}">
                    Showing <strong><c:out value="${pgStart}"/>&ndash;<c:out value="${pgEnd}"/></strong> of <strong><c:out value="${totalElements}"/></strong> records
                    (page <c:out value="${currentPage + 1}"/> of <c:out value="${totalPages}"/>)
                </c:when>
                <c:otherwise>
                    Page <strong><c:out value="${currentPage + 1}"/></strong> of <strong><c:out value="${totalPages}"/></strong>
                </c:otherwise>
            </c:choose>
        </small>
        <ul class="pagination pagination-sm mb-0">
            <%-- First Page --%>
            <li class="page-item ${currentPage == 0 ? 'disabled' : ''}">
                <a class="page-link" href="${pageContext.request.contextPath}${baseUrl}?page=0${queryString}" aria-label="First">
                    <i class="bi bi-chevron-double-left"></i>
                </a>
            </li>
            <%-- Previous Page --%>
            <li class="page-item ${currentPage == 0 ? 'disabled' : ''}">
                <a class="page-link" href="${pageContext.request.contextPath}${baseUrl}?page=${currentPage - 1}${queryString}" aria-label="Previous">
                    <i class="bi bi-chevron-left"></i>
                </a>
            </li>

            <%-- Page Numbers (show up to 5 pages around current) --%>
            <c:set var="startPage" value="${currentPage - 2 < 0 ? 0 : currentPage - 2}"/>
            <c:set var="endPage" value="${startPage + 4 >= totalPages ? totalPages - 1 : startPage + 4}"/>
            <c:if test="${endPage - startPage < 4 && endPage - 4 >= 0}">
                <c:set var="startPage" value="${endPage - 4}"/>
            </c:if>
            <c:forEach var="i" begin="${startPage}" end="${endPage}">
                <li class="page-item ${i == currentPage ? 'active' : ''}">
                    <a class="page-link" href="${pageContext.request.contextPath}${baseUrl}?page=${i}${queryString}">
                        <c:out value="${i + 1}"/>
                    </a>
                </li>
            </c:forEach>

            <%-- Next Page --%>
            <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="${pageContext.request.contextPath}${baseUrl}?page=${currentPage + 1}${queryString}" aria-label="Next">
                    <i class="bi bi-chevron-right"></i>
                </a>
            </li>
            <%-- Last Page --%>
            <li class="page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}">
                <a class="page-link" href="${pageContext.request.contextPath}${baseUrl}?page=${totalPages - 1}${queryString}" aria-label="Last">
                    <i class="bi bi-chevron-double-right"></i>
                </a>
            </li>
        </ul>
    </div>
</nav>
</c:if>
