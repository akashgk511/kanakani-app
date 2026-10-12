package com.kanakani.web;

import com.kanakani.model.Belief;
import com.kanakani.model.Topic;
import com.kanakani.model.User;
import com.kanakani.util.AppContext;
import com.kanakani.util.UserFacingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * GET  /feed          the feed, optionally filtered with ?topic=HIRING_BUZZ
 * POST /feed          write a post
 * POST /feed/like     like or unlike
 * POST /feed/belief   Pakki khabar or Gappa
 * POST /feed/delete   remove your own post
 *
 * Every POST passes AuthFilter first, so it already has a valid anti-forgery token.
 */
@WebServlet({"/feed", "/feed/*"})
public final class FeedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String FEED_JSP = "/WEB-INF/jsp/feed.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        show(req, resp, null, null);
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, String error, String draft) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        String topicParam = req.getParameter("topic");
        Topic filter = topicParam == null || topicParam.isBlank() ? null : Topic.fromForm(topicParam);

        req.setAttribute("posts", AppContext.get().feed.feed(user, filter));
        req.setAttribute("topics", Topic.values());
        req.setAttribute("filter", filter);
        req.setAttribute("notice", notice(req.getParameter("notice")));
        if (error != null) {
            req.setAttribute("error", error);
            req.setAttribute("draft", draft);
            req.setAttribute("needsConfirm", error.startsWith("Careful"));
        } else if (req.getParameter("about") != null) {
            // "Whisper about this company" from the Jobs page pre-fills the box. The text is escaped on output.
            req.setAttribute("draft", "About " + req.getParameter("about") + ": ");
        }
        req.getRequestDispatcher(FEED_JSP).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        AppContext ctx = AppContext.get();
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String back = req.getContextPath() + "/feed";

        switch (path) {
            case "" -> {
                String text = req.getParameter("text");
                try {
                    ctx.feed.post(user, text, Topic.fromForm(req.getParameter("topic")),
                        "on".equals(req.getParameter("rumour")), "yes".equals(req.getParameter("confirm")));
                    resp.sendRedirect(back + "?notice=posted");
                } catch (UserFacingException e) {
                    show(req, resp, e.getMessage(), text);
                }
            }
            case "/like" -> {
                UUID id = WebUtil.uuid(req.getParameter("postId"));
                if (id != null) {
                    ctx.feed.toggleLike(user, id);
                }
                resp.sendRedirect(back);
            }
            case "/belief" -> {
                UUID id = WebUtil.uuid(req.getParameter("postId"));
                String vote = req.getParameter("vote");
                if (id != null && ("PAKKI".equals(vote) || "GAPPA".equals(vote))) {
                    ctx.feed.believe(user, id, Belief.valueOf(vote));
                }
                resp.sendRedirect(back);
            }
            case "/delete" -> {
                UUID id = WebUtil.uuid(req.getParameter("postId"));
                if (id != null) {
                    ctx.feed.delete(user, id);
                }
                resp.sendRedirect(back + "?notice=deleted");
            }
            default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /** Only fixed codes are accepted, so no text from the URL is ever shown on the page. */
    private static String notice(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "posted" -> "Spilled. Others see only your secret name and gender.";
            case "deleted" -> "Post deleted. It also leaves the rankings.";
            case "opened" -> "The community vote is open. Rumours settle when 5 or more people agree.";
            case "voted" -> "Vote counted.";
            case "blocked" -> "That did not go through. Rumour votes need an account at least 7 days old, and you cannot vote on your own rumour or open a vote before 24 hours have passed.";
            case "came_true" -> "Settled: it came true. The author's truth score goes up.";
            case "busted" -> "Settled: busted. The author's truth score takes a bruise.";
            default -> null;
        };
    }
}
