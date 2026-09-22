package com.teachnet.network;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.notification.NotificationService;
import com.teachnet.notification.NotificationType;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import com.teachnet.security.AuthUser;
import com.teachnet.user.Role;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import com.teachnet.user.UserSummary;
import com.teachnet.user.UserSummaryService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NetworkService {

    /** A connection as seen by one side: who the other person is and whether it is pending. */
    public record ConnectionDto(Long connectionId, UserSummary user, ConnectionStatus status, Instant createdAt) {}

    public record PendingDto(List<ConnectionDto> incoming, List<ConnectionDto> outgoing) {}

    private final ConnectionRepository connections;
    private final UserRepository users;
    private final TeacherProfileRepository profiles;
    private final UserSummaryService summaries;
    private final NotificationService notifications;

    public NetworkService(ConnectionRepository connections, UserRepository users, TeacherProfileRepository profiles,
                          UserSummaryService summaries, NotificationService notifications) {
        this.connections = connections;
        this.users = users;
        this.profiles = profiles;
        this.summaries = summaries;
        this.notifications = notifications;
    }

    public ConnectionDto request(AuthUser me, Long targetUserId) {
        if (me.id().equals(targetUserId)) {
            throw ApiException.badRequest("You cannot connect with yourself");
        }
        User requester = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        User target = users.findById(targetUserId).orElseThrow(() -> ApiException.notFound("User"));
        if (requester.getRole() != Role.TEACHER || target.getRole() != Role.TEACHER) {
            throw ApiException.badRequest("Connections are between teachers. Follow institutions instead.");
        }

        var existing = connections.findBetween(me.id(), targetUserId);
        if (existing.isPresent()) {
            Connection c = existing.get();
            // They already asked us: treat our request as accepting theirs.
            if (c.getStatus() == ConnectionStatus.PENDING && c.getAddressee().getId().equals(me.id())) {
                return accept(me, c.getId());
            }
            return toDto(c, me.id());
        }

        Connection saved = connections.save(new Connection(requester, target));
        notifications.notify(target, me.id(), NotificationType.CONNECTION_REQUEST,
                requester.getFullName() + " wants to connect with you", "/network");
        return toDto(saved, me.id());
    }

    public ConnectionDto accept(AuthUser me, Long connectionId) {
        Connection c = connections.findById(connectionId).orElseThrow(() -> ApiException.notFound("Connection"));
        if (!c.getAddressee().getId().equals(me.id())) {
            throw ApiException.forbidden("Only the invited person can accept this request");
        }
        if (c.getStatus() != ConnectionStatus.ACCEPTED) {
            c.setStatus(ConnectionStatus.ACCEPTED);
            notifications.notify(c.getRequester(), me.id(), NotificationType.CONNECTION_ACCEPTED,
                    c.getAddressee().getFullName() + " accepted your connection request",
                    "/profile/" + me.id());
        }
        return toDto(c, me.id());
    }

    /** Rejects an incoming request, cancels an outgoing one, or removes an existing connection. */
    public void remove(AuthUser me, Long connectionId) {
        Connection c = connections.findById(connectionId).orElseThrow(() -> ApiException.notFound("Connection"));
        if (!c.involves(me.id())) {
            throw ApiException.forbidden("This is not your connection");
        }
        connections.delete(c);
    }

    @Transactional(readOnly = true)
    public PageResponse<ConnectionDto> list(AuthUser me, int page, int size) {
        var result = connections.findAccepted(me.id(), Paging.newestFirst(page, size));
        return PageResponse.of(result, list -> toDtos(list, me.id()));
    }

    @Transactional(readOnly = true)
    public PendingDto pending(AuthUser me) {
        return new PendingDto(
                toDtos(connections.findIncomingPending(me.id()), me.id()),
                toDtos(connections.findOutgoingPending(me.id()), me.id()));
    }

    @Transactional(readOnly = true)
    public List<UserSummary> suggestions(AuthUser me, int limit) {
        Set<Long> exclude = new HashSet<>(connections.findRelatedUserIds(me.id()));
        exclude.add(me.id());
        String city = profiles.findByUserId(me.id())
                .map(TeacherProfile::getCity)
                .map(c -> c.toLowerCase(Locale.ROOT))
                .orElse("");
        List<User> candidates = profiles.findSuggestions(exclude, city, PageRequest.of(0, Math.min(limit, 20)))
                .stream().map(TeacherProfile::getUser).toList();
        return new ArrayList<>(summaries.summarize(candidates).values());
    }

    private List<ConnectionDto> toDtos(List<Connection> list, Long myId) {
        Map<Long, UserSummary> byId = summaries.summarize(list.stream().map(c -> c.other(myId)).toList());
        return list.stream()
                .map(c -> new ConnectionDto(c.getId(), byId.get(c.other(myId).getId()), c.getStatus(), c.getCreatedAt()))
                .toList();
    }

    private ConnectionDto toDto(Connection c, Long myId) {
        return toDtos(List.of(c), myId).get(0);
    }
}
