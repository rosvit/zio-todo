CREATE TABLE IF NOT EXISTS todo
(
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    description  text    NOT NULL,
    completed    boolean NOT NULL DEFAULT false,
    completed_at timestamptz
);
