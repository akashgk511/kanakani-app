package com.kanakani.web;

import com.kanakani.model.Outcome;
import com.kanakani.model.Resolution;
import com.kanakani.model.User;
import com.kanakani.util.AppContext;
import com.kanakani.util.UserFacingException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Rumour Receipts.
 *   POST /receipt/open   the author opens the community vote on their own rumour
 *   POST /receipt/vote   a community member says whether it came true or was busted
 */
@WebServlet("/receipt/*")
public final class ReceiptServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = (User) req.getAttribute("user");
        AppContext ctx = AppContext.get();
        String back = req.getContextPath() + "/feed";
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();

        UUID postId = WebUtil.uuid(req.getParameter("postId"));
        Outcome outcome = parse(req.getParameter("outcome"));
        if (postId == null || outcome == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if ("/open".equals(path)) {
                ctx.receipts.openVote(user, postId, outcome);
                resp.sendRedirect(back + "?notice=opened");
            } else if ("/vote".equals(path)) {
                Resolution r = ctx.receipts.vote(user, postId, outcome);
                String code = switch (r) {
                    case CAME_TRUE -> "came_true";
                    case BUSTED -> "busted";
                    default -> "voted";
                };
                resp.sendRedirect(back + "?notice=" + code);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (UserFacingException e) {
            // The rule that blocked the request is explained on the feed page, not in a raw error.
            resp.sendRedirect(back + "?notice=blocked");
        }
    }

    private static Outcome parse(String value) {
        if ("CAME_TRUE".equals(value)) {
            return Outcome.CAME_TRUE;
        }
        return "BUSTED".equals(value) ? Outcome.BUSTED : null;
    }
}
