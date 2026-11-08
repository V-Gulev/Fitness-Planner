package bg.fitnessplanner.planning.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    @EntityGraph(attributePaths = {"days", "days.exercises"})
    Optional<WorkoutPlan> findByIdAndUserId(Long id, Long userId);
    List<WorkoutPlan> findTop10ByUserIdOrderByCreatedAtDescIdDesc(Long userId);
    long countByUserId(Long userId);
}
