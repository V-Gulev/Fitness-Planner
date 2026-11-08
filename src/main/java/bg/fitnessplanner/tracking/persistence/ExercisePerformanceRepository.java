package bg.fitnessplanner.tracking.persistence;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface ExercisePerformanceRepository extends JpaRepository<ExercisePerformance, Long> {
    // Page IDs first. Fetching a collection in the paginated query would paginate joined set rows.
    @Query("select p.id from ExercisePerformance p join p.session s where s.userId = :owner and p.exerciseId = :exercise "
        + "and s.status = bg.fitnessplanner.tracking.persistence.SessionStatus.COMPLETED and s.sessionDate <= :date "
        + "order by s.sessionDate desc, s.completedAt desc, p.id desc")
    List<Long> recentIds(@Param("owner") Long owner, @Param("exercise") Long exercise, @Param("date") LocalDate date, Pageable page);
    @Query("""
        select count(w) from ExercisePerformance p join p.sets w join p.session s
        where s.userId = :owner and s.sessionDate between :from and :through
        and s.status = bg.fitnessplanner.tracking.persistence.SessionStatus.COMPLETED
        and p.status = bg.fitnessplanner.tracking.persistence.PerformanceStatus.COMPLETED
        """)
    long countCompletedSets(@Param("owner") Long owner, @Param("from") LocalDate from,
                            @Param("through") LocalDate through);

    @EntityGraph(attributePaths = {"sets", "session"})
    List<ExercisePerformance> findByIdIn(List<Long> ids);
}
