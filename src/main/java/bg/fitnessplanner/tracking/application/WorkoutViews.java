package bg.fitnessplanner.tracking.application;
import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.progression.engine.ProgressionModel.*;
import bg.fitnessplanner.tracking.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public final class WorkoutViews {
    private WorkoutViews() { }
    public record Prescription(Long exerciseId, String name, int sets, int repMin, int repMax, int restSeconds,
                               LoadType loadType, Equipment loadEquipment, BigDecimal loadStep) {
        public Target target() { return new Target(exerciseId, sets, repMin, repMax, restSeconds, loadType, loadStep); }
    }
    public record Choice(Long plannedExerciseId, int position, Prescription prescription, Decision decision) { }
    public record Preview(Long dayId, Long planId, LocalDate date, Long profileVersion, List<Choice> choices, String hash, Long openSessionId) { }
    public record Performance(Long id, Prescription prescription, Decision decision, BigDecimal acceptedTarget,
                              PerformanceStatus status, List<WorkSet> actual) { }
    public record Detail(Long id, Long planId, LocalDate date, SessionStatus status, Long version,
                         String startedLabel, String completedLabel, String progressionVersion, List<Performance> performances) {
        public boolean editable() { return status == SessionStatus.IN_PROGRESS; }
    }
    public record Summary(Long id, Long planId, LocalDate date, SessionStatus status) { }
    public record ExerciseLink(Long id, String name) { }
    public record Activity(Summary draft, List<Summary> recent, List<ExerciseLink> exercises,
                           long completedWorkouts, long loggedSets, java.util.Set<Long> finishedToday) { }
    public record History(Long sessionId, LocalDate date, Prescription prescription, PerformanceStatus status, List<WorkSet> actual) {
        public Integer totalReps() {
            return actual.isEmpty() ? null : actual.stream().mapToInt(WorkSet::reps).sum();
        }

        public BigDecimal heaviestLoad() {
            if (prescription.loadType() != LoadType.TOTAL_EXTERNAL && prescription.loadType() != LoadType.PER_HAND_EXTERNAL) {
                return null;
            }
            return actual.stream().map(WorkSet::weightKg).max(BigDecimal::compareTo).orElse(null);
        }
    }
}
