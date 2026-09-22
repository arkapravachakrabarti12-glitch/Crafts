package com.teachnet.feed;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Post> findByAuthorIdIn(Collection<Long> authorIds, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Post> findAllBy(Pageable pageable);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :id")
    void adjustLikes(@Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + :delta where p.id = :id")
    void adjustComments(@Param("id") Long id, @Param("delta") int delta);
}
