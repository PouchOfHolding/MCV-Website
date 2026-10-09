package pt.ualg.mcv.family;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyHasMemberRepository extends JpaRepository<FamilyHasMember, FamilyHasMemberId> {
    @EntityGraph(attributePaths = {"member", "member.photos"})
    Page<FamilyHasMember> findByFamilyUuid(UUID familyUuid, Pageable pageable);

    long countByFamilyUuidAndRole(UUID familyUuid, FamilyMemberRole role);
}