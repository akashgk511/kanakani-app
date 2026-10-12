package com.kanakani.web;

import com.kanakani.model.Company;
import com.kanakani.util.AppContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * GET /jobs                    all companies, with the hiring-weather filter
 * GET /jobs?weather=SUNNY      only companies in that weather
 * GET /jobs?id=3               one company in detail
 */
@WebServlet("/jobs")
public final class JobsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Set<String> WEATHER = Set.of("SUNNY", "CLOUDY", "STORMY", "FROZEN");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        AppContext ctx = AppContext.get();
        String idParam = req.getParameter("id");
        if (idParam != null) {
            Optional<Company> company = parseId(idParam).flatMap(ctx.jobs::detail);
            if (company.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            req.setAttribute("company", company.get());
        } else {
            String w = req.getParameter("weather");
            String weather = w != null && WEATHER.contains(w) ? w : null;
            req.setAttribute("companies", ctx.jobs.list(weather));
            req.setAttribute("weather", weather);
        }
        req.getRequestDispatcher("/WEB-INF/jsp/jobs.jsp").forward(req, resp);
    }

    private static Optional<Integer> parseId(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
