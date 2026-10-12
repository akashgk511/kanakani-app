<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Security"/>
<c:set var="active" value="security"/>
<%@ include file="_header.jsp" %>

<section class="detail">
  <h1>Security Center</h1>
  <p class="small">What protects you, and where the limits are.</p>

  <h2 class="label">What the server does</h2>
  <div class="secrow"><div><b>Your number is sealed</b><div class="small">Turned into a one-way token with a secret key. The number is never stored.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Secret name</b><div class="small">Others see a made-up name, your gender and your days. Nothing else.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Posts stored encrypted</b><div class="small">AES-256-GCM. A copied row will not open under another post.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Sessions</b><div class="small">The cookie is HttpOnly, Secure and SameSite Strict. Only its hash is stored.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Slip-guard</b><div class="small">Warns before you post an email, phone number, link or employee ID. Checked again on the server.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Vanishing chat</b><div class="small">Messages are deleted after reading unless both people keep them.</div></div><span class="st on">On</span></div>
  <div class="secrow"><div><b>Screenshot shield</b><div class="small">The phone app uses the system block where the phone allows it. The website can only warn.</div></div><span class="st part">Partly</span></div>

  <h2 class="label">What you should know</h2>
  <p class="small">
    The text-message company sees your number when it delivers the code. If you post something only you could know,
    people can guess who you are. Slip-guard cannot catch that, so keep gossip general.
  </p>

  <h2 class="label">Your account</h2>
  <form method="post" action="<c:url value='/account/logout'/>">
    <input type="hidden" name="_csrf" value="${csrf}">
    <button class="btn ghost" type="submit">Sign out</button>
  </form>

  <form class="wipe" method="post" action="<c:url value='/account/wipe'/>">
    <input type="hidden" name="_csrf" value="${csrf}">
    <h2 class="label">Panic wipe</h2>
    <p class="small">Deletes your account, posts, likes and votes for good. This cannot be undone.</p>
    <c:if test="${not empty error}"><p class="err" role="alert"><c:out value="${error}"/></p></c:if>
    <label class="fieldlabel" for="confirm">Type WIPE to confirm</label>
    <input class="field" id="confirm" name="confirm" autocomplete="off">
    <button class="btn danger" type="submit">Wipe everything</button>
  </form>
</section>

<%@ include file="_footer.jsp" %>
