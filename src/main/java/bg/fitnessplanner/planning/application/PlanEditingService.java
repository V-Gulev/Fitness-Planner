package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.planning.engine.PlanEvaluator;
import bg.fitnessplanner.planning.engine.PlanningModel.*;
import bg.fitnessplanner.planning.engine.PlanningRules;
import bg.fitnessplanner.planning.persistence.WorkoutPlanRepository;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PlanEditingService {
    private static final Map<String, String> MESSAGES = Map.ofEntries(
            Map.entry("equipment", "An exercise requires equipment outside your profile."),
            Map.entry("exclusion", "An exercise conflicts with your exclusions."),
            Map.entry("catalog_skill", "An exercise is unavailable for your current profile."),
            Map.entry("day_type", "An exercise does not fit the selected day type. Change the day type or replace the exercise."),
            Map.entry("prescription", "Use 2–4 sets, a rep range from 1–50, and 0–600 seconds of rest."),
            Map.entry("same_day_duplicate", "An exercise appears more than once on a day."),
            Map.entry("schedule", "Keep the training days in your saved profile."),
            Map.entry("empty_day", "Every training day needs at least one exercise."),
            Map.entry("duration", "A workout exceeds your saved session time limit."),
            Map.entry("recovery", "Two sessions train the same primary muscle too close together, including across the week boundary."),
            Map.entry("bounds", "A workout exceeds the exercise or set limit."));

    private final PlanInputReader reader;
    private final WorkoutPlanRepository plans;
    private final FitnessProfileRepository profiles;
    private final PlanEvaluator evaluator;
    private final SnapshotCodec codec;

    public PlanEditingService(PlanInputReader reader, WorkoutPlanRepository plans, FitnessProfileRepository profiles,
                              PlanEvaluator evaluator, SnapshotCodec codec) {
        this.reader = reader;
        this.plans = plans;
        this.profiles = profiles;
        this.evaluator = evaluator;
        this.codec = codec;
    }

    @Transactional(readOnly = true)
    public Editor open(Long owner, Long id) {
        var saved = plans.findByIdAndUserId(id, owner).orElseThrow(PlanNotFoundException::new);
        var input = reader.read(owner);
        if (!id.equals(profiles.findById(owner).orElseThrow(MissingProfileException::new).getActivePlanId())
                || !saved.getProfileHash().equals(codec.profileHash(input.profile()))) {
            throw new PlanConflictException();
        }
        var days = saved.getDays().stream().sorted(Comparator.comparingInt(day -> day.getWeekday()))
                .map(day -> new Day(day.getWeekday(), day.getDayType(), day.getExercises().stream()
                        .map(e -> new Exercise(e.getExerciseId(), e.getSets(), e.getRepMin(), e.getRepMax(),
                                e.getRestSeconds(), e.getExplanation(), null)).toList())).toList();
        var base = new Plan(saved.getCandidateKey(), days);
        var eligible = PlanningRules.validateAndSort(input.profile(), input.catalog()).stream()
                .filter(e -> PlanningRules.eligible(e, input.profile()))
                .sorted(Comparator.comparing(ExerciseSnapshot::name)).toList();
        return new Editor(input, base, codec.read(saved.getEvaluationJson(), Evaluation.class), eligible);
    }

    public Review review(Editor editor, Plan draft) {
        var evaluation = evaluator.evaluateEdited(editor.input().profile(), editor.input().catalog(), draft);
        var old = editor.previous();
        var known = PlanningRules.byId(editor.input().catalog());
        var days = draft.days().stream().map(day -> {
            var original = editor.base().days().stream().filter(d -> d.weekday() == day.weekday()).findFirst().orElseThrow();
            return new DayChange(DayOfWeek.of(day.weekday()).toString(), original.type().getLabel(), day.type().getLabel(),
                    old.daySeconds().get(day.weekday()), evaluation.daySeconds().get(day.weekday()),
                    describe(original, known), describe(day, known));
        }).toList();
        var muscles = java.util.Arrays.stream(MuscleGroup.values()).map(muscle -> {
            int before = old.volumeHalfSets().get(muscle);
            int after = evaluation.volumeHalfSets().get(muscle);
            int target = evaluation.targetHalfSets().get(muscle);
            int impact = Integer.compare(Math.abs(before - target), Math.abs(after - target));
            return new MuscleChange(muscle.getLabel(), half(before), half(after), half(target), impact);
        }).toList();
        var problems = evaluation.violations().keySet().stream().sorted().map(MESSAGES::get).toList();
        var result = new Result(evaluation.status(), draft, evaluation,
                List.of("Edited by you. Time, equipment, exercise choices and recovery were checked before saving."), 1, 0);
        String token = codec.hash(List.of(editor.input(), codec.json(editor.base()), codec.json(draft)));
        return new Review(result, new Impact(days, muscles), problems, token);
    }

    private static List<String> describe(Day day, Map<Long, ExerciseSnapshot> known) {
        return day.exercises().stream().map(item -> {
            var exercise = known.get(item.exerciseId());
            return (exercise == null ? "Unavailable exercise" : exercise.name()) + " · " + item.sets() + " × "
                    + item.repMin() + "–" + item.repMax() + " · " + item.restSeconds() + " s rest";
        }).toList();
    }

    private static BigDecimal half(int value) {
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(2));
    }

    public record Editor(PlanInputReader.Inputs input, Plan base, Evaluation previous, List<ExerciseSnapshot> eligible) { }
    public record Review(Result result, Impact impact, List<String> problems, String token) { }
    public record Impact(List<DayChange> days, List<MuscleChange> muscles) {
        public boolean changed() {
            return days.stream().anyMatch(DayChange::changed);
        }
    }
    public record DayChange(String weekday, String beforeType, String afterType, int beforeSeconds, int afterSeconds,
                            List<String> before, List<String> after) {
        public boolean changed() {
            return !beforeType.equals(afterType) || beforeSeconds != afterSeconds || !before.equals(after);
        }
        public List<String> removedOrUpdated() {
            return before.stream().filter(item -> !after.contains(item)).toList();
        }

        public List<String> addedOrUpdated() {
            return after.stream().filter(item -> !before.contains(item)).toList();
        }

        public String label() {
            return weekday.substring(0, 1) + weekday.substring(1).toLowerCase(Locale.ENGLISH);
        }
    }
    public record MuscleChange(String name, BigDecimal before, BigDecimal after, BigDecimal target, int impact) { }
}
