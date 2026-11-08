package bg.fitnessplanner.profile.application;

import bg.fitnessplanner.catalog.domain.Equipment;
import bg.fitnessplanner.catalog.domain.MovementPattern;
import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;

import java.util.Set;

public record ProfileSnapshot(Long userId, Long version, ExperienceLevel experience, TrainingGoal goal,
                              int sessionMinutes, Set<Integer> weekdays, Set<Equipment> equipment,
                              Set<Long> preferredExerciseIds, Set<Long> excludedExerciseIds,
                              Set<MovementPattern> excludedMovements) {
    public ProfileSnapshot {
        weekdays = Set.copyOf(weekdays);
        equipment = Set.copyOf(equipment);
        preferredExerciseIds = Set.copyOf(preferredExerciseIds);
        excludedExerciseIds = Set.copyOf(excludedExerciseIds);
        excludedMovements = Set.copyOf(excludedMovements);
    }

    public int trainingDays() { return weekdays.size(); }
}
