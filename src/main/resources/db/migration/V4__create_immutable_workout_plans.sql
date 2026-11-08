CREATE TABLE workout_plan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    algorithm_version VARCHAR(40) NOT NULL,
    config_version VARCHAR(64) NOT NULL,
    catalog_hash CHAR(64) NOT NULL,
    config_hash CHAR(64) NOT NULL,
    profile_hash CHAR(64) NOT NULL,
    profile_version BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    candidate_key VARCHAR(120) NOT NULL,
    input_snapshot JSON NOT NULL,
    config_snapshot JSON NOT NULL,
    catalog_snapshot JSON NOT NULL,
    evaluation_json JSON NOT NULL,
    diagnostics_json JSON NOT NULL,
    candidates_constructed INT NOT NULL,
    additions_examined BIGINT NOT NULL,
    CONSTRAINT uk_plan_owner_id UNIQUE (user_id, id),
    CONSTRAINT fk_plan_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_plan_status CHECK (status IN ('VALID', 'DEGRADED')),
    CONSTRAINT ck_plan_trace CHECK (profile_version >= 0 AND candidates_constructed > 0 AND additions_examined >= 0),
    INDEX idx_plan_owner_created (user_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE workout_day (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    weekday INT NOT NULL,
    day_type VARCHAR(16) NOT NULL,
    estimated_seconds INT NOT NULL,
    CONSTRAINT uk_plan_weekday UNIQUE (plan_id, weekday),
    CONSTRAINT uk_plan_day_id UNIQUE (plan_id, id),
    CONSTRAINT fk_day_plan FOREIGN KEY (plan_id) REFERENCES workout_plan(id),
    CONSTRAINT ck_day_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT ck_day_type CHECK (day_type IN ('FULL', 'UPPER', 'LOWER', 'PUSH', 'PULL', 'LEGS')),
    CONSTRAINT ck_day_duration CHECK (estimated_seconds > 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE planned_exercise (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    workout_day_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    position INT NOT NULL,
    sets INT NOT NULL,
    rep_min INT NOT NULL,
    rep_max INT NOT NULL,
    rest_seconds INT NOT NULL,
    estimated_seconds INT NOT NULL,
    code_snapshot VARCHAR(64) NOT NULL,
    name_snapshot VARCHAR(100) NOT NULL,
    primary_muscle_snapshot VARCHAR(24) NOT NULL,
    load_type_snapshot VARCHAR(24) NOT NULL,
    explanation TEXT NOT NULL,
    score_json JSON NULL,
    CONSTRAINT uk_planned_position UNIQUE (workout_day_id, position),
    CONSTRAINT uk_planned_exercise UNIQUE (workout_day_id, exercise_id),
    CONSTRAINT fk_planned_day FOREIGN KEY (workout_day_id) REFERENCES workout_day(id),
    CONSTRAINT fk_planned_catalog FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    CONSTRAINT ck_planned_prescription CHECK (position >= 0 AND sets BETWEEN 2 AND 4
        AND rep_min >= 1 AND rep_max BETWEEN rep_min AND 50 AND rest_seconds >= 0 AND estimated_seconds > 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

ALTER TABLE fitness_profile ADD COLUMN active_plan_id BIGINT NULL;
ALTER TABLE fitness_profile ADD CONSTRAINT fk_profile_active_plan
    FOREIGN KEY (user_id, active_plan_id) REFERENCES workout_plan(user_id, id);
