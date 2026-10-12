package com.kanakani.service;

import com.kanakani.dao.RankDao;
import com.kanakani.model.RankRow;
import com.kanakani.model.User;
import com.kanakani.security.PostCrypto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Builds the five ranking pages. */
public final class RankService {

    public enum Board {
        LIKED("Most liked", "likes", "Single posts ranked by likes. One post, one place."),
        VIEWED("Most viewed", "views", "Single posts ranked by how many different people opened them."),
        ACCURATE("Most accurate", "% true", "Who is right most often. The community settles each rumour. You need 3 settled rumours to enter."),
        HEAVY("Heavy users", "points", "Who uses Kanakani the most. 5 points per post, 2 per chat message, 1 per like given and 1 per day here."),
        STREAK("Longest streak", "days", "Who has been on Kanakani the most days.");

        public final String title;
        public final String unit;
        public final String note;

        Board(String title, String unit, String note) {
            this.title = title;
            this.unit = unit;
            this.note = note;
        }

        public static Board fromParam(String p) {
            if (p == null) {
                return LIKED;
            }
            try {
                return valueOf(p.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return LIKED;
            }
        }
    }

    private static final int SIZE = 10;

    private final RankDao ranks;
    private final PostCrypto crypto;

    public RankService(RankDao ranks, PostCrypto crypto) {
        this.ranks = ranks;
        this.crypto = crypto;
    }

    public List<RankRow> rows(Board board) {
        List<RankRow> out = new ArrayList<>();
        switch (board) {
            case LIKED, VIEWED -> {
                int rank = 1;
                for (RankDao.PostRank p : ranks.topPosts(board == Board.LIKED, SIZE)) {
                    out.add(new RankRow(rank++, p.alias(), p.gender(), p.value(), snippet(crypto.open(p.sealed(), p.postId().toString())), false));
                }
            }
            case ACCURATE -> addUsers(out, ranks.mostAccurate(SIZE));
            case HEAVY -> addUsers(out, ranks.heavyUsers(SIZE));
            case STREAK -> addUsers(out, ranks.longestStreak(SIZE));
        }
        return out;
    }

    /** A plain sentence about where the viewer stands, shown above the podium. */
    public String standing(Board board, User viewer, List<RankRow> rows) {
        for (RankRow r : rows) {
            if (r.alias().equals(viewer.alias())) {
                String what = board == Board.LIKED || board == Board.VIEWED ? "Your best post is #" : "You are #";
                return what + r.rank() + " with " + r.value() + " " + board.unit + ".";
            }
        }
        return switch (board) {
            case LIKED, VIEWED -> "None of your posts is in the top " + SIZE + " yet. Spill something in Feed to get on this list.";
            case ACCURATE -> "Get 3 of your rumours settled by the community to enter this list.";
            default -> "You are not in the top " + SIZE + " yet.";
        };
    }

    private static void addUsers(List<RankRow> out, List<RankDao.UserRank> users) {
        int rank = 1;
        for (RankDao.UserRank u : users) {
            out.add(new RankRow(rank++, u.alias(), u.gender(), u.value(), u.detail(), u.oracle()));
        }
    }

    private static String snippet(String body) {
        return body.length() <= 70 ? body : body.substring(0, 67) + "...";
    }
}
