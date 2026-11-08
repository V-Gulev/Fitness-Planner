package bg.fitnessplanner.catalog.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    @EntityGraph(attributePaths = {"muscles", "requiredEquipment", "tags"})
    @Query("select distinct e from Exercise e where e.active = true order by e.code")
    List<Exercise> findActiveCatalog();
}
