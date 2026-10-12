package com.kanakani.web;

import com.kanakani.model.Gender;
import com.kanakani.service.AuthService;
import com.kanakani.util.AppContext;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.UserFacingException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Two steps on one page: enter a number, then enter the code (and pick the gender to show on first sign-up).
 *
 * The number is kept in the temporary pre-login HttpSession for the few minutes between the two steps
 * and removed as soon as the code is checked. It is never written to the database or the logs.
 */
@WebServlet("/login")
public final class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private static final String PENDING_PHONE = "pending_phone";
    private static final String LOGIN_JSP = "/WEB-INF/jsp/login.jsp";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (AppContext.get().auth.userForSession(WebUtil.cookie(req, WebUtil.SESSION_COOKIE)).isPresent()) {
            resp.sendRedirect(req.getContextPath() + "/feed");
            return;
        }
        req.setAttribute("step", "phone");
        req.getRequestDispatcher(LOGIN_JSP).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AppContext ctx = AppContext.get();
        if (!WebUtil.sameOrigin(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Request blocked");
            return;
        }
        // The client address is used only for this in-memory counter. It is not stored or logged.
        if (!ctx.limiter.allow("ip:" + req.getRemoteAddr(), 20, Duration.ofMinutes(10))) {
            resp.sendError(429, "Too many requests");
            return;
        }

        String action = req.getParameter("action");
        HttpSession pre = req.getSession(true);
        try {
            if ("code".equals(action)) {
                String phone = req.getParameter("phone");
                ctx.auth.requestCode(phone);
                pre.setAttribute(PENDING_PHONE, phone);
                pre.setMaxInactiveInterval(10 * 60);
                req.setAttribute("step", "verify");
            } else if ("verify".equals(action)) {
                String phone = (String) pre.getAttribute(PENDING_PHONE);
                if (phone == null) {
                    throw new UserFacingException("Your sign-in expired. Enter your number again.");
                }
                AuthService.Login login = ctx.auth.verify(phone, req.getParameter("code"), Gender.fromForm(req.getParameter("gender")));
                pre.invalidate(); // drops the number from memory
                WebUtil.setSessionCookie(req, resp, login.sessionToken(), 30 * 24 * 3600);
                resp.sendRedirect(req.getContextPath() + "/feed");
                return;
            } else {
                req.setAttribute("step", "phone");
            }
        } catch (UserFacingException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("step", pre.getAttribute(PENDING_PHONE) != null && "verify".equals(action) ? "verify" : "phone");
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Login failed", e); // the message never contains the number
            req.setAttribute("error", "Something went wrong on our side. Please try again.");
            req.setAttribute("step", "phone");
        }
        req.getRequestDispatcher(LOGIN_JSP).forward(req, resp);
    }
}
