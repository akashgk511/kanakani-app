<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Jobs"/>
<c:set var="active" value="jobs"/>
<%@ include file="_header.jsp" %>

<c:choose>
  <c:when test="${not empty company}">
    <%-- One company in detail --%>
    <section class="detail">
      <p><a href="<c:url value='/jobs'/>">&larr; All companies</a></p>
      <h1><c:out value="${company.name}"/></h1>
      <p class="small"><c:out value="${company.sector}"/> &middot; <c:out value="${company.city}"/><c:if test="${company.sample}"> &middot; sample data</c:if></p>
      <p><span class="wx ${company.weatherClass}"><i></i><c:out value="${company.weatherLabel}"/></span>
         &middot; <c:out value="${company.openings}"/> openings</p>
      <p class="small"><c:out value="${company.weatherGossip}"/></p>

      <h2 class="label">Hiring for</h2>
      <div class="tagrow"><c:forEach var="r" items="${company.roles}"><span class="gpill"><c:out value="${r}"/></span></c:forEach></div>

      <h2 class="label">How they hire, about <c:out value="${company.avgDays}"/> days</h2>
      <ol class="steps">
        <c:forEach var="s" items="${company.steps}">
          <li><span class="sn"><c:out value="${s.ord}"/></span><div><b><c:out value="${s.name}"/></b><div class="small"><c:out value="${s.detail}"/></div></div></li>
        </c:forEach>
      </ol>

      <h2 class="label">Money and fine print</h2>
      <dl class="facts">
        <dt>Fresher</dt><dd><c:out value="${company.fresherBand}"/></dd>
        <dt>3 to 6 years</dt><dd><c:out value="${company.midBand}"/></dd>
        <dt>Referral bonus</dt><dd><c:out value="${company.referral}"/></dd>
        <dt>Notice period</dt><dd><c:out value="${company.notice}"/></dd>
        <dt>Interview toughness</dt>
        <dd><c:forEach begin="1" end="5" var="i"><span class="dot <c:if test='${i <= company.toughness}'>on</c:if>"></span></c:forEach>
            <c:out value="${company.toughness}"/> of 5</dd>
      </dl>

      <h2 class="label">Insider whispers (unverified)</h2>
      <c:forEach var="w" items="${company.whispers}">
        <p class="whisper"><c:out value="${w.body}"/><br><span class="small"><c:out value="${w.pakkiPercent}"/>% say pakki khabar</span>
          <progress class="bbar" value="${w.pakkiPercent}" max="100"></progress></p>
      </c:forEach>

      <p><a class="btn" href="<c:url value='/feed'><c:param name='about' value='${company.name}'/></c:url>">Whisper about this company</a></p>
    </section>
  </c:when>

  <c:otherwise>
    <%-- Company list --%>
    <section class="infocard">
      <h1 class="label">How hiring usually runs</h1>
      <p class="flow"><span>Apply</span><span>Screening</span><span>Interviews</span><span>Offer</span></p>
      <p class="small">Most offers take two to five weeks. Before you resign, get the notice period, the background check and the joining date in writing.</p>
      <p class="small">Companies marked sample use made-up numbers to show the layout. Real companies fill in as employees share what they know.</p>
    </section>

    <div class="chips" role="group" aria-label="Hiring weather">
      <a class="chipbtn" href="<c:url value='/jobs'/>" <c:if test="${empty weather}">aria-current="true"</c:if>>All</a>
      <c:forEach var="w" items="SUNNY,CLOUDY,STORMY,FROZEN">
        <a class="chipbtn" href="<c:url value='/jobs'><c:param name='weather' value='${w}'/></c:url>" <c:if test="${weather == w}">aria-current="true"</c:if>><c:out value="${w}"/></a>
      </c:forEach>
    </div>

    <h2 class="label">Hiring weather by company</h2>
    <c:forEach var="c" items="${companies}">
      <a class="row" href="<c:url value='/jobs'><c:param name='id' value='${c.id}'/></c:url>">
        <div class="grow">
          <div class="name"><c:out value="${c.name}"/></div>
          <div class="sub"><c:out value="${c.sector}"/> &middot; <c:out value="${c.city}"/></div>
        </div>
        <div class="end">
          <span class="wx ${c.weatherClass}"><i></i><c:out value="${c.weatherLabel}"/></span>
          <span class="days"><c:out value="${c.openings}"/> openings &middot; ~<c:out value="${c.avgDays}"/>d</span>
        </div>
      </a>
    </c:forEach>
    <c:if test="${empty companies}"><p class="empty">No company in this weather right now.</p></c:if>
  </c:otherwise>
</c:choose>

<%@ include file="_footer.jsp" %>
