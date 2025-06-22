package Study.Assistant.Studia.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "friends")
@Getter
@Setter
@NoArgsConstructor
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_id", nullable = false)
    private User friend;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendStatus status = FriendStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    public enum FriendStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        BLOCKED
    }

    // Constructor
    public Friend(User user, User friend) {
        this.user = user;
        this.friend = friend;
        this.status = FriendStatus.PENDING;
    }

    // Helper method to accept friend request
    public void accept() {
        this.status = FriendStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    // Helper method to decline friend request
    public void decline() {
        this.status = FriendStatus.DECLINED;
    }

    // Helper method to block user
    public void block() {
        this.status = FriendStatus.BLOCKED;
    }
}
