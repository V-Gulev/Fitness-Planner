package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.*;

import java.util.*;

/** Recomputes constraints and quality from the output; never trusts construction state or scores. */
public final class PlanEvaluator {
    private final PlannerConfig config;
    public PlanEvaluator(PlannerConfig config) { this.config = config; }

    public Evaluation evaluate(ProfileSnapshot profile, List<ExerciseSnapshot> catalog, Plan plan) {
        return evaluate(profile, catalog, plan, false);
    }

    public Evaluation evaluateEdited(ProfileSnapshot profile, List<ExerciseSnapshot> catalog, Plan plan) {
        return evaluate(profile, catalog, plan, true);
    }

    private Evaluation evaluate(ProfileSnapshot profile, List<ExerciseSnapshot> catalog, Plan plan, boolean edited) {
        PlanningRules.validateAndSort(profile, catalog);
        var known = PlanningRules.byId(catalog);
        Map<String, Integer> violations = new TreeMap<>();
        var actual = PlanningRules.emptyVolume();
        var target = PlanningRules.targets(profile, config);
        Map<Integer, Integer> seconds = new TreeMap<>();
        Map<MuscleGroup, SortedSet<Integer>> exposures = new EnumMap<>(MuscleGroup.class);
        Set<Integer> days = new HashSet<>();
        Set<Long> used = new HashSet<>();
        int occurrences = 0;
        for (Day day : plan.days()) {
            if (!profile.weekdays().contains(day.weekday()) || !days.add(day.weekday())) hit(violations, "schedule");
            if (day.exercises().isEmpty()) hit(violations, "empty_day");
            int duration = config.warmupSeconds();
            int totalSets = 0;
            Set<Long> withinDay = new HashSet<>();
            for (Exercise item : day.exercises()) {
                occurrences++;
                used.add(item.exerciseId());
                if (!withinDay.add(item.exerciseId())) hit(violations, "same_day_duplicate");
                ExerciseSnapshot e = known.get(item.exerciseId());
                if (e == null) { hit(violations, "catalog_skill"); continue; }
                if (!profile.equipment().containsAll(e.requiredEquipment())) hit(violations, "equipment");
                if (profile.excludedExerciseIds().contains(e.id()) || profile.excludedMovements().contains(e.movementPattern())) hit(violations, "exclusion");
                if (e.difficulty().ordinal() > profile.experience().ordinal()) hit(violations, "catalog_skill");
                if (day.type() == null || !day.type().allows(e.primaryMuscle())) hit(violations, "day_type");
                var expected = PlanningRules.prescribe(e, profile, item.sets(), config, "", null);
                boolean invalidRepsOrRest = edited
                        ? item.repMin() < 1 || item.repMax() < item.repMin() || item.repMax() > 50
                                || item.restSeconds() < 0 || item.restSeconds() > 600
                        : item.repMin() != expected.repMin() || item.repMax() != expected.repMax()
                                || item.restSeconds() != expected.restSeconds();
                if (item.sets() < 2 || item.sets() > 4 || invalidRepsOrRest) {
                    hit(violations, "prescription");
                    continue;
                }
                totalSets += item.sets();
                duration += PlanningRules.seconds(item, e);
                for (MuscleGroup m : MuscleGroup.values()) actual.merge(m, item.sets() * PlanningRules.credits(e, m, config), Integer::sum);
                if (day.weekday() >= 1 && day.weekday() <= 7) {
                    exposures.computeIfAbsent(e.primaryMuscle(), ignored -> new TreeSet<>()).add(day.weekday());
                }
            }
            if (day.exercises().size() > config.maxExercisesPerDay() || totalSets > config.maxSetsPerDay()) hit(violations, "bounds");
            if (duration > profile.sessionMinutes() * 60) hit(violations, "duration");
            seconds.merge(day.weekday(), duration, Integer::sum);
        }
        for (Integer requested : profile.weekdays()) if (!days.contains(requested)) hit(violations, "schedule");
        for (SortedSet<Integer> muscleDays : exposures.values()) {
            List<Integer> ordered = new ArrayList<>(muscleDays);
            for (int i = 0; i < ordered.size(); i++) {
                int next = i + 1 == ordered.size() ? ordered.getFirst() + 7 : ordered.get(i + 1);
                if (next - ordered.get(i) < config.minRecoveryGapDays()) hit(violations, "recovery");
            }
        }
        int deviation = 0, missing = 0;
        for (MuscleGroup m : MuscleGroup.values()) {
            deviation += Math.abs(target.get(m) - actual.get(m));
            if (actual.get(m) == 0) missing++;
        }
        return new Evaluation(violations, target, actual, seconds, deviation, missing,
                occurrences - used.size(), seconds.values().stream().mapToInt(Integer::intValue).sum());
    }

    private static void hit(Map<String, Integer> counts, String code) { counts.merge(code, 1, Integer::sum); }
}
