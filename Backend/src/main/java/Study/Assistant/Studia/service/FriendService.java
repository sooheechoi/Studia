package Study.Assistant.Studia.service;

import Study.Assistant.Studia.domain.entity.Friend;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.dto.response.FriendResponse;
import Study.Assistant.Studia.dto.response.UserSearchResponse;
import Study.Assistant.Studia.repository.FriendRepository;
import Study.Assistant.Studia.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FriendService {
    
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    
    // Send friend request
    public void sendFriendRequest(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User targetUser = userRepository.findById(targetUserId)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found"));
        
        // Check if relationship already exists
        friendRepository.findFriendship(user, targetUser).ifPresent(friendship -> {
            throw new IllegalArgumentException("Friend relationship already exists");
        });
        
        Friend friendRequest = new Friend(user, targetUser);
        friendRepository.save(friendRequest);
        
        // Send notification
        notificationService.sendNotification(targetUserId, 
            "Friend Request", 
            user.getName() + " sent you a friend request");
        
        log.info("Friend request sent from user {} to user {}", userId, targetUserId);
    }
    
    // Accept friend request
    public void acceptFriendRequest(Long userId, Long requestId) {
        Friend friendRequest = friendRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        
        if (!friendRequest.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to accept this request");
        }
        
        if (friendRequest.getStatus() != Friend.FriendStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }
        
        friendRequest.accept();
        friendRepository.save(friendRequest);
        
        // Send notification
        notificationService.sendNotification(friendRequest.getUser().getId(), 
            "Friend Request Accepted", 
            friendRequest.getFriend().getName() + " accepted your friend request");
        
        log.info("Friend request {} accepted", requestId);
    }
    
    // Decline friend request
    public void declineFriendRequest(Long userId, Long requestId) {
        Friend friendRequest = friendRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));
        
        if (!friendRequest.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to decline this request");
        }
        
        if (friendRequest.getStatus() != Friend.FriendStatus.PENDING) {
            throw new IllegalArgumentException("Friend request is not pending");
        }
        
        friendRequest.decline();
        friendRepository.save(friendRequest);
        
        log.info("Friend request {} declined", requestId);
    }
    
    // Remove friend
    public void removeFriend(Long userId, Long friendId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User friend = userRepository.findById(friendId)
            .orElseThrow(() -> new IllegalArgumentException("Friend not found"));
        
        Friend friendship = friendRepository.findFriendship(user, friend)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        
        friendRepository.delete(friendship);
        
        log.info("Friendship removed between users {} and {}", userId, friendId);
    }
    
    // Get all friends
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<Friend> friendships = friendRepository.findAcceptedFriends(user);
        List<FriendResponse> friends = new ArrayList<>();
        
        for (Friend friendship : friendships) {
            User friendUser = friendship.getUser().getId().equals(userId) 
                ? friendship.getFriend() 
                : friendship.getUser();
            
            friends.add(FriendResponse.builder()
                .id(friendUser.getId())
                .name(friendUser.getName())
                .email(friendUser.getEmail())
                .university(friendUser.getUniversity())
                .major(friendUser.getMajor())
                .profileImage(friendUser.getProfileImage())
                .statusMessage(friendUser.getStatusMessage())
                .isOnline(friendUser.getIsOnline())
                .friendSince(friendship.getAcceptedAt())
                .build());
        }
        
        return friends;
    }
    
    // Get pending friend requests
    @Transactional(readOnly = true)
    public List<FriendResponse> getPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<Friend> pendingRequests = friendRepository.findByFriendAndStatus(user, Friend.FriendStatus.PENDING);
        
        return pendingRequests.stream()
            .map(request -> FriendResponse.builder()
                .id(request.getUser().getId())
                .name(request.getUser().getName())
                .email(request.getUser().getEmail())
                .university(request.getUser().getUniversity())
                .major(request.getUser().getMajor())
                .profileImage(request.getUser().getProfileImage())
                .requestId(request.getId())
                .requestedAt(request.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }
    
    // Search users
    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(Long userId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<User> users = friendRepository.searchUsers(userId, query);
        User currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return users.stream()
            .map(user -> {
                boolean isFriend = friendRepository.areFriends(currentUser, user);
                Friend pendingRequest = friendRepository.findFriendship(currentUser, user)
                    .filter(f -> f.getStatus() == Friend.FriendStatus.PENDING)
                    .orElse(null);
                
                return UserSearchResponse.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .university(user.getUniversity())
                    .major(user.getMajor())
                    .profileImage(user.getProfileImage())
                    .isFriend(isFriend)
                    .hasPendingRequest(pendingRequest != null)
                    .requestSentByMe(pendingRequest != null && pendingRequest.getUser().getId().equals(userId))
                    .build();
            })
            .collect(Collectors.toList());
    }
    
    // Count pending requests
    @Transactional(readOnly = true)
    public Long countPendingRequests(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        return friendRepository.countPendingRequests(user);
    }
}
