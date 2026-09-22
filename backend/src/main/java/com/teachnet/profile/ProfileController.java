package com.teachnet.profile;

import com.teachnet.common.PageResponse;
import com.teachnet.profile.ProfileDtos.ExperienceRequest;
import com.teachnet.profile.ProfileDtos.PortfolioRequest;
import com.teachnet.profile.ProfileDtos.ProfileView;
import com.teachnet.profile.ProfileDtos.QualificationRequest;
import com.teachnet.profile.ProfileDtos.SubjectRequest;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import com.teachnet.profile.ProfileDtos.UpdateProfileRequest;
import com.teachnet.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping("/api/teachers")
    public PageResponse<TeacherCard> search(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) String city,
                                            @RequestParam(required = false) String subject,
                                            @RequestParam(required = false) Board board,
                                            @RequestParam(required = false) Boolean openToWork,
                                            @RequestParam(required = false) Integer minExperience,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.search(q, city, subject, board, openToWork, minExperience, page, size);
    }

    @GetMapping("/api/profiles/me")
    public ProfileView mine(@AuthenticationPrincipal AuthUser me) {
        return service.view(me.id(), me);
    }

    @GetMapping("/api/profiles/{userId}")
    public ProfileView view(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId) {
        return service.view(userId, me);
    }

    @PutMapping("/api/profiles/me")
    public ProfileView update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody UpdateProfileRequest req) {
        return service.updateMine(me, req);
    }

    @PostMapping("/api/profiles/me/subjects")
    public ProfileView addSubject(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SubjectRequest req) {
        return service.addSubject(me, req);
    }

    @DeleteMapping("/api/profiles/me/subjects/{id}")
    public ProfileView removeSubject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.removeSubject(me, id);
    }

    @PostMapping("/api/profiles/me/qualifications")
    public ProfileView addQualification(@AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody QualificationRequest req) {
        return service.addQualification(me, req);
    }

    @DeleteMapping("/api/profiles/me/qualifications/{id}")
    public ProfileView removeQualification(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.removeQualification(me, id);
    }

    @PostMapping("/api/profiles/me/experiences")
    public ProfileView addExperience(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ExperienceRequest req) {
        return service.addExperience(me, req);
    }

    @DeleteMapping("/api/profiles/me/experiences/{id}")
    public ProfileView removeExperience(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.removeExperience(me, id);
    }

    @PostMapping("/api/profiles/me/portfolio")
    public ProfileView addPortfolio(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PortfolioRequest req) {
        return service.addPortfolioItem(me, req);
    }

    @DeleteMapping("/api/profiles/me/portfolio/{id}")
    public ProfileView removePortfolio(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.removePortfolioItem(me, id);
    }
}
