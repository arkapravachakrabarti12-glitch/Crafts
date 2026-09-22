package com.teachnet.institution;

import com.teachnet.common.PageResponse;
import com.teachnet.institution.InstitutionDtos.InstitutionCard;
import com.teachnet.institution.InstitutionDtos.InstitutionView;
import com.teachnet.institution.InstitutionDtos.UpdateInstitutionRequest;
import com.teachnet.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutions")
public class InstitutionController {

    private final InstitutionService service;

    public InstitutionController(InstitutionService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InstitutionCard> search(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) String city,
                                                @RequestParam(required = false) InstitutionType type,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.search(q, city, type, page, size);
    }

    @GetMapping("/me")
    public InstitutionView mine(@AuthenticationPrincipal AuthUser me) {
        return service.mine(me);
    }

    @PutMapping("/me")
    public InstitutionView updateMine(@AuthenticationPrincipal AuthUser me,
                                      @Valid @RequestBody UpdateInstitutionRequest req) {
        return service.updateMine(me, req);
    }

    @GetMapping("/{id}")
    public InstitutionView get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.get(id, me);
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.follow(me, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.unfollow(me, id);
        return ResponseEntity.noContent().build();
    }
}
