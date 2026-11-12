package bg.fitnessplanner.planning.application;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.planning.engine.ExerciseExplainer;
import bg.fitnessplanner.planning.engine.PlannerConfig;
import bg.fitnessplanner.planning.engine.PlanningModel.Evaluation;
import bg.fitnessplanner.planning.engine.PlanningRules;
import bg.fitnessplanner.planning.persistence.WorkoutPlan;
import bg.fitnessplanner.planning.persistence.WorkoutPlanRepository;
import bg.fitnessplanner.profile.application.ProfileService;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.profile.persistence.FitnessProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class PlanQueryService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH)
            .withZone(ZoneId.of("Europe/Sofia"));
    private final WorkoutPlanRepository plans;
    private final FitnessProfileRepository profiles;
    private final ProfileService profileService;
    private final SnapshotCodec codec;

    public PlanQueryService(WorkoutPlanRepository plans, FitnessProfileRepository profiles,
                            ProfileService profileService, SnapshotCodec codec) {
        this.plans = plans; this.profiles = profiles; this.profileService = profileService; this.codec = codec;
    }

    @Transactional(readOnly = true)
    public PlanViews.Page page(Long owner, Long requestedPlanId) {
        var profile = profiles.findById(owner);
        Long activeId = profile.map(p -> p.getActivePlanId()).orElse(null);
        Long selectedId = requestedPlanId == null ? activeId : requestedPlanId;
        var snapshot = profileService.snapshot(owner).orElse(null);
        var history = plans.findTop10ByUserIdOrderByCreatedAtDescIdDesc(owner).stream()
                .map(p -> new PlanViews.Summary(p.getId(), DATE.format(p.getCreatedAt()), Objects.equals(p.getId(), activeId))).toList();
        PlanViews.Details details = null;
        if (selectedId != null) {
            WorkoutPlan plan = plans.findByIdAndUserId(selectedId, owner).orElseThrow(PlanNotFoundException::new);
            Evaluation evaluation = codec.read(plan.getEvaluationJson(), Evaluation.class);
            PlannerConfig usedConfig = codec.read(plan.getConfigSnapshot(), PlannerConfig.class);
            Map<Long, ExerciseSnapshot> catalogMap = null;
            ProfileSnapshot profileSnapshot = null;
            try {
                if (plan.getCatalogSnapshot() != null) {
                    var catalogList = codec.read(plan.getCatalogSnapshot(), new TypeReference<List<ExerciseSnapshot>>() { });
                    catalogMap = PlanningRules.byId(catalogList);
                }
                if (plan.getInputSnapshot() != null) {
                    profileSnapshot = codec.read(plan.getInputSnapshot(), ProfileSnapshot.class);
                }
            } catch (Exception ignored) { }
            if (profileSnapshot == null) {
                profileSnapshot = snapshot;
            }
            final Map<Long, ExerciseSnapshot> finalCatalog = catalogMap;
            final ProfileSnapshot finalProfile = profileSnapshot;

            List<PlanViews.Volume> volumes = new ArrayList<>();
            for (MuscleGroup muscle : MuscleGroup.values()) {
                int target = evaluation.targetHalfSets().get(muscle), actual = evaluation.volumeHalfSets().get(muscle);
                int delta = actual - target;
                String difference = delta == 0 ? "On target" : half(Math.abs(delta)).toPlainString() + (delta < 0 ? " below target" : " above target");
                volumes.add(new PlanViews.Volume(muscle.getLabel(), half(target), half(actual), difference));
            }
            var days = plan.getDays().stream().sorted(Comparator.comparingInt(d -> d.getWeekday())).map(day ->
                    new PlanViews.Day(day.getId(), day.getWeekday(), DayOfWeek.of(day.getWeekday()).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            day.getDayType().getLabel(), (day.getEstimatedSeconds() + 59) / 60,
                            day.getExercises().stream().map(e -> {
                                String explanation = e.getExplanation();
                                if (ExerciseExplainer.isLegacyExplanation(explanation) && finalCatalog != null) {
                                    ExerciseSnapshot meta = finalCatalog.get(e.getExerciseId());
                                    if (meta != null) {
                                        explanation = ExerciseExplainer.explain(meta, finalProfile, e.getSets(),
                                                e.getRepMin(), e.getRepMax(), e.getRestSeconds(), day.getDayType(), e.getPosition());
                                    }
                                }
                                return new PlanViews.Exercise(e.getNameSnapshot(),
                                        MuscleGroup.valueOf(e.getPrimaryMuscleSnapshot()).getLabel(), e.getSets(),
                                        e.getRepMin() + "–" + e.getRepMax(), e.getRestSeconds(), explanation);
                            }).toList())).toList();
            details = new PlanViews.Details(plan.getId(), DATE.format(plan.getCreatedAt()), plan.getStatus(),
                    Objects.equals(activeId, plan.getId()), snapshot == null || !codec.profileHash(snapshot).equals(plan.getProfileHash()),
                    plan.getAlgorithmVersion(), plan.getConfigVersion(), days.stream().map(PlanViews.Day::type).collect(java.util.stream.Collectors.joining(" / ")),
                    plan.getCandidatesConstructed(), (usedConfig.warmupSeconds() + 59) / 60,
                    half(usedConfig.secondaryHalfCredits()),
                    codec.read(plan.getDiagnosticsJson(), new TypeReference<List<String>>() { }), days, List.copyOf(volumes));
        }
        return new PlanViews.Page(snapshot, details, history);
    }

    private static BigDecimal half(int units) { return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(2)); }
}
