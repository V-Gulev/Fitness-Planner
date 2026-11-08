package bg.fitnessplanner.tracking.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

@Entity @Table(name = "workout_session")
public class WorkoutSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "plan_id", nullable = false) private Long planId;
    @Column(name = "workout_day_id", nullable = false) private Long workoutDayId;
    @Column(name = "session_date", nullable = false) private LocalDate sessionDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private SessionStatus status = SessionStatus.IN_PROGRESS;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "submission_token", nullable = false, length = 36, columnDefinition = "char(36)") private String submissionToken;
    @Version private Long version;
    @Column(name = "actual_data_hash", nullable = false, length = 64) private String actualDataHash = "";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "progression_config_snapshot", columnDefinition = "json", nullable = false)
    private String progressionConfigSnapshot;
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL) @OrderBy("position ASC")
    private Set<ExercisePerformance> performances = new LinkedHashSet<>();
    protected WorkoutSession() { }
    public WorkoutSession(Long owner, Long plan, Long day, LocalDate date, Instant now, String token, String config) {
        userId = owner; planId = plan; workoutDayId = day; sessionDate = date; startedAt = now; updatedAt = now;
        submissionToken = token; progressionConfigSnapshot = config;
    }
    public void add(ExercisePerformance performance) { performances.add(performance); }
    public void record(String hash, Instant now, boolean finish) {
        requireDraft(); actualDataHash = hash; updatedAt = now;
        if (finish) { status = SessionStatus.COMPLETED; completedAt = now; }
    }
    public void cancel(Instant now) { requireDraft(); status = SessionStatus.CANCELLED; updatedAt = now; }
    private void requireDraft() { if (status != SessionStatus.IN_PROGRESS) throw new IllegalStateException("Workout history is immutable."); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPlanId() { return planId; }
    public Long getWorkoutDayId() { return workoutDayId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public SessionStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getVersion() { return version; }
    public String getProgressionConfigSnapshot() { return progressionConfigSnapshot; }
    public List<ExercisePerformance> getPerformances() { return performances.stream().sorted(Comparator.comparingInt(ExercisePerformance::getPosition)).toList(); }
}
