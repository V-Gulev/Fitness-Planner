package bg.fitnessplanner.profile.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface FitnessProfileRepository extends JpaRepository<FitnessProfile, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FitnessProfile p where p.userId = :userId")
    Optional<FitnessProfile> lockOwner(@Param("userId") Long userId);
}
