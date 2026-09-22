package com.teachnet.network;

import com.teachnet.common.PageResponse;
import com.teachnet.network.NetworkService.ConnectionDto;
import com.teachnet.network.NetworkService.PendingDto;
import com.teachnet.security.AuthUser;
import com.teachnet.user.UserSummary;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/connections")
public class NetworkController {

    private final NetworkService service;

    public NetworkController(NetworkService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ConnectionDto> list(@AuthenticationPrincipal AuthUser me,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.list(me, page, size);
    }

    @GetMapping("/pending")
    public PendingDto pending(@AuthenticationPrincipal AuthUser me) {
        return service.pending(me);
    }

    @GetMapping("/suggestions")
    public List<UserSummary> suggestions(@AuthenticationPrincipal AuthUser me,
                                         @RequestParam(defaultValue = "6") int limit) {
        return service.suggestions(me, limit);
    }

    @PostMapping("/request/{userId}")
    public ConnectionDto request(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId) {
        return service.request(me, userId);
    }

    @PostMapping("/{id}/accept")
    public ConnectionDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.accept(me, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.remove(me, id);
        return ResponseEntity.noContent().build();
    }
}
