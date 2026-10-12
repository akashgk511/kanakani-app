package com.kanakani.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * Picks the language for each request: the ?lang= choice first, then the saved cookie, then English.
 * The page shows one language at a time. Only the three listed codes are ever accepted, so nothing
 * from the URL or cookie reaches the page unchecked. The choice is stored in a cookie called kn_lang.
 */
@WebFilter(urlPatterns = "/*")
public final class LangFilter implements Filter {

    public static final String COOKIE = "kn_lang";
    public static final String ATTR = "lang";
    private static final Set<String> SUPPORTED = Set.of("en", "bn", "hi");
    private static final int ONE_YEAR = 365 * 24 * 60 * 60;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String chosen = req.getParameter("lang");
        if ("GET".equals(req.getMethod()) && chosen != null && SUPPORTED.contains(chosen)) {
            Cookie c = new Cookie(COOKIE, chosen);
            c.setHttpOnly(true);
            c.setSecure(true);
            c.setPath("/");
            c.setMaxAge(ONE_YEAR);
            c.setAttribute("SameSite", "Lax");
            resp.addCookie(c);
            // Drop the parameter so the address stays clean and the back button does not flip the language again.
            resp.sendRedirect(req.getRequestURI());
            return;
        }

        String saved = WebUtil.cookie(req, COOKIE);
        req.setAttribute(ATTR, saved != null && SUPPORTED.contains(saved) ? saved : "en");
        chain.doFilter(request, response);
    }
}
