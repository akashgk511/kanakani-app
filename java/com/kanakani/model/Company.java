package com.kanakani.model;

import java.util.List;
import java.util.Locale;

/**
 * Hiring information for one company. {@code steps} and {@code whispers} are filled only on the detail page.
 * The get... methods exist because JSP expression language reads JavaBean getters.
 */
public record Company(
        int id,
        String name,
        String sector,
        String city,
        String weather,
        int openings,
        int avgDays,
        int toughness,
        String fresherBand,
        String midBand,
        String referral,
        String notice,
        List<String> roles,
        boolean sample,
        List<Step> steps,
        List<Whisper> whispers) {

    public record Step(int ord, String name, String detail) {
        public int getOrd() { return ord; }
        public String getName() { return name; }
        public String getDetail() { return detail; }
    }

    public record Whisper(String body, int pakki, int gappa) {
        public String getBody() { return body; }

        public int getPakkiPercent() {
            int t = pakki + gappa;
            return t == 0 ? 50 : (int) Math.round(100.0 * pakki / t);
        }
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSector() { return sector; }
    public String getCity() { return city; }
    public int getOpenings() { return openings; }
    public int getAvgDays() { return avgDays; }
    public int getToughness() { return toughness; }
    public String getFresherBand() { return fresherBand; }
    public String getMidBand() { return midBand; }
    public String getReferral() { return referral; }
    public String getNotice() { return notice; }
    public List<String> getRoles() { return roles; }
    public boolean isSample() { return sample; }
    public List<Step> getSteps() { return steps; }
    public List<Whisper> getWhispers() { return whispers; }

    /** CSS class: sunny, cloudy, stormy or frozen. */
    public String getWeatherClass() { return weather.toLowerCase(Locale.ROOT); }

    /** "Sunny", "Cloudy", "Stormy" or "Frozen". */
    public String getWeatherLabel() {
        return weather.substring(0, 1) + weather.substring(1).toLowerCase(Locale.ROOT);
    }

    /** One line of gossip about the weather, shown on the detail page. */
    public String getWeatherGossip() {
        return switch (weather) {
            case "SUNNY" -> "Hiring like it is Diwali. Offers are flying.";
            case "CLOUDY" -> "Still hiring, but every offer needs three approvals and a prayer.";
            case "STORMY" -> "Whispers of restructuring. Ask careful questions.";
            default -> "Hiring paused. Only replacements, and only if the boss remembers.";
        };
    }
}
