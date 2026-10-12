<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Ranks"/>
<c:set var="active" value="ranks"/>
<%@ include file="_header.jsp" %>

<div class="chips" role="group" aria-label="Ranking criteria">
  <c:forEach var="b" items="${boards}">
    <a class="chipbtn" href="<c:url value='/ranks'><c:param name='board' value='${b}'/></c:url>" <c:if test="${b == board}">aria-current="true"</c:if>><c:out value="${b.title}"/></a>
  </c:forEach>
</div>

<p class="critnote"><c:out value="${board.note}"/></p>
<p class="standing" role="status"><c:out value="${standing}"/></p>

<c:if test="${empty podium}"><p class="empty">Nobody is on this list yet.</p></c:if>

<%-- Podium: first place in the middle and tallest --%>
<c:if test="${not empty podium}">
  <div class="podium">
    <c:forEach var="r" items="${podium}" varStatus="s">
      <div class="pod p${r.rank}">
        <div class="pn"><c:out value="${r.alias}"/></div>
        <span class="gpill"><c:out value="${r.gender.label}"/></span>
        <c:if test="${r.oracle}"><span class="pill">Oracle</span></c:if>
        <c:if test="${board == 'LIKED' or board == 'VIEWED'}"><div class="psnip"><c:out value="${r.detail}"/></div></c:if>
        <div class="pv"><c:out value="${r.value}"/> <small><c:out value="${board.unit}"/></small></div>
        <div class="stand"><c:out value="${r.rank}"/></div>
      </div>
    </c:forEach>
  </div>
</c:if>

<div class="rlist">
  <c:forEach var="r" items="${rest}">
    <div class="rrow <c:if test='${r.alias == user.alias}'>you</c:if>">
      <span class="rank"><c:out value="${r.rank}"/></span>
      <div class="grow">
        <div class="rtop">
          <span class="name"><c:out value="${r.alias}"/></span>
          <span class="gpill"><c:out value="${r.gender.label}"/></span>
          <c:if test="${r.oracle}"><span class="pill">Oracle</span></c:if>
          <c:if test="${r.alias == user.alias}"><span class="pill">You</span></c:if>
        </div>
        <div class="sub"><c:out value="${r.detail}"/></div>
        <progress class="meter" value="${r.value}" max="${max}"></progress>
      </div>
      <div class="rval"><c:out value="${r.value}"/> <small><c:out value="${board.unit}"/></small></div>
    </div>
  </c:forEach>
</div>

<%@ include file="_footer.jsp" %>
