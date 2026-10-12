<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="${empty lang ? 'en' : lang}"/>
<fmt:setBundle basename="i18n.messages"/>
<fmt:message key="login.title" var="pageTitle"/>
<%@ include file="_header.jsp" %>

<section class="onboard">
  <h1><fmt:message key="login.headline"/></h1>
  <p class="lead"><fmt:message key="login.lead"/></p>

  <c:if test="${not empty error}"><p class="err" role="alert"><c:out value="${error}"/></p></c:if>

  <c:choose>
    <c:when test="${step == 'verify'}">
      <form method="post" action="<c:url value='/login'/>" autocomplete="off">
        <input type="hidden" name="action" value="verify">
        <label class="fieldlabel" for="code"><fmt:message key="login.code.label"/></label>
        <input class="field" id="code" name="code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required autofocus>

        <fieldset class="radios">
          <legend class="fieldlabel"><fmt:message key="login.gender.legend"/></legend>
          <label><input type="radio" name="gender" value="WOMAN"> <fmt:message key="login.gender.woman"/></label>
          <label><input type="radio" name="gender" value="MAN"> <fmt:message key="login.gender.man"/></label>
          <label><input type="radio" name="gender" value="NON_BINARY"> <fmt:message key="login.gender.nonbinary"/></label>
          <label><input type="radio" name="gender" value="NOT_SHARED" checked> <fmt:message key="login.gender.none"/></label>
        </fieldset>

        <button class="btn block" type="submit"><fmt:message key="login.enter"/></button>
      </form>
    </c:when>
    <c:otherwise>
      <form method="post" action="<c:url value='/login'/>" autocomplete="off">
        <input type="hidden" name="action" value="code">
        <label class="fieldlabel" for="phone"><fmt:message key="login.phone.label"/></label>
        <div class="numrow">
          <span class="prefix">+91</span>
          <input class="field" id="phone" name="phone" type="tel" inputmode="numeric" maxlength="14" required autofocus>
        </div>
        <button class="btn block" type="submit"><fmt:message key="login.send"/></button>
        <p class="small"><fmt:message key="login.phone.note"/></p>
      </form>
    </c:otherwise>
  </c:choose>
</section>

<%@ include file="_footer.jsp" %>
