-- Wishbox: members suggest improvements to the app, everyone votes yes or no (comment optional).
-- A majority of yes votes moves the idea to "Coming soon". When the team ships it, the feature
-- is switched on for every phone through a feature flag, with no store update needed.
-- Run after 01_schema.sql.

CREATE TYPE idea_status_t AS ENUM ('VOTING', 'APPROVED', 'BUILDING', 'SHIPPED', 'REJECTED', 'EXPIRED');
CREATE TYPE yesno_t       AS ENUM ('YES', 'NO');

CREATE TABLE feature_ideas (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id      UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(80)   NOT NULL CHECK (char_length(btrim(title)) >= 5),
    body           VARCHAR(500)  NOT NULL DEFAULT '',
    status         idea_status_t NOT NULL DEFAULT 'VOTING',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    voting_ends_at TIMESTAMPTZ   NOT NULL DEFAULT now() + interval '14 days',
    decided_at     TIMESTAMPTZ,
    shipped_at     TIMESTAMPTZ,
    version_label  VARCHAR(20),              -- e.g. "0.4", shown on the Shipped tab
    flag_key       VARCHAR(40) UNIQUE CHECK (flag_key ~ '^[a-z0-9_]+$'),  -- the switch that turns the feature on
    removed_at     TIMESTAMPTZ               -- set by moderators
);
CREATE INDEX feature_ideas_status ON feature_ideas (status, created_at DESC);

-- One vote per person per idea. The comment is optional and shows the voter's secret name only.
CREATE TABLE idea_votes (
    idea_id  UUID        NOT NULL REFERENCES feature_ideas(id) ON DELETE CASCADE,
    voter_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vote     yesno_t     NOT NULL,
    comment  VARCHAR(200),
    voted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (idea_id, voter_id)
);

-- Remote switches. The app asks the server which features are on, so a feature that is already
-- inside the installed app can be turned on for everyone at once.
CREATE TABLE feature_flags (
    flag_key   VARCHAR(40) PRIMARY KEY CHECK (flag_key ~ '^[a-z0-9_]+$'),
    enabled    BOOLEAN     NOT NULL DEFAULT FALSE,
    min_build  INTEGER     NOT NULL DEFAULT 0,   -- oldest app build that contains the feature
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE VIEW v_idea_stats AS
SELECT i.id,
       count(v.*) FILTER (WHERE v.vote = 'YES') AS yes_votes,
       count(v.*) FILTER (WHERE v.vote = 'NO')  AS no_votes
FROM feature_ideas i
LEFT JOIN idea_votes v ON v.idea_id = i.id
GROUP BY i.id;

-- ---------------------------------------------------------------- team steps (no admin screen yet)
-- After an idea is APPROVED the team moves it along by hand, for example:
--   UPDATE feature_ideas SET status = 'BUILDING' WHERE id = '...';
--   INSERT INTO feature_flags (flag_key) VALUES ('dark_mode');
--   UPDATE feature_ideas SET flag_key = 'dark_mode' WHERE id = '...';
-- When the code is live:
--   UPDATE feature_flags SET enabled = TRUE, updated_at = now() WHERE flag_key = 'dark_mode';
--   UPDATE feature_ideas SET status = 'SHIPPED', shipped_at = now(), version_label = '0.4' WHERE flag_key = 'dark_mode';
