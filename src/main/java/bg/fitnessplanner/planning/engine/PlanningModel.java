package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.domain.MuscleGroup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlanningModel {
    private PlanningModel() { }

    public enum Status { VALID, DEGRADED, INFEASIBLE, INVALID_INPUT }

    public enum DayType {
        FULL("Full body"), UPPER("Upper body"), LOWER("Lower body"), PUSH("Push"), PULL("Pull"), LEGS("Legs");
        private final String label;
        DayType(String label) { this.label = label; }
        public String getLabel() { return label; }
        public boolean allows(MuscleGroup muscle) {
            Set<MuscleGroup> upper = Set.of(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS,
                    MuscleGroup.BICEPS, MuscleGroup.TRICEPS);
            return switch (this) {
                case FULL -> true;
                case UPPER -> upper.contains(muscle);
                case LOWER, LEGS -> !upper.contains(muscle);
                case PUSH -> Set.of(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS).contains(muscle);
                case PULL -> Set.of(MuscleGroup.BACK, MuscleGroup.BICEPS).contains(muscle);
            };
        }
    }

    public record Score(BigDecimal muscle, BigDecimal goal, BigDecimal experience, BigDecimal preference,
                        BigDecimal novelty, BigDecimal redundancy, BigDecimal fatigue, BigDecimal overshoot,
                        BigDecimal total) { }

    public record Exercise(Long exerciseId, int sets, int repMin, int repMax, int restSeconds,
                           String explanation, Score score) { }

    public record Day(int weekday, DayType type, List<Exercise> exercises) {
        public Day { exercises = List.copyOf(exercises); }
    }

    public record Plan(String candidateKey, List<Day> days) {
        public Plan { days = List.copyOf(days); }
    }

    /** All volume values use half-set units: one primary set is two units. */
    public record Evaluation(Map<String, Integer> violations, Map<MuscleGroup, Integer> targetHalfSets,
                             Map<MuscleGroup, Integer> volumeHalfSets, Map<Integer, Integer> daySeconds,
                             int absoluteDeviationHalfSets, int missingMuscles, int repeatedOccurrences,
                             int totalSeconds) {
        public Evaluation {
            violations = Map.copyOf(violations);
            targetHalfSets = Map.copyOf(targetHalfSets);
            volumeHalfSets = Map.copyOf(volumeHalfSets);
            daySeconds = Map.copyOf(daySeconds);
        }
        public int hardViolations() { return violations.values().stream().mapToInt(Integer::intValue).sum(); }
        public BigDecimal normalizedVolumeDeviation() {
            int target = targetHalfSets.values().stream().mapToInt(Integer::intValue).sum();
            return BigDecimal.valueOf(absoluteDeviationHalfSets).divide(BigDecimal.valueOf(target), 4, RoundingMode.HALF_UP);
        }
        public Status status() {
            return hardViolations() > 0 ? Status.INFEASIBLE : absoluteDeviationHalfSets == 0 ? Status.VALID : Status.DEGRADED;
        }
    }

    public record Result(Status status, Plan plan, Evaluation evaluation, List<String> diagnostics,
                         int candidatesConstructed, long additionsExamined) {
        public Result { diagnostics = List.copyOf(diagnostics); }
        public boolean publishable() { return status == Status.VALID || status == Status.DEGRADED; }
    }
}
