CREATE TABLE equipment (
    code VARCHAR(24) NOT NULL PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE muscle_group (
    code VARCHAR(24) NOT NULL PRIMARY KEY,
    target_class VARCHAR(12) NOT NULL,
    CONSTRAINT ck_muscle_class CHECK (target_class IN ('MAJOR', 'ACCESSORY'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE exercise (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    movement_pattern VARCHAR(24) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    compound BOOLEAN NOT NULL,
    seconds_per_rep INT NOT NULL,
    setup_seconds INT NOT NULL,
    load_type VARCHAR(24) NOT NULL,
    load_equipment VARCHAR(24) NULL,
    fatigue_cost DECIMAL(3,2) NOT NULL,
    instructions VARCHAR(500) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_exercise_code UNIQUE (code),
    CONSTRAINT fk_exercise_load_equipment FOREIGN KEY (load_equipment) REFERENCES equipment(code),
    CONSTRAINT ck_exercise_difficulty CHECK (difficulty IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_exercise_movement CHECK (movement_pattern IN ('SQUAT', 'HINGE', 'HORIZONTAL_PUSH', 'VERTICAL_PUSH',
        'HORIZONTAL_PULL', 'VERTICAL_PULL', 'KNEE_FLEXION', 'HIP_EXTENSION', 'ELBOW_FLEXION', 'ELBOW_EXTENSION', 'CALF_RAISE', 'SHOULDER_ABDUCTION', 'CORE')),
    CONSTRAINT ck_exercise_timing CHECK (seconds_per_rep BETWEEN 1 AND 30 AND setup_seconds BETWEEN 0 AND 300),
    CONSTRAINT ck_exercise_fatigue CHECK (fatigue_cost BETWEEN 0 AND 1),
    CONSTRAINT ck_exercise_load CHECK (
        (load_type IN ('BODYWEIGHT', 'ASSISTED') AND load_equipment IS NULL) OR
        (load_type IN ('TOTAL_EXTERNAL', 'PER_HAND_EXTERNAL') AND load_equipment IS NOT NULL)),
    CONSTRAINT ck_exercise_text CHECK (CHAR_LENGTH(TRIM(name)) > 0 AND CHAR_LENGTH(TRIM(instructions)) > 0),
    INDEX idx_exercise_active_code (active, code)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE exercise_muscle (
    exercise_id BIGINT NOT NULL,
    muscle_code VARCHAR(24) NOT NULL,
    role VARCHAR(12) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_code),
    CONSTRAINT fk_exercise_muscle_exercise FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    CONSTRAINT fk_exercise_muscle_group FOREIGN KEY (muscle_code) REFERENCES muscle_group(code),
    CONSTRAINT ck_exercise_muscle_role CHECK (role IN ('PRIMARY', 'SECONDARY'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE exercise_equipment (
    exercise_id BIGINT NOT NULL,
    equipment_code VARCHAR(24) NOT NULL,
    PRIMARY KEY (exercise_id, equipment_code),
    CONSTRAINT fk_exercise_equipment_exercise FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    CONSTRAINT fk_exercise_equipment_code FOREIGN KEY (equipment_code) REFERENCES equipment(code)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE exercise_tag (
    exercise_id BIGINT NOT NULL,
    tag VARCHAR(24) NOT NULL,
    PRIMARY KEY (exercise_id, tag),
    CONSTRAINT fk_exercise_tag FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    CONSTRAINT ck_exercise_tag CHECK (tag IN ('UNILATERAL', 'SUPPORTED', 'OVERHEAD'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE fitness_profile (
    user_id BIGINT NOT NULL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    experience VARCHAR(16) NOT NULL,
    goal VARCHAR(24) NOT NULL,
    session_minutes INT NOT NULL,
    zone_id VARCHAR(64) NOT NULL,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT ck_profile_experience CHECK (experience IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    CONSTRAINT ck_profile_goal CHECK (goal IN ('HYPERTROPHY', 'STRENGTH', 'GENERAL_FITNESS')),
    CONSTRAINT ck_profile_minutes CHECK (session_minutes BETWEEN 20 AND 120),
    CONSTRAINT ck_profile_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE profile_weekday (
    user_id BIGINT NOT NULL,
    weekday INT NOT NULL,
    PRIMARY KEY (user_id, weekday),
    CONSTRAINT fk_profile_weekday FOREIGN KEY (user_id) REFERENCES fitness_profile(user_id),
    CONSTRAINT ck_profile_weekday CHECK (weekday BETWEEN 1 AND 7)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE profile_equipment (
    user_id BIGINT NOT NULL,
    equipment_code VARCHAR(24) NOT NULL,
    PRIMARY KEY (user_id, equipment_code),
    CONSTRAINT fk_profile_equipment_user FOREIGN KEY (user_id) REFERENCES fitness_profile(user_id),
    CONSTRAINT fk_profile_equipment_code FOREIGN KEY (equipment_code) REFERENCES equipment(code)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE profile_exercise_rule (
    user_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    rule VARCHAR(12) NOT NULL,
    PRIMARY KEY (user_id, exercise_id),
    CONSTRAINT fk_profile_rule_user FOREIGN KEY (user_id) REFERENCES fitness_profile(user_id),
    CONSTRAINT fk_profile_rule_exercise FOREIGN KEY (exercise_id) REFERENCES exercise(id),
    CONSTRAINT ck_profile_exercise_rule CHECK (rule IN ('PREFERRED', 'EXCLUDED'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;

CREATE TABLE profile_movement_exclusion (
    user_id BIGINT NOT NULL,
    movement_pattern VARCHAR(24) NOT NULL,
    PRIMARY KEY (user_id, movement_pattern),
    CONSTRAINT fk_profile_exclusion FOREIGN KEY (user_id) REFERENCES fitness_profile(user_id),
    CONSTRAINT ck_profile_excluded_movement CHECK (movement_pattern IN ('SQUAT', 'HINGE', 'HORIZONTAL_PUSH', 'VERTICAL_PUSH',
        'HORIZONTAL_PULL', 'VERTICAL_PULL', 'KNEE_FLEXION', 'HIP_EXTENSION', 'ELBOW_FLEXION', 'ELBOW_EXTENSION', 'CALF_RAISE', 'SHOULDER_ABDUCTION', 'CORE'))
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4;
