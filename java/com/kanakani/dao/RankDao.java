package com.kanakani.dao;

import com.kanakani.model.Gender;
import com.kanakani.security.PostCrypto;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reads the ranking views from db/02_views_rankings.sql. */
public final class RankDao {

    public record PostRank(UUID postId, String alias, Gender gender, long value, PostCrypto.Sealed sealed) { }

    public record UserRank(String alias, Gender gender, long value, String detail, boolean oracle) { }

    /** Criterion 1 and 2. {@code byLikes} picks the column from a fixed list, never from user input. */
    public List<PostRank> topPosts(boolean byLikes, int limit) {
        String column = byLikes ? "s.likes" : "s.views";
        String sql = "SELECT p.id, u.alias, u.gender::text AS gender, " + column + " AS value, p.body_ct, p.body_iv, p.key_id "
                   + "FROM v_post_stats s JOIN posts p ON p.id = s.post_id JOIN users u ON u.id = p.author_id "
                   + "WHERE u.banned_at IS NULL ORDER BY " + column + " DESC, p.created_at DESC LIMIT ?";
        List<PostRank> out = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PostRank(rs.getObject("id", UUID.class), rs.getString("alias"), Gender.valueOf(rs.getString("gender")),
                            rs.getLong("value"), new PostCrypto.Sealed(rs.getInt("key_id"), rs.getBytes("body_iv"), rs.getBytes("body_ct"))));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the post ranking", e);
        }
        return out;
    }

    /** Criterion 3: most accurate, ordered by the Wilson lower bound so lucky streaks do not win. */
    public List<UserRank> mostAccurate(int limit) {
        String sql = "SELECT alias, gender::text AS gender, truth_pct, hits, settled, is_oracle "
                   + "FROM v_rank_most_accurate ORDER BY wilson_lower_bound DESC, settled DESC LIMIT ?";
        List<UserRank> out = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UserRank(rs.getString("alias"), Gender.valueOf(rs.getString("gender")), rs.getLong("truth_pct"),
                            rs.getLong("hits") + " of " + rs.getLong("settled") + " rumours came true", rs.getBoolean("is_oracle")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the accuracy ranking", e);
        }
        return out;
    }

    /** Criterion 4: heavy users by activity points. */
    public List<UserRank> heavyUsers(int limit) {
        String sql = "SELECT alias, gender::text AS gender, points, posts, msgs_sent, likes_given "
                   + "FROM v_rank_heavy_users ORDER BY points DESC LIMIT ?";
        List<UserRank> out = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UserRank(rs.getString("alias"), Gender.valueOf(rs.getString("gender")), rs.getLong("points"),
                            rs.getLong("posts") + " posts · " + rs.getLong("msgs_sent") + " messages · " + rs.getLong("likes_given") + " likes given", false));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the heavy-user ranking", e);
        }
        return out;
    }

    /** Criterion 5: longest streak, meaning days used. */
    public List<UserRank> longestStreak(int limit) {
        String sql = "SELECT alias, gender::text AS gender, days FROM v_rank_longest_streak ORDER BY days DESC LIMIT ?";
        List<UserRank> out = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new UserRank(rs.getString("alias"), Gender.valueOf(rs.getString("gender")), rs.getLong("days"), "days on Kanakani", false));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the streak ranking", e);
        }
        return out;
    }
}
