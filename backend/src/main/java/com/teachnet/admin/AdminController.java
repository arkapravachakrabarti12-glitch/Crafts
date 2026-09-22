package com.teachnet.admin;

import com.teachnet.admin.AdminService.AdminInstitution;
import com.teachnet.admin.AdminService.Stats;
import com.teachnet.profile.ProfileDtos.TeacherCard;
import com.teachnet.security.AuthUser;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Access is restricted to ADMIN users in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public Stats stats() {
        return service.stats();
    }

    @GetMapping("/teachers")
    public List<TeacherCard> teachers() {
        return service.teachers();
    }

    @GetMapping("/institutions")
    public List<AdminInstitution> institutions() {
        return service.institutions();
    }

    @PostMapping("/teachers/{userId}/verify")
    public ResponseEntity<Void> verifyTeacher(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId,
                                              @RequestParam(defaultValue = "true") boolean verified) {
        service.verifyTeacher(me, userId, verified);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/institutions/{id}/verify")
    public ResponseEntity<Void> verifyInstitution(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                  @RequestParam(defaultValue = "true") boolean verified) {
        service.verifyInstitution(me, id, verified);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users/{userId}/enabled")
    public ResponseEntity<Void> setEnabled(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId,
                                           @RequestParam boolean enabled) {
        service.setEnabled(me, userId, enabled);
        return ResponseEntity.noContent().build();
    }
}
