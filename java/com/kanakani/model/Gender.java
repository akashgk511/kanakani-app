package com.kanakani.model;

/** The only personal detail shown next to a secret name. Matches the gender_t type in the database. */
public enum Gender {
    WOMAN("Woman"),
    MAN("Man"),
    NON_BINARY("Non-binary"),
    NOT_SHARED("Not shared");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Reads a form value. Anything unknown becomes NOT_SHARED, so a bad request never fails signup. */
    public static Gender fromForm(String value) {
        if (value == null) {
            return NOT_SHARED;
        }
        try {
            return valueOf(value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return NOT_SHARED;
        }
    }
}
