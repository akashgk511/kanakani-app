package com.kanakani.service;

import com.kanakani.dao.PostDao;
import com.kanakani.model.Belief;
import com.kanakani.model.Claim;
import com.kanakani.model.PostCard;
import com.kanakani.model.Topic;
import com.kanakani.model.User;
import com.kanakani.security.PostCrypto;
import com.kanakani.security.SlipGuard;
import com.kanakani.util.UserFacingException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FeedService {

    public static final int MAX_LENGTH = 280;
    private static final int FEED_SIZE = 50;

    private final PostDao posts;
    private final PostCrypto crypto;

    public FeedService(PostDao posts, PostCrypto crypto) {
        this.posts = posts;
        this.crypto = crypto;
    }

    /** Loads the feed, decrypts it, and counts one view per person for each post shown. */
    public List<PostCard> feed(User viewer, Topic topicOrNull) {
        List<PostDao.Row> rows = posts.feed(viewer.id(), topicOrNull, FEED_SIZE);
        List<PostCard> cards = new ArrayList<>(rows.size());
        List<UUID> shown = new ArrayList<>(rows.size());
        for (PostDao.Row r : rows) {
            String body = crypto.open(r.sealed(), r.id().toString());
            cards.add(new PostCard(r.id(), r.alias(), r.gender(), r.authorDays(), r.topic(), r.claim(), r.resolution(),
                    r.createdAt(), body, r.likes(), r.views(), r.pakki(), r.gappa(), r.liked(), r.myBelief(), r.mine(),
                    r.votingOpen(), r.canOpenVote(), r.myReceipt(), r.votesCameTrue(), r.votesBusted()));
            shown.add(r.id());
        }
        posts.recordViews(viewer.id(), shown);
        return cards;
    }

    /**
     * Saves a post. Slip-guard runs here too, because the browser warning can be skipped.
     * The first time a risky draft arrives, the person is warned and must confirm.
     */
    public UUID post(User author, String text, Topic topic, boolean isRumour, boolean confirmedRisky) {
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) {
            throw new UserFacingException("A blank whisper is just silence. Write something.");
        }
        if (clean.length() > MAX_LENGTH) {
            throw new UserFacingException("Keep it under " + MAX_LENGTH + " characters.");
        }
        List<String> slips = SlipGuard.check(clean);
        if (!slips.isEmpty() && !confirmedRisky) {
            throw new UserFacingException("Careful: this has " + String.join(" and ", slips)
                + ". Anyone who knows you could trace it. Edit it, or choose Post anyway.");
        }
        UUID id = UUID.randomUUID();
        PostCrypto.Sealed sealed = crypto.seal(clean, id.toString());
        Instant settleOpens = Instant.now().plus(isRumour ? ReceiptRules.MIN_AGE_BEFORE_VOTE : Duration.ZERO);
        posts.create(id, author.id(), topic, isRumour ? Claim.RUMOUR : Claim.OPINION, sealed, settleOpens);
        return id;
    }

    public boolean toggleLike(User user, UUID postId) {
        return posts.toggleLike(postId, user.id());
    }

    /** Tapping the same side again takes the vote back. Authors cannot vote on their own post. */
    public void believe(User user, UUID postId, Belief vote) {
        Optional<Belief> current = posts.myBelief(postId, user.id());
        if (current.isPresent() && current.get() == vote) {
            posts.clearBelief(postId, user.id());
        } else {
            posts.setBelief(postId, user.id(), vote);
        }
    }

    public boolean delete(User author, UUID postId) {
        return posts.remove(postId, author.id());
    }
}
