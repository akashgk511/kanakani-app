-- Scheduled cleanup. Run every 5 minutes from cron or pg_cron.
-- Everything here makes the system hold LESS about people over time.

-- Vanishing chat: delete read messages once their timer ends, unless BOTH people chose to keep them.
DELETE FROM chat_envelopes
WHERE read_at IS NOT NULL
  AND expires_at < now()
  AND NOT (keep_sender AND keep_receiver);

-- Rumour Receipts: a vote that never reached quorum within 14 days ends as UNCLEAR.
-- UNCLEAR changes nobody's truth score.
UPDATE posts
SET resolution = 'UNCLEAR', resolved_at = now()
WHERE resolution = 'PENDING'
  AND proposed_at IS NOT NULL
  AND proposed_at < now() - interval '14 days';

-- Old one-time passwords and expired sessions.
DELETE FROM otp_challenges WHERE expires_at < now() - interval '1 day';
DELETE FROM sessions       WHERE expires_at < now();

-- Wishbox: close votes whose time is up. Keep these numbers the same as IdeaRules.java.
-- At least 10 voters and more yes than no: APPROVED. At least 10 voters otherwise: REJECTED.
-- Too few voters: EXPIRED (the author may suggest it again).
UPDATE feature_ideas i
SET status = CASE
        WHEN s.yes_votes + s.no_votes >= 10 AND s.yes_votes > s.no_votes THEN 'APPROVED'::idea_status_t
        WHEN s.yes_votes + s.no_votes >= 10 THEN 'REJECTED'::idea_status_t
        ELSE 'EXPIRED'::idea_status_t
    END,
    decided_at = now()
FROM v_idea_stats s
WHERE s.id = i.id AND i.status = 'VOTING' AND i.voting_ends_at < now();
