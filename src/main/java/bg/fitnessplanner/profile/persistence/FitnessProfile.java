package bg.fitnessplanner.profile.persistence;

import bg.fitnessplanner.catalog.domain.Equipment;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.profile.domain.ExerciseRule;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "fitness_profile")
public class FitnessProfile {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @Version
    private Long version;
    @Column(name = "active_plan_id")
    private Long activePlanId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExperienceLevel experience;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TrainingGoal goal;
    @Column(name = "session_minutes", nullable = false)
    private int sessionMinutes;
    @Column(name = "zone_id", nullable = false, length = 64)
    private String zoneId = "Europe/Sofia";

    @ElementCollection
    @CollectionTable(name = "profile_weekday", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "weekday", nullable = false)
    private Set<Integer> weekdays = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "profile_equipment", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_code", nullable = false, length = 24)
    private Set<Equipment> equipment = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "profile_exercise_rule", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "exercise_id")
    @Enumerated(EnumType.STRING)
    @Column(name = "rule", nullable = false, length = 12)
    private Map<Long, ExerciseRule> exerciseRules = new HashMap<>();
    @ElementCollection
    @CollectionTable(name = "profile_movement_exclusion", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", nullable = false, length = 24)
    private Set<MovementPattern> excludedMovements = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "profile_load_step", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "equipment_code", length = 24) @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "step_kg", nullable = false, precision = 7, scale = 2)
    private Map<Equipment, BigDecimal> loadSteps = new HashMap<>();
    public Map<Equipment, BigDecimal> getLoadSteps() { return Map.copyOf(loadSteps); }
    public void updateLoadSteps(Map<Equipment, BigDecimal> steps) { loadSteps.clear(); loadSteps.putAll(steps); }

    protected FitnessProfile() { }

    public FitnessProfile(Long userId) { this.userId = userId; }

    public void update(ExperienceLevel experience, TrainingGoal goal, int sessionMinutes,
                       Set<Integer> weekdays, Set<Equipment> equipment, Map<Long, ExerciseRule> rules,
                       Set<MovementPattern> excludedMovements) {
        this.experience = experience;
        this.goal = goal;
        this.sessionMinutes = sessionMinutes;
        this.weekdays.clear(); this.weekdays.addAll(weekdays);
        this.equipment.clear(); this.equipment.addAll(equipment);
        this.exerciseRules.clear(); this.exerciseRules.putAll(rules);
        this.excludedMovements.clear(); this.excludedMovements.addAll(excludedMovements);
    }

    public Long getUserId() { return userId; }
    public Long getVersion() { return version; }
    public Long getActivePlanId() { return activePlanId; }
    public void activatePlan(Long planId) { this.activePlanId = planId; }
    public ExperienceLevel getExperience() { return experience; }
    public TrainingGoal getGoal() { return goal; }
    public int getSessionMinutes() { return sessionMinutes; }
    public Set<Integer> getWeekdays() { return Set.copyOf(weekdays); }
    public Set<Equipment> getEquipment() { return Set.copyOf(equipment); }
    public Map<Long, ExerciseRule> getExerciseRules() { return Map.copyOf(exerciseRules); }
    public Set<MovementPattern> getExcludedMovements() { return Set.copyOf(excludedMovements); }
}
