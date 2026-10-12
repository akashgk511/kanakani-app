package com.kanakani.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;

/** Small helpers shared by the servlets and filters. */
final class WebUtil {

    static final String SESSION_COOKIE = "kn_session";

    private WebUtil() { }

    static String cookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    /** HttpOnly (scripts cannot read it), Secure (HTTPS only) and SameSite=Strict (not sent from other sites). */
    static void setSessionCookie(HttpServletRequest req, HttpServletResponse resp, String token, int maxAgeSeconds) {
        Cookie c = new Cookie(SESSION_COOKIE, token);
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        c.setMaxAge(maxAgeSeconds);
        c.setAttribute("SameSite", "Strict");
        resp.addCookie(c);
    }

    static void clearSessionCookie(HttpServletRequest req, HttpServletResponse resp) {
        setSessionCookie(req, resp, "", 0);
    }

    /** Parses a UUID from a form field. Returns null for anything else, so bad input never causes an error page. */
    static UUID uuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Blocks requests started from another site. Browsers send Sec-Fetch-Site on every request;
     * if it is missing (old browsers) the Origin header must match this host.
     */
    static boolean sameOrigin(HttpServletRequest req) {
        String site = req.getHeader("Sec-Fetch-Site");
        if (site != null) {
            return "same-origin".equals(site) || "none".equals(site);
        }
        String origin = req.getHeader("Origin");
        if (origin == null) {
            return true;
        }
        String host = req.getScheme() + "://" + req.getServerName()
            + ((req.getServerPort() == 80 || req.getServerPort() == 443) ? "" : ":" + req.getServerPort());
        return origin.equals(host);
    }
}
