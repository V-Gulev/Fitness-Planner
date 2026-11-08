package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.planning.engine.ScoredPlanner;
import bg.fitnessplanner.planning.engine.PlanningModel.Result;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;

@Service
public class PlanGenerationService {
    private final PlanInputReader reader;
    private final ScoredPlanner planner;
    private final PlanPublisher publisher;
    private final SnapshotCodec codec;
    public PlanGenerationService(PlanInputReader reader, ScoredPlanner planner, PlanPublisher publisher, SnapshotCodec codec) {
        this.reader = reader; this.planner = planner; this.publisher = publisher; this.codec = codec;
    }

    /** The expensive pure calculation occurs after the reader's transaction has closed. */
    public Outcome generate(Long owner, Long expectedProfileVersion) {
        var input = reader.read(owner);
        if (!Objects.equals(expectedProfileVersion, input.profile().version())) throw new PlanConflictException();
        Result result = planner.generate(input.profile(), input.catalog());
        Long id = result.publishable() ? publisher.publish(owner, input, result, planner.version()) : null;
        return new Outcome(id, result);
    }
    public Choices choices(Long owner, Long version) {
        var input = reader.read(owner);
        if (!Objects.equals(version, input.profile().version())) throw new PlanConflictException();
        return choices(input);
    }

    public Long select(Long owner, Long version, String token, String candidate) {
        var input = reader.read(owner);
        if (!Objects.equals(version, input.profile().version()) || !token.equals(token(input))) {
            throw new PlanConflictException();
        }
        var result = choices(input).options().stream()
                .filter(option -> option.plan().candidateKey().equals(candidate))
                .findFirst().orElseThrow(PlanConflictException::new);
        return publisher.publish(owner, input, result, planner.version());
    }

    private Choices choices(PlanInputReader.Inputs input) {
        var results = planner.candidates(input.profile(), input.catalog());
        return new Choices(input, token(input), results.stream().filter(Result::publishable).toList(),
                results.getFirst().diagnostics());
    }

    private String token(PlanInputReader.Inputs input) {
        return codec.hash(input);
    }

    public record Choices(PlanInputReader.Inputs input, String token, List<Result> options, List<String> diagnostics) { }

    public record Outcome(Long planId, Result result) { }
}
