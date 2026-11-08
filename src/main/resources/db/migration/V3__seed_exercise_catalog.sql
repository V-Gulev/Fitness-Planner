-- Curated project catalog. Timing, difficulty and fatigue are explicit heuristic metadata, not clinical findings.

INSERT INTO equipment (code) VALUES ('BARBELL'), ('RACK'), ('BENCH'), ('DUMBBELL'), ('CABLE'), ('PULLUP_BAR'), ('LEG_PRESS'), ('LEG_CURL'), ('CALF_MACHINE');

INSERT INTO muscle_group (code, target_class) VALUES ('CHEST', 'MAJOR'), ('BACK', 'MAJOR'), ('QUADRICEPS', 'MAJOR'), ('HAMSTRINGS', 'MAJOR'), ('GLUTES', 'MAJOR'), ('SHOULDERS', 'ACCESSORY'), ('BICEPS', 'ACCESSORY'), ('TRICEPS', 'ACCESSORY'), ('CALVES', 'ACCESSORY'), ('CORE', 'ACCESSORY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (1, 'BODYWEIGHT_SQUAT', 'Bodyweight squat', 'SQUAT', 'BEGINNER', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Sit down between your hips, keep your feet planted, then stand tall with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (1, 'QUADRICEPS', 'PRIMARY'), (1, 'GLUTES', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (2, 'BARBELL_BACK_SQUAT', 'Barbell back squat', 'SQUAT', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Set the bar securely across your upper back, brace, squat with control, and stand back up.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (2, 'QUADRICEPS', 'PRIMARY'), (2, 'GLUTES', 'SECONDARY'), (2, 'CORE', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (2, 'BARBELL'), (2, 'RACK');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (3, 'DUMBBELL_GOBLET_SQUAT', 'Dumbbell goblet squat', 'SQUAT', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'DUMBBELL', 0.70, 'Hold one dumbbell at your chest, squat between your hips, and stand tall.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (3, 'QUADRICEPS', 'PRIMARY'), (3, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (3, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (4, 'DUMBBELL_SPLIT_SQUAT', 'Dumbbell split squat', 'SQUAT', 'INTERMEDIATE', TRUE, 8, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Use a split stance, lower the rear knee with control, then press through the front foot. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (4, 'QUADRICEPS', 'PRIMARY'), (4, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (4, 'DUMBBELL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (4, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (5, 'BARBELL_FRONT_SQUAT', 'Barbell front squat', 'SQUAT', 'ADVANCED', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Support the bar at the front of your shoulders, keep elbows raised, and squat with a braced torso.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (5, 'QUADRICEPS', 'PRIMARY'), (5, 'GLUTES', 'SECONDARY'), (5, 'CORE', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (5, 'BARBELL'), (5, 'RACK');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (6, 'LEG_PRESS', 'Leg press', 'SQUAT', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'LEG_PRESS', 0.70, 'Keep your back supported, lower the platform through a controlled range, and press without locking the knees.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (6, 'QUADRICEPS', 'PRIMARY'), (6, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (6, 'LEG_PRESS');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (6, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (7, 'BODYWEIGHT_REVERSE_LUNGE', 'Bodyweight reverse lunge', 'SQUAT', 'BEGINNER', TRUE, 8, 60, 'BODYWEIGHT', NULL, 0.70, 'Step back into a lunge, lower with control, and return through the front foot. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (7, 'QUADRICEPS', 'PRIMARY'), (7, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (7, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (8, 'DUMBBELL_REVERSE_LUNGE', 'Dumbbell reverse lunge', 'SQUAT', 'INTERMEDIATE', TRUE, 8, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Hold dumbbells at your sides, step back into a controlled lunge, and return. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (8, 'QUADRICEPS', 'PRIMARY'), (8, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (8, 'DUMBBELL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (8, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (9, 'BARBELL_DEADLIFT', 'Barbell deadlift', 'HINGE', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Brace, hinge to grip the bar, keep it close, and stand by extending the hips and knees.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (9, 'HAMSTRINGS', 'PRIMARY'), (9, 'GLUTES', 'SECONDARY'), (9, 'BACK', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (9, 'BARBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (10, 'BARBELL_ROMANIAN_DEADLIFT', 'Barbell Romanian deadlift', 'HINGE', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'With softly bent knees, push your hips back while keeping the bar close, then extend the hips.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (10, 'HAMSTRINGS', 'PRIMARY'), (10, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (10, 'BARBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (11, 'DUMBBELL_ROMANIAN_DEADLIFT', 'Dumbbell Romanian deadlift', 'HINGE', 'BEGINNER', TRUE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Hold dumbbells at your sides, hinge the hips backward with a braced torso, then stand tall.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (11, 'HAMSTRINGS', 'PRIMARY'), (11, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (11, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (12, 'LEG_CURL', 'Seated leg curl', 'KNEE_FLEXION', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'LEG_CURL', 0.25, 'Adjust the pads, keep your hips supported, curl the legs, then return slowly.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (12, 'HAMSTRINGS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (12, 'LEG_CURL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (12, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (13, 'HAMSTRING_WALKOUT', 'Hamstring walkout', 'KNEE_FLEXION', 'INTERMEDIATE', TRUE, 6, 60, 'BODYWEIGHT', NULL, 0.70, 'From a glute bridge, take small heel steps away and back while keeping the hips controlled.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (13, 'HAMSTRINGS', 'PRIMARY'), (13, 'GLUTES', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (14, 'GLUTE_BRIDGE', 'Glute bridge', 'HIP_EXTENSION', 'BEGINNER', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Lie with knees bent and feet planted, lift the hips, then lower without arching your back.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (14, 'GLUTES', 'PRIMARY'), (14, 'HAMSTRINGS', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (15, 'BARBELL_HIP_THRUST', 'Barbell hip thrust', 'HIP_EXTENSION', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Support your upper back on a bench, pad the bar across your hips, and raise the hips under control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (15, 'GLUTES', 'PRIMARY'), (15, 'HAMSTRINGS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (15, 'BARBELL'), (15, 'BENCH');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (15, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (16, 'DUMBBELL_GLUTE_BRIDGE', 'Dumbbell glute bridge', 'HIP_EXTENSION', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'DUMBBELL', 0.70, 'Place a padded dumbbell across your hips, plant the feet, and lift and lower the hips with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (16, 'GLUTES', 'PRIMARY'), (16, 'HAMSTRINGS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (16, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (17, 'SINGLE_LEG_GLUTE_BRIDGE', 'Single-leg glute bridge', 'HIP_EXTENSION', 'INTERMEDIATE', TRUE, 8, 60, 'BODYWEIGHT', NULL, 0.70, 'Keep one foot planted, lift the hips without rotating, and lower slowly. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (17, 'GLUTES', 'PRIMARY'), (17, 'HAMSTRINGS', 'SECONDARY');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (17, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (18, 'PUSH_UP', 'Push-up', 'HORIZONTAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Keep your body in a straight line, lower your chest between your hands, and press back up.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (18, 'CHEST', 'PRIMARY'), (18, 'TRICEPS', 'SECONDARY'), (18, 'SHOULDERS', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (19, 'KNEE_PUSH_UP', 'Knee push-up', 'HORIZONTAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Support yourself on hands and knees, keep a straight line from knees to shoulders, and press with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (19, 'CHEST', 'PRIMARY'), (19, 'TRICEPS', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (20, 'BARBELL_BENCH_PRESS', 'Barbell bench press', 'HORIZONTAL_PUSH', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Use a secure rack setup, keep feet planted, lower the bar to the chest with control, and press upward.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (20, 'CHEST', 'PRIMARY'), (20, 'TRICEPS', 'SECONDARY'), (20, 'SHOULDERS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (20, 'BARBELL'), (20, 'BENCH'), (20, 'RACK');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (20, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (21, 'DUMBBELL_BENCH_PRESS', 'Dumbbell bench press', 'HORIZONTAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Lie on a bench, lower the dumbbells beside your chest, and press them upward with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (21, 'CHEST', 'PRIMARY'), (21, 'TRICEPS', 'SECONDARY'), (21, 'SHOULDERS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (21, 'DUMBBELL'), (21, 'BENCH');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (21, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (22, 'DUMBBELL_FLOOR_PRESS', 'Dumbbell floor press', 'HORIZONTAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Lie on the floor, lower until your upper arms gently reach the floor, then press upward.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (22, 'CHEST', 'PRIMARY'), (22, 'TRICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (22, 'DUMBBELL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (22, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (23, 'CABLE_CHEST_PRESS', 'Cable chest press', 'HORIZONTAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.70, 'Use a stable stance, press the handles forward from chest level, and return slowly.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (23, 'CHEST', 'PRIMARY'), (23, 'TRICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (23, 'CABLE');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (24, 'DUMBBELL_FLY', 'Dumbbell fly', 'HORIZONTAL_PUSH', 'INTERMEDIATE', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'With softly bent elbows, open the arms in a controlled arc, then bring the dumbbells above your chest.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (24, 'CHEST', 'PRIMARY'), (24, 'SHOULDERS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (24, 'DUMBBELL'), (24, 'BENCH');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (24, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (25, 'BARBELL_ROW', 'Barbell row', 'HORIZONTAL_PULL', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Hinge and brace your torso, pull the bar toward your lower ribs, and lower without swinging.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (25, 'BACK', 'PRIMARY'), (25, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (25, 'BARBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (26, 'DUMBBELL_ROW', 'Supported dumbbell row', 'HORIZONTAL_PULL', 'BEGINNER', TRUE, 8, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Support one hand on a bench, row the dumbbell toward your hip, and lower slowly. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (26, 'BACK', 'PRIMARY'), (26, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (26, 'DUMBBELL'), (26, 'BENCH');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (26, 'UNILATERAL'), (26, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (27, 'CABLE_ROW', 'Seated cable row', 'HORIZONTAL_PULL', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.70, 'Sit upright, pull the handles toward your ribs, and return without rounding or swinging the torso.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (27, 'BACK', 'PRIMARY'), (27, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (27, 'CABLE');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (27, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (28, 'PULL_UP', 'Pull-up', 'VERTICAL_PULL', 'ADVANCED', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Hang from the bar, pull your body upward without swinging, then lower with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (28, 'BACK', 'PRIMARY'), (28, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (28, 'PULLUP_BAR');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (29, 'CHIN_UP', 'Chin-up', 'VERTICAL_PULL', 'INTERMEDIATE', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Use an underhand grip, pull your body toward the bar, then lower through a controlled range.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (29, 'BACK', 'PRIMARY'), (29, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (29, 'PULLUP_BAR');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (30, 'LAT_PULLDOWN', 'Lat pulldown', 'VERTICAL_PULL', 'BEGINNER', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.70, 'Secure your thighs, pull the bar toward the upper chest, then let it rise with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (30, 'BACK', 'PRIMARY'), (30, 'BICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (30, 'CABLE');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (30, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (31, 'DUMBBELL_PULLOVER', 'Dumbbell pullover', 'VERTICAL_PULL', 'INTERMEDIATE', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'DUMBBELL', 0.25, 'Support your back on a bench, hold one dumbbell, lower it overhead in a controlled arc, and return.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (31, 'BACK', 'PRIMARY'), (31, 'CHEST', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (31, 'DUMBBELL'), (31, 'BENCH');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (31, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (32, 'DUMBBELL_SHOULDER_PRESS', 'Dumbbell shoulder press', 'VERTICAL_PUSH', 'BEGINNER', TRUE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.70, 'Brace your torso, press the dumbbells overhead, and lower toward shoulder level with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (32, 'SHOULDERS', 'PRIMARY'), (32, 'TRICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (32, 'DUMBBELL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (32, 'OVERHEAD');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (33, 'BARBELL_OVERHEAD_PRESS', 'Barbell overhead press', 'VERTICAL_PUSH', 'INTERMEDIATE', TRUE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.70, 'Start at shoulder height, brace without leaning back, and press the bar overhead.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (33, 'SHOULDERS', 'PRIMARY'), (33, 'TRICEPS', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (33, 'BARBELL'), (33, 'RACK');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (33, 'OVERHEAD');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (34, 'DUMBBELL_LATERAL_RAISE', 'Dumbbell lateral raise', 'SHOULDER_ABDUCTION', 'BEGINNER', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'With softly bent elbows, raise the arms out to the sides and lower slowly without swinging.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (34, 'SHOULDERS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (34, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (35, 'DUMBBELL_REVERSE_FLY', 'Dumbbell reverse fly', 'HORIZONTAL_PULL', 'BEGINNER', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'Hinge and brace, open the arms to the sides, then lower the dumbbells with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (35, 'SHOULDERS', 'PRIMARY'), (35, 'BACK', 'SECONDARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (35, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (36, 'CABLE_LATERAL_RAISE', 'Cable lateral raise', 'SHOULDER_ABDUCTION', 'BEGINNER', FALSE, 8, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.25, 'Stand beside the cable, raise one arm outward with control, then lower slowly. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (36, 'SHOULDERS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (36, 'CABLE');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (36, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (37, 'DUMBBELL_CURL', 'Dumbbell curl', 'ELBOW_FLEXION', 'BEGINNER', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'Keep upper arms steady, bend the elbows to lift the dumbbells, then lower without swinging.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (37, 'BICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (37, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (38, 'BARBELL_CURL', 'Barbell curl', 'ELBOW_FLEXION', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'BARBELL', 0.25, 'Keep the upper arms near your sides, curl the bar, and lower under control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (38, 'BICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (38, 'BARBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (39, 'CABLE_CURL', 'Cable curl', 'ELBOW_FLEXION', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.25, 'Hold the cable handle, keep elbows near your sides, and curl and lower with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (39, 'BICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (39, 'CABLE');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (40, 'DUMBBELL_HAMMER_CURL', 'Dumbbell hammer curl', 'ELBOW_FLEXION', 'BEGINNER', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'Use a neutral grip, keep upper arms steady, and lift and lower the dumbbells without swinging.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (40, 'BICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (40, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (41, 'CABLE_TRICEPS_PUSHDOWN', 'Cable triceps pushdown', 'ELBOW_EXTENSION', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.25, 'Keep elbows beside your torso, extend the arms downward, then return slowly.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (41, 'TRICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (41, 'CABLE');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (42, 'DUMBBELL_OVERHEAD_TRICEPS_EXTENSION', 'Dumbbell overhead triceps extension', 'ELBOW_EXTENSION', 'INTERMEDIATE', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'DUMBBELL', 0.25, 'Hold one dumbbell overhead, bend the elbows with control, then extend while keeping upper arms steady.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (42, 'TRICEPS', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (42, 'DUMBBELL');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (42, 'OVERHEAD');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (43, 'CLOSE_GRIP_PUSH_UP', 'Close-grip push-up', 'HORIZONTAL_PUSH', 'INTERMEDIATE', TRUE, 4, 60, 'BODYWEIGHT', NULL, 0.70, 'Place hands closer than a standard push-up, keep the body braced, and lower and press with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (43, 'TRICEPS', 'PRIMARY'), (43, 'CHEST', 'SECONDARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (44, 'BODYWEIGHT_CALF_RAISE', 'Bodyweight calf raise', 'CALF_RAISE', 'BEGINNER', FALSE, 4, 60, 'BODYWEIGHT', NULL, 0.25, 'Stand tall, rise onto the balls of your feet, pause briefly, and lower the heels with control.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (44, 'CALVES', 'PRIMARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (45, 'DUMBBELL_CALF_RAISE', 'Dumbbell calf raise', 'CALF_RAISE', 'BEGINNER', FALSE, 4, 60, 'PER_HAND_EXTERNAL', 'DUMBBELL', 0.25, 'Hold dumbbells at your sides, raise the heels, then lower slowly while keeping balance.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (45, 'CALVES', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (45, 'DUMBBELL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (46, 'MACHINE_CALF_RAISE', 'Machine calf raise', 'CALF_RAISE', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'CALF_MACHINE', 0.25, 'Adjust the machine pads, lift the heels through a controlled range, then lower slowly.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (46, 'CALVES', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (46, 'CALF_MACHINE');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (46, 'SUPPORTED');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (47, 'DEAD_BUG', 'Dead bug', 'CORE', 'BEGINNER', FALSE, 8, 60, 'BODYWEIGHT', NULL, 0.25, 'Lie on your back, brace gently, extend the opposite arm and leg, and return. Alternate both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (47, 'CORE', 'PRIMARY');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (47, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (48, 'REVERSE_CRUNCH', 'Reverse crunch', 'CORE', 'BEGINNER', FALSE, 4, 60, 'BODYWEIGHT', NULL, 0.25, 'Lie on your back, draw the knees inward with a small controlled hip lift, then lower slowly.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (48, 'CORE', 'PRIMARY');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (49, 'BIRD_DOG', 'Bird dog', 'CORE', 'BEGINNER', FALSE, 8, 60, 'BODYWEIGHT', NULL, 0.25, 'From hands and knees, extend the opposite arm and leg without rotating the trunk. Complete both sides.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (49, 'CORE', 'PRIMARY'), (49, 'GLUTES', 'SECONDARY');

INSERT INTO exercise_tag (exercise_id, tag) VALUES (49, 'UNILATERAL');

INSERT INTO exercise (id, code, name, movement_pattern, difficulty, compound, seconds_per_rep, setup_seconds, load_type, load_equipment, fatigue_cost, instructions, active) VALUES (50, 'CABLE_CRUNCH', 'Cable crunch', 'CORE', 'BEGINNER', FALSE, 4, 60, 'TOTAL_EXTERNAL', 'CABLE', 0.25, 'Hold the cable near your head, curl the trunk under control, then return without pulling with your arms.', TRUE);

INSERT INTO exercise_muscle (exercise_id, muscle_code, role) VALUES (50, 'CORE', 'PRIMARY');

INSERT INTO exercise_equipment (exercise_id, equipment_code) VALUES (50, 'CABLE');
