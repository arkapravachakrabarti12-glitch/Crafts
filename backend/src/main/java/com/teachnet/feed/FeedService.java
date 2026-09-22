package com.teachnet.feed;

import com.teachnet.common.ApiException;
import com.teachnet.common.PageResponse;
import com.teachnet.common.Paging;
import com.teachnet.common.Strings;
import com.teachnet.network.ConnectionRepository;
import com.teachnet.network.FollowRepository;
import com.teachnet.notification.NotificationService;
import com.teachnet.notification.NotificationType;
import com.teachnet.security.AuthUser;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import com.teachnet.user.UserSummary;
import com.teachnet.user.UserSummaryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FeedService {

    public enum FeedScope { NETWORK, ALL }

    public record PostDto(Long id, UserSummary author, String content, String imageUrl, int likeCount,
                          int commentCount, boolean likedByMe, boolean canDelete, Instant createdAt) {}

    public record CommentDto(Long id, UserSummary author, String content, boolean canDelete, Instant createdAt) {}

    public record CreatePostRequest(
            @NotBlank @Size(max = 3000) String content,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "must start with http:// or https://")
            String imageUrl) {}

    public record CreateCommentRequest(@NotBlank @Size(max = 1000) String content) {}

    private final PostRepository posts;
    private final PostLikeRepository likes;
    private final CommentRepository comments;
    private final ConnectionRepository connections;
    private final FollowRepository follows;
    private final UserRepository users;
    private final UserSummaryService summaries;
    private final NotificationService notifications;

    public FeedService(PostRepository posts, PostLikeRepository likes, CommentRepository comments,
                       ConnectionRepository connections, FollowRepository follows, UserRepository users,
                       UserSummaryService summaries, NotificationService notifications) {
        this.posts = posts;
        this.likes = likes;
        this.comments = comments;
        this.connections = connections;
        this.follows = follows;
        this.users = users;
        this.summaries = summaries;
        this.notifications = notifications;
    }

    /**
     * NETWORK: posts by me, my connections and institutions I follow.
     * ALL: every post on the platform (useful while your network is small).
     */
    @Transactional(readOnly = true)
    public PageResponse<PostDto> feed(AuthUser me, FeedScope scope, int page, int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<Post> result;
        if (scope == FeedScope.ALL) {
            result = posts.findAllBy(pageable);
        } else {
            Set<Long> authors = new HashSet<>(connections.findConnectedUserIds(me.id()));
            authors.addAll(follows.findFollowedOwnerIds(me.id()));
            authors.add(me.id());
            result = posts.findByAuthorIdIn(authors, pageable);
        }
        return PageResponse.of(result, list -> toDtos(list, me));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostDto> byAuthor(AuthUser me, Long authorId, int page, int size) {
        var result = posts.findByAuthorId(authorId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return PageResponse.of(result, list -> toDtos(list, me));
    }

    public PostDto create(AuthUser me, CreatePostRequest req) {
        User author = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        Post post = posts.save(new Post(author, req.content().trim(), Strings.blankToNull(req.imageUrl())));
        return toDtos(List.of(post), me).get(0);
    }

    public void delete(AuthUser me, Long postId) {
        Post post = find(postId);
        if (!post.getAuthor().getId().equals(me.id()) && !me.isAdmin()) {
            throw ApiException.forbidden("You can only delete your own posts");
        }
        posts.delete(post);
    }

    public void like(AuthUser me, Long postId) {
        Post post = find(postId);
        if (likes.existsByPostIdAndUserId(postId, me.id())) {
            return;
        }
        User user = users.getReferenceById(me.id());
        likes.save(new PostLike(post, user));
        posts.adjustLikes(postId, 1);
        notifications.notify(post.getAuthor(), me.id(), NotificationType.POST_LIKED,
                displayName(me.id()) + " liked your post: \"" + preview(post.getContent()) + "\"", "/posts/" + postId);
    }

    public void unlike(AuthUser me, Long postId) {
        likes.findByPostIdAndUserId(postId, me.id()).ifPresent(like -> {
            likes.delete(like);
            posts.adjustLikes(postId, -1);
        });
    }

    @Transactional(readOnly = true)
    public PostDto get(AuthUser me, Long postId) {
        return toDtos(List.of(find(postId)), me).get(0);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentDto> comments(AuthUser me, Long postId, int page, int size) {
        find(postId);
        var result = comments.findByPostId(postId, Paging.of(page, size, Sort.by("createdAt", "id")));
        return PageResponse.of(result, list -> {
            Map<Long, UserSummary> authors = summaries.summarize(list.stream().map(Comment::getAuthor).toList());
            return list.stream().map(c -> toCommentDto(c, authors.get(c.getAuthor().getId()), me)).toList();
        });
    }

    public CommentDto comment(AuthUser me, Long postId, CreateCommentRequest req) {
        Post post = find(postId);
        User author = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        Comment saved = comments.save(new Comment(post, author, req.content().trim()));
        posts.adjustComments(postId, 1);
        notifications.notify(post.getAuthor(), me.id(), NotificationType.POST_COMMENTED,
                author.getFullName() + " commented on your post: \"" + preview(req.content()) + "\"",
                "/posts/" + postId);
        return toCommentDto(saved, summaries.summarize(author), me);
    }

    public void deleteComment(AuthUser me, Long postId, Long commentId) {
        Comment c = comments.findById(commentId)
                .filter(x -> x.getPost().getId().equals(postId))
                .orElseThrow(() -> ApiException.notFound("Comment"));
        boolean allowed = c.getAuthor().getId().equals(me.id())
                || c.getPost().getAuthor().getId().equals(me.id())
                || me.isAdmin();
        if (!allowed) {
            throw ApiException.forbidden("You cannot delete this comment");
        }
        comments.delete(c);
        posts.adjustComments(postId, -1);
    }

    private Post find(Long id) {
        return posts.findById(id).orElseThrow(() -> ApiException.notFound("Post"));
    }

    private List<PostDto> toDtos(List<Post> list, AuthUser me) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, UserSummary> authors = summaries.summarize(list.stream().map(Post::getAuthor).toList());
        Set<Long> liked = new HashSet<>(likes.findLikedPostIds(me.id(), list.stream().map(Post::getId).toList()));
        return list.stream()
                .map(p -> new PostDto(p.getId(), authors.get(p.getAuthor().getId()), p.getContent(), p.getImageUrl(),
                        p.getLikeCount(), p.getCommentCount(), liked.contains(p.getId()),
                        p.getAuthor().getId().equals(me.id()) || me.isAdmin(), p.getCreatedAt()))
                .toList();
    }

    private CommentDto toCommentDto(Comment c, UserSummary author, AuthUser me) {
        boolean canDelete = c.getAuthor().getId().equals(me.id())
                || c.getPost().getAuthor().getId().equals(me.id())
                || me.isAdmin();
        return new CommentDto(c.getId(), author, c.getContent(), canDelete, c.getCreatedAt());
    }

    private String displayName(Long userId) {
        return users.findById(userId).map(User::getFullName).orElse("Someone");
    }

    private static String preview(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 60 ? oneLine.substring(0, 57) + "..." : oneLine;
    }
}
