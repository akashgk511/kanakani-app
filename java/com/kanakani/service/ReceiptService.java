package com.kanakani.service;

import com.kanakani.dao.ReceiptDao;
import com.kanakani.model.Outcome;
import com.kanakani.model.Resolution;
import com.kanakani.model.User;
import com.kanakani.util.UserFacingException;

import java.time.Instant;
import java.util.UUID;

/** Rumour Receipts workflow: the author opens a vote, the community decides, the score follows. */
public final class ReceiptService {

    private final ReceiptDao receipts;

    public ReceiptService(ReceiptDao receipts) {
        this.receipts = receipts;
    }

    /** The author asks the community to settle their rumour. The suggestion is shown but never counted. */
    public void openVote(User author, UUID postId, Outcome suggestion) {
        if (!receipts.openVote(postId, author.id(), suggestion)) {
            throw new UserFacingException("You can open the vote on a rumour once, and only 24 hours after posting it.");
        }
    }

    /**
     * A community member votes on what happened. After each vote the tally is checked, and the
     * rumour is settled as soon as the rules allow. Returns where the rumour stands now.
     */
    public Resolution vote(User voter, UUID postId, Outcome outcome) {
        if (voter.days() < ReceiptRules.MIN_VOTER_DAYS) {
            throw new UserFacingException("You can vote on rumours after " + ReceiptRules.MIN_VOTER_DAYS + " days on Kanakani.");
        }
        if (!receipts.vote(postId, voter.id(), outcome)) {
            throw new UserFacingException("You cannot vote on this rumour. It may be yours, or the vote is not open.");
        }
        int[] tally = receipts.tally(postId);
        Instant opened = receipts.voteOpenedAt(postId).orElse(Instant.now());
        Resolution r = ReceiptRules.decide(tally[0], tally[1], opened, Instant.now());
        if (r != Resolution.PENDING) {
            receipts.resolve(postId, r);
        }
        return r;
    }
}
