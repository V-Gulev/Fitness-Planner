CREATE TABLE profile_load_step (
    user_id BIGINT NOT NULL,
    equipment_code VARCHAR(24) NOT NULL,
    step_kg DECIMAL(7,2) NOT NULL,
    PRIMARY KEY (user_id, equipment_code),
    CONSTRAINT fk_step_profile FOREIGN KEY (user_id) REFERENCES fitness_profile(user_id),
    CONSTRAINT fk_step_equipment FOREIGN KEY (equipment_code) REFERENCES equipment(code),
    CONSTRAINT ck_load_step CHECK (step_kg > 0 AND step_kg <= 100)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE workout_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    workout_day_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    submission_token CHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    actual_data_hash VARCHAR(64) NOT NULL,
    progression_config_snapshot JSON NOT NULL,
    open_day_id BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'IN_PROGRESS' THEN workout_day_id ELSE NULL END) STORED,
    CONSTRAINT uk_session_submission UNIQUE (user_id, submission_token),
    CONSTRAINT uk_session_open_day UNIQUE (user_id, open_day_id),
    CONSTRAINT uk_session_day UNIQUE (id, workout_day_id),
    CONSTRAINT fk_session_owner_plan FOREIGN KEY (user_id, plan_id) REFERENCES workout_plan(user_id, id),
    CONSTRAINT fk_session_plan_day FOREIGN KEY (plan_id, workout_day_id) REFERENCES workout_day(plan_id, id),
    CONSTRAINT ck_session_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_session_completion CHECK ((status = 'COMPLETED') = (completed_at IS NOT NULL)),
    INDEX idx_session_owner_date (user_id, session_date, id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

ALTER TABLE planned_exercise ADD CONSTRAINT uk_planned_day_id_exercise UNIQUE (workout_day_id, id, exercise_id);

CREATE TABLE exercise_performance (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    workout_day_id BIGINT NOT NULL,
    planned_exercise_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    position INT NOT NULL,
    status VARCHAR(16) NOT NULL,
    prescription_snapshot JSON NOT NULL,
    suggestion_snapshot JSON NOT NULL,
    target_weight_kg DECIMAL(7,2) NULL,
    CONSTRAINT uk_performance_planned UNIQUE (session_id, planned_exercise_id),
    CONSTRAINT fk_performance_session FOREIGN KEY (session_id, workout_day_id) REFERENCES workout_session(id, workout_day_id),
    CONSTRAINT fk_performance_prescription FOREIGN KEY (workout_day_id, planned_exercise_id, exercise_id)
        REFERENCES planned_exercise(workout_day_id, id, exercise_id),
    CONSTRAINT ck_performance_status CHECK (status IN ('PENDING', 'COMPLETED', 'SKIPPED')),
    CONSTRAINT ck_performance_target CHECK (target_weight_kg IS NULL OR target_weight_kg BETWEEN 0 AND 1000),
    INDEX idx_performance_exercise (exercise_id, session_id)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE performed_set (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    performance_id BIGINT NOT NULL,
    set_number INT NOT NULL,
    reps INT NOT NULL,
    weight_kg DECIMAL(7,2) NOT NULL,
    rir INT NULL,
    CONSTRAINT uk_performed_set_number UNIQUE (performance_id, set_number),
    CONSTRAINT fk_set_performance FOREIGN KEY (performance_id) REFERENCES exercise_performance(id),
    CONSTRAINT ck_set_values CHECK (set_number BETWEEN 1 AND 12 AND reps BETWEEN 1 AND 100
        AND weight_kg BETWEEN 0 AND 1000 AND (rir IS NULL OR rir BETWEEN 0 AND 10))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;
