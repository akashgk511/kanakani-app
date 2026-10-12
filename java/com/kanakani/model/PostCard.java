package com.kanakani.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A post ready to show: decrypted text, author alias and gender, counts, and the viewer's own reactions.
 * The receipt fields drive the Rumour Receipts buttons on the feed page.
 * The get... and is... methods exist because JSP expression language reads JavaBean getters.
 */
public record PostCard(
        UUID id,
        String alias,
        Gender gender,
        int authorDays,
        Topic topic,
        Claim claim,
        Resolution resolution,
        Instant createdAt,
        String body,
        long likes,
        long views,
        long pakki,
        long gappa,
        boolean liked,
        Belief myBelief,
        boolean mine,
        boolean votingOpen,
        boolean canOpenVote,
        Outcome myReceiptVote,
        long votesCameTrue,
        long votesBusted) {

    public String getId() { return id.toString(); }
    public String getAlias() { return alias; }
    public Gender getGender() { return gender; }
    public int getAuthorDays() { return authorDays; }
    public Topic getTopic() { return topic; }
    public String getBody() { return body; }
    public long getLikes() { return likes; }
    public long getViews() { return views; }
    public long getPakki() { return pakki; }
    public long getGappa() { return gappa; }
    public boolean isLiked() { return liked; }
    public boolean isMine() { return mine; }
    public boolean isVotingOpen() { return votingOpen; }
    public boolean isCanOpenVote() { return canOpenVote; }
    public long getVotesCameTrue() { return votesCameTrue; }
    public long getVotesBusted() { return votesBusted; }

    public boolean isRumour() { return claim == Claim.RUMOUR; }
    public boolean isCameTrue() { return resolution == Resolution.CAME_TRUE; }
    public boolean isBusted() { return resolution == Resolution.BUSTED; }
    public boolean isUnclear() { return resolution == Resolution.UNCLEAR; }
    public boolean isBelievedPakki() { return myBelief == Belief.PAKKI; }
    public boolean isBelievedGappa() { return myBelief == Belief.GAPPA; }
    public boolean isMyVoteCameTrue() { return myReceiptVote == Outcome.CAME_TRUE; }
    public boolean isMyVoteBusted() { return myReceiptVote == Outcome.BUSTED; }

    /** Share of believers, 0 to 100. 50 when nobody has voted. */
    public int getPakkiPercent() {
        long total = pakki + gappa;
        return total == 0 ? 50 : (int) Math.round(100.0 * pakki / total);
    }

    /** "now", "5m", "3h" or "2d". */
    public String getAgo() {
        long s = Duration.between(createdAt, Instant.now()).getSeconds();
        if (s < 60) {
            return "now";
        }
        long m = s / 60;
        if (m < 60) {
            return m + "m";
        }
        long h = m / 60;
        return h < 24 ? h + "h" : (h / 24) + "d";
    }
}
