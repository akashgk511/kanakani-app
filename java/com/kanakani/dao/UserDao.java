package com.kanakani.dao;

import com.kanakani.model.Gender;
import com.kanakani.model.User;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class UserDao {

    private static final String SELECT_USER =
        "SELECT u.id, u.alias, u.gender::text AS gender, "
      + "(SELECT count(*) FROM user_days d WHERE d.user_id = u.id) AS days FROM users u ";

    public Optional<User> findByPhoneToken(String phoneToken) {
        return one(SELECT_USER + "WHERE u.phone_token = ? AND u.banned_at IS NULL", phoneToken);
    }

    public Optional<User> findById(UUID id) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(SELECT_USER + "WHERE u.id = ? AND u.banned_at IS NULL")) {
            ps.setObject(1, id);
            return map(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read user", e);
        }
    }

    private Optional<User> one(String sql, String arg) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, arg);
            return map(ps);
        } catch (SQLException e) {
            throw new DataAccessException("Could not read user", e);
        }
    }

    private Optional<User> map(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new User(
                rs.getObject("id", UUID.class),
                rs.getString("alias"),
                Gender.valueOf(rs.getString("gender")),
                rs.getInt("days")));
        }
    }

    /** Creates a user. Throws DataAccessException with isUniqueViolation() when the alias is taken. */
    public UUID create(String phoneToken, String alias, Gender gender) {
        String sql = "INSERT INTO users (phone_token, alias, gender) VALUES (?, ?, ?::gender_t) RETURNING id";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, phoneToken);
            ps.setString(2, alias);
            ps.setString(3, gender.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getObject(1, UUID.class);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create user", e);
        }
    }

    /** Records that the user opened the app today. Safe to call many times a day. */
    public void touchToday(UUID userId) {
        String sql = "INSERT INTO user_days (user_id, day) VALUES (?, CURRENT_DATE) ON CONFLICT DO NOTHING";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not record the day", e);
        }
    }

    public void addMessages(UUID userId, int count) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement("UPDATE users SET msgs_sent = msgs_sent + ? WHERE id = ?")) {
            ps.setInt(1, count);
            ps.setObject(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the message count", e);
        }
    }

    /** Panic wipe on the server side: every post, like, vote and session of this user goes with the row (ON DELETE CASCADE). */
    public void deleteAccount(UUID userId) {
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id = ?")) {
            ps.setObject(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the account", e);
        }
    }
}
