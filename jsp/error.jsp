<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="${empty lang ? 'en' : lang}"/>
<fmt:setBundle basename="i18n.messages"/>
<fmt:message key="error.title" var="pageTitle"/>
<%@ include file="_header.jsp" %>

<%-- Deliberately plain: no stack trace, no error details, nothing that helps an attacker. --%>
<section class="onboard">
  <h1><fmt:message key="error.headline"/></h1>
  <p class="lead"><fmt:message key="error.lead"/></p>
  <p><a class="btn" href="<c:url value='/feed'/>"><fmt:message key="error.back"/></a></p>
</section>

<%@ include file="_footer.jsp" %>
