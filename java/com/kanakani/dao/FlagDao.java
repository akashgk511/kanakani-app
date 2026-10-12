package com.kanakani.dao;

import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Remote feature switches. */
public final class FlagDao {

    /** Flag key to enabled, for every flag whose oldest supported build is not newer than the caller's. */
    public Map<String, Boolean> flagsFor(int appBuild) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        String sql = "SELECT flag_key, enabled FROM feature_flags WHERE min_build <= ? ORDER BY flag_key";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, appBuild);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getBoolean(2));
                }
            }
            return out;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read feature flags", e);
        }
    }
}
