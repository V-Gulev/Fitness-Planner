package bg.fitnessplanner.account.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailNormalized(String email);

    boolean existsByEmailNormalized(String email);
}
