package com.teachnet.network;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndInstitutionId(Long followerId, Long institutionId);

    boolean existsByFollowerIdAndInstitutionId(Long followerId, Long institutionId);

    long countByInstitutionId(Long institutionId);

    /** User ids of the owners of every institution this user follows (used to build the feed). */
    @Query("select f.institution.owner.id from Follow f where f.follower.id = :userId")
    List<Long> findFollowedOwnerIds(@Param("userId") Long userId);

    @Query("select f.institution.id from Follow f where f.follower.id = :userId")
    List<Long> findFollowedInstitutionIds(@Param("userId") Long userId);
}
