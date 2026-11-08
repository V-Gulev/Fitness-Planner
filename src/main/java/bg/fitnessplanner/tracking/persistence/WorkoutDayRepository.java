package bg.fitnessplanner.tracking.persistence;
import bg.fitnessplanner.planning.persistence.WorkoutDay;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface WorkoutDayRepository extends JpaRepository<WorkoutDay, Long> {
    @EntityGraph(attributePaths = {"plan", "exercises"})
    @Query("select d from WorkoutDay d where d.id = :day and d.plan.userId = :owner")
    Optional<WorkoutDay> owned(@Param("day") Long day, @Param("owner") Long owner);
}
