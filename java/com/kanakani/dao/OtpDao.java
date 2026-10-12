package com.kanakani.dao;

import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public final class OtpDao {

    public record Challenge(long id, String mac, Instant expiresAt, int attempts, boolean consumed) { }

    public void create(String phoneToken, String mac, Instant expiresAt) {
        String sql = "INSERT INTO otp_challenges (phone_token, otp_mac, expires_at) VALUES (?, ?, ?)";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phoneToken);
            ps.setString(2, mac);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not store the code", e);
        }
    }

    /** How many codes were requested for this number since the given time. Backs up the in-memory rate limit. */
    public int countSince(String phoneToken, Instant since) {
        String sql = "SELECT count(*) FROM otp_challenges WHERE phone_token = ? AND created_at > ?";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phoneToken);
            ps.setTimestamp(2, Timestamp.from(since));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count codes", e);
        }
    }

    public Optional<Challenge> latest(String phoneToken) {
        String sql = "SELECT id, otp_mac, expires_at, attempts, consumed FROM otp_challenges "
                   + "WHERE phone_token = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phoneToken);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Challenge(rs.getLong("id"), rs.getString("otp_mac"),
                        rs.getTimestamp("expires_at").toInstant(), rs.getInt("attempts"), rs.getBoolean("consumed")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the code", e);
        }
    }

    public void addAttempt(long id) {
        run("UPDATE otp_challenges SET attempts = attempts + 1 WHERE id = ?", id);
    }

    public void consume(long id) {
        run("UPDATE otp_challenges SET consumed = true WHERE id = ?", id);
    }

    private void run(String sql, long id) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the code", e);
        }
    }
}
