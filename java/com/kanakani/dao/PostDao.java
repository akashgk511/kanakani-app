package com.kanakani.dao;

import com.kanakani.model.Belief;
import com.kanakani.model.Claim;
import com.kanakani.model.Gender;
import com.kanakani.model.Outcome;
import com.kanakani.model.Resolution;
import com.kanakani.model.Topic;
import com.kanakani.security.PostCrypto;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Posts, views, likes and believe-o-meter votes. Text is stored sealed; this class never sees plaintext. */
public final class PostDao {

    /** A feed row before decryption. FeedService turns it into a PostCard. */
    public record Row(
            UUID id, String alias, Gender gender, int authorDays, Topic topic, Claim claim, Resolution resolution,
            Instant createdAt, PostCrypto.Sealed sealed, long likes, long views, long pakki, long gappa,
            boolean liked, Belief myBelief, boolean mine, boolean votingOpen, boolean canOpenVote,
            Outcome myReceipt, long votesCameTrue, long votesBusted) { }

    private static final String FEED_SQL =
        "SELECT p.id, p.topic::text AS topic, p.claim::text AS claim, p.resolution::text AS resolution, p.created_at, "
      + "       p.body_ct, p.body_iv, p.key_id, u.alias, u.gender::text AS gender, "
      + "       (SELECT count(*) FROM user_days d WHERE d.user_id = u.id) AS author_days, "
      + "       s.likes, s.views, s.pakki, s.gappa, "
      + "       EXISTS (SELECT 1 FROM post_likes l WHERE l.post_id = p.id AND l.user_id = ?) AS liked, "
      + "       (SELECT b.vote::text FROM belief_votes b WHERE b.post_id = p.id AND b.user_id = ?) AS my_belief, "
      + "       (p.author_id = ?) AS mine, "
      + "       (p.proposed_at IS NOT NULL AND p.resolution = 'PENDING') AS voting_open, "
      + "       (p.author_id = ? AND p.claim = 'RUMOUR' AND p.proposed_at IS NULL AND p.resolution = 'PENDING' "
      + "          AND p.settle_opens_at <= now()) AS can_open, "
      + "       (SELECT r.outcome::text FROM receipt_votes r WHERE r.post_id = p.id AND r.voter_id = ?) AS my_receipt, "
      + "       (SELECT count(*) FROM receipt_votes r WHERE r.post_id = p.id AND r.outcome = 'CAME_TRUE') AS rv_true, "
      + "       (SELECT count(*) FROM receipt_votes r WHERE r.post_id = p.id AND r.outcome = 'BUSTED') AS rv_busted "
      + "FROM posts p "
      + "JOIN users u ON u.id = p.author_id AND u.banned_at IS NULL "
      + "JOIN v_post_stats s ON s.post_id = p.id "
      + "WHERE (?::text IS NULL OR p.topic::text = ?) "
      + "ORDER BY p.created_at DESC LIMIT ?";

    public void create(UUID id, UUID authorId, Topic topic, Claim claim, PostCrypto.Sealed sealed, Instant settleOpensAt) {
        String sql = "INSERT INTO posts (id, author_id, topic, claim, body_ct, body_iv, key_id, settle_opens_at) "
                   + "VALUES (?, ?, ?::topic_t, ?::claim_t, ?, ?, ?, ?)";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.setObject(2, authorId);
            ps.setString(3, topic.name());
            ps.setString(4, claim.name());
            ps.setBytes(5, sealed.ciphertext());
            ps.setBytes(6, sealed.iv());
            ps.setShort(7, (short) sealed.keyId());
            ps.setTimestamp(8, Timestamp.from(settleOpensAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the post", e);
        }
    }

    public List<Row> feed(UUID viewerId, Topic topicOrNull, int limit) {
        List<Row> rows = new ArrayList<>();
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(FEED_SQL)) {
            for (int i = 1; i <= 5; i++) {
                ps.setObject(i, viewerId);
            }
            ps.setObject(6, topicOrNull == null ? null : topicOrNull.name(), Types.VARCHAR);
            ps.setObject(7, topicOrNull == null ? null : topicOrNull.name(), Types.VARCHAR);
            ps.setInt(8, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String belief = rs.getString("my_belief");
                    String receipt = rs.getString("my_receipt");
                    rows.add(new Row(
                        rs.getObject("id", UUID.class), rs.getString("alias"), Gender.valueOf(rs.getString("gender")),
                        rs.getInt("author_days"), Topic.valueOf(rs.getString("topic")), Claim.valueOf(rs.getString("claim")),
                        Resolution.valueOf(rs.getString("resolution")), rs.getTimestamp("created_at").toInstant(),
                        new PostCrypto.Sealed(rs.getInt("key_id"), rs.getBytes("body_iv"), rs.getBytes("body_ct")),
                        rs.getLong("likes"), rs.getLong("views"), rs.getLong("pakki"), rs.getLong("gappa"),
                        rs.getBoolean("liked"), belief == null ? null : Belief.valueOf(belief),
                        rs.getBoolean("mine"), rs.getBoolean("voting_open"), rs.getBoolean("can_open"),
                        receipt == null ? null : Outcome.valueOf(receipt), rs.getLong("rv_true"), rs.getLong("rv_busted")));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the feed", e);
        }
        return rows;
    }

    /** A view counts once per person and never for the author, so refreshing cannot inflate the numbers. */
    public void recordViews(UUID viewerId, List<UUID> postIds) {
        if (postIds.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO post_views (post_id, user_id) "
                   + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM posts WHERE id = ? AND author_id = ?) "
                   + "ON CONFLICT DO NOTHING";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (UUID postId : postIds) {
                ps.setObject(1, postId);
                ps.setObject(2, viewerId);
                ps.setObject(3, postId);
                ps.setObject(4, viewerId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DataAccessException("Could not record views", e);
        }
    }

    /** Likes or unlikes. Returns true if the post is liked afterwards. Authors cannot like their own posts. */
    public boolean toggleLike(UUID postId, UUID userId) {
        try (Connection c = Db.connection()) {
            try (PreparedStatement del = c.prepareStatement("DELETE FROM post_likes WHERE post_id = ? AND user_id = ?")) {
                del.setObject(1, postId);
                del.setObject(2, userId);
                if (del.executeUpdate() > 0) {
                    return false;
                }
            }
            String ins = "INSERT INTO post_likes (post_id, user_id) "
                       + "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM posts WHERE id = ? AND author_id = ?) "
                       + "ON CONFLICT DO NOTHING";
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setObject(1, postId);
                ps.setObject(2, userId);
                ps.setObject(3, postId);
                ps.setObject(4, userId);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the like", e);
        }
    }

    public Optional<Belief> myBelief(UUID postId, UUID userId) {
        String sql = "SELECT vote::text FROM belief_votes WHERE post_id = ? AND user_id = ?";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, postId);
            ps.setObject(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(Belief.valueOf(rs.getString(1))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the vote", e);
        }
    }

    public void setBelief(UUID postId, UUID userId, Belief vote) {
        String sql = "INSERT INTO belief_votes (post_id, user_id, vote) "
                   + "SELECT ?, ?, ?::belief_t WHERE NOT EXISTS (SELECT 1 FROM posts WHERE id = ? AND author_id = ?) "
                   + "ON CONFLICT (post_id, user_id) DO UPDATE SET vote = EXCLUDED.vote";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, postId);
            ps.setObject(2, userId);
            ps.setString(3, vote.name());
            ps.setObject(4, postId);
            ps.setObject(5, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the vote", e);
        }
    }

    public void clearBelief(UUID postId, UUID userId) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM belief_votes WHERE post_id = ? AND user_id = ?")) {
            ps.setObject(1, postId);
            ps.setObject(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clear the vote", e);
        }
    }

    /** Soft delete. The post leaves the feed and the rankings, and no longer counts toward truth scores. */
    public boolean remove(UUID postId, UUID authorId) {
        String sql = "UPDATE posts SET removed_at = now() WHERE id = ? AND author_id = ? AND removed_at IS NULL";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, postId);
            ps.setObject(2, authorId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the post", e);
        }
    }
}
