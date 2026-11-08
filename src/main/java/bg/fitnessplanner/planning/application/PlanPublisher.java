package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.catalog.application.CatalogService;
import bg.fitnessplanner.planning.engine.*;
import bg.fitnessplanner.planning.engine.PlanningModel.Result;
import bg.fitnessplanner.planning.persistence.*;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;

@Service
public class PlanPublisher {
    private final FitnessProfileRepository profiles;
    private final WorkoutPlanRepository plans;
    private final CatalogService catalog;
    private final PlannerConfig config;
    private final PlanEvaluator evaluator;
    private final SnapshotCodec codec;
    private final Clock clock;

    public PlanPublisher(FitnessProfileRepository profiles, WorkoutPlanRepository plans, CatalogService catalog,
                         PlannerConfig config, PlanEvaluator evaluator, SnapshotCodec codec, Clock clock) {
        this.profiles = profiles; this.plans = plans; this.catalog = catalog; this.config = config;
        this.evaluator = evaluator; this.codec = codec; this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long publish(Long owner, PlanInputReader.Inputs input, Result result, String algorithmVersion) {
        if (!owner.equals(input.profile().userId()) || !result.publishable() || result.plan() == null) {
            throw new IllegalArgumentException("Only a valid owned planning result can be published.");
        }
        var profile = profiles.lockOwner(owner).orElseThrow(MissingProfileException::new);
        var currentCatalog = catalog.snapshot();
        if (!Objects.equals(profile.getVersion(), input.profile().version())
                || !codec.hash(currentCatalog).equals(codec.hash(input.catalog()))) throw new PlanConflictException();
        var evaluation = evaluator.evaluate(input.profile(), currentCatalog, result.plan());
        if (evaluation.hardViolations() != 0) throw new IllegalArgumentException("The final plan failed independent hard-constraint validation.");
        var plan = new WorkoutPlan(owner, clock.instant(), algorithmVersion, config.version(),
                codec.hash(input.catalog()), codec.hash(config), codec.profileHash(input.profile()), input.profile().version(),
                evaluation.status(), result.plan().candidateKey(), codec.canonicalJson(input.profile()), codec.canonicalJson(config),
                codec.canonicalJson(input.catalog()), codec.json(evaluation), codec.json(result.diagnostics()),
                result.candidatesConstructed(), result.additionsExamined());
        var known = PlanningRules.byId(input.catalog());
        for (var day : result.plan().days()) {
            var storedDay = new WorkoutDay(plan, day.weekday(), day.type(), evaluation.daySeconds().get(day.weekday()));
            for (int i = 0; i < day.exercises().size(); i++) {
                var item = day.exercises().get(i);
                var metadata = known.get(item.exerciseId());
                storedDay.addExercise(new PlannedExercise(storedDay, i, item, metadata,
                        PlanningRules.seconds(item, metadata), item.score() == null ? null : codec.json(item.score())));
            }
            plan.addDay(storedDay);
        }
        plans.saveAndFlush(plan);
        profile.activatePlan(plan.getId());
        profiles.saveAndFlush(profile);
        return plan.getId();
    }
}
