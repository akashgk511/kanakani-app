package com.kanakani.model;

/** Post topics. Matches the topic_t type in the database. */
public enum Topic {
    OFFICE_LIFE("Office life"),
    BOSS_RADAR("Boss radar"),
    CANTEEN("Canteen"),
    CAKE_ALERT("Cake alert"),
    HIRING_BUZZ("Hiring buzz");

    private final String label;

    Topic(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Topic fromForm(String value) {
        if (value == null || value.isBlank()) {
            return OFFICE_LIFE;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OFFICE_LIFE;
        }
    }
}
