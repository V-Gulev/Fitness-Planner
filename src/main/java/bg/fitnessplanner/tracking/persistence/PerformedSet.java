package bg.fitnessplanner.tracking.persistence;
import bg.fitnessplanner.progression.engine.ProgressionModel.WorkSet;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name = "performed_set")
public class PerformedSet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "performance_id") private ExercisePerformance performance;
    @Column(name = "set_number", nullable = false) private int setNumber;
    @Column(nullable = false) private int reps;
    @Column(name = "weight_kg", nullable = false, precision = 7, scale = 2) private BigDecimal weightKg;
    private Integer rir;
    protected PerformedSet() { }
    public PerformedSet(ExercisePerformance performance, WorkSet set) { this.performance = performance; update(set); }
    public void update(WorkSet set) { setNumber = set.number(); reps = set.reps(); weightKg = set.weightKg(); rir = set.rir(); }
    public WorkSet snapshot() { return new WorkSet(setNumber, reps, weightKg, rir); }
}
