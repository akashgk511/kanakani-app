package com.kanakani.dao;

import com.kanakani.model.Company;
import com.kanakani.util.DataAccessException;
import com.kanakani.util.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Company hiring information for the Jobs page. */
public final class JobDao {

    private static final String SELECT =
        "SELECT id, name, sector, city, weather::text AS weather, openings, avg_days, toughness, "
      + "fresher_band, mid_band, referral, notice, roles, is_sample FROM companies ";

    public List<Company> list(String weatherOrNull) {
        List<Company> out = new ArrayList<>();
        String sql = SELECT + "WHERE (?::text IS NULL OR weather::text = ?) ORDER BY openings DESC, name";
        try (Connection c = Db.connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, weatherOrNull);
            ps.setString(2, weatherOrNull);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs, List.of(), List.of()));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read companies", e);
        }
        return out;
    }

    public Optional<Company> detail(int id) {
        try (Connection c = Db.connection()) {
            Company base;
            try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    base = map(rs, List.of(), List.of());
                }
            }
            List<Company.Step> steps = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT ord, name, detail FROM company_steps WHERE company_id = ? ORDER BY ord")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        steps.add(new Company.Step(rs.getInt(1), rs.getString(2), rs.getString(3)));
                    }
                }
            }
            List<Company.Whisper> whispers = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement("SELECT body, pakki, gappa FROM company_whispers WHERE company_id = ? ORDER BY created_at DESC LIMIT 10")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        whispers.add(new Company.Whisper(rs.getString(1), rs.getInt(2), rs.getInt(3)));
                    }
                }
            }
            return Optional.of(new Company(base.id(), base.name(), base.sector(), base.city(), base.weather(), base.openings(),
                    base.avgDays(), base.toughness(), base.fresherBand(), base.midBand(), base.referral(), base.notice(),
                    base.roles(), base.sample(), steps, whispers));
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the company", e);
        }
    }

    private static Company map(ResultSet rs, List<Company.Step> steps, List<Company.Whisper> whispers) throws SQLException {
        List<String> roles = Arrays.stream(rs.getString("roles").split(",")).map(String::trim).toList();
        return new Company(rs.getInt("id"), rs.getString("name"), rs.getString("sector"), rs.getString("city"),
                rs.getString("weather"), rs.getInt("openings"), rs.getInt("avg_days"), rs.getInt("toughness"),
                rs.getString("fresher_band"), rs.getString("mid_band"), rs.getString("referral"), rs.getString("notice"),
                roles, rs.getBoolean("is_sample"), steps, whispers);
    }
}
