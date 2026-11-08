package bg.fitnessplanner.progression.engine;

import bg.fitnessplanner.catalog.domain.Equipment;
import java.math.BigDecimal;
import java.util.Map;

public record ProgressionConfig(String version, int maxAgeDays, int minRir, BigDecimal maxIncreasePercent,
                                int declineMinReps, BigDecimal declinePercent, Map<Equipment, BigDecimal> defaultSteps) {
    public ProgressionConfig {
        defaultSteps = Map.copyOf(defaultSteps);
        if (version == null || version.isBlank() || maxAgeDays < 1 || minRir < 0 || minRir > 10
                || maxIncreasePercent == null || maxIncreasePercent.signum() <= 0 || declineMinReps < 1
                || declinePercent == null || declinePercent.signum() <= 0
                || defaultSteps.values().stream().anyMatch(v -> !validStep(v))) {
            throw new IllegalArgumentException("Invalid progression configuration.");
        }
    }
    public static boolean validStep(BigDecimal step) {
        return step != null && step.signum() > 0 && step.compareTo(new BigDecimal("100")) <= 0
                && step.stripTrailingZeros().scale() <= 2;
    }
}
