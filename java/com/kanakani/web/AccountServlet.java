package com.kanakani.web;

import com.kanakani.model.User;
import com.kanakani.util.AppContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET  /account          the Security Center page
 * POST /account/logout   end this session
 * POST /account/wipe     panic wipe: delete the account and everything on it. Needs the word WIPE typed in.
 */
@WebServlet({"/account", "/account/*"})
public final class AccountServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/security.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        AppContext ctx = AppContext.get();
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        String sessionToken = WebUtil.cookie(req, WebUtil.SESSION_COOKIE);

        if ("/logout".equals(path)) {
            ctx.auth.logout(sessionToken);
            WebUtil.clearSessionCookie(req, resp);
            resp.sendRedirect(req.getContextPath() + "/login");
        } else if ("/wipe".equals(path)) {
            if (!"WIPE".equals(req.getParameter("confirm"))) {
                req.setAttribute("error", "Type WIPE in capital letters to confirm.");
                req.getRequestDispatcher("/WEB-INF/jsp/security.jsp").forward(req, resp);
                return;
            }
            ctx.auth.deleteAccount(user);
            WebUtil.clearSessionCookie(req, resp);
            resp.sendRedirect(req.getContextPath() + "/login");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
