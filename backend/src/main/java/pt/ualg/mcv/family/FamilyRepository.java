package pt.ualg.mcv.family;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FamilyRepository extends JpaRepository<Family, UUID> {
    @Query("SELECT f FROM Family f WHERE " +
           "(:minYear IS NULL OR f.year >= :minYear) AND " +
           "(:maxYear IS NULL OR f.year <= :maxYear)")
    Page<Family> findByYearBetween(
            @Param("minYear") Long minYear, 
            @Param("maxYear") Long maxYear, 
            Pageable pageable);
}
