package com.teachnet.messaging;

import com.teachnet.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A one-to-one conversation. {@code userA} always has the smaller id so each pair has exactly one row. */
@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id", nullable = false)
    private User userA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id", nullable = false)
    private User userB;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Conversation(User first, User second) {
        boolean firstIsSmaller = first.getId() < second.getId();
        this.userA = firstIsSmaller ? first : second;
        this.userB = firstIsSmaller ? second : first;
    }

    public boolean involves(Long userId) {
        return userA.getId().equals(userId) || userB.getId().equals(userId);
    }

    public User other(Long userId) {
        return userA.getId().equals(userId) ? userB : userA;
    }
}
