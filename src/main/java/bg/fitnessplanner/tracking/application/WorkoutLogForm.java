package bg.fitnessplanner.tracking.application;
import bg.fitnessplanner.tracking.persistence.PerformanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
public class WorkoutLogForm {
    @NotNull @PositiveOrZero private Long version;
    @NotNull @Size(min = 1, max = 8) @Valid private List<@NotNull PerformanceForm> performances = new ArrayList<>();
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<PerformanceForm> getPerformances() { return performances; }
    public void setPerformances(List<PerformanceForm> rows) { performances = rows; }
    public static class PerformanceForm {
        @NotNull @Positive private Long id;
        @NotNull private PerformanceStatus status = PerformanceStatus.PENDING;
        @NotNull @Size(max = 12) @Valid private List<@NotNull SetForm> sets = new ArrayList<>();
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public PerformanceStatus getStatus() { return status; }
        public void setStatus(PerformanceStatus status) { this.status = status; }
        public List<SetForm> getSets() { return sets; }
        public void setSets(List<SetForm> sets) { this.sets = sets; }
    }
    public static class SetForm {
        @Min(1) @Max(100) private Integer reps;
        @DecimalMin("0") @DecimalMax("1000") @Digits(integer = 4, fraction = 2) private BigDecimal weightKg;
        @Min(0) @Max(10) private Integer rir;
        public Integer getReps() { return reps; }
        public void setReps(Integer reps) { this.reps = reps; }
        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal kg) { weightKg = kg; }
        public Integer getRir() { return rir; }
        public void setRir(Integer rir) { this.rir = rir; }
        public boolean blank() { return reps == null && weightKg == null && rir == null; }
    }
}
