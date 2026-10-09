package pt.ualg.mcv.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "u.uuid <> :excludeUuid")
    Page<User> searchByFullNameAndUuidNot(
            @Param("search") String search, 
            @Param("excludeUuid") UUID excludeUuid, 
            Pageable pageable
    );
    Page<User> findByUuidNot(UUID excludedUuid, Pageable pageable);
}
