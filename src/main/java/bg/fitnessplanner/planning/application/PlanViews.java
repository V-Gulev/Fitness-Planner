package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.planning.engine.PlanningModel.Status;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import java.math.BigDecimal;
import java.util.List;

public final class PlanViews {
    private PlanViews() { }
    public record Page(ProfileSnapshot profile, Details plan, List<Summary> history) { }
    public record Summary(Long id, String createdLabel, boolean active) { }
    public record Details(Long id, String createdLabel, Status status, boolean active, boolean profileChanged,
                          String algorithmVersion, String configVersion, String structure,
                          int candidatesConstructed, int warmupMinutes, BigDecimal secondaryCredit, List<String> diagnostics,
                          List<Day> days, List<Volume> volumes) { }
    public record Day(Long id, int weekdayNumber, String weekday, String type, int estimatedMinutes, List<Exercise> exercises) { }
    public record Exercise(String name, String muscle, int sets, String reps, int restSeconds, String explanation) { }
    public record Volume(String muscle, BigDecimal target, BigDecimal prescribed, String difference) { }
}
