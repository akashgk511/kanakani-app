package com.kanakani.dao;

import com.kanakani.model.Gender;
import com.kanakani.model.User;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Sessions are stored by the SHA-256 of the cookie token, never the token itself. */
public final class SessionDao {

    public void create(String tokenHash, UUID userId, Instant expiresAt) {
        String sql = "INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, ?)";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setObject(2, userId);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the session", e);
        }
    }

    public Optional<User> findUser(String tokenHash) {
        String sql = "SELECT u.id, u.alias, u.gender::text AS gender, "
                   + "(SELECT count(*) FROM user_days d WHERE d.user_id = u.id) AS days "
                   + "FROM sessions s JOIN users u ON u.id = s.user_id "
                   + "WHERE s.token_hash = ? AND s.expires_at > now() AND u.banned_at IS NULL";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(rs.getObject("id", UUID.class), rs.getString("alias"),
                        Gender.valueOf(rs.getString("gender")), rs.getInt("days")));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the session", e);
        }
    }

    public void delete(String tokenHash) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM sessions WHERE token_hash = ?")) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not end the session", e);
        }
    }
}
