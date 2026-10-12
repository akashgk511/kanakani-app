package com.kanakani.web;

import com.kanakani.model.RankRow;
import com.kanakani.model.User;
import com.kanakani.service.RankService;
import com.kanakani.util.AppContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/** GET /ranks?board=LIKED|VIEWED|ACCURATE|HEAVY|STREAK */
@WebServlet("/ranks")
public final class RankServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getAttribute("user");
        RankService ranks = AppContext.get().ranks;
        RankService.Board board = RankService.Board.fromParam(req.getParameter("board"));

        List<RankRow> rows = ranks.rows(board);
        req.setAttribute("boards", RankService.Board.values());
        req.setAttribute("board", board);
        req.setAttribute("podium", rows.size() > 3 ? rows.subList(0, 3) : rows);
        req.setAttribute("rest", rows.size() > 3 ? rows.subList(3, rows.size()) : List.of());
        req.setAttribute("max", rows.isEmpty() ? 1 : Math.max(1, rows.get(0).value()));
        req.setAttribute("standing", ranks.standing(board, user, rows));
        req.getRequestDispatcher("/WEB-INF/jsp/ranks.jsp").forward(req, resp);
    }
}
