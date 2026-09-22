package com.teachnet.notification;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.user.User;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

    public record NotificationDto(Long id, NotificationType type, String message, String link, boolean read,
                                  Instant createdAt) {}

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** Stores a notification for {@code recipient}; silently ignored when the actor notifies themselves. */
    public void notify(User recipient, Long actorId, NotificationType type, String message, String link) {
        if (recipient.getId().equals(actorId)) {
            return;
        }
        String trimmed = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        repository.save(new Notification(recipient, type, trimmed, link));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> list(Long userId, int page, int size) {
        return PageResponse.of(repository.findByUserId(userId, Paging.newestFirst(page, size)),
                items -> items.stream().map(NotificationService::toDto).toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    public void markRead(Long userId, Long notificationId) {
        Notification n = repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.setRead(true);
    }

    public void markAllRead(Long userId) {
        repository.markAllRead(userId);
    }

    private static NotificationDto toDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
