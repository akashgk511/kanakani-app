<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Feed"/>
<c:set var="active" value="feed"/>
<%@ include file="_header.jsp" %>

<c:if test="${not empty notice}"><p class="notice" role="status"><c:out value="${notice}"/></p></c:if>

<%-- Composer. Every form carries the anti-forgery token. All text is escaped with c:out. --%>
<form class="pcompose" method="post" action="<c:url value='/feed'/>" id="postform">
  <input type="hidden" name="_csrf" value="${csrf}">
  <c:if test="${needsConfirm}"><input type="hidden" name="confirm" value="yes"></c:if>

  <label class="fieldlabel" for="postdraft">Spill the chai</label>
  <textarea class="field" id="postdraft" name="text" rows="3" maxlength="280"
            placeholder="Whisper it here. Your name stays in the matka."><c:out value="${draft}"/></textarea>

  <div class="row2">
    <label for="topic" class="small">Topic</label>
    <select id="topic" name="topic" class="field">
      <c:forEach var="t" items="${topics}">
        <option value="${t}" <c:if test="${t == 'HIRING_BUZZ' and not empty param.about}">selected</c:if>><c:out value="${t.label}"/></option>
      </c:forEach>
    </select>
  </div>
  <label class="check">
    <input type="checkbox" name="rumour"> This is a rumour that can be checked later (it can earn a receipt)
  </label>

  <p class="err" id="slip" role="alert"><c:if test="${not empty error}"><c:out value="${error}"/></c:if></p>
  <div class="line">
    <span class="small">As <c:out value="${user.alias}"/> &middot; <c:out value="${user.gender.label}"/></span>
    <button class="btn" id="postbtn" type="submit"><c:choose><c:when test="${needsConfirm}">Post anyway</c:when><c:otherwise>Post</c:otherwise></c:choose></button>
  </div>
  <p class="small">Stored encrypted. Others see only your secret name and gender.</p>
</form>

<%-- Topic filter --%>
<div class="chips" role="group" aria-label="Filter by topic">
  <a class="chipbtn" href="<c:url value='/feed'/>" <c:if test="${empty filter}">aria-current="true"</c:if>>Everything</a>
  <c:forEach var="t" items="${topics}">
    <a class="chipbtn" href="<c:url value='/feed'><c:param name='topic' value='${t}'/></c:url>" <c:if test="${filter == t}">aria-current="true"</c:if>><c:out value="${t.label}"/></a>
  </c:forEach>
</div>

<h2 class="label">Fresh off the tapri</h2>

<c:forEach var="p" items="${posts}">
  <article class="post" id="post-${p.id}">
    <div class="phead">
      <div class="avatar sm" aria-hidden="true"><c:out value="${p.alias.substring(p.alias.indexOf(' ') + 1, p.alias.indexOf(' ') + 2)}"/></div>
      <div class="grow">
        <div class="name"><c:out value="${p.alias}"/><c:if test="${p.mine}"> <span class="pill">You</span></c:if></div>
        <div class="sub">
          <span class="gpill"><c:out value="${p.gender.label}"/></span>
          <span class="days"><c:out value="${p.authorDays}"/>d</span>
          <span>&middot; <c:out value="${p.ago}"/></span>
        </div>
      </div>
      <c:if test="${p.mine}">
        <form method="post" action="<c:url value='/feed/delete'/>">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}">
          <button class="mini" type="submit">Delete</button>
        </form>
      </c:if>
    </div>

    <div class="tagrow">
      <span class="tagp"><c:out value="${p.topic.label}"/></span>
      <c:if test="${p.cameTrue}"><span class="stamp true">Came true</span></c:if>
      <c:if test="${p.busted}"><span class="stamp busted">Busted</span></c:if>
      <c:if test="${p.unclear}"><span class="stamp">Unclear</span></c:if>
      <c:if test="${p.rumour and not p.cameTrue and not p.busted and not p.unclear}"><span class="tagp">Rumour</span></c:if>
    </div>

    <p class="ptext"><c:out value="${p.body}"/></p>

    <%-- Believe-o-meter --%>
    <div class="belief">
      <form method="post" action="<c:url value='/feed/belief'/>">
        <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}"><input type="hidden" name="vote" value="PAKKI">
        <button class="vote" type="submit" aria-pressed="${p.believedPakki}" <c:if test="${p.mine}">disabled</c:if>>Pakki khabar <b><c:out value="${p.pakki}"/></b></button>
      </form>
      <form method="post" action="<c:url value='/feed/belief'/>">
        <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}"><input type="hidden" name="vote" value="GAPPA">
        <button class="vote" type="submit" aria-pressed="${p.believedGappa}" <c:if test="${p.mine}">disabled</c:if>>Gappa <b><c:out value="${p.gappa}"/></b></button>
      </form>
    </div>
    <progress class="bbar" value="${p.pakkiPercent}" max="100" aria-label="${p.pakkiPercent} percent say pakki khabar"></progress>

    <%-- Rumour Receipts: the author opens the vote, the community decides --%>
    <c:if test="${p.canOpenVote}">
      <div class="settle">
        Did it play out? Ask the community to settle it:
        <form method="post" action="<c:url value='/receipt/open'/>">
          <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}">
          <button class="mini" name="outcome" value="CAME_TRUE" type="submit">I think it came true</button>
          <button class="mini" name="outcome" value="BUSTED" type="submit">I think it was busted</button>
        </form>
      </div>
    </c:if>
    <c:if test="${p.votingOpen}">
      <div class="settle">
        Community vote open &middot; came true <c:out value="${p.votesCameTrue}"/> &middot; busted <c:out value="${p.votesBusted}"/>
        <c:if test="${not p.mine}">
          <form method="post" action="<c:url value='/receipt/vote'/>">
            <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}">
            <button class="mini" name="outcome" value="CAME_TRUE" type="submit" aria-pressed="${p.myVoteCameTrue}">It came true</button>
            <button class="mini" name="outcome" value="BUSTED" type="submit" aria-pressed="${p.myVoteBusted}">It was busted</button>
          </form>
        </c:if>
      </div>
    </c:if>

    <div class="pfoot">
      <form method="post" action="<c:url value='/feed/like'/>">
        <input type="hidden" name="_csrf" value="${csrf}"><input type="hidden" name="postId" value="${p.id}">
        <button class="act" type="submit" aria-pressed="${p.liked}" <c:if test="${p.mine}">disabled</c:if>>Like <c:out value="${p.likes}"/></button>
      </form>
      <span class="act static">Views <c:out value="${p.views}"/></span>
    </div>
  </article>
</c:forEach>

<c:if test="${empty posts}"><p class="empty">Nobody has gossiped about this yet. Be the first.</p></c:if>

<%@ include file="_footer.jsp" %>
