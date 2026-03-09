<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="../layout/header.jsp" %>

<div class="d-flex justify-content-between align-items-center mb-4">
    <h3><i class="bi bi-currency-exchange"></i> FX Rates Management</h3>
</div>

<div class="card shadow mb-4">
    <div class="card-header bg-white">
        <h6 class="mb-0"><i class="bi bi-info-circle"></i> Exchange Rate Configuration</h6>
    </div>
    <div class="card-body">
        <p class="text-muted mb-0">
            Exchange rates are managed through the system configuration. The default currency is <strong>INR</strong>.
            Contact the system administrator to configure multi-currency exchange rates.
        </p>
    </div>
</div>

<div class="card shadow">
    <div class="card-header bg-white d-flex justify-content-between align-items-center">
        <h6 class="mb-0"><i class="bi bi-table"></i> Current Rates</h6>
    </div>
    <div class="card-body">
        <div class="table-responsive">
            <table class="table table-hover table-sm mb-0">
                <thead class="table-light">
                    <tr>
                        <th>From Currency</th>
                        <th>To Currency</th>
                        <th>Rate</th>
                        <th>Effective Date</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="rate" items="${rates}">
                        <tr>
                            <td><c:out value="${rate.currencyFrom}"/></td>
                            <td><c:out value="${rate.currencyTo}"/></td>
                            <td><c:out value="${rate.rate}"/></td>
                            <td><c:out value="${rate.effectiveDate}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty rates}">
                        <tr>
                            <td colspan="4" class="text-center text-muted py-4">
                                <i class="bi bi-currency-exchange fs-3 d-block mb-2"></i>
                                No exchange rates configured. Default currency: INR.
                            </td>
                        </tr>
                    </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="../layout/footer.jsp" %>
