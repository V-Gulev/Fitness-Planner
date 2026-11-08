package bg.fitnessplanner.planning.engine;

import bg.fitnessplanner.planning.engine.PlanningModel.DayType;
import java.util.*;

public final class SplitCandidates {
    private SplitCandidates() { }

    public static List<List<DayType>> forFrequency(int frequency) {
        if (frequency < 2 || frequency > 5) throw new IllegalArgumentException("Frequency must be 2–5.");
        List<List<DayType>> families = List.of(List.of(DayType.FULL), List.of(DayType.UPPER, DayType.LOWER),
                List.of(DayType.PUSH, DayType.PULL, DayType.LEGS), List.of(DayType.UPPER, DayType.LOWER, DayType.FULL),
                List.of(DayType.PUSH, DayType.PULL, DayType.LEGS, DayType.UPPER, DayType.LOWER));
        Map<String, List<DayType>> candidates = new TreeMap<>();
        for (var family : families) {
            List<DayType> sequence = new ArrayList<>();
            for (int i = 0; i < frequency; i++) sequence.add(family.get(i % family.size()));
            for (int rotation = 0; rotation < frequency; rotation++) {
                List<DayType> rotated = new ArrayList<>();
                for (int i = 0; i < frequency; i++) rotated.add(sequence.get((i + rotation) % frequency));
                candidates.put(rotated.toString(), List.copyOf(rotated));
            }
        }
        return List.copyOf(candidates.values());
    }
}
