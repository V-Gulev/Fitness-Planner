package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.planning.engine.PlanningModel.DayType;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ExerciseExplainerTest {

    @Test
    void detectsLegacyExplanationsCorrectly() {
        assertThat(ExerciseExplainer.isLegacyExplanation(null)).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("")).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("   ")).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("Addresses remaining weekly volume. Adds 3 primary sets for chest. Equipment, experience, exclusions, estimated time and primary-muscle spacing pass.")).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("Keeps this selected day nonempty. Adds 3 primary sets for back. Equipment, experience, exclusions, estimated time and primary-muscle spacing pass.")).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("Selected by stable-code muscle rotation with three sets; equipment, skill, time, exclusions and primary spacing pass.")).isTrue();
        assertThat(ExerciseExplainer.isLegacyExplanation("Selected in your plan editor.")).isTrue();

        assertThat(ExerciseExplainer.isLegacyExplanation("Primary compound lift (Squat) for quadriceps. Acts as your foundational knee-extension compound.")).isFalse();
    }

    @Test
    void generatesSpecificExplanationForCompoundOpener() {
        var squat = new ExerciseSnapshot(1L, "BARBELL_BACK_SQUAT", "Barbell back squat",
                MuscleGroup.QUADRICEPS, Set.of(MuscleGroup.GLUTES, MuscleGroup.CORE), MovementPattern.SQUAT,
                Set.of(Equipment.BARBELL, Equipment.RACK), ExperienceLevel.INTERMEDIATE, true,
                4, 60, LoadType.TOTAL_EXTERNAL, Equipment.BARBELL, new BigDecimal("0.7"), Set.of(), "Cues");

        var profile = new ProfileSnapshot(1L, 0L, ExperienceLevel.INTERMEDIATE, TrainingGoal.HYPERTROPHY,
                60, Set.of(1, 3, 5), Set.of(Equipment.BARBELL, Equipment.RACK), Set.of(), Set.of(), Set.of());

        String explanation = ExerciseExplainer.explain(squat, profile, 3, 8, 12, 90, DayType.LOWER, 0);

        assertThat(explanation).contains("Primary compound lift (Squat) for quadriceps.");
        assertThat(explanation).contains("knee-extension compound to drive quadriceps recruitment");
        assertThat(explanation).contains("Prescribed for 3 sets toward your weekly quadriceps volume target.");
        assertThat(explanation).contains("recruits core and glutes as secondary movers");
        assertThat(explanation).contains("rack and barbell setup");
        assertThat(explanation).contains("8–12 reps with 90s rest to maximize mechanical tension and hypertrophy");
    }

    @Test
    void generatesSpecificExplanationForIsolationFinisher() {
        var lateralRaise = new ExerciseSnapshot(20L, "DUMBBELL_LATERAL_RAISE", "Dumbbell lateral raise",
                MuscleGroup.SHOULDERS, Set.of(), MovementPattern.SHOULDER_ABDUCTION,
                Set.of(Equipment.DUMBBELL), ExperienceLevel.BEGINNER, false,
                3, 30, LoadType.PER_HAND_EXTERNAL, Equipment.DUMBBELL, new BigDecimal("0.2"), Set.of(), "Cues");

        var profile = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY,
                60, Set.of(1, 3, 5), Set.of(Equipment.DUMBBELL), Set.of(), Set.of(), Set.of());

        String explanation = ExerciseExplainer.explain(lateralRaise, profile, 3, 10, 15, 60, DayType.UPPER, 4);

        assertThat(explanation).contains("Targeted isolation finisher (Shoulder abduction) for shoulders.");
        assertThat(explanation).contains("Directly isolates the lateral deltoids to build shoulder width and capping");
        assertThat(explanation).contains("Prescribed for 3 sets toward your weekly shoulders volume target.");
        assertThat(explanation).contains("Focuses stimulation cleanly on the shoulders with zero fatigue spillover into other muscle groups.");
        assertThat(explanation).contains("Uses dumbbells for independent limb loading");
        assertThat(explanation).contains("10–15 reps with 60s rest");
    }

    @Test
    void highlightsPreferredExercise() {
        var bench = new ExerciseSnapshot(10L, "BARBELL_BENCH_PRESS", "Barbell bench press",
                MuscleGroup.CHEST, Set.of(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS), MovementPattern.HORIZONTAL_PUSH,
                Set.of(Equipment.BARBELL, Equipment.BENCH), ExperienceLevel.INTERMEDIATE, true,
                4, 60, LoadType.TOTAL_EXTERNAL, Equipment.BARBELL, new BigDecimal("0.6"), Set.of(), "Cues");

        var profile = new ProfileSnapshot(1L, 0L, ExperienceLevel.INTERMEDIATE, TrainingGoal.STRENGTH,
                60, Set.of(1, 3, 5), Set.of(Equipment.BARBELL, Equipment.BENCH), Set.of(10L), Set.of(), Set.of());

        String explanation = ExerciseExplainer.explain(bench, profile, 4, 4, 6, 180, DayType.PUSH, 0);

        assertThat(explanation).contains("Prioritized from your preferred exercise favorites.");
        assertThat(explanation).contains("Develops pressing power and pectoral thickness");
        assertThat(explanation).contains("4–6 rep range with 180s rest to build maximal force and neuromuscular drive");
    }

    @Test
    void handlesUnilateralExercises() {
        var lunge = new ExerciseSnapshot(15L, "DUMBBELL_SPLIT_SQUAT", "Dumbbell split squat",
                MuscleGroup.QUADRICEPS, Set.of(MuscleGroup.GLUTES), MovementPattern.SQUAT,
                Set.of(Equipment.DUMBBELL), ExperienceLevel.INTERMEDIATE, true,
                8, 60, LoadType.PER_HAND_EXTERNAL, Equipment.DUMBBELL, new BigDecimal("0.7"),
                Set.of(ExerciseTag.UNILATERAL), "Cues");

        var profile = new ProfileSnapshot(1L, 0L, ExperienceLevel.INTERMEDIATE, TrainingGoal.HYPERTROPHY,
                60, Set.of(1, 3), Set.of(Equipment.DUMBBELL), Set.of(), Set.of(), Set.of());

        String explanation = ExerciseExplainer.explain(lunge, profile, 3, 8, 12, 90, DayType.LOWER, 1);

        assertThat(explanation).contains("unilateral loading to eliminate side-to-side strength imbalances");
    }

    @Test
    void handlesBodyweightWithZeroEquipment() {
        var pushup = new ExerciseSnapshot(30L, "BODYWEIGHT_PUSH_UP", "Bodyweight push-up",
                MuscleGroup.CHEST, Set.of(MuscleGroup.TRICEPS), MovementPattern.HORIZONTAL_PUSH,
                Set.of(), ExperienceLevel.BEGINNER, true,
                3, 0, LoadType.BODYWEIGHT, null, new BigDecimal("0.4"), Set.of(), "Cues");

        String explanation = ExerciseExplainer.explain(pushup, null, 3, 10, 15, 60, DayType.FULL, 2);

        assertThat(explanation).contains("Uses bodyweight resistance with zero setup requirements.");
    }
}
