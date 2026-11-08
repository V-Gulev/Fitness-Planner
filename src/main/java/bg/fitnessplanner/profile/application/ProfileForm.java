package bg.fitnessplanner.profile.application;

import bg.fitnessplanner.catalog.domain.Equipment;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Validated command DTO shared by MVC binding and the profile application service. */
public class ProfileForm {
    @NotNull(message = "Choose your training experience.")
    private ExperienceLevel experience;
    @NotNull(message = "Choose your primary goal.")
    private TrainingGoal goal;
    @NotNull(message = "Enter your session length.")
    @Min(value = 20, message = "Allow at least 20 minutes per session.")
    @Max(value = 120, message = "Session length must be at most 120 minutes.")
    private Integer sessionMinutes;
    @NotNull
    @Size(min = 2, max = 5, message = "Select between 2 and 5 training days.")
    private Set<@NotNull @Min(1) @Max(7) Integer> weekdays = new HashSet<>();
    @NotNull
    private Set<@NotNull Equipment> equipment = new HashSet<>();
    @NotNull
    @Size(max = 50, message = "Choose at most 50 preferred exercises.")
    private Set<@NotNull @Positive Long> preferredExerciseIds = new HashSet<>();
    @NotNull
    @Size(max = 50, message = "Choose at most 50 excluded exercises.")
    private Set<@NotNull @Positive Long> excludedExerciseIds = new HashSet<>();
    @NotNull
    private Set<@NotNull MovementPattern> excludedMovements = new HashSet<>();
    @NotNull @Size(max = 6)
    private Map<@NotNull Equipment, @DecimalMin("0.01") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal> loadSteps = new HashMap<>();
    public Map<Equipment, BigDecimal> getLoadSteps() { return loadSteps; }
    public void setLoadSteps(Map<Equipment, BigDecimal> loadSteps) { this.loadSteps = loadSteps; }
    @PositiveOrZero
    private Long version;

    public ExperienceLevel getExperience() { return experience; }
    public void setExperience(ExperienceLevel experience) { this.experience = experience; }
    public TrainingGoal getGoal() { return goal; }
    public void setGoal(TrainingGoal goal) { this.goal = goal; }
    public Integer getSessionMinutes() { return sessionMinutes; }
    public void setSessionMinutes(Integer sessionMinutes) { this.sessionMinutes = sessionMinutes; }
    public Set<Integer> getWeekdays() { return weekdays; }
    public void setWeekdays(Set<Integer> weekdays) { this.weekdays = weekdays; }
    public Set<Equipment> getEquipment() { return equipment; }
    public void setEquipment(Set<Equipment> equipment) { this.equipment = equipment; }
    public Set<Long> getPreferredExerciseIds() { return preferredExerciseIds; }
    public void setPreferredExerciseIds(Set<Long> ids) { this.preferredExerciseIds = ids; }
    public Set<Long> getExcludedExerciseIds() { return excludedExerciseIds; }
    public void setExcludedExerciseIds(Set<Long> ids) { this.excludedExerciseIds = ids; }
    public Set<MovementPattern> getExcludedMovements() { return excludedMovements; }
    public void setExcludedMovements(Set<MovementPattern> movements) { this.excludedMovements = movements; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
