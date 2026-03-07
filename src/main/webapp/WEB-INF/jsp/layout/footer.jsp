<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty sessionScope.username}">
    </div><!-- /.cbs-content -->
</main><!-- /.cbs-main -->

<%-- Sidebar overlay for mobile --%>
<div class="cbs-sidebar-overlay" id="sidebarOverlay"></div>

<footer class="cbs-footer" id="cbsFooter">
    <div class="cbs-footer-content">
        <span>Ledgora Core Banking Platform &copy; 2026</span>
        <span class="cbs-footer-separator">|</span>
        <span>Powered by Spring Boot</span>
    </div>
</footer>
</c:if>

<c:if test="${empty sessionScope.username}">
<footer class="mt-5 py-3 bg-light text-center text-muted border-top">
    <div class="container">
        <p class="mb-0">Ledgora Core Banking Platform &copy; 2026 | Powered by Spring Boot</p>
    </div>
</footer>
</c:if>

<script src="${pageContext.request.contextPath}/resources/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/resources/js/app.js"></script>
</body>
</html>
