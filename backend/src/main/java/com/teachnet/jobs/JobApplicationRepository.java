package com.teachnet.jobs;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByJobIdAndTeacherId(Long jobId, Long teacherId);

    Optional<JobApplication> findByJobIdAndTeacherId(Long jobId, Long teacherId);

    long countByJobId(Long jobId);

    @EntityGraph(attributePaths = "teacher")
    List<JobApplication> findByJobIdOrderByCreatedAtDesc(Long jobId);

    @EntityGraph(attributePaths = {"job", "job.institution"})
    List<JobApplication> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    @Query("select a.job.id, count(a) from JobApplication a where a.job.id in :jobIds group by a.job.id")
    List<Object[]> countByJobIds(@Param("jobIds") Collection<Long> jobIds);
}
