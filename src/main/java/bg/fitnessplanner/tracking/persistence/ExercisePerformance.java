package bg.fitnessplanner.tracking.persistence;

import bg.fitnessplanner.progression.engine.ProgressionModel.WorkSet;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "exercise_performance")
public class ExercisePerformance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id") private WorkoutSession session;
    @Column(name = "workout_day_id", nullable = false) private Long workoutDayId;
    @Column(name = "planned_exercise_id", nullable = false) private Long plannedExerciseId;
    @Column(name = "exercise_id", nullable = false) private Long exerciseId;
    @Column(nullable = false) private int position;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private PerformanceStatus status = PerformanceStatus.PENDING;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "prescription_snapshot", nullable = false, columnDefinition = "json") private String prescriptionSnapshot;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "suggestion_snapshot", nullable = false, columnDefinition = "json") private String suggestionSnapshot;
    @Column(name = "target_weight_kg", precision = 7, scale = 2) private BigDecimal targetWeightKg;
    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("setNumber ASC")
    private List<PerformedSet> sets = new ArrayList<>();
    protected ExercisePerformance() { }
    public ExercisePerformance(WorkoutSession session, Long day, Long planned, Long exercise, int position,
                               String prescription, String suggestion, BigDecimal target) {
        this.session = session; workoutDayId = day; plannedExerciseId = planned; exerciseId = exercise; this.position = position;
        prescriptionSnapshot = prescription; suggestionSnapshot = suggestion; targetWeightKg = target;
    }
    public void record(PerformanceStatus status, List<WorkSet> actual) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) throw new IllegalStateException("Workout history is immutable.");
        this.status = status;
        // Reuse rows by set number so a replacement cannot collide with the unique set-number constraint.
        for (int i = 0; i < actual.size(); i++) {
            if (i < sets.size()) sets.get(i).update(actual.get(i));
            else sets.add(new PerformedSet(this, actual.get(i)));
        }
        while (sets.size() > actual.size()) sets.removeLast();
    }
    public Long getId() { return id; }
    public WorkoutSession getSession() { return session; }
    public Long getExerciseId() { return exerciseId; }
    public int getPosition() { return position; }
    public PerformanceStatus getStatus() { return status; }
    public String getPrescriptionSnapshot() { return prescriptionSnapshot; }
    public String getSuggestionSnapshot() { return suggestionSnapshot; }
    public BigDecimal getTargetWeightKg() { return targetWeightKg; }
    public List<WorkSet> actualSets() { return sets.stream().map(PerformedSet::snapshot).toList(); }
}
