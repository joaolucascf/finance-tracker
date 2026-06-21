package com.joaolucas.finance_tracker.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.joaolucas.finance_tracker.entity.Friendship;
import com.joaolucas.finance_tracker.entity.FriendshipStatus;


public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query ("""
                SELECT f FROM Friendship f
                WHERE (f.requester.id = :userId OR f.addressee.id = :userId)
                AND f.status = :status
            """)
    List<Friendship> findAllByUserAndStatus(Long userId, FriendshipStatus status);

    @Query ("""
                SELECT f FROM Friendship f
                WHERE f.addressee.id = :addresseeId
                AND f.status = :status
            """)
    List<Friendship> findAllByAddresseeAndStatus(Long addresseeId, FriendshipStatus status);

    @Query ("""
                SELECT f FROM Friendship f
                WHERE (f.requester.id = :userOne AND f.addressee.id = :userTwo)
                OR (f.requester.id = :userTwo AND f.addressee.id = :userOne)
            """)
    Optional<Friendship> findBetween(Long userOne, Long userTwo);
}
