package com.teachnet.institution;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.common.Strings;
import com.teachnet.institution.InstitutionDtos.InstitutionCard;
import com.teachnet.institution.InstitutionDtos.InstitutionView;
import com.teachnet.institution.InstitutionDtos.UpdateInstitutionRequest;
import com.teachnet.jobs.JobOpeningRepository;
import com.teachnet.jobs.JobStatus;
import com.teachnet.network.Follow;
import com.teachnet.network.FollowRepository;
import com.teachnet.notification.NotificationService;
import com.teachnet.notification.NotificationType;
import com.teachnet.security.AuthUser;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InstitutionService {

    private final InstitutionRepository institutions;
    private final FollowRepository follows;
    private final JobOpeningRepository jobs;
    private final UserRepository users;
    private final NotificationService notifications;

    public InstitutionService(InstitutionRepository institutions, FollowRepository follows, JobOpeningRepository jobs,
                              UserRepository users, NotificationService notifications) {
        this.institutions = institutions;
        this.follows = follows;
        this.jobs = jobs;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public InstitutionView get(Long id, AuthUser viewer) {
        return toView(find(id), viewer);
    }

    @Transactional(readOnly = true)
    public InstitutionView mine(AuthUser me) {
        return toView(findOwnedBy(me), me);
    }

    @Transactional(readOnly = true)
    public PageResponse<InstitutionCard> search(String q, String city, InstitutionType type, int page, int size) {
        String qLike = like(q);
        String cityLower = lower(city);
        return PageResponse.of(
                institutions.search(qLike, cityLower, type, Paging.of(page, size, Sort.by("name"))),
                list -> list.stream().map(InstitutionService::toCard).toList());
    }

    public InstitutionView updateMine(AuthUser me, UpdateInstitutionRequest req) {
        Institution inst = findOwnedBy(me);
        inst.setName(req.name().trim());
        inst.setInstitutionType(req.institutionType());
        inst.setBoard(req.board());
        inst.setCity(Strings.blankToNull(req.city()));
        inst.setState(Strings.blankToNull(req.state()));
        inst.setAbout(Strings.blankToNull(req.about()));
        inst.setWebsite(Strings.blankToNull(req.website()));
        inst.setLogoUrl(Strings.blankToNull(req.logoUrl()));
        // Keep the owner's display name in sync so posts show the institution name.
        inst.getOwner().setFullName(inst.getName());
        return toView(inst, me);
    }

    public void follow(AuthUser me, Long institutionId) {
        Institution inst = find(institutionId);
        if (inst.getOwner().getId().equals(me.id())) {
            throw ApiException.badRequest("You cannot follow your own institution");
        }
        if (follows.existsByFollowerIdAndInstitutionId(me.id(), institutionId)) {
            return;
        }
        User follower = users.getReferenceById(me.id());
        follows.save(new Follow(follower, inst));
        notifications.notify(inst.getOwner(), me.id(), NotificationType.NEW_FOLLOWER,
                users.findById(me.id()).map(User::getFullName).orElse("Someone") + " started following "
                        + inst.getName(),
                "/profile/" + me.id());
    }

    public void unfollow(AuthUser me, Long institutionId) {
        follows.findByFollowerIdAndInstitutionId(me.id(), institutionId).ifPresent(follows::delete);
    }

    public Institution findOwnedBy(AuthUser me) {
        return institutions.findByOwnerId(me.id())
                .orElseThrow(() -> ApiException.forbidden("Only institution accounts can do this"));
    }

    private Institution find(Long id) {
        return institutions.findById(id).orElseThrow(() -> ApiException.notFound("Institution"));
    }

    private InstitutionView toView(Institution i, AuthUser viewer) {
        boolean owner = viewer != null && i.getOwner().getId().equals(viewer.id());
        boolean following = viewer != null && !owner
                && follows.existsByFollowerIdAndInstitutionId(viewer.id(), i.getId());
        return new InstitutionView(i.getId(), i.getOwner().getId(), i.getName(), i.getInstitutionType(), i.getBoard(),
                i.getCity(), i.getState(), i.getAbout(), i.getWebsite(), i.getLogoUrl(), i.isVerified(),
                follows.countByInstitutionId(i.getId()),
                jobs.countByInstitutionIdAndStatus(i.getId(), JobStatus.OPEN),
                following, owner);
    }

    static InstitutionCard toCard(Institution i) {
        return new InstitutionCard(i.getId(), i.getName(), i.getInstitutionType(), i.getCity(), i.getLogoUrl(),
                i.isVerified());
    }

    private static String lower(String value) {
        String v = Strings.blankToNull(value);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    private static String like(String value) {
        String v = lower(value);
        return v == null ? null : "%" + v + "%";
    }
}
