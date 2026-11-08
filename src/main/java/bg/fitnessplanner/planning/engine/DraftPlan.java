package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.*;

import java.util.*;

/** Local mutable construction state; never escapes the engine. */
final class DraftPlan {
    final List<Integer> weekdays;
    final List<DayType> types;
    final List<List<Exercise>> days = new ArrayList<>();
    final Map<Long, ExerciseSnapshot> catalog;
    final ProfileSnapshot profile;
    final PlannerConfig config;
    final String key;

    DraftPlan(ProfileSnapshot profile, List<ExerciseSnapshot> catalog, PlannerConfig config, List<DayType> types) {
        this.profile = profile;
        this.weekdays = profile.weekdays().stream().sorted().toList();
        this.types = List.copyOf(types);
        this.config = config;
        this.catalog = PlanningRules.byId(catalog);
        this.key = types.stream().map(Enum::name).collect(java.util.stream.Collectors.joining("/"));
        weekdays.forEach(ignored -> days.add(new ArrayList<>()));
    }

    boolean canAdd(int day, ExerciseSnapshot e, Exercise prescription) {
        List<Exercise> items = days.get(day);
        if (!PlanningRules.eligible(e, profile) || !types.get(day).allows(e.primaryMuscle())
                || items.size() >= config.maxExercisesPerDay()
                || items.stream().anyMatch(i -> i.exerciseId().equals(e.id()))
                || items.stream().mapToInt(Exercise::sets).sum() + prescription.sets() > config.maxSetsPerDay()) return false;
        int duration = config.warmupSeconds() + items.stream().mapToInt(i -> PlanningRules.seconds(i, catalog.get(i.exerciseId()))).sum();
        if (duration + PlanningRules.seconds(prescription, e) > profile.sessionMinutes() * 60) return false;
        for (int other = 0; other < days.size(); other++) {
            if (other == day) continue;
            int gap = Math.abs(weekdays.get(day) - weekdays.get(other));
            if (Math.min(gap, 7 - gap) < config.minRecoveryGapDays()
                    && days.get(other).stream().anyMatch(i -> catalog.get(i.exerciseId()).primaryMuscle() == e.primaryMuscle())) return false;
        }
        return true;
    }

    EnumMap<MuscleGroup, Integer> volume() {
        var volume = PlanningRules.emptyVolume();
        for (var day : days) for (Exercise item : day) for (MuscleGroup muscle : MuscleGroup.values()) {
            volume.merge(muscle, item.sets() * PlanningRules.credits(catalog.get(item.exerciseId()), muscle, config), Integer::sum);
        }
        return volume;
    }

    Plan freeze() {
        List<Day> result = new ArrayList<>();
        for (int i = 0; i < days.size(); i++) result.add(new Day(weekdays.get(i), types.get(i), days.get(i)));
        return new Plan(key, result);
    }
}
