package com.teachnet.jobs;

import com.teachnet.common.PageResponse;
import com.teachnet.jobs.JobDtos.ApplicantDto;
import com.teachnet.jobs.JobDtos.ApplicationStatusRequest;
import com.teachnet.jobs.JobDtos.ApplyRequest;
import com.teachnet.jobs.JobDtos.JobDetail;
import com.teachnet.jobs.JobDtos.JobRequest;
import com.teachnet.jobs.JobDtos.JobSummary;
import com.teachnet.jobs.JobDtos.MyApplicationDto;
import com.teachnet.jobs.JobDtos.MyJobRow;
import com.teachnet.jobs.JobDtos.StatusRequest;
import com.teachnet.profile.Board;
import com.teachnet.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final JobService service;

    public JobController(JobService service) {
        this.service = service;
    }

    @GetMapping("/api/jobs")
    public PageResponse<JobSummary> search(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) String subject,
                                           @RequestParam(required = false) String city,
                                           @RequestParam(required = false) Board board,
                                           @RequestParam(required = false) EmploymentType type,
                                           @RequestParam(required = false) Long institutionId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.search(q, subject, city, board, type, institutionId, page, size);
    }

    @GetMapping("/api/jobs/mine")
    public List<MyJobRow> mine(@AuthenticationPrincipal AuthUser me) {
        return service.myJobs(me);
    }

    @GetMapping("/api/jobs/recommended")
    public List<JobSummary> recommended(@AuthenticationPrincipal AuthUser me,
                                        @RequestParam(defaultValue = "5") int limit) {
        return service.recommended(me, limit);
    }

    @GetMapping("/api/jobs/{id}")
    public JobDetail detail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.detail(id, me);
    }

    @PostMapping("/api/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobDetail create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody JobRequest req) {
        return service.create(me, req);
    }

    @PutMapping("/api/jobs/{id}")
    public JobDetail update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                            @Valid @RequestBody JobRequest req) {
        return service.update(me, id, req);
    }

    @PutMapping("/api/jobs/{id}/status")
    public JobDetail setStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                               @Valid @RequestBody StatusRequest req) {
        return service.setStatus(me, id, req.status());
    }

    @DeleteMapping("/api/jobs/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/jobs/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public MyApplicationDto apply(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                  @Valid @RequestBody(required = false) ApplyRequest req) {
        return service.applyTo(me, id, req);
    }

    @GetMapping("/api/jobs/{id}/applications")
    public List<ApplicantDto> applicants(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.applicants(me, id);
    }

    @GetMapping("/api/applications/mine")
    public List<MyApplicationDto> myApplications(@AuthenticationPrincipal AuthUser me) {
        return service.myApplications(me);
    }

    @PutMapping("/api/applications/{id}/status")
    public ApplicantDto updateStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                     @Valid @RequestBody ApplicationStatusRequest req) {
        return service.updateApplicationStatus(me, id, req.status());
    }

    @DeleteMapping("/api/applications/{id}")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.withdraw(me, id);
        return ResponseEntity.noContent().build();
    }
}
