package bg.fitnessplanner.catalog.application;

import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.catalog.domain.MuscleRole;
import bg.fitnessplanner.catalog.persistence.Exercise;
import bg.fitnessplanner.catalog.persistence.ExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    private final ExerciseRepository exercises;

    public CatalogService(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Transactional(readOnly = true)
    public List<ExerciseSnapshot> snapshot() {
        return exercises.findActiveCatalog().stream().map(this::snapshot).toList();
    }

    private ExerciseSnapshot snapshot(Exercise e) {
        Map<MuscleGroup, MuscleRole> muscles = e.getMuscles();
        var primary = muscles.entrySet().stream().filter(m -> m.getValue() == MuscleRole.PRIMARY).toList();
        if (primary.size() != 1) {
            throw new IllegalStateException("Catalog exercise must have exactly one primary muscle: " + e.getCode());
        }
        var secondary = muscles.entrySet().stream().filter(m -> m.getValue() == MuscleRole.SECONDARY)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        if (e.getLoadEquipment() != null && !e.getRequiredEquipment().contains(e.getLoadEquipment())) {
            throw new IllegalStateException("Load equipment must be required equipment: " + e.getCode());
        }
        return new ExerciseSnapshot(e.getId(), e.getCode(), e.getName(), primary.getFirst().getKey(), secondary,
                e.getMovementPattern(), e.getRequiredEquipment(), e.getDifficulty(), e.isCompound(),
                e.getSecondsPerRep(), e.getSetupSeconds(), e.getLoadType(), e.getLoadEquipment(),
                e.getFatigueCost(), e.getTags(), e.getInstructions());
    }
}
