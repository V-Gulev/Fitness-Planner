package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.profile.application.ProfileSnapshot;

import java.util.List;

public interface Planner {
    String version();
    PlanningModel.Result generate(ProfileSnapshot profile, List<ExerciseSnapshot> catalog);
}
