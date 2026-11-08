package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.LoadType;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.Exercise;

import java.math.BigDecimal;
import java.util.*;

public final class PlanningRules {
    private PlanningRules() { }
    private static final Set<MuscleGroup> MAJOR = Set.of(MuscleGroup.CHEST, MuscleGroup.BACK,
            MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES);

    public static List<ExerciseSnapshot> validateAndSort(ProfileSnapshot p, List<ExerciseSnapshot> catalog) {
        if (p == null || p.experience() == null || p.goal() == null || p.weekdays().size() < 2
                || p.weekdays().size() > 5 || p.weekdays().stream().anyMatch(d -> d < 1 || d > 7)
                || p.sessionMinutes() < 20 || p.sessionMinutes() > 120 || catalog == null) {
            throw new IllegalArgumentException("Complete a profile with 2–5 weekdays and 20–120 minutes per session.");
        }
        Set<Long> ids = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (ExerciseSnapshot e : catalog) {
            if (e == null || e.id() == null || e.id() <= 0 || e.code() == null || e.code().isBlank()
                    || !ids.add(e.id()) || !codes.add(e.code()) || e.primaryMuscle() == null
                    || e.secondaryMuscles().contains(e.primaryMuscle()) || e.difficulty() == null
                    || e.movementPattern() == null || e.loadType() == null || e.secondsPerRep() < 1
                    || e.secondsPerRep() > 30 || e.setupSeconds() < 0 || e.setupSeconds() > 300
                    || e.fatigueCost() == null || e.fatigueCost().signum() < 0 || e.fatigueCost().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("The catalog contains invalid or duplicate metadata.");
            }
            boolean external = e.loadType() == LoadType.TOTAL_EXTERNAL || e.loadType() == LoadType.PER_HAND_EXTERNAL;
            if (external != (e.loadEquipment() != null)
                    || e.loadEquipment() != null && !e.requiredEquipment().contains(e.loadEquipment())) {
                throw new IllegalArgumentException("The catalog contains inconsistent load equipment.");
            }
        }
        if (!ids.containsAll(p.preferredExerciseIds()) || !ids.containsAll(p.excludedExerciseIds())) {
            throw new IllegalArgumentException("Your exercise choices reference entries outside the current active catalog. Update your profile.");
        }
        for (ExerciseSnapshot e : catalog) {
            if (p.preferredExerciseIds().contains(e.id()) && (p.excludedExerciseIds().contains(e.id())
                    || p.excludedMovements().contains(e.movementPattern()))) {
                throw new IllegalArgumentException("A preferred exercise is also excluded. Update your profile.");
            }
        }
        return catalog.stream().sorted(Comparator.comparing(ExerciseSnapshot::code).thenComparing(ExerciseSnapshot::id)).toList();
    }

    public static boolean eligible(ExerciseSnapshot e, ProfileSnapshot p) {
        return p.equipment().containsAll(e.requiredEquipment()) && !p.excludedExerciseIds().contains(e.id())
                && !p.excludedMovements().contains(e.movementPattern())
                && e.difficulty().ordinal() <= p.experience().ordinal();
    }

    public static EnumMap<MuscleGroup, Integer> targets(ProfileSnapshot p, PlannerConfig c) {
        var base = c.volumes().get(p.experience());
        int percent = c.goals().get(p.goal()).volumePercent();
        var targets = emptyVolume();
        for (MuscleGroup muscle : MuscleGroup.values()) {
            int sets = MAJOR.contains(muscle) ? base.major() : base.accessory();
            targets.put(muscle, 2 * ((sets * percent + 99) / 100));
        }
        return targets;
    }

    public static EnumMap<MuscleGroup, Integer> emptyVolume() {
        var volume = new EnumMap<MuscleGroup, Integer>(MuscleGroup.class);
        for (MuscleGroup muscle : MuscleGroup.values()) volume.put(muscle, 0);
        return volume;
    }

    public static int credits(ExerciseSnapshot e, MuscleGroup muscle, PlannerConfig c) {
        return e.primaryMuscle() == muscle ? 2 : e.secondaryMuscles().contains(muscle) ? c.secondaryHalfCredits() : 0;
    }

    public static Exercise prescribe(ExerciseSnapshot e, ProfileSnapshot p, int sets, PlannerConfig c,
                                      String explanation, PlanningModel.Score score) {
        var goal = c.goals().get(p.goal());
        var reps = e.compound() ? goal.compound() : goal.isolation();
        return new Exercise(e.id(), sets, reps.min(), reps.max(), reps.restSeconds(), explanation, score);
    }

    public static int seconds(Exercise e, ExerciseSnapshot metadata) {
        return Math.toIntExact((long) metadata.setupSeconds() + (long) e.sets() * e.repMax() * metadata.secondsPerRep()
                + (long) (e.sets() - 1) * e.restSeconds());
    }

    public static boolean targetsReached(Map<MuscleGroup, Integer> actual, Map<MuscleGroup, Integer> target) {
        return target.entrySet().stream().allMatch(e -> actual.get(e.getKey()) >= e.getValue());
    }

    public static Map<Long, ExerciseSnapshot> byId(List<ExerciseSnapshot> catalog) {
        var result = new HashMap<Long, ExerciseSnapshot>();
        catalog.forEach(e -> result.put(e.id(), e));
        return result;
    }
}
