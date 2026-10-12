package com.kanakani.service;

/** The "heavy user" score: 5 per post, 2 per chat message, 1 per like given, 1 per day on the app. */
public final class ActivityPoints {

    private ActivityPoints() { }

    public static long compute(long posts, long messages, long likesGiven, long days) {
        return posts * 5 + messages * 2 + likesGiven + days;
    }
}
