package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.Equipment;
import bg.fitnessplanner.catalog.domain.ExerciseTag;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.planning.engine.PlanningModel.DayType;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.profile.domain.TrainingGoal;

import java.util.*;

public final class ExerciseExplainer {
    private ExerciseExplainer() { }

    public static boolean isLegacyExplanation(String explanation) {
        if (explanation == null || explanation.isBlank()) return true;
        if (explanation.contains("Equipment, experience, exclusions, estimated time and primary-muscle spacing pass")) return true;
        if (explanation.contains("Selected by stable-code muscle rotation")) return true;
        if (explanation.equals("Selected in your plan editor.")) return true;
        if (explanation.startsWith("Addresses remaining weekly volume.")) return true;
        if (explanation.startsWith("Keeps this selected day nonempty.")) return true;
        return false;
    }

    public static String explain(ExerciseSnapshot e, ProfileSnapshot profile, int sets, PlannerConfig config,
                                 DayType dayType, int position) {
        var goal = profile != null && profile.goal() != null
                ? config.goals().get(profile.goal())
                : config.goals().get(TrainingGoal.HYPERTROPHY);
        var reps = e.compound() ? goal.compound() : goal.isolation();
        return explain(e, profile, sets, reps.min(), reps.max(), reps.restSeconds(), dayType, position);
    }

    public static String explain(ExerciseSnapshot e, ProfileSnapshot profile, int sets,
                                 int repMin, int repMax, int restSeconds, DayType dayType, int position) {
        if (e == null) return "Prescribed exercise.";

        StringBuilder sb = new StringBuilder();

        // 1. Role in the workout session
        sb.append(sessionRole(e, position)).append(" ");

        // 2. Favorite / user preference highlight if applicable
        if (profile != null && profile.preferredExerciseIds().contains(e.id())) {
            sb.append("Prioritized from your preferred exercise favorites. ");
        }

        // 3. Movement biomechanics & purpose
        sb.append(movementBiomechanics(e)).append(" ");

        // 4. Volume contribution & muscle synergy
        sb.append(volumeAndSynergy(e, sets)).append(" ");

        // 5. Equipment suitability
        sb.append(equipmentSuitability(e)).append(" ");

        // 6. Programming goal & rep/rest prescription
        sb.append(prescriptionGoal(profile, repMin, repMax, restSeconds));

        return sb.toString().trim();
    }

    private static String sessionRole(ExerciseSnapshot e, int position) {
        String patternLabel = e.movementPattern() != null ? e.movementPattern().getLabel() : "Compound";
        String muscleLabel = e.primaryMuscle() != null ? e.primaryMuscle().getLabel().toLowerCase(Locale.ROOT) : "target muscle";

        if (position == 0) {
            return e.compound()
                    ? "Primary compound lift (" + patternLabel + ") for " + muscleLabel + "."
                    : "Lead movement (" + patternLabel + ") targeting " + muscleLabel + ".";
        } else if (position == 1) {
            return e.compound()
                    ? "Secondary compound movement (" + patternLabel + ") for " + muscleLabel + "."
                    : "Accessory movement (" + patternLabel + ") targeting " + muscleLabel + ".";
        } else if (position >= 3 && !e.compound()) {
            return "Targeted isolation finisher (" + patternLabel + ") for " + muscleLabel + ".";
        } else {
            return e.compound()
                    ? "Compound builder (" + patternLabel + ") for " + muscleLabel + "."
                    : "Targeted isolation exercise (" + patternLabel + ") for " + muscleLabel + ".";
        }
    }

    private static String movementBiomechanics(ExerciseSnapshot e) {
        if (e.tags() != null && e.tags().contains(ExerciseTag.UNILATERAL)) {
            return "Provides unilateral loading to eliminate side-to-side strength imbalances and challenge core stability.";
        }

        MovementPattern pattern = e.movementPattern();
        if (pattern == null) {
            return e.compound()
                    ? "Engages multiple muscle groups simultaneously for efficient mechanical tension and progressive overload."
                    : "Isolates the target muscle directly to accumulate volume with minimal systemic fatigue.";
        }

        return switch (pattern) {
            case SQUAT -> e.compound()
                    ? "Acts as your foundational knee-extension compound to drive quadriceps recruitment and lower-body force production."
                    : "Provides focused knee-extension stimulus with minimal systemic fatigue.";
            case HINGE -> "Recruits the posterior chain with a hip-hinge pattern, emphasizing hamstring stretch and glute extension while sparing the knees.";
            case HIP_EXTENSION -> "Emphasizes hip extension mechanics to develop glute power and posterior-chain resilience.";
            case HORIZONTAL_PUSH -> e.compound()
                    ? "Develops pressing power and pectoral thickness through a stable horizontal plane of motion."
                    : "Isolates the chest fibers across a horizontal arc without heavy triceps lockout strain.";
            case VERTICAL_PUSH -> "Builds overhead pressing strength, anterior deltoid development, and upward scapular stability.";
            case HORIZONTAL_PULL -> "Provides horizontal rowing mechanics to build mid-back thickness, rhomboids, and scapular retractors.";
            case VERTICAL_PULL -> "Provides vertical pulling mechanics to build lat width and balance horizontal pressing volume.";
            case ELBOW_FLEXION -> "Directly isolates the biceps through focused elbow flexion without requiring back or grip fatigue.";
            case ELBOW_EXTENSION -> "Isolates the triceps with pure elbow extension, completing pressing volume without extra shoulder joint strain.";
            case SHOULDER_ABDUCTION -> "Directly isolates the lateral deltoids to build shoulder width and capping with minimal systemic fatigue.";
            case KNEE_FLEXION -> "Directly isolates the hamstrings through knee flexion, delivering peak contraction without spinal loading.";
            case CALF_RAISE -> "Provides focused plantar flexion volume to stimulate calf development and ankle joint stability.";
            case CORE -> e.compound()
                    ? "Engages the abdominal wall under compound load to stabilize the trunk and transfer force efficiently."
                    : "Directly challenges abdominal bracing and core stability without systemic fatigue.";
        };
    }

    private static String volumeAndSynergy(ExerciseSnapshot e, int sets) {
        String muscle = e.primaryMuscle() != null ? e.primaryMuscle().getLabel().toLowerCase(Locale.ROOT) : "target";
        String primary = "Prescribed for " + sets + " " + (sets == 1 ? "set" : "sets") + " toward your weekly " + muscle + " volume target.";

        if (e.secondaryMuscles() != null && !e.secondaryMuscles().isEmpty()) {
            String secondaryList = formatSecondaryMuscles(e.secondaryMuscles());
            return primary + " Also recruits " + secondaryList + " as secondary movers for multi-muscle synergy.";
        }
        return primary + " Focuses stimulation cleanly on the " + muscle + " with zero fatigue spillover into other muscle groups.";
    }

    private static String equipmentSuitability(ExerciseSnapshot e) {
        if (e.requiredEquipment() == null || e.requiredEquipment().isEmpty()) {
            return "Uses bodyweight resistance with zero setup requirements.";
        }
        if (e.requiredEquipment().contains(Equipment.RACK) && e.requiredEquipment().contains(Equipment.BARBELL)) {
            return "Utilizes the rack and barbell setup for safe, heavy loading.";
        }
        if (e.requiredEquipment().contains(Equipment.BARBELL)) {
            return "Utilizes your barbell setup for steady progressive overload.";
        }
        if (e.requiredEquipment().contains(Equipment.DUMBBELL)) {
            return "Uses dumbbells for independent limb loading and natural wrist/joint paths.";
        }
        if (e.requiredEquipment().contains(Equipment.CABLE)) {
            return "Takes advantage of constant cable tension throughout the entire range of motion.";
        }
        if (e.requiredEquipment().contains(Equipment.PULLUP_BAR)) {
            return "Utilizes the pull-up bar for a functional closed-chain movement.";
        }
        if (e.requiredEquipment().contains(Equipment.LEG_PRESS)) {
            return "Utilizes the leg press machine for heavy quadriceps loading with lower back support.";
        }
        if (e.requiredEquipment().contains(Equipment.LEG_CURL)) {
            return "Utilizes the leg curl machine for isolated hamstring loading with joint support.";
        }
        if (e.requiredEquipment().contains(Equipment.CALF_MACHINE)) {
            return "Utilizes the calf machine for focused calf overload through a full stretch.";
        }
        return "Matches your available " + e.equipmentLabel().toLowerCase(Locale.ROOT) + " equipment.";
    }

    private static String prescriptionGoal(ProfileSnapshot profile, int repMin, int repMax, int restSeconds) {
        TrainingGoal goal = profile != null ? profile.goal() : TrainingGoal.HYPERTROPHY;
        if (goal == TrainingGoal.STRENGTH) {
            return "Programmed in the " + repMin + "–" + repMax + " rep range with " + restSeconds + "s rest to build maximal force and neuromuscular drive.";
        } else if (goal == TrainingGoal.GENERAL_FITNESS) {
            return "Programmed for " + repMin + "–" + repMax + " reps with " + restSeconds + "s rest to support movement quality and muscular endurance.";
        } else {
            return "Programmed for " + repMin + "–" + repMax + " reps with " + restSeconds + "s rest to maximize mechanical tension and hypertrophy.";
        }
    }

    private static String formatSecondaryMuscles(Set<MuscleGroup> secondary) {
        List<String> names = secondary.stream()
                .sorted(Comparator.comparing(MuscleGroup::name))
                .map(m -> m.getLabel().toLowerCase(Locale.ROOT))
                .toList();
        if (names.size() == 1) return names.get(0);
        if (names.size() == 2) return names.get(0) + " and " + names.get(1);
        return String.join(", ", names.subList(0, names.size() - 1)) + ", and " + names.get(names.size() - 1);
    }
}

