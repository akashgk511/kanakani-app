package com.kanakani.model;

/** A yes or no vote on a suggested improvement. */
public enum Ballot {
    YES, NO;

    public static Ballot fromForm(String value) {
        if ("YES".equals(value)) {
            return YES;
        }
        return "NO".equals(value) ? NO : null;
    }
}
