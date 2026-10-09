package pt.ualg.mcv.family;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, UUID>
{
    @EntityGraph(attributePaths =
    {"photos"})
    Page<Member> findAll(Pageable pageable);

    @Query("SELECT m FROM Member m WHERE "
            + "LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) OR "
            + "LOWER(m.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR "
            + "LOWER(m.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Member> searchByName(@Param("name") String name, Pageable pageable);
}
