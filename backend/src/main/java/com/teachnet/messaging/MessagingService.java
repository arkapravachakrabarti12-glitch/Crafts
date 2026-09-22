package com.teachnet.messaging;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.security.AuthUser;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import com.teachnet.user.UserSummary;
import com.teachnet.user.UserSummaryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MessagingService {

    public record ConversationDto(Long id, UserSummary otherUser, String lastMessage, long unreadCount,
                                  Instant updatedAt) {}

    public record MessageDto(Long id, Long conversationId, Long senderId, String body, Instant createdAt,
                             Instant readAt, boolean mine) {}

    public record SendMessageRequest(@NotBlank @Size(max = 4000) String body) {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final UserRepository users;
    private final UserSummaryService summaries;

    public MessagingService(ConversationRepository conversations, MessageRepository messages, UserRepository users,
                            UserSummaryService summaries) {
        this.conversations = conversations;
        this.messages = messages;
        this.users = users;
        this.summaries = summaries;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> list(AuthUser me) {
        List<Conversation> list = conversations.findForUser(me.id());
        Map<Long, Long> unread = messages.countUnreadByConversation(me.id()).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        Map<Long, UserSummary> others = summaries.summarize(list.stream().map(c -> c.other(me.id())).toList());
        return list.stream()
                .map(c -> new ConversationDto(c.getId(), others.get(c.other(me.id()).getId()), c.getLastMessage(),
                        unread.getOrDefault(c.getId(), 0L), c.getUpdatedAt()))
                .toList();
    }

    public ConversationDto openWith(AuthUser me, Long otherUserId) {
        if (me.id().equals(otherUserId)) {
            throw ApiException.badRequest("You cannot message yourself");
        }
        User meUser = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        User other = users.findById(otherUserId).orElseThrow(() -> ApiException.notFound("User"));
        long a = Math.min(me.id(), otherUserId);
        long b = Math.max(me.id(), otherUserId);
        Conversation c = conversations.findByUserAIdAndUserBId(a, b)
                .orElseGet(() -> conversations.save(new Conversation(meUser, other)));
        return new ConversationDto(c.getId(), summaries.summarize(other), c.getLastMessage(), 0, c.getUpdatedAt());
    }

    /** Newest first; the client reverses for display. Opening a page marks incoming messages as read. */
    public PageResponse<MessageDto> messages(AuthUser me, Long conversationId, int page, int size) {
        Conversation c = find(me, conversationId);
        messages.markRead(c.getId(), me.id(), Instant.now());
        var result = messages.findByConversationId(c.getId(),
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.of(result, list -> list.stream().map(m -> toDto(m, me.id())).toList());
    }

    public MessageDto send(AuthUser me, Long conversationId, SendMessageRequest req) {
        Conversation c = find(me, conversationId);
        User sender = users.getReferenceById(me.id());
        String body = req.body().trim();
        Message saved = messages.save(new Message(c, sender, body));
        c.setLastMessage(body.length() > 300 ? body.substring(0, 297) + "..." : body);
        c.setUpdatedAt(saved.getCreatedAt());
        return toDto(saved, me.id());
    }

    @Transactional(readOnly = true)
    public long unreadCount(AuthUser me) {
        return messages.countUnread(me.id());
    }

    private Conversation find(AuthUser me, Long id) {
        Conversation c = conversations.findById(id).orElseThrow(() -> ApiException.notFound("Conversation"));
        if (!c.involves(me.id())) {
            throw ApiException.notFound("Conversation");
        }
        return c;
    }

    private static MessageDto toDto(Message m, Long myId) {
        Long senderId = m.getSender().getId();
        return new MessageDto(m.getId(), m.getConversation().getId(), senderId, m.getBody(), m.getCreatedAt(),
                m.getReadAt(), senderId.equals(myId));
    }
}
