package com.teachnet.institution;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {

    Optional<Institution> findByOwnerId(Long ownerId);

    List<Institution> findByOwnerIdIn(Collection<Long> ownerIds);

    long countByVerifiedFalse();

    /** Text filters must already be lower-cased and wrapped in % where needed. */
    @Query("""
            select i from Institution i
            where (:q is null or lower(i.name) like :q)
              and (:city is null or lower(i.city) = :city)
              and (:type is null or i.institutionType = :type)
            """)
    Page<Institution> search(@Param("q") String q, @Param("city") String city,
                             @Param("type") InstitutionType type, Pageable pageable);
}
