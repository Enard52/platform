-- Append-only history of Dialogue Branch Variable values (issue #259). Every change to a user's
-- variable store (a set, a removal, or a clear) appends one row per affected variable; nothing is
-- ever updated or deleted here, unlike `variables`, which only holds the current value.
--
-- A removal is recorded as a row with value = NULL (null is already the "unset" value elsewhere
-- in the model), not as a separate tombstone flag. Retention is unbounded for now (#259, #320).
--
-- recorded_time is the Web Service's own clock at the moment of the change (epoch millis, in the
-- user's time zone recorded_time_zone) -- not the change's own timestamp, which for External
-- Variable Service updates is the EVS's clock and can be arbitrarily old (#259, #321).
--
-- The auto-increment id gives a stable insertion order to sort on, independent of clock ties.

CREATE TABLE variable_history (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    user_id            CHAR(36)     NOT NULL,
    project_id         CHAR(36)     NOT NULL,
    name               VARCHAR(255) NOT NULL,
    value              VARCHAR(255) NULL,
    recorded_time      BIGINT       NOT NULL,
    recorded_time_zone VARCHAR(64)  NOT NULL,
    source             VARCHAR(32)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_variable_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_variable_history_project FOREIGN KEY (project_id) REFERENCES projects (id)
);

-- Covers both lookups the read end-point makes: one variable's history, and a whole project's
-- history for a user, each newest-first (by id).
CREATE INDEX ix_variable_history_user_project_name ON variable_history (user_id, project_id, name, id);
CREATE INDEX ix_variable_history_user_project ON variable_history (user_id, project_id, id);
