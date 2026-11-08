package bg.fitnessplanner.tracking.persistence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {
    Optional<WorkoutSession> findByIdAndUserId(Long id, Long userId);
    Optional<WorkoutSession> findByUserIdAndSubmissionToken(Long userId, String token);
    Optional<WorkoutSession> findByUserIdAndWorkoutDayIdAndStatus(Long userId, Long dayId, SessionStatus status);
    List<WorkoutSession> findTop20ByUserIdOrderBySessionDateDescIdDesc(Long userId);
    Optional<WorkoutSession> findFirstByUserIdAndStatusOrderByUpdatedAtDescIdDesc(Long userId, SessionStatus status);
    Optional<WorkoutSession> findFirstByUserIdAndStatusOrderBySessionDateDescIdDesc(Long userId, SessionStatus status);
    long countByUserIdAndStatusAndSessionDateBetween(Long userId, SessionStatus status,
                                                    java.time.LocalDate from, java.time.LocalDate through);
    @Query("select distinct s.workoutDayId from WorkoutSession s where s.userId = :owner and s.status = :status and s.sessionDate = :date")
    Set<Long> findDayIds(@Param("owner") Long owner, @Param("status") SessionStatus status,
                        @Param("date") java.time.LocalDate date);
    @Query("select distinct s.workoutDayId from WorkoutSession s where s.userId = :owner and s.status = :status")
    Set<Long> findDayIdsByStatus(@Param("owner") Long owner, @Param("status") SessionStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from WorkoutSession s where s.id = :id and s.userId = :owner")
    Optional<WorkoutSession> lockOwned(@Param("id") Long id, @Param("owner") Long owner);
}
