-- Ranking and statistics views. Run after 01_schema.sql.

-- Per-post numbers, counted from the one-row-per-person tables.
CREATE VIEW v_post_stats AS
SELECT p.id AS post_id,
       p.author_id,
       p.topic,
       p.claim,
       p.resolution,
       p.created_at,
       (SELECT count(*) FROM post_likes   l WHERE l.post_id = p.id)                      AS likes,
       (SELECT count(*) FROM post_views   v WHERE v.post_id = p.id)                      AS views,
       (SELECT count(*) FROM belief_votes b WHERE b.post_id = p.id AND b.vote = 'PAKKI') AS pakki,
       (SELECT count(*) FROM belief_votes b WHERE b.post_id = p.id AND b.vote = 'GAPPA') AS gappa
FROM posts p
WHERE p.removed_at IS NULL;

-- Criterion 1 and 2: most liked and most viewed posts. Read with ORDER BY likes / views DESC LIMIT 10.

-- Criterion 3: most accurate. Truth score = settled rumours that came true.
-- Ranked by the Wilson lower bound (95%) so that 3 lucky guesses cannot beat 40 good ones.
-- Needs at least 3 settled rumours to appear.
CREATE VIEW v_truth_stats AS
SELECT u.id AS user_id, u.alias, u.gender,
       count(p.id) FILTER (WHERE p.resolution IN ('CAME_TRUE', 'BUSTED')) AS settled,
       count(p.id) FILTER (WHERE p.resolution = 'CAME_TRUE')              AS hits
FROM users u
LEFT JOIN posts p ON p.author_id = u.id AND p.removed_at IS NULL
WHERE u.banned_at IS NULL
GROUP BY u.id, u.alias, u.gender;

CREATE VIEW v_rank_most_accurate AS
SELECT user_id, alias, gender, settled, hits,
       round(100.0 * hits / settled)::int AS truth_pct,
       ( (hits::float8 / settled + 1.9208 / settled)
         - 1.96 * sqrt( (hits::float8 / settled * (1 - hits::float8 / settled) + 0.9604 / settled) / settled )
       ) / (1 + 3.8416 / settled) AS wilson_lower_bound,
       (settled >= 5 AND hits::float8 / settled >= 0.85) AS is_oracle
FROM v_truth_stats
WHERE settled >= 3;

-- Criterion 4: heavy users. 5 points per post, 2 per chat message, 1 per like given, 1 per day.
CREATE VIEW v_rank_heavy_users AS
SELECT t.*, t.posts * 5 + t.msgs_sent * 2 + t.likes_given + t.days AS points
FROM (
    SELECT u.id AS user_id, u.alias, u.gender, u.msgs_sent,
           (SELECT count(*) FROM posts      p WHERE p.author_id = u.id AND p.removed_at IS NULL) AS posts,
           (SELECT count(*) FROM post_likes l WHERE l.user_id   = u.id)                          AS likes_given,
           (SELECT count(*) FROM user_days  d WHERE d.user_id   = u.id)                          AS days
    FROM users u
    WHERE u.banned_at IS NULL
) t;

-- Criterion 5: longest streak, meaning days used.
CREATE VIEW v_rank_longest_streak AS
SELECT u.id AS user_id, u.alias, u.gender,
       (SELECT count(*) FROM user_days d WHERE d.user_id = u.id) AS days
FROM users u
WHERE u.banned_at IS NULL;

-- Rumours waiting for community votes, with the current tally.
CREATE VIEW v_receipt_queue AS
SELECT p.id AS post_id, p.author_id, p.proposed_outcome, p.proposed_at,
       count(r.voter_id) FILTER (WHERE r.outcome = 'CAME_TRUE') AS votes_came_true,
       count(r.voter_id) FILTER (WHERE r.outcome = 'BUSTED')    AS votes_busted
FROM posts p
LEFT JOIN receipt_votes r ON r.post_id = p.id
WHERE p.resolution = 'PENDING' AND p.proposed_at IS NOT NULL AND p.removed_at IS NULL
GROUP BY p.id;
