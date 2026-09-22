package com.teachnet.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {

    Optional<TeacherProfile> findByUserId(Long userId);

    List<TeacherProfile> findByUserIdIn(Collection<Long> userIds);

    long countByVerifiedFalse();

    /** All filters are optional; pass null to skip. Text filters must already be lower-cased. */
    @Query("""
            select p from TeacherProfile p join fetch p.user u
            where u.enabled = true
              and (:q is null or lower(u.fullName) like :q or lower(p.headline) like :q)
              and (:city is null or lower(p.city) = :city)
              and (:openToWork is null or p.openToWork = :openToWork)
              and (:minExperience is null or p.yearsExperience >= :minExperience)
              and (:subject is null or exists (
                    select 1 from TeacherSubject s where s.profile = p and lower(s.subject) like :subject))
              and (:board is null or exists (
                    select 1 from TeacherSubject s where s.profile = p and s.board = :board))
            """)
    Page<TeacherProfile> search(
            @Param("q") String q,
            @Param("city") String city,
            @Param("subject") String subject,
            @Param("board") Board board,
            @Param("openToWork") Boolean openToWork,
            @Param("minExperience") Integer minExperience,
            Pageable pageable);

    /** Teachers the user is not yet related to, preferring the same city. */
    @Query("""
            select p from TeacherProfile p join fetch p.user u
            where u.enabled = true and u.id not in :excludeIds
            order by case when lower(p.city) = :city then 0 else 1 end, p.verified desc, p.id desc
            """)
    List<TeacherProfile> findSuggestions(@Param("excludeIds") Collection<Long> excludeIds,
                                         @Param("city") String city, Pageable pageable);
}
