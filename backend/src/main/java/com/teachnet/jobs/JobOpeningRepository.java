package com.teachnet.jobs;

import com.teachnet.profile.Board;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobOpeningRepository extends JpaRepository<JobOpening, Long> {

    long countByInstitutionIdAndStatus(Long institutionId, JobStatus status);

    long countByStatus(JobStatus status);

    @EntityGraph(attributePaths = "institution")
    List<JobOpening> findByInstitutionIdOrderByCreatedAtDesc(Long institutionId);

    /** Open jobs only. Text filters must already be lower-cased and wrapped in % where needed. */
    @EntityGraph(attributePaths = "institution")
    @Query("""
            select j from JobOpening j
            where j.status = com.teachnet.jobs.JobStatus.OPEN
              and (:q is null or lower(j.title) like :q or lower(j.description) like :q
                   or lower(j.institution.name) like :q)
              and (:subject is null or lower(j.subject) like :subject)
              and (:city is null or lower(j.city) = :city)
              and (:board is null or j.board = :board)
              and (:type is null or j.employmentType = :type)
              and (:institutionId is null or j.institution.id = :institutionId)
            """)
    Page<JobOpening> search(@Param("q") String q, @Param("subject") String subject, @Param("city") String city,
                            @Param("board") Board board, @Param("type") EmploymentType type,
                            @Param("institutionId") Long institutionId, Pageable pageable);

    /** Open jobs matching any of the teacher's subjects or their city. */
    @EntityGraph(attributePaths = "institution")
    @Query("""
            select j from JobOpening j
            where j.status = com.teachnet.jobs.JobStatus.OPEN
              and (lower(j.subject) in :subjects or lower(j.city) = :city)
              and j.id not in (select a.job.id from JobApplication a where a.teacher.id = :teacherId)
            order by case when lower(j.subject) in :subjects then 0 else 1 end, j.createdAt desc
            """)
    List<JobOpening> recommend(@Param("subjects") Collection<String> subjects, @Param("city") String city,
                               @Param("teacherId") Long teacherId, Pageable pageable);
}
