package bg.fitnessplanner.catalog.persistence;

import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.shared.domain.ExperienceLevel;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "exercise")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_pattern", nullable = false, length = 24)
    private MovementPattern movementPattern;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ExperienceLevel difficulty;
    @Column(nullable = false)
    private boolean compound;
    @Column(name = "seconds_per_rep", nullable = false)
    private int secondsPerRep;
    @Column(name = "setup_seconds", nullable = false)
    private int setupSeconds;
    @Enumerated(EnumType.STRING)
    @Column(name = "load_type", nullable = false, length = 24)
    private LoadType loadType;
    @Enumerated(EnumType.STRING)
    @Column(name = "load_equipment", length = 24)
    private Equipment loadEquipment;
    @Column(name = "fatigue_cost", nullable = false, precision = 3, scale = 2)
    private BigDecimal fatigueCost;
    @Column(nullable = false, length = 500)
    private String instructions;
    @Column(nullable = false)
    private boolean active;

    @ElementCollection
    @CollectionTable(name = "exercise_muscle", joinColumns = @JoinColumn(name = "exercise_id"))
    @MapKeyColumn(name = "muscle_code", length = 24)
    @MapKeyEnumerated(EnumType.STRING)
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 12)
    private Map<MuscleGroup, MuscleRole> muscles = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "exercise_equipment", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_code", nullable = false, length = 24)
    private Set<Equipment> requiredEquipment = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "exercise_tag", joinColumns = @JoinColumn(name = "exercise_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false, length = 24)
    private Set<ExerciseTag> tags = new HashSet<>();

    protected Exercise() { }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public MovementPattern getMovementPattern() { return movementPattern; }
    public ExperienceLevel getDifficulty() { return difficulty; }
    public boolean isCompound() { return compound; }
    public int getSecondsPerRep() { return secondsPerRep; }
    public int getSetupSeconds() { return setupSeconds; }
    public LoadType getLoadType() { return loadType; }
    public Equipment getLoadEquipment() { return loadEquipment; }
    public BigDecimal getFatigueCost() { return fatigueCost; }
    public String getInstructions() { return instructions; }
    public Map<MuscleGroup, MuscleRole> getMuscles() { return Map.copyOf(muscles); }
    public Set<Equipment> getRequiredEquipment() { return Set.copyOf(requiredEquipment); }
    public Set<ExerciseTag> getTags() { return Set.copyOf(tags); }
}
