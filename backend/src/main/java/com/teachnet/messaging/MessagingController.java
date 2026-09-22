package com.teachnet.messaging;

import com.teachnet.common.PageResponse;
import com.teachnet.messaging.MessagingService.ConversationDto;
import com.teachnet.messaging.MessagingService.MessageDto;
import com.teachnet.messaging.MessagingService.SendMessageRequest;
import com.teachnet.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class MessagingController {

    private final MessagingService service;

    public MessagingController(MessagingService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConversationDto> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AuthUser me) {
        return Map.of("count", service.unreadCount(me));
    }

    @PostMapping("/with/{userId}")
    public ConversationDto openWith(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId) {
        return service.openWith(me, userId);
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageDto> messages(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "30") int size) {
        return service.messages(me, id, page, size);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto send(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                           @Valid @RequestBody SendMessageRequest req) {
        return service.send(me, id, req);
    }
}
