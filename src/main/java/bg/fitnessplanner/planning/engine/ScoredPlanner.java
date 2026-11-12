package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.*;

import java.util.*;

public final class ScoredPlanner implements Planner {
    private final PlannerConfig config;
    private final PlanEvaluator evaluator;
    private final ExerciseScorer scorer;

    public ScoredPlanner(PlannerConfig config) {
        this.config = config;
        this.evaluator = new PlanEvaluator(config);
        this.scorer = new ExerciseScorer(config);
    }
    @Override public String version() { return "scored-v1"; }

    @Override
    public Result generate(ProfileSnapshot profile, List<ExerciseSnapshot> catalog) {
        return search(profile, catalog, false).getFirst();
    }

    /** Returns up to three distinct prescriptions in the same order used to choose the winner. */
    public List<Result> candidates(ProfileSnapshot profile, List<ExerciseSnapshot> catalog) {
        return search(profile, catalog, true);
    }

    private List<Result> search(ProfileSnapshot profile, List<ExerciseSnapshot> catalog, boolean alternatives) {
        List<ExerciseSnapshot> sorted;
        try { sorted = PlanningRules.validateAndSort(profile, catalog); }
        catch (IllegalArgumentException invalid) { return List.of(new Result(Status.INVALID_INPUT, null, null, List.of(invalid.getMessage()), 0, 0)); }
        var eligible = sorted.stream().filter(e -> PlanningRules.eligible(e, profile)).toList();
        var target = PlanningRules.targets(profile, config);
        var ranked = new ArrayList<Result>();
        long[] examined = {0};
        int candidates = 0;
        for (var structure : SplitCandidates.forFrequency(profile.trainingDays())) {
            candidates++;
            DraftPlan draft = new DraftPlan(profile, sorted, config, structure);
            boolean nonempty = true;
            for (int day = 0; day < structure.size(); day++) {
                Addition addition = bestAddition(draft, eligible, target, day, false, examined);
                if (addition == null) { nonempty = false; break; }
                draft.days.get(day).add(addition.exercise());
            }
            if (!nonempty) continue;
            int limit = config.maxExercisesPerDay() * profile.trainingDays();
            for (int step = profile.trainingDays(); step < limit && !PlanningRules.targetsReached(draft.volume(), target); step++) {
                Addition addition = bestAddition(draft, eligible, target, -1, true, examined);
                if (addition == null) break;
                draft.days.get(addition.day()).add(addition.exercise());
            }
            Plan plan = draft.freeze();
            Evaluation quality = evaluator.evaluate(profile, sorted, plan);
            if (quality.hardViolations() != 0) continue;
            Result result = new Result(quality.status(), plan, quality, List.of(), candidates, examined[0]);
            if (alternatives || ranked.isEmpty()) {
                ranked.add(result);
            } else if (compare(result, ranked.getFirst()) < 0) {
                ranked.set(0, result);
            }
        }
        if (ranked.isEmpty()) return List.of(new Result(Status.INFEASIBLE, null, null,
                List.of("No plan found with a nonempty workout on every selected day while respecting equipment, exclusions, time and primary-muscle spacing.",
                        "Try different weekdays, more time or equipment, or fewer exclusions. This bounded heuristic does not prove that no solution exists."), candidates, examined[0]));
        if (alternatives) ranked.sort(ScoredPlanner::compare);
        var distinct = new HashSet<List<List<Prescription>>>();
        int constructed = candidates;
        return ranked.stream().filter(result -> !alternatives || distinct.add(result.plan().days().stream()
                .map(day -> day.exercises().stream().map(item -> new Prescription(item.exerciseId(), item.sets()))
                        .sorted(Comparator.comparing(Prescription::exerciseId)).toList()).toList()))
                .limit(3).map(best -> new Result(best.status(), best.plan(), best.evaluation(), List.of(
                "Evaluated " + constructed + " candidate structures using your actual weekdays and eligible catalog.",
                best.status() == Status.DEGRADED ? "Hard constraints pass; some modeled weekly volume targets are under or over target. See the volume table."
                        : "Hard constraints pass and modeled weekly volume targets match."), constructed, examined[0])).toList();
    }

    private Addition bestAddition(DraftPlan draft, List<ExerciseSnapshot> eligible,
                                  Map<MuscleGroup, Integer> target, int onlyDay, boolean improvement, long[] examined) {
        var volume = draft.volume();
        Set<Long> used = new HashSet<>();
        draft.days.forEach(day -> day.forEach(item -> used.add(item.exerciseId())));
        Addition best = null;
        for (int day = 0; day < draft.days.size(); day++) {
            if (onlyDay >= 0 && onlyDay != day) continue;
            for (ExerciseSnapshot e : eligible) for (int sets = 2; sets <= 4; sets++) {
                examined[0]++;
                Exercise base = PlanningRules.prescribe(e, draft.profile, sets, config, "", null);
                if (!draft.canAdd(day, e, base)) continue;
                int patterns = 0, compoundSets = 0;
                for (Exercise item : draft.days.get(day)) {
                    var previous = draft.catalog.get(item.exerciseId());
                    if (previous.movementPattern() == e.movementPattern()) patterns++;
                    if (previous.compound()) compoundSets += item.sets();
                }
                boolean preferred = draft.profile.preferredExerciseIds().contains(e.id());
                Score score = scorer.score(e, sets, draft.profile, volume, target, preferred, !used.contains(e.id()), patterns, compoundSets);
                if (improvement && (score.muscle().signum() <= 0 || score.total().signum() <= 0)) continue;
                Exercise item = new Exercise(e.id(), sets, base.repMin(), base.repMax(), base.restSeconds(), "", score);
                Addition addition = new Addition(day, item, score, PlanningRules.seconds(item, e), e.code(), draft.weekdays.get(day));
                if (best == null || ADDITION_ORDER.compare(addition, best) < 0) best = addition;
            }
        }
        if (best == null) return null;
        var chosen = draft.catalog.get(best.exercise().exerciseId());
        String explanation = ExerciseExplainer.explain(chosen, draft.profile, best.exercise().sets(),
                best.exercise().repMin(), best.exercise().repMax(), best.exercise().restSeconds(),
                draft.types.get(best.day()), draft.days.get(best.day()).size());
        Exercise finalItem = new Exercise(best.exercise().exerciseId(), best.exercise().sets(),
                best.exercise().repMin(), best.exercise().repMax(), best.exercise().restSeconds(),
                explanation, best.score());
        return new Addition(best.day(), finalItem, best.score(), best.seconds(), best.code(), best.weekday());
    }

    private static int compare(Result a, Result b) {
        int comparison = Comparator.comparingInt(Evaluation::absoluteDeviationHalfSets).thenComparingInt(Evaluation::missingMuscles)
                .thenComparingInt(Evaluation::repeatedOccurrences).thenComparingInt(Evaluation::totalSeconds)
                .compare(a.evaluation(), b.evaluation());
        return comparison != 0 ? comparison : a.plan().candidateKey().compareTo(b.plan().candidateKey());
    }

    private record Prescription(Long exerciseId, int sets) { }

    private record Addition(int day, Exercise exercise, Score score, int seconds, String code, int weekday) { }
    private static final Comparator<Addition> ADDITION_ORDER = Comparator
            .comparing((Addition a) -> a.score().total(), Comparator.reverseOrder()).thenComparingInt(Addition::seconds)
            .thenComparing(Addition::code).thenComparingInt(a -> a.exercise().sets()).thenComparingInt(Addition::weekday);
}
