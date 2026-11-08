package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.profile.domain.TrainingGoal;
import bg.fitnessplanner.shared.domain.ExperienceLevel;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/** Versioned, framework-free configuration; the application loads it from planner-v1.json. */
public record PlannerConfig(String version, int warmupSeconds, int maxExercisesPerDay, int maxSetsPerDay,
                            int minRecoveryGapDays, int secondaryHalfCredits,
                            Map<ExperienceLevel, VolumeTarget> volumes, Map<TrainingGoal, GoalRules> goals,
                            Weights weights) {
    public PlannerConfig {
        Objects.requireNonNull(version);
        Objects.requireNonNull(weights);
        volumes = Map.copyOf(volumes);
        goals = Map.copyOf(goals);
        if (version.isBlank() || warmupSeconds < 0 || warmupSeconds > 900 || maxExercisesPerDay < 1
                || maxExercisesPerDay > 8 || maxSetsPerDay < 2 || maxSetsPerDay > 32
                || minRecoveryGapDays < 1 || minRecoveryGapDays > 3
                || secondaryHalfCredits < 0 || secondaryHalfCredits > 2
                || !volumes.keySet().containsAll(java.util.Set.of(ExperienceLevel.values()))
                || !goals.keySet().containsAll(java.util.Set.of(TrainingGoal.values()))) {
            throw new IllegalArgumentException("Invalid planner configuration.");
        }
    }

    public record VolumeTarget(int major, int accessory) {
        public VolumeTarget {
            if (major < 1 || major > 30 || accessory < 1 || accessory > 30) {
                throw new IllegalArgumentException("Volume targets must be between 1 and 30.");
            }
        }
    }

    public record RepRange(int min, int max, int restSeconds) {
        public RepRange {
            if (min < 1 || max < min || max > 50 || restSeconds < 0 || restSeconds > 600) {
                throw new IllegalArgumentException("Invalid repetition/rest prescription.");
            }
        }
    }

    public record GoalRules(int volumePercent, RepRange compound, RepRange isolation,
                            BigDecimal compoundSuitability, BigDecimal isolationSuitability) {
        public GoalRules {
            Objects.requireNonNull(compound);
            Objects.requireNonNull(isolation);
            if (volumePercent < 1 || volumePercent > 200) throw new IllegalArgumentException("Invalid volume multiplier.");
            for (BigDecimal suitability : new BigDecimal[]{compoundSuitability, isolationSuitability}) {
                if (suitability == null || suitability.signum() < 0 || suitability.compareTo(BigDecimal.ONE) > 0) {
                    throw new IllegalArgumentException("Suitability must be between zero and one.");
                }
            }
        }
    }

    public record Weights(BigDecimal muscle, BigDecimal goal, BigDecimal experience, BigDecimal preference,
                          BigDecimal novelty, BigDecimal redundancy, BigDecimal fatigue, BigDecimal overshoot) {
        public Weights {
            for (BigDecimal weight : new BigDecimal[]{muscle, goal, experience, preference, novelty, redundancy, fatigue, overshoot}) {
                if (weight == null || weight.signum() < 0 || weight.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new IllegalArgumentException("Score weights must be between zero and 100.");
                }
            }
        }
    }
}
