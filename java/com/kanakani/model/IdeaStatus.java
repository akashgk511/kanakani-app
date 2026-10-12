package com.kanakani.model;

/** Where a suggested improvement stands. */
public enum IdeaStatus {
    VOTING("Voting open"),
    APPROVED("Coming soon"),
    BUILDING("Being built"),
    SHIPPED("Live"),
    REJECTED("Not this time"),
    EXPIRED("Not enough votes");

    private final String label;

    IdeaStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Approved and being built both appear on the Coming soon tab. */
    public boolean isComingSoon() {
        return this == APPROVED || this == BUILDING;
    }
}
