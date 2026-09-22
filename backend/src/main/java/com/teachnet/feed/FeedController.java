package com.teachnet.feed;

import com.teachnet.common.PageResponse;
import com.teachnet.feed.FeedService.CommentDto;
import com.teachnet.feed.FeedService.CreateCommentRequest;
import com.teachnet.feed.FeedService.CreatePostRequest;
import com.teachnet.feed.FeedService.FeedScope;
import com.teachnet.feed.FeedService.PostDto;
import com.teachnet.security.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class FeedController {

    private final FeedService service;

    public FeedController(FeedService service) {
        this.service = service;
    }

    @GetMapping("/feed")
    public PageResponse<PostDto> feed(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(defaultValue = "NETWORK") FeedScope scope,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return service.feed(me, scope, page, size);
    }

    @GetMapping("/user/{userId}")
    public PageResponse<PostDto> byAuthor(@AuthenticationPrincipal AuthUser me, @PathVariable Long userId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return service.byAuthor(me, userId, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody CreatePostRequest req) {
        return service.create(me, req);
    }

    @GetMapping("/{id}")
    public PostDto get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.get(me, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> like(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.like(me, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.unlike(me, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/comments")
    public PageResponse<CommentDto> comments(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return service.comments(me, id, page, size);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto comment(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                              @Valid @RequestBody CreateCommentRequest req) {
        return service.comment(me, id, req);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal AuthUser me, @PathVariable Long postId,
                                              @PathVariable Long commentId) {
        service.deleteComment(me, postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
