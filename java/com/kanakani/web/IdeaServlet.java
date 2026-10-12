package com.kanakani.web;

import com.kanakani.model.Ballot;
import com.kanakani.model.User;
import com.kanakani.service.IdeaRules;
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
 * Wishbox.
 *   GET  /wishbox?tab=voting|soon|live
 *   POST /wishbox/new    suggest an improvement
 *   POST /wishbox/vote   vote yes or no, with an optional comment
 */
@WebServlet({"/wishbox", "/wishbox/*"})
public final class IdeaServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        String tab = tab(req.getParameter("tab"));
        req.setAttribute("tab", tab);
        req.setAttribute("ideas", AppContext.get().ideas.list(tab, user));
        req.setAttribute("minVoters", IdeaRules.MIN_VOTERS);
        req.setAttribute("minDays", IdeaRules.MIN_DAYS_TO_VOTE);
        String flash = flash(req.getParameter("notice"));
        if (flash != null) {
            req.setAttribute("notice", flash);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/wishbox.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        AppContext ctx = AppContext.get();
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String back = req.getContextPath() + "/wishbox";
        try {
            if ("/new".equals(path)) {
                ctx.ideas.suggest(user, req.getParameter("title"), req.getParameter("body"));
                resp.sendRedirect(back + "?notice=added");
            } else if ("/vote".equals(path)) {
                UUID id = WebUtil.uuid(req.getParameter("ideaId"));
                Ballot ballot = Ballot.fromForm(req.getParameter("vote"));
                if (id == null || ballot == null) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                ctx.ideas.vote(user, id, ballot, req.getParameter("comment"));
                resp.sendRedirect(back + "?notice=voted");
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (UserFacingException e) {
            // The message is written by our own code and is safe to show. It is shown as text, never as HTML.
            req.setAttribute("error", e.getMessage());
            req.setAttribute("tab", "voting");
            req.setAttribute("ideas", ctx.ideas.list("voting", user));
            req.setAttribute("minVoters", IdeaRules.MIN_VOTERS);
            req.setAttribute("minDays", IdeaRules.MIN_DAYS_TO_VOTE);
            req.setAttribute("draftTitle", req.getParameter("title"));
            req.setAttribute("draftBody", req.getParameter("body"));
            req.getRequestDispatcher("/WEB-INF/jsp/wishbox.jsp").forward(req, resp);
        }
    }

    private static String tab(String p) {
        return "soon".equals(p) || "live".equals(p) ? p : "voting";
    }

    private static String flash(String code) {
        if ("added".equals(code)) {
            return "Idea added. Voting stays open for 14 days.";
        }
        return "voted".equals(code) ? "Vote counted." : null;
    }
}
