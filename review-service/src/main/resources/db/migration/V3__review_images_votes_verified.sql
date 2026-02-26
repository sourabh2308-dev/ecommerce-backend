-- V3: Review images, votes, and verified purchase badge

ALTER TABLE review ADD COLUMN IF NOT EXISTS verified_purchase BOOLEAN NOT NULL DEFAULT FALSE;

-- Review images (up to 5 per review)
CREATE TABLE IF NOT EXISTS review_image (
    id            BIGSERIAL    PRIMARY KEY,
    review_id     BIGINT       NOT NULL REFERENCES review(id) ON DELETE CASCADE,
    image_url     VARCHAR(1000) NOT NULL,
    alt_text      VARCHAR(200),
    display_order INTEGER
);

CREATE INDEX IF NOT EXISTS idx_review_image_review ON review_image(review_id);

-- Review votes (helpful / not helpful)
CREATE TABLE IF NOT EXISTS review_vote (
    id          BIGSERIAL    PRIMARY KEY,
    review_id   BIGINT       NOT NULL REFERENCES review(id) ON DELETE CASCADE,
    voter_uuid  VARCHAR(36)  NOT NULL,
    helpful     BOOLEAN      NOT NULL,
    voted_at    TIMESTAMP    NOT NULL,
    UNIQUE(review_id, voter_uuid)
);

CREATE INDEX IF NOT EXISTS idx_review_vote_review ON review_vote(review_id);
