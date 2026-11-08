package bg.fitnessplanner.planning.persistence;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.Exercise;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "planned_exercise")
public class PlannedExercise {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "workout_day_id") private WorkoutDay day;
    @Column(name = "exercise_id", nullable = false) private Long exerciseId;
    @Column(nullable = false) private int position;
    @Column(nullable = false) private int sets;
    @Column(name = "rep_min", nullable = false) private int repMin;
    @Column(name = "rep_max", nullable = false) private int repMax;
    @Column(name = "rest_seconds", nullable = false) private int restSeconds;
    @Column(name = "estimated_seconds", nullable = false) private int estimatedSeconds;
    @Column(name = "code_snapshot", nullable = false, length = 64) private String codeSnapshot;
    @Column(name = "name_snapshot", nullable = false, length = 100) private String nameSnapshot;
    @Column(name = "primary_muscle_snapshot", nullable = false, length = 24) private String primaryMuscleSnapshot;
    @Column(name = "load_type_snapshot", nullable = false, length = 24) private String loadTypeSnapshot;
    @Column(nullable = false, columnDefinition = "text") private String explanation;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "score_json", columnDefinition = "json") private String scoreJson;
    protected PlannedExercise() { }
    public PlannedExercise(WorkoutDay day, int position, Exercise item, ExerciseSnapshot metadata, int seconds, String scoreJson) {
        this.day = day; this.position = position; this.exerciseId = item.exerciseId(); this.sets = item.sets();
        this.repMin = item.repMin(); this.repMax = item.repMax(); this.restSeconds = item.restSeconds();
        this.estimatedSeconds = seconds; this.codeSnapshot = metadata.code(); this.nameSnapshot = metadata.name();
        this.primaryMuscleSnapshot = metadata.primaryMuscle().name(); this.loadTypeSnapshot = metadata.loadType().name();
        this.explanation = item.explanation(); this.scoreJson = scoreJson;
    }
    public Long getId() { return id; }
    public Long getExerciseId() { return exerciseId; }
    public int getPosition() { return position; }
    public int getSets() { return sets; }
    public int getRepMin() { return repMin; }
    public int getRepMax() { return repMax; }
    public int getRestSeconds() { return restSeconds; }
    public int getEstimatedSeconds() { return estimatedSeconds; }
    public String getCodeSnapshot() { return codeSnapshot; }
    public String getNameSnapshot() { return nameSnapshot; }
    public String getPrimaryMuscleSnapshot() { return primaryMuscleSnapshot; }
    public String getLoadTypeSnapshot() { return loadTypeSnapshot; }
    public String getExplanation() { return explanation; }
    public String getScoreJson() { return scoreJson; }
}
