package bg.fitnessplanner.progression.engine;

import bg.fitnessplanner.catalog.domain.LoadType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProgressionModel {
    private ProgressionModel() { }
    public enum Action { INSUFFICIENT_DATA, MAINTAIN, INCREASE, REVIEW }
    public record Target(Long exerciseId, int sets, int repMin, int repMax, int restSeconds,
                         LoadType loadType, BigDecimal step) { }
    public record WorkSet(int number, int reps, BigDecimal weightKg, Integer rir) { }
    public record Exposure(Long id, Long exerciseId, LocalDate date, Instant recordedAt, int prescribedSets,
                           int repMin, int repMax, int restSeconds, LoadType loadType, boolean skipped, List<WorkSet> sets) {
        public Exposure { sets = List.copyOf(sets); }
    }
    public record Decision(Action action, String reason, String explanation, BigDecimal currentLoad,
                           BigDecimal suggestedLoad, List<Long> evidenceIds) {
        public Decision { evidenceIds = List.copyOf(evidenceIds); }
        public boolean acceptable() { return (action == Action.MAINTAIN || action == Action.INCREASE) && suggestedLoad != null; }
    }
}
