package com.kanakani.service;

import com.kanakani.dao.IdeaDao;
import com.kanakani.model.Ballot;
import com.kanakani.model.IdeaCard;
import com.kanakani.model.User;
import com.kanakani.security.RateLimiter;
import com.kanakani.security.SlipGuard;
import com.kanakani.util.UserFacingException;

import java.time.Duration;
import java.util.List;

/** Suggesting and voting on improvements. */
public final class IdeaService {

    private final IdeaDao dao;
    private final RateLimiter limiter;

    public IdeaService(IdeaDao dao, RateLimiter limiter) {
        this.dao = dao;
        this.limiter = limiter;
    }

    public void suggest(User user, String title, String body) {
        String t = title == null ? "" : title.strip();
        String b = body == null ? "" : body.strip();
        if (!IdeaRules.titleOk(t)) {
            throw new UserFacingException("Give your idea a short title, 5 to 80 characters.");
        }
        if (b.length() > IdeaRules.BODY_MAX) {
            throw new UserFacingException("Keep the details under 500 characters.");
        }
        List<String> slips = SlipGuard.check(t + " " + b);
        if (!slips.isEmpty()) {
            throw new UserFacingException("Remove " + String.join(", ", slips) + " first. It could point back to you.");
        }
        if (!limiter.allow("idea:" + user.id(), 5, Duration.ofDays(1))) {
            throw new UserFacingException("That is enough ideas for today. Come back tomorrow.");
        }
        if (!dao.create(user.id(), t, b)) {
            throw new UserFacingException("Ideas need an account at least " + IdeaRules.MIN_DAYS_TO_VOTE
                + " days old, and you can have " + IdeaRules.MAX_OPEN_PER_AUTHOR + " open at a time.");
        }
    }

    public void vote(User user, java.util.UUID ideaId, Ballot ballot, String comment) {
        String c = comment == null ? null : comment.strip();
        if (c != null && c.isEmpty()) {
            c = null;
        }
        if (c != null) {
            if (c.length() > IdeaRules.COMMENT_MAX) {
                throw new UserFacingException("Keep the comment under 200 characters.");
            }
            List<String> slips = SlipGuard.check(c);
            if (!slips.isEmpty()) {
                throw new UserFacingException("Remove " + String.join(", ", slips) + " from the comment first.");
            }
        }
        if (!limiter.allow("ivote:" + user.id(), 60, Duration.ofHours(1))) {
            throw new UserFacingException("Slow down a little.");
        }
        if (!dao.vote(ideaId, user.id(), ballot, c)) {
            throw new UserFacingException("You cannot vote on this idea. Voting needs an account at least "
                + IdeaRules.MIN_DAYS_TO_VOTE + " days old, and you cannot vote on your own idea.");
        }
    }

    public List<IdeaCard> list(String tab, User viewer) {
        return dao.list(tab, viewer.id());
    }
}
