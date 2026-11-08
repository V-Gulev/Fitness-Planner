package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.catalog.application.ExerciseSnapshot;
import bg.fitnessplanner.catalog.domain.MuscleGroup;
import bg.fitnessplanner.profile.application.ProfileSnapshot;
import bg.fitnessplanner.planning.engine.PlanningModel.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class ExerciseScorer {
    private final PlannerConfig config;
    public ExerciseScorer(PlannerConfig config) { this.config = config; }

    public Score score(ExerciseSnapshot e, int sets, ProfileSnapshot p, Map<MuscleGroup, Integer> volume,
                       Map<MuscleGroup, Integer> target, boolean preferred, boolean novel,
                       int samePatternCount, int compoundSetsToday) {
        int gained = 0, excess = 0, units = 0;
        for (MuscleGroup m : MuscleGroup.values()) {
            int delta = sets * PlanningRules.credits(e, m, config);
            units += delta;
            gained += Math.min(Math.max(target.get(m) - volume.get(m), 0), delta);
            excess += Math.max(0, volume.get(m) + delta - Math.max(volume.get(m), target.get(m)));
        }
        BigDecimal muscle = ratio(gained, units), overshoot = ratio(excess, units);
        var goalRules = config.goals().get(p.goal());
        BigDecimal goal = (e.compound() ? goalRules.compoundSuitability() : goalRules.isolationSuitability()).setScale(8, RoundingMode.HALF_UP);
        BigDecimal experience = BigDecimal.ONE.subtract(ratio(p.experience().ordinal() - e.difficulty().ordinal(), 2));
        BigDecimal preference = preferred ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal novelty = novel ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal redundancy = ratio(Math.min(2, samePatternCount), 2);
        BigDecimal fatigue = e.fatigueCost().multiply(ratio(Math.min(12, compoundSetsToday), 12)).setScale(8, RoundingMode.HALF_UP);
        var w = config.weights();
        BigDecimal total = muscle.multiply(w.muscle()).add(goal.multiply(w.goal()))
                .add(experience.multiply(w.experience())).add(preference.multiply(w.preference()))
                .add(novelty.multiply(w.novelty())).subtract(redundancy.multiply(w.redundancy()))
                .subtract(fatigue.multiply(w.fatigue())).subtract(overshoot.multiply(w.overshoot()))
                .setScale(8, RoundingMode.HALF_UP);
        return new Score(muscle, goal, experience, preference, novelty, redundancy, fatigue, overshoot, total);
    }

    private static BigDecimal ratio(int numerator, int denominator) {
        return BigDecimal.valueOf(numerator).divide(BigDecimal.valueOf(denominator), 8, RoundingMode.HALF_UP);
    }
}
