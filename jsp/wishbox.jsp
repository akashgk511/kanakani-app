<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Wishbox"/>
<c:set var="active" value="wishbox"/>
<%@ include file="_header.jsp" %>

<section class="infocard">
  <h1 class="label">Wishbox: shape the app</h1>
  <p class="small">
    Suggest something you want in Kanakani. Everyone votes yes or no, with a comment if they like.
    After 14 days, an idea with at least <c:out value="${minVoters}"/> voters and more yes than no moves to Coming soon.
    Once it is built, it switches on for everyone, with no store update needed for features already inside the app.
  </p>
</section>

<c:if test="${not empty notice}"><p class="notice" role="status"><c:out value="${notice}"/></p></c:if>
<c:if test="${not empty error}"><p class="err" role="alert"><c:out value="${error}"/></p></c:if>

<form class="pcompose" method="post" action="<c:url value='/wishbox/new'/>">
  <input type="hidden" name="_csrf" value="${csrf}">
  <label class="fieldlabel" for="ititle">Your idea, in a few words</label>
  <input class="field" id="ititle" name="title" maxlength="80" required value="<c:out value='${draftTitle}'/>" placeholder="Example: Dark mode for night shifts">
  <label class="fieldlabel" for="ibody">Details (optional)</label>
  <textarea class="field" id="ibody" name="body" rows="3" maxlength="500" placeholder="Why would it help?"><c:out value="${draftBody}"/></textarea>
  <div class="line">
    <span class="small">Needs an account at least <c:out value="${minDays}"/> days old. Shown under your secret name.</span>
    <button class="btn" type="submit">Add idea</button>
  </div>
</form>

<div class="chips" role="group" aria-label="Idea stages">
  <a class="chipbtn" href="<c:url value='/wishbox'/>" <c:if test="${tab == 'voting'}">aria-current="true"</c:if>>Voting</a>
  <a class="chipbtn" href="<c:url value='/wishbox'><c:param name='tab' value='soon'/></c:url>" <c:if test="${tab == 'soon'}">aria-current="true"</c:if>>Coming soon</a>
  <a class="chipbtn" href="<c:url value='/wishbox'><c:param name='tab' value='live'/></c:url>" <c:if test="${tab == 'live'}">aria-current="true"</c:if>>Live</a>
</div>

<c:forEach var="i" items="${ideas}">
  <article class="post idea" id="idea-${i.id}">
    <div class="phead">
      <div class="grow">
        <div class="name"><c:out value="${i.title}"/></div>
        <div class="sub">
          <span><c:out value="${i.alias}"/></span>
          <span class="gpill"><c:out value="${i.gender.label}"/></span>
          <span>&middot; <c:out value="${i.ago}"/></span>
          <c:if test="${i.mine}"><span class="pill">Yours</span></c:if>
        </div>
      </div>
      <span class="tagp"><c:out value="${i.status.label}"/></span>
    </div>

    <c:if test="${not empty i.body}"><p class="ptext"><c:out value="${i.body}"/></p></c:if>

    <p class="small">
      Yes <b><c:out value="${i.yes}"/></b> &middot; No <b><c:out value="${i.no}"/></b>
      <c:if test="${i.votingOpen}">&middot; <c:out value="${i.endsIn}"/></c:if>
      <c:if test="${not empty i.versionLabel}">&middot; in version <c:out value="${i.versionLabel}"/></c:if>
    </p>
    <progress class="bbar" value="${i.yesPercent}" max="100" aria-label="${i.yesPercent} percent say yes"></progress>

    <c:if test="${i.votingOpen and not i.mine}">
      <form class="ballot" method="post" action="<c:url value='/wishbox/vote'/>">
        <input type="hidden" name="_csrf" value="${csrf}">
        <input type="hidden" name="ideaId" value="${i.id}">
        <label class="fieldlabel" for="c-${i.id}">Comment (optional)</label>
        <input class="field" id="c-${i.id}" name="comment" maxlength="200" placeholder="Why yes or no?">
        <div class="belief">
          <button class="vote" name="vote" value="YES" type="submit" aria-pressed="${i.votedYes}">Yes, build it</button>
          <button class="vote" name="vote" value="NO" type="submit" aria-pressed="${i.votedNo}">No</button>
        </div>
      </form>
    </c:if>

    <c:forEach var="m" items="${i.comments}">
      <p class="whisper"><c:out value="${m.text}"/><br>
        <span class="small"><c:out value="${m.alias}"/> voted <c:out value="${m.vote}"/> &middot; <c:out value="${m.ago}"/></span></p>
    </c:forEach>
  </article>
</c:forEach>

<c:if test="${empty ideas}">
  <p class="empty">
    <c:choose>
      <c:when test="${tab == 'soon'}">Nothing approved yet. Vote on a few ideas first.</c:when>
      <c:when test="${tab == 'live'}">Nothing shipped from the Wishbox yet.</c:when>
      <c:otherwise>No ideas open for voting. Add the first one.</c:otherwise>
    </c:choose>
  </p>
</c:if>

<%@ include file="_footer.jsp" %>
