package bg.fitnessplanner.catalog.application;

import bg.fitnessplanner.catalog.domain.*;
import bg.fitnessplanner.shared.domain.ExperienceLevel;

import java.math.BigDecimal;
import java.util.Set;

public record ExerciseSnapshot(Long id, String code, String name, MuscleGroup primaryMuscle,
                               Set<MuscleGroup> secondaryMuscles, MovementPattern movementPattern,
                               Set<Equipment> requiredEquipment, ExperienceLevel difficulty, boolean compound,
                               int secondsPerRep, int setupSeconds, LoadType loadType, Equipment loadEquipment,
                               BigDecimal fatigueCost, Set<ExerciseTag> tags, String instructions) {
    public ExerciseSnapshot {
        secondaryMuscles = Set.copyOf(secondaryMuscles);
        requiredEquipment = Set.copyOf(requiredEquipment);
        tags = Set.copyOf(tags);
    }

    public String equipmentLabel() {
        return requiredEquipment.isEmpty() ? "No equipment" : requiredEquipment.stream()
                .sorted().map(Equipment::getLabel).collect(java.util.stream.Collectors.joining(", "));
    }
}
