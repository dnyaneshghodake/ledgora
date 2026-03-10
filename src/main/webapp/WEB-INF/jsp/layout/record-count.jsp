<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  CBS Record Count Summary Fragment
  Displays "Showing X-Y of Z records" above data tables.

  Usage: <%@ include file="../layout/record-count.jsp" %>

  Expected model attributes (same as pagination.jsp):
    currentPage   - int, current page number (0-based)
    pageSize      - int, items per page
    totalElements - long, total number of records
--%>
<c:if test="${not empty totalElements && totalElements > 0}">
<div class="cbs-record-count">
    <c:set var="startRecord" value="${currentPage * pageSize + 1}"/>
    <c:set var="endRecord" value="${(currentPage + 1) * pageSize}"/>
    <c:if test="${endRecord > totalElements}"><c:set var="endRecord" value="${totalElements}"/></c:if>
    <small>Showing <strong><c:out value="${startRecord}"/>&ndash;<c:out value="${endRecord}"/></strong> of <strong><c:out value="${totalElements}"/></strong> records</small>
</div>
</c:if>
