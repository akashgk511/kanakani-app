package com.kanakani.dao;

import com.kanakani.model.Ballot;
import com.kanakani.model.Gender;
import com.kanakani.model.IdeaCard;
import com.kanakani.model.IdeaComment;
import com.kanakani.model.IdeaStatus;
import com.kanakani.service.IdeaRules;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Database side of Wishbox. */
public final class IdeaDao {

    /** Adds an idea. Returns false if the author is too new or already has 3 open ideas. */
    public boolean create(UUID authorId, String title, String body) {
        String sql = "INSERT INTO feature_ideas (author_id, title, body) "
                   + "SELECT ?, ?, ? WHERE (SELECT count(*) FROM user_days WHERE user_id = ?) >= ? "
                   + "AND (SELECT count(*) FROM feature_ideas WHERE author_id = ? AND status = 'VOTING' AND removed_at IS NULL) < ?";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, authorId);
            ps.setString(2, title);
            ps.setString(3, body);
            ps.setObject(4, authorId);
            ps.setInt(5, IdeaRules.MIN_DAYS_TO_VOTE);
            ps.setObject(6, authorId);
            ps.setInt(7, IdeaRules.MAX_OPEN_PER_AUTHOR);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the idea", e);
        }
    }

    /** Records or changes a vote while voting is open. False if this person may not vote. */
    public boolean vote(UUID ideaId, UUID voterId, Ballot ballot, String comment) {
        String sql = "INSERT INTO idea_votes (idea_id, voter_id, vote, comment) "
                   + "SELECT i.id, ?, ?::yesno_t, ? FROM feature_ideas i "
                   + "WHERE i.id = ? AND i.author_id <> ? AND i.status = 'VOTING' AND i.voting_ends_at > now() "
                   + "AND i.removed_at IS NULL "
                   + "AND (SELECT count(*) FROM user_days d WHERE d.user_id = ?) >= ? "
                   + "ON CONFLICT (idea_id, voter_id) DO UPDATE SET vote = EXCLUDED.vote, "
                   + "comment = COALESCE(EXCLUDED.comment, idea_votes.comment), voted_at = now()";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, voterId);
            ps.setString(2, ballot.name());
            ps.setString(3, comment);
            ps.setObject(4, ideaId);
            ps.setObject(5, voterId);
            ps.setObject(6, voterId);
            ps.setInt(7, IdeaRules.MIN_DAYS_TO_VOTE);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the vote", e);
        }
    }

    /** Ideas for one tab. Tabs: "voting", "soon", "live". Comments are loaded for the same ideas. */
    public List<IdeaCard> list(String tab, UUID viewerId) {
        String where = switch (tab) {
            case "soon" -> "i.status IN ('APPROVED','BUILDING')";
            case "live" -> "i.status = 'SHIPPED'";
            default -> "i.status = 'VOTING'";
        };
        String sql = "SELECT i.id, i.author_id, u.alias, u.gender::text AS gender, i.title, i.body, i.status::text AS status, "
                   + "i.created_at, i.voting_ends_at, i.version_label, s.yes_votes, s.no_votes, "
                   + "(SELECT vote::text FROM idea_votes m WHERE m.idea_id = i.id AND m.voter_id = ?) AS my_vote "
                   + "FROM feature_ideas i JOIN users u ON u.id = i.author_id JOIN v_idea_stats s ON s.id = i.id "
                   + "WHERE i.removed_at IS NULL AND " + where + " "
                   + "ORDER BY s.yes_votes DESC, i.created_at DESC LIMIT 50";
        List<Object[]> raw = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, viewerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID id = rs.getObject("id", UUID.class);
                    ids.add(id);
                    raw.add(new Object[] {id, rs.getObject("author_id", UUID.class), rs.getString("alias"), rs.getString("gender"),
                        rs.getString("title"), rs.getString("body"), rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("voting_ends_at").toInstant(),
                        rs.getString("version_label"), rs.getInt("yes_votes"), rs.getInt("no_votes"), rs.getString("my_vote")});
                }
            }
            Map<UUID, List<IdeaComment>> comments = comments(c, ids);
            List<IdeaCard> out = new ArrayList<>();
            Instant now = Instant.now();
            for (Object[] r : raw) {
                UUID id = (UUID) r[0];
                IdeaStatus status = IdeaStatus.valueOf((String) r[6]);
                Instant ends = (Instant) r[8];
                out.add(new IdeaCard(id, (String) r[2], Gender.valueOf((String) r[3]), (String) r[4], (String) r[5], status,
                    (Integer) r[10], (Integer) r[11], r[12] == null ? null : Ballot.valueOf((String) r[12]),
                    viewerId.equals(r[1]), status == IdeaStatus.VOTING && ends.isAfter(now),
                    ago((Instant) r[7], now), left(ends, now), (String) r[9],
                    comments.getOrDefault(id, List.of())));
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load ideas", e);
        }
    }

    private static Map<UUID, List<IdeaComment>> comments(Connection c, List<UUID> ids) throws SQLException {
        Map<UUID, List<IdeaComment>> out = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return out;
        }
        String sql = "SELECT v.idea_id, u.alias, v.vote::text AS vote, v.comment, v.voted_at FROM idea_votes v "
                   + "JOIN users u ON u.id = v.voter_id WHERE v.idea_id = ANY (?) AND v.comment IS NOT NULL "
                   + "ORDER BY v.voted_at DESC";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, c.createArrayOf("uuid", ids.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                Instant now = Instant.now();
                while (rs.next()) {
                    List<IdeaComment> list = out.computeIfAbsent(rs.getObject("idea_id", UUID.class), k -> new ArrayList<>());
                    if (list.size() < 5) {
                        list.add(new IdeaComment(rs.getString("alias"), Ballot.valueOf(rs.getString("vote")),
                            rs.getString("comment"), ago(rs.getTimestamp("voted_at").toInstant(), now)));
                    }
                }
            }
        }
        return out;
    }

    static String ago(Instant then, Instant now) {
        long m = Duration.between(then, now).toMinutes();
        if (m < 1) {
            return "just now";
        }
        if (m < 60) {
            return m + " min ago";
        }
        if (m < 1440) {
            return (m / 60) + " h ago";
        }
        return (m / 1440) + " d ago";
    }

    static String left(Instant ends, Instant now) {
        long h = Duration.between(now, ends).toHours();
        if (h <= 0) {
            return "closing now";
        }
        return h < 48 ? h + " h left" : (h / 24) + " d left";
    }
}
