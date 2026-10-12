package com.kanakani.service;

import com.kanakani.model.Outcome;
import com.kanakani.model.Resolution;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Rumour Receipts: the rules that decide whether a rumour "came true" or was "busted".
 *
 * The prototype let authors mark their own rumours, which anyone could cheat. Here the author
 * can only OPEN a vote, and the community decides:
 *
 *   1. Only a post marked RUMOUR can get a receipt, and only 24 hours after it was posted.
 *   2. The author opens the vote with a suggested outcome. The suggestion is shown but not counted.
 *   3. Voters must have used the app for at least 7 days and cannot be the author.
 *   4. The vote needs at least 5 voters and a two-thirds majority for one outcome.
 *   5. If no clear answer arrives within 14 days, the rumour is UNCLEAR and changes nobody's score.
 *
 * A lying author does not get a free pass either: if two thirds say it did not come true,
 * the rumour is BUSTED and the author's truth score drops.
 */
public final class ReceiptRules {

    public static final Duration MIN_AGE_BEFORE_VOTE = Duration.ofHours(24);
    public static final Duration VOTING_WINDOW = Duration.ofDays(14);
    public static final int MIN_VOTERS = 5;
    public static final int MIN_VOTER_DAYS = 7;

    private ReceiptRules() { }

    /** May the author open the vote now? */
    public static boolean authorMayOpenVote(boolean isRumour, Instant settleOpensAt, Instant now, boolean alreadyOpened) {
        return isRumour && !alreadyOpened && !now.isBefore(settleOpensAt);
    }

    /** May this person vote on the outcome? */
    public static boolean mayVote(UUID voter, UUID author, int voterDaysUsed) {
        return !voter.equals(author) && voterDaysUsed >= MIN_VOTER_DAYS;
    }

    /** Votes needed for a two-thirds majority: ceil(2 * total / 3), done in integers. */
    public static int majorityNeeded(int total) {
        return (2 * total + 2) / 3;
    }

    /** Decides where a rumour stands from the current tally. */
    public static Resolution decide(int cameTrueVotes, int bustedVotes, Instant voteOpenedAt, Instant now) {
        int total = cameTrueVotes + bustedVotes;
        if (total >= MIN_VOTERS) {
            int need = majorityNeeded(total);
            if (cameTrueVotes >= need) {
                return Resolution.CAME_TRUE;
            }
            if (bustedVotes >= need) {
                return Resolution.BUSTED;
            }
        }
        if (now.isAfter(voteOpenedAt.plus(VOTING_WINDOW))) {
            return Resolution.UNCLEAR;
        }
        return Resolution.PENDING;
    }

    /** Convenience for callers that hold the outcome enum. */
    public static Resolution toResolution(Outcome o) {
        return o == Outcome.CAME_TRUE ? Resolution.CAME_TRUE : Resolution.BUSTED;
    }
}
