package com.teachnet.network;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    @Query("""
            select c from Connection c
            where (c.requester.id = :a and c.addressee.id = :b)
               or (c.requester.id = :b and c.addressee.id = :a)
            """)
    Optional<Connection> findBetween(@Param("a") Long a, @Param("b") Long b);

    @Query("""
            select count(c) from Connection c
            where c.status = com.teachnet.network.ConnectionStatus.ACCEPTED
              and (c.requester.id = :userId or c.addressee.id = :userId)
            """)
    long countAccepted(@Param("userId") Long userId);

    @Query(value = """
            select c from Connection c join fetch c.requester join fetch c.addressee
            where c.status = com.teachnet.network.ConnectionStatus.ACCEPTED
              and (c.requester.id = :userId or c.addressee.id = :userId)
            """,
            countQuery = """
            select count(c) from Connection c
            where c.status = com.teachnet.network.ConnectionStatus.ACCEPTED
              and (c.requester.id = :userId or c.addressee.id = :userId)
            """)
    Page<Connection> findAccepted(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select c from Connection c join fetch c.requester
            where c.status = com.teachnet.network.ConnectionStatus.PENDING and c.addressee.id = :userId
            order by c.createdAt desc
            """)
    List<Connection> findIncomingPending(@Param("userId") Long userId);

    @Query("""
            select c from Connection c join fetch c.addressee
            where c.status = com.teachnet.network.ConnectionStatus.PENDING and c.requester.id = :userId
            order by c.createdAt desc
            """)
    List<Connection> findOutgoingPending(@Param("userId") Long userId);

    /** Ids of everyone this user has an accepted connection with. */
    @Query("""
            select case when c.requester.id = :userId then c.addressee.id else c.requester.id end
            from Connection c
            where c.status = com.teachnet.network.ConnectionStatus.ACCEPTED
              and (c.requester.id = :userId or c.addressee.id = :userId)
            """)
    List<Long> findConnectedUserIds(@Param("userId") Long userId);

    /** Ids of everyone this user is connected to or has a pending request with (either direction). */
    @Query("""
            select case when c.requester.id = :userId then c.addressee.id else c.requester.id end
            from Connection c
            where c.requester.id = :userId or c.addressee.id = :userId
            """)
    List<Long> findRelatedUserIds(@Param("userId") Long userId);
}
