package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.planning.engine.ScoredPlanner;
import bg.fitnessplanner.planning.engine.PlanningModel.Result;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class PlanGenerationService {
    private final PlanInputReader reader;
    private final ScoredPlanner planner;
    private final PlanPublisher publisher;
    public PlanGenerationService(PlanInputReader reader, ScoredPlanner planner, PlanPublisher publisher) {
        this.reader = reader; this.planner = planner; this.publisher = publisher;
    }

    /** The expensive pure calculation occurs after the reader's transaction has closed. */
    public Outcome generate(Long owner, Long expectedProfileVersion) {
        var input = reader.read(owner);
        if (!Objects.equals(expectedProfileVersion, input.profile().version())) throw new PlanConflictException();
        Result result = planner.generate(input.profile(), input.catalog());
        Long id = result.publishable() ? publisher.publish(owner, input, result, planner.version()) : null;
        return new Outcome(id, result);
    }
    public record Outcome(Long planId, Result result) { }
}
