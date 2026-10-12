package com.kanakani.service;

import com.kanakani.model.IdeaStatus;

import java.time.Duration;

/**
 * Wishbox rules. The same numbers are repeated in db/04_maintenance.sql (closing votes) and in the
 * IdeaDao queries (who may vote), so a mistake in one place cannot bypass them.
 *
 *   1. Anyone with an account at least 3 days old can suggest an improvement; 3 open ideas at a time.
 *   2. Voting stays open for 14 days. Authors cannot vote on their own idea.
 *   3. A vote needs at least 10 voters. More yes than no: APPROVED (Coming soon). Otherwise REJECTED.
 *   4. Fewer than 10 voters when time is up: EXPIRED. The author may suggest it again.
 *   5. APPROVED only means the community wants it. The team still decides when it can be built.
 */
public final class IdeaRules {

    public static final Duration VOTING_WINDOW = Duration.ofDays(14);
    public static final int MIN_VOTERS = 10;
    public static final int MIN_DAYS_TO_VOTE = 3;
    public static final int MAX_OPEN_PER_AUTHOR = 3;
    public static final int TITLE_MIN = 5;
    public static final int TITLE_MAX = 80;
    public static final int BODY_MAX = 500;
    public static final int COMMENT_MAX = 200;

    private IdeaRules() { }

    /** The result once voting time is up. */
    public static IdeaStatus decideAtClose(int yes, int no) {
        if (yes < 0 || no < 0) {
            throw new IllegalArgumentException("negative votes");
        }
        if (yes + no < MIN_VOTERS) {
            return IdeaStatus.EXPIRED;
        }
        return yes > no ? IdeaStatus.APPROVED : IdeaStatus.REJECTED;
    }

    public static boolean titleOk(String title) {
        if (title == null) {
            return false;
        }
        int n = title.strip().length();
        return n >= TITLE_MIN && n <= TITLE_MAX;
    }
}
