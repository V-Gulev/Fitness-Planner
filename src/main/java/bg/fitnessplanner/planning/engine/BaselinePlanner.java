package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.*;

import java.util.*;

/** Fixed three-set muscle rotation, with the same hard gates but no score/split search. */
public final class BaselinePlanner implements Planner {
    private final PlannerConfig config;
    public BaselinePlanner(PlannerConfig config) { this.config = config; }
    @Override public String version() { return "baseline-v1"; }

    @Override
    public Result generate(ProfileSnapshot profile, List<ExerciseSnapshot> catalog) {
        List<ExerciseSnapshot> sorted;
        try { sorted = PlanningRules.validateAndSort(profile, catalog); }
        catch (IllegalArgumentException invalid) { return new Result(Status.INVALID_INPUT, null, null, List.of(invalid.getMessage()), 0, 0); }
        var eligible = sorted.stream().filter(e -> PlanningRules.eligible(e, profile)).toList();
        var draft = new DraftPlan(profile, sorted, config, Collections.nCopies(profile.trainingDays(), DayType.FULL));
        int[] cursor = {0};
        long[] examined = {0};
        for (int day = 0; day < draft.days.size(); day++) {
            if (!addNext(draft, day, eligible, cursor, examined)) return infeasible(examined[0]);
        }
        var target = PlanningRules.targets(profile, config);
        int limit = profile.trainingDays() * config.maxExercisesPerDay();
        for (int step = 0; step < limit && !PlanningRules.targetsReached(draft.volume(), target); step++) {
            boolean added = false;
            for (int day = 0; day < draft.days.size(); day++) {
                if (PlanningRules.targetsReached(draft.volume(), target)) break;
                added |= addNext(draft, day, eligible, cursor, examined);
            }
            if (!added) break;
        }
        Plan plan = draft.freeze();
        Evaluation evaluation = new PlanEvaluator(config).evaluate(profile, sorted, plan);
        if (evaluation.hardViolations() != 0) return infeasible(examined[0]);
        return new Result(evaluation.status(), plan, evaluation,
                List.of("Simple fixed-set muscle rotation; hard constraints are enforced and preferences are ignored."), 1, examined[0]);
    }

    private boolean addNext(DraftPlan draft, int day, List<ExerciseSnapshot> eligible, int[] cursor, long[] examined) {
        var muscles = MuscleGroup.values();
        for (int offset = 0; offset < muscles.length; offset++) {
            int index = (cursor[0] + offset) % muscles.length;
            for (ExerciseSnapshot e : eligible) {
                if (e.primaryMuscle() != muscles[index]) continue;
                examined[0]++;
                Exercise item = PlanningRules.prescribe(e, draft.profile, 3, config,
                        "Selected by stable-code muscle rotation with three sets; equipment, skill, time, exclusions and primary spacing pass.", null);
                if (draft.canAdd(day, e, item)) {
                    draft.days.get(day).add(item);
                    cursor[0] = (index + 1) % muscles.length;
                    return true;
                }
            }
        }
        return false;
    }

    private Result infeasible(long examined) {
        return new Result(Status.INFEASIBLE, null, null,
                List.of("The baseline rotation did not find a nonempty, hard-constraint-compliant workout for every selected day."), 1, examined);
    }
}
