package com.teachnet.messaging;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationId(Long conversationId, Pageable pageable);

    @Query("""
            select m.conversation.id, count(m) from Message m
            where m.readAt is null and m.sender.id <> :userId
              and (m.conversation.userA.id = :userId or m.conversation.userB.id = :userId)
            group by m.conversation.id
            """)
    List<Object[]> countUnreadByConversation(@Param("userId") Long userId);

    @Query("""
            select count(m) from Message m
            where m.readAt is null and m.sender.id <> :userId
              and (m.conversation.userA.id = :userId or m.conversation.userB.id = :userId)
            """)
    long countUnread(@Param("userId") Long userId);

    @Modifying
    @Query("""
            update Message m set m.readAt = :now
            where m.conversation.id = :conversationId and m.sender.id <> :userId and m.readAt is null
            """)
    int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId,
                 @Param("now") Instant now);
}
