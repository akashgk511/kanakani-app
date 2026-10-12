package com.kanakani.web;

import com.kanakani.model.User;
import com.kanakani.util.AppContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Guards every signed-in page. It finds the person from the session cookie, puts them on the request
 * as "user", puts the anti-forgery token on the request as "csrf", and rejects any POST without a valid token.
 */
@WebFilter(urlPatterns = {"/feed", "/feed/*", "/ranks", "/jobs", "/receipt/*", "/account", "/account/*", "/wishbox", "/wishbox/*", "/api/flags"})
public final class AuthFilter implements Filter {

    static final String USER = "user";
    static final String CSRF = "csrf";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        AppContext ctx = AppContext.get();

        String sessionToken = WebUtil.cookie(req, WebUtil.SESSION_COOKIE);
        Optional<User> user = ctx.auth.userForSession(sessionToken);
        if (user.isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if ("POST".equals(req.getMethod())) {
            if (!WebUtil.sameOrigin(req) || !Csrf.valid(ctx.csrfKey, sessionToken, req.getParameter("_csrf"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Request blocked");
                return;
            }
        }
        req.setAttribute(USER, user.get());
        req.setAttribute(CSRF, Csrf.token(ctx.csrfKey, sessionToken));
        chain.doFilter(request, response);
    }
}
