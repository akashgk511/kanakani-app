package com.kanakani.dao;

import com.kanakani.model.Outcome;
import com.kanakani.model.Resolution;
import com.kanakani.service.ReceiptRules;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Database side of Rumour Receipts. The rules live in ReceiptRules; these queries repeat the
 * important ones in SQL (24-hour wait, 7-day voters, no author votes) so a bug in a servlet cannot skip them.
 */
public final class ReceiptDao {

    /** Author opens the community vote. The suggested outcome is stored but never counted as a vote. */
    public boolean openVote(UUID postId, UUID authorId, Outcome suggestion) {
        String sql = "UPDATE posts SET proposed_outcome = ?::outcome_t, proposed_at = now() "
                   + "WHERE id = ? AND author_id = ? AND claim = 'RUMOUR' AND resolution = 'PENDING' "
                   + "AND proposed_at IS NULL AND settle_opens_at <= now() AND removed_at IS NULL";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, suggestion.name());
            ps.setObject(2, postId);
            ps.setObject(3, authorId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the vote", e);
        }
    }

    /** Records or changes a vote while the rumour is pending. Returns false if this person may not vote. */
    public boolean vote(UUID postId, UUID voterId, Outcome outcome) {
        String sql = "INSERT INTO receipt_votes (post_id, voter_id, outcome) "
                   + "SELECT p.id, ?, ?::outcome_t FROM posts p "
                   + "WHERE p.id = ? AND p.author_id <> ? AND p.proposed_at IS NOT NULL AND p.resolution = 'PENDING' "
                   + "AND p.removed_at IS NULL "
                   + "AND (SELECT count(*) FROM user_days d WHERE d.user_id = ?) >= ? "
                   + "ON CONFLICT (post_id, voter_id) DO UPDATE SET outcome = EXCLUDED.outcome, voted_at = now()";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, voterId);
            ps.setString(2, outcome.name());
            ps.setObject(3, postId);
            ps.setObject(4, voterId);
            ps.setObject(5, voterId);
            ps.setInt(6, ReceiptRules.MIN_VOTER_DAYS);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the vote", e);
        }
    }

    /** Returns {cameTrueVotes, bustedVotes}. */
    public int[] tally(UUID postId) {
        String sql = "SELECT count(*) FILTER (WHERE outcome = 'CAME_TRUE'), count(*) FILTER (WHERE outcome = 'BUSTED') "
                   + "FROM receipt_votes WHERE post_id = ?";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new int[] {rs.getInt(1), rs.getInt(2)};
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the votes", e);
        }
    }

    public Optional<Instant> voteOpenedAt(UUID postId) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement("SELECT proposed_at FROM posts WHERE id = ?")) {
            ps.setObject(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getTimestamp(1) != null) {
                    return Optional.of(rs.getTimestamp(1).toInstant());
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the vote", e);
        }
    }

    /** Writes the final answer. Only a PENDING rumour can be resolved, and only once. */
    public boolean resolve(UUID postId, Resolution resolution) {
        String sql = "UPDATE posts SET resolution = ?::resolution_t, resolved_at = now() WHERE id = ? AND resolution = 'PENDING'";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, resolution.name());
            ps.setObject(2, postId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not resolve the rumour", e);
        }
    }
}
