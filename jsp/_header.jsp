<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="${empty lang ? 'en' : lang}"/>
<fmt:setBundle basename="i18n.messages"/>
<%-- Shared page top. Set "pageTitle" and "active" (feed, jobs, ranks, wishbox, security) before including. --%>
<!doctype html>
<html lang="${empty lang ? 'en' : lang}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="referrer" content="no-referrer">
  <meta name="theme-color" content="#11110f">
  <link rel="icon" type="image/png" href="<c:url value='/static/images/kanakani-logo.png'/>">
  <title><c:out value="${pageTitle}"/> | Kanakani</title>
  <link rel="stylesheet" href="<c:url value='/static/css/app.css'/>">
  <script src="<c:url value='/static/js/app.js'/>" defer></script>
</head>
<body>
<div class="app">
  <header class="top">
    <a class="logo" href="<c:url value='/feed'/>"><img src="<c:url value='/static/images/kanakani-logo.png'/>" alt="" width="38" height="38"><span>Kanakani</span></a>
    <div class="langs" role="group" aria-label="<fmt:message key='lang.label'/>">
      <a href="<c:url value='?lang=en'/>" lang="en" <c:if test="${empty lang or lang == 'en'}">aria-current="true"</c:if>>English</a>
      <a href="<c:url value='?lang=bn'/>" lang="bn" <c:if test="${lang == 'bn'}">aria-current="true"</c:if>>বাংলা</a>
      <a href="<c:url value='?lang=hi'/>" lang="hi" <c:if test="${lang == 'hi'}">aria-current="true"</c:if>>हिन्दी</a>
    </div>
    <c:if test="${not empty user}">
      <span class="chip"><c:out value="${user.alias}"/> &middot; <c:out value="${user.days}"/> <fmt:message key="header.days"/></span>
    </c:if>
  </header>
  <c:if test="${not empty user}">
    <nav class="tabs" aria-label="<fmt:message key='nav.sections'/>">
      <a href="<c:url value='/feed'/>"    <c:if test="${active == 'feed'}">aria-current="page"</c:if>><fmt:message key="nav.feed"/></a>
      <a href="<c:url value='/jobs'/>"    <c:if test="${active == 'jobs'}">aria-current="page"</c:if>><fmt:message key="nav.jobs"/></a>
      <a href="<c:url value='/ranks'/>"   <c:if test="${active == 'ranks'}">aria-current="page"</c:if>><fmt:message key="nav.ranks"/></a>
      <a href="<c:url value='/wishbox'/>" <c:if test="${active == 'wishbox'}">aria-current="page"</c:if>><fmt:message key="nav.wishbox"/></a>
      <a href="<c:url value='/account'/>" <c:if test="${active == 'security'}">aria-current="page"</c:if>><fmt:message key="nav.security"/></a>
    </nav>
  </c:if>
  <main>
