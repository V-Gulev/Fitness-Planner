package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import bg.fitnessplanner.planning.engine.PlanningModel.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class PlannerTest {
    static final PlannerConfig CONFIG = config();

    static PlannerConfig config() {
        try (var input = PlannerTest.class.getResourceAsStream("/planner-v1.json")) {
            return new ObjectMapper().readValue(input, PlannerConfig.class);
        } catch (Exception failure) { throw new IllegalStateException(failure); }
    }

    static ProfileSnapshot profile(Set<Integer> weekdays) {
        return new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY, 60,
                weekdays, Set.of(), Set.of(), Set.of(), Set.of());
    }

    static ExerciseSnapshot exercise(long id, MuscleGroup primary, Set<MuscleGroup> secondary,
                                      Set<Equipment> equipment, ExperienceLevel difficulty, int q, int setup) {
        var patterns = MovementPattern.values();
        return new ExerciseSnapshot(id, "EXERCISE_" + String.format(Locale.ROOT, "%02d", id), "Fixture " + id,
                primary, secondary, patterns[(int) ((id - 1) % patterns.length)], equipment, difficulty, true,
                q, setup, equipment.isEmpty() ? LoadType.BODYWEIGHT : LoadType.TOTAL_EXTERNAL,
                equipment.isEmpty() ? null : equipment.iterator().next(), new BigDecimal("0.4"), Set.of(), "Test fixture cues.");
    }

    static List<ExerciseSnapshot> catalog() {
        List<ExerciseSnapshot> result = new ArrayList<>();
        int id = 1;
        for (MuscleGroup muscle : MuscleGroup.values()) result.add(exercise(id++, muscle, Set.of(), Set.of(), ExperienceLevel.BEGINNER, 4, 60));
        return result;
    }

    private List<Planner> planners() { return List.of(new ScoredPlanner(CONFIG), new BaselinePlanner(CONFIG)); }

    @Test
    void bothPlannersPublishOnlyHardConstraintCompliantPlans() {
        var p = profile(Set.of(1, 3, 5));
        for (Planner planner : planners()) {
            var result = planner.generate(p, catalog());
            assertThat(result.publishable()).isTrue();
            assertThat(result.evaluation().hardViolations()).isZero();
            assertThat(result.plan().days()).extracting(Day::weekday).containsExactly(1, 3, 5);
            assertThat(result.plan().days()).allSatisfy(day -> assertThat(day.exercises()).isNotEmpty());
            assertThat(result.additionsExamined()).isLessThanOrEqualTo(25L * 41 * 5 * 10 * 3);
        }
    }

    @Test
    void catalogOrderDoesNotAffectEitherPlanner() {
        var reversed = new ArrayList<>(catalog());
        Collections.reverse(reversed);
        for (Planner planner : planners()) {
            assertThat(planner.generate(profile(Set.of(1, 3, 5)), reversed))
                    .isEqualTo(planner.generate(profile(Set.of(5, 3, 1)), catalog()));
        }
    }

    @Test
    void unavailableEquipmentCannotBeSelectedEvenWhenPreferred() {
        var e = exercise(1, MuscleGroup.CHEST, Set.of(), Set.of(Equipment.BARBELL), ExperienceLevel.BEGINNER, 4, 60);
        var p = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY, 60,
                Set.of(1, 3), Set.of(), Set.of(1L), Set.of(), Set.of());
        for (Planner planner : planners()) assertThat(planner.generate(p, List.of(e)).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void exclusionsAndDifficultyAreHardFilters() {
        var e = catalog().getFirst();
        var p = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY, 60,
                Set.of(1, 3), Set.of(), Set.of(), Set.of(1L), Set.of());
        assertThat(PlanningRules.eligible(e, p)).isFalse();
        var excludedMovement = new ProfileSnapshot(1L, 0L, p.experience(), p.goal(), 60, p.weekdays(),
                Set.of(), Set.of(), Set.of(), Set.of(e.movementPattern()));
        assertThat(PlanningRules.eligible(e, excludedMovement)).isFalse();
        assertThat(PlanningRules.eligible(exercise(2, MuscleGroup.BACK, Set.of(), Set.of(), ExperienceLevel.ADVANCED, 4, 60), p)).isFalse();
    }

    @Test
    void emptyCatalogTerminatesWithNoInventedPlan() {
        for (Planner planner : planners()) {
            var result = planner.generate(profile(Set.of(1, 3)), List.of());
            assertThat(result.status()).isEqualTo(Status.INFEASIBLE);
            assertThat(result.plan()).isNull();
            assertThat(result.diagnostics()).isNotEmpty();
        }
    }

    @Test
    void conflictingOrUnknownRulesAreInvalidInputs() {
        var p = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY, 60,
                Set.of(1, 3), Set.of(), Set.of(1L), Set.of(1L), Set.of());
        for (Planner planner : planners()) assertThat(planner.generate(p, catalog()).status()).isEqualTo(Status.INVALID_INPUT);
        var unknown = new ProfileSnapshot(1L, 0L, p.experience(), p.goal(), 60, p.weekdays(), Set.of(), Set.of(999L), Set.of(), Set.of());
        assertThat(new ScoredPlanner(CONFIG).generate(unknown, catalog()).status()).isEqualTo(Status.INVALID_INPUT);
    }

    @Test
    void duplicateCatalogIdsAreRejected() {
        var e = catalog().getFirst();
        assertThat(new ScoredPlanner(CONFIG).generate(profile(Set.of(1, 3)), List.of(e, e)).status()).isEqualTo(Status.INVALID_INPUT);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    void splitFamiliesAreBoundedAndDeduplicated(int count) {
        var candidates = SplitCandidates.forFrequency(count);
        assertThat(candidates).hasSizeBetween(2, 25).doesNotHaveDuplicates();
        assertThat(candidates).allSatisfy(c -> assertThat(c).hasSize(count));
    }

    @Test
    void actualCatalogFeasibilityCanRejectNominalUpperLowerSplit() {
        var upperOnly = catalog().stream().filter(e -> e.primaryMuscle() == MuscleGroup.CHEST || e.primaryMuscle() == MuscleGroup.BACK).toList();
        var result = new ScoredPlanner(CONFIG).generate(profile(Set.of(1, 2)), upperOnly);
        assertThat(result.publishable()).isTrue();
        assertThat(result.plan().candidateKey()).isEqualTo("FULL/FULL");
        assertThat(result.candidatesConstructed()).isGreaterThan(1);
    }

    @Test
    void timeCalculationMatchesTheWorkedExample() {
        var p = profile(Set.of(1, 3));
        var e = catalog().getFirst();
        assertThat(PlanningRules.seconds(PlanningRules.prescribe(e, p, 3, CONFIG, "", null), e)).isEqualTo(420);
    }

    @Test
    void independentEvaluatorAcceptsExactBudgetAndRejectsOneSecondOver() {
        var p = new ProfileSnapshot(1L, 0L, ExperienceLevel.BEGINNER, TrainingGoal.HYPERTROPHY, 20,
                Set.of(1, 3), Set.of(), Set.of(), Set.of(), Set.of());
        var e = exercise(1, MuscleGroup.CHEST, Set.of(), Set.of(), ExperienceLevel.BEGINNER, 30, 180);
        var item = PlanningRules.prescribe(e, p, 2, CONFIG, "", null);
        var plan = new Plan("FULL/FULL", List.of(new Day(1, DayType.FULL, List.of(item)), new Day(3, DayType.FULL, List.of(item))));
        assertThat(new PlanEvaluator(CONFIG).evaluate(p, List.of(e), plan).hardViolations()).isZero();
        var slower = exercise(1, MuscleGroup.CHEST, Set.of(), Set.of(), ExperienceLevel.BEGINNER, 30, 181);
        assertThat(new PlanEvaluator(CONFIG).evaluate(p, List.of(slower), plan).violations().get("duration")).isEqualTo(2);
    }

    @Test
    void cyclicRecoveryIncludesSundayToMonday() {
        var p = profile(Set.of(1, 7));
        var e = catalog().getFirst();
        var item = PlanningRules.prescribe(e, p, 3, CONFIG, "", null);
        var plan = new Plan("FULL/FULL", List.of(new Day(1, DayType.FULL, List.of(item)), new Day(7, DayType.FULL, List.of(item))));
        assertThat(new PlanEvaluator(CONFIG).evaluate(p, catalog(), plan).violations().get("recovery")).isEqualTo(1);
        for (Planner planner : planners()) assertThat(planner.generate(p, List.of(e)).status()).isEqualTo(Status.INFEASIBLE);
    }

    @Test
    void primaryAndSecondaryVolumeUseExplicitHalfSetCredits() {
        var p = profile(Set.of(1, 3));
        var e = exercise(1, MuscleGroup.CHEST, Set.of(MuscleGroup.TRICEPS), Set.of(), ExperienceLevel.BEGINNER, 4, 60);
        var item = PlanningRules.prescribe(e, p, 3, CONFIG, "", null);
        var plan = new Plan("FULL/FULL", List.of(new Day(1, DayType.FULL, List.of(item)), new Day(3, DayType.FULL, List.of(item))));
        var evaluation = new PlanEvaluator(CONFIG).evaluate(p, List.of(e), plan);
        assertThat(evaluation.volumeHalfSets().get(MuscleGroup.CHEST)).isEqualTo(12);
        assertThat(evaluation.volumeHalfSets().get(MuscleGroup.TRICEPS)).isEqualTo(6);
    }

    @Test
    void scoreComponentsAndTotalAreHandCheckable() {
        var p = profile(Set.of(1, 3));
        var e = catalog().getFirst();
        var target = PlanningRules.targets(p, CONFIG);
        var actual = PlanningRules.emptyVolume();
        actual.put(MuscleGroup.CHEST, 13); // target 16 units; proposed two sets add four units.
        var score = new ExerciseScorer(CONFIG).score(e, 2, p, actual, target, false, true, 0, 6);
        assertThat(score.muscle()).isEqualByComparingTo("0.75");
        assertThat(score.overshoot()).isEqualByComparingTo("0.25");
        assertThat(score.fatigue()).isEqualByComparingTo("0.2");
        assertThat(score.total()).isEqualByComparingTo("4.8");
    }

    @Test
    void evaluatorRejectsTamperedOutputWithoutTrustingExplanations() {
        var p = profile(Set.of(1, 3));
        var item = new Exercise(999L, 0, 99, 99, -1, "All constraints pass", null);
        var result = new PlanEvaluator(CONFIG).evaluate(p, catalog(), new Plan("fake", List.of(new Day(1, DayType.FULL, List.of(item, item)))));
        assertThat(result.hardViolations()).isPositive();
        assertThat(result.violations()).containsKeys("catalog_skill", "same_day_duplicate", "schedule");
    }
}
