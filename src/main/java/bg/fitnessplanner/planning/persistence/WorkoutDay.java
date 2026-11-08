package bg.fitnessplanner.planning.persistence;

import bg.fitnessplanner.planning.engine.PlanningModel.DayType;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workout_day")
public class WorkoutDay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id") private WorkoutPlan plan;
    @Column(nullable = false) private int weekday;
    @Enumerated(EnumType.STRING) @Column(name = "day_type", nullable = false, length = 16) private DayType dayType;
    @Column(name = "estimated_seconds", nullable = false) private int estimatedSeconds;
    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL)
    @OrderBy("position ASC") private List<PlannedExercise> exercises = new ArrayList<>();
    protected WorkoutDay() { }
    public WorkoutDay(WorkoutPlan plan, int weekday, DayType type, int seconds) {
        this.plan = plan; this.weekday = weekday; this.dayType = type; this.estimatedSeconds = seconds;
    }
    public void addExercise(PlannedExercise exercise) { exercises.add(exercise); }
    public WorkoutPlan getPlan() { return plan; }
    public Long getId() { return id; }
    public int getWeekday() { return weekday; }
    public DayType getDayType() { return dayType; }
    public int getEstimatedSeconds() { return estimatedSeconds; }
    public List<PlannedExercise> getExercises() { return List.copyOf(exercises); }
}
