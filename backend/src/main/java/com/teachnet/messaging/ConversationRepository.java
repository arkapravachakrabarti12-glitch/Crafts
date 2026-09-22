package com.teachnet.messaging;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUserAIdAndUserBId(Long userAId, Long userBId);

    @Query("""
            select c from Conversation c join fetch c.userA join fetch c.userB
            where c.userA.id = :userId or c.userB.id = :userId
            order by c.updatedAt desc
            """)
    List<Conversation> findForUser(@Param("userId") Long userId);
}
