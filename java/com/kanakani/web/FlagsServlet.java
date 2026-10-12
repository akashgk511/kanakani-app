package com.kanakani.web;

import com.kanakani.util.AppContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GET /api/flags?build=12 returns {"flags":{"dark_mode":true,"voice_notes":false}}.
 * The phone app asks this at start-up and shows only the features that are switched on.
 * Flag keys are limited to [a-z0-9_] by the database, so the JSON needs no escaping.
 */
@WebServlet("/api/flags")
public final class FlagsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int build = 0;
        try {
            build = Math.max(0, Integer.parseInt(req.getParameter("build")));
        } catch (NumberFormatException e) {
            // no build given: show only flags that every build supports
        }
        Map<String, Boolean> flags = AppContext.get().flags.flagsFor(build);
        StringBuilder json = new StringBuilder("{\"flags\":{");
        boolean first = true;
        for (Map.Entry<String, Boolean> e : flags.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(e.getKey()).append("\":").append(e.getValue());
        }
        json.append("}}");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getOutputStream().write(json.toString().getBytes(StandardCharsets.UTF_8));
    }
}
