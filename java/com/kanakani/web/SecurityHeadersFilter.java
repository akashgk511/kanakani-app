package com.kanakani.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Adds the browser-side protections to every response.
 * The strict Content-Security-Policy means no inline scripts or styles: all JavaScript and CSS
 * live in static/js and static/css. That is why the JSP pages contain no script blocks.
 */
@WebFilter("/*")
public final class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        resp.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; "
          + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Referrer-Policy", "no-referrer");
        resp.setHeader("X-Frame-Options", "DENY");
        resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        resp.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        resp.setHeader("Cross-Origin-Opener-Policy", "same-origin");

        // Pages show private content, so browsers and proxies must not keep copies. Static files may be cached.
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (!path.startsWith("/static/")) {
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Pragma", "no-cache");
        }
        chain.doFilter(request, response);
    }
}
