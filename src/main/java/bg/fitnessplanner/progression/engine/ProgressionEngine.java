package bg.fitnessplanner.progression.engine;

import bg.fitnessplanner.catalog.domain.LoadType;
import static bg.fitnessplanner.progression.engine.ProgressionModel.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Pure, conservative double progression; no persistence, clock, or inferred performed weights. */
public final class ProgressionEngine {
    private final ProgressionConfig config;
    public ProgressionEngine(ProgressionConfig config) { this.config = config; }

    public Decision recommend(Target target, List<Exposure> history, LocalDate asOf) {
        if (target.loadType() == LoadType.BODYWEIGHT || target.loadType() == LoadType.ASSISTED)
            return result(Action.INSUFFICIENT_DATA, "UNSUPPORTED_LOAD", "Automatic load progression is unavailable for bodyweight and assisted exercises.", null, null, List.of());
        if (!ProgressionConfig.validStep(target.step()))
            return result(Action.INSUFFICIENT_DATA, "MISSING_INCREMENT", "Set an available load increment in your profile.", null, null, List.of());
        var recent = history.stream().sorted(Comparator.comparing(Exposure::date).thenComparing(Exposure::recordedAt)
                .thenComparing(Exposure::id).reversed()).limit(3).toList();
        if (recent.isEmpty()) return result(Action.INSUFFICIENT_DATA, "NO_HISTORY", "Log a completed workout before choosing a load from history.", null, null, List.of());
        Exposure latest = recent.getFirst();
        List<Long> latestId = List.of(latest.id());
        if (latest.date().isAfter(asOf) || latest.date().isBefore(asOf.minusDays(config.maxAgeDays())))
            return result(Action.INSUFFICIENT_DATA, "OUTSIDE_WINDOW", "The latest performance is outside the recent training window.", null, null, latestId);
        if (!comparable(target, latest))
            return result(Action.INSUFFICIENT_DATA, "INCOMPARABLE_LATEST", "The latest performance was skipped, incomplete, used mixed loads, or had a different prescription.", null, null, latestId);
        BigDecimal load = latest.sets().getFirst().weightKg();
        var chain = new ArrayList<Exposure>();
        for (Exposure e : recent) {
            if (e.date().isBefore(asOf.minusDays(config.maxAgeDays())) || !comparable(target, e)
                    || load.compareTo(e.sets().getFirst().weightKg()) != 0) break;
            chain.add(e);
        }
        List<Long> ids = chain.stream().map(Exposure::id).toList();
        if (chain.size() >= 3 && declining(chain.get(2), chain.get(1)) && declining(chain.get(1), chain.get(0)))
            return result(Action.REVIEW, "DECLINING_REPS", "Total reps declined across three comparable performances. Review your training before changing the load; the log cannot identify the cause.", load, null, ids);
        if (chain.size() >= 2 && ready(target, chain.get(0)) && ready(target, chain.get(1))) {
            BigDecimal next = load.add(target.step());
            if (target.step().multiply(BigDecimal.valueOf(100)).compareTo(load.multiply(config.maxIncreasePercent())) > 0
                    || next.compareTo(new BigDecimal("1000")) > 0)
                return result(Action.REVIEW, "INCREMENT_TOO_LARGE", "The available increment exceeds the configured increase limit. Review a smaller available step.", load, null, ids);
            return result(Action.INCREASE, "DOUBLE_PROGRESSION", "Every set reached the top of the rep range with at least " + config.minRir() + " reps in reserve in two consecutive comparable performances.", load, next, ids);
        }
        String reason = latest.sets().stream().anyMatch(s -> s.rir() == null) ? "EFFORT_UNKNOWN"
                : latest.sets().stream().anyMatch(s -> s.rir() < config.minRir()) ? "EFFORT_HIGH" : "BUILD_REPS";
        return result(Action.MAINTAIN, reason, switch (reason) {
            case "EFFORT_UNKNOWN" -> "Keep the load. Record RIR on every set before an increase can be suggested.";
            case "EFFORT_HIGH" -> "Keep the load. Some sets were below the required reps in reserve.";
            default -> "Keep the load and build reps. Two consecutive performances at the top of the range are required.";
        }, load, load, ids);
    }

    private boolean comparable(Target t, Exposure e) {
        if (!Objects.equals(t.exerciseId(), e.exerciseId()) || t.loadType() != e.loadType() || e.skipped()
                || t.sets() != e.prescribedSets() || t.repMin() != e.repMin() || t.repMax() != e.repMax()
                || t.restSeconds() != e.restSeconds() || e.sets().size() != t.sets() || e.sets().isEmpty()) return false;
        BigDecimal load = e.sets().getFirst().weightKg();
        if (load == null || load.signum() <= 0 || load.compareTo(new BigDecimal("1000")) > 0) return false;
        var numbers = new HashSet<Integer>();
        for (WorkSet s : e.sets()) {
            if (s.number() < 1 || s.number() > t.sets() || !numbers.add(s.number()) || s.reps() < 1 || s.reps() > 100
                    || s.weightKg() == null || load.compareTo(s.weightKg()) != 0
                    || (s.rir() != null && (s.rir() < 0 || s.rir() > 10))) return false;
        }
        return true;
    }
    private boolean ready(Target t, Exposure e) {
        return e.sets().stream().allMatch(s -> s.reps() >= t.repMax() && s.rir() != null && s.rir() >= config.minRir());
    }
    private boolean declining(Exposure previous, Exposure current) {
        int before = previous.sets().stream().mapToInt(WorkSet::reps).sum();
        int after = current.sets().stream().mapToInt(WorkSet::reps).sum();
        int threshold = Math.max(config.declineMinReps(), BigDecimal.valueOf(before).multiply(config.declinePercent())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.CEILING).intValueExact());
        return before - after >= threshold;
    }
    private Decision result(Action action, String reason, String explanation, BigDecimal load, BigDecimal next, List<Long> ids) {
        return new Decision(action, reason, explanation, load, next, ids);
    }
}
