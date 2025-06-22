package Study.Assistant.Studia.repository;

import Study.Assistant.Studia.domain.entity.Friend;
import Study.Assistant.Studia.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {
    
    // Find a specific friend relationship between two users
    @Query("SELECT f FROM Friend f WHERE (f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)")
    Optional<Friend> findFriendship(@Param("user1") User user1, @Param("user2") User user2);
    
    // Find all accepted friends of a user
    @Query("SELECT f FROM Friend f WHERE (f.user = :user AND f.status = 'ACCEPTED') OR (f.friend = :user AND f.status = 'ACCEPTED')")
    List<Friend> findAcceptedFriends(@Param("user") User user);
    
    // Find pending friend requests sent by a user
    List<Friend> findByUserAndStatus(User user, Friend.FriendStatus status);
    
    // Find pending friend requests received by a user
    List<Friend> findByFriendAndStatus(User friend, Friend.FriendStatus status);
    
    // Count pending friend requests for a user
    @Query("SELECT COUNT(f) FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    Long countPendingRequests(@Param("user") User user);
    
    // Check if two users are friends
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE ((f.user = :user1 AND f.friend = :user2) OR (f.user = :user2 AND f.friend = :user1)) AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") User user1, @Param("user2") User user2);
    
    // Search users by name or email (excluding current user and existing friends)
    @Query("SELECT u FROM User u WHERE u.id != :userId AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<User> searchUsers(@Param("userId") Long userId, @Param("query") String query);
}
