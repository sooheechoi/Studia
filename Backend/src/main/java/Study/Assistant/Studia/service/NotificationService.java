package Study.Assistant.Studia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send notification to a specific user
     * @param userId The user to send notification to
     * @param title The notification title
     * @param message The notification message
     */
    public void sendNotification(Long userId, String title, String message) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NOTIFICATION");
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("read", false);
        
        try {
            // Send via WebSocket if connected
            messagingTemplate.convertAndSend(
                "/user/" + userId + "/queue/notifications", 
                notification
            );
            
            log.info("Notification sent to user {}: {}", userId, title);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}", userId, e);
            // In a real application, you might want to:
            // 1. Store notification in database for later retrieval
            // 2. Send push notification
            // 3. Send email notification
        }
    }
    
    /**
     * Send friend request notification
     * @param targetUserId The user receiving the friend request
     * @param requesterId The user sending the friend request
     * @param requesterName The name of the requester
     */
    public void sendFriendRequestNotification(Long targetUserId, Long requesterId, String requesterName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "FRIEND_REQUEST");
        notification.put("requesterId", requesterId);
        notification.put("requesterName", requesterName);
        notification.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend(
            "/user/" + targetUserId + "/queue/notifications", 
            notification
        );
        
        log.info("Friend request notification sent from {} to user {}", requesterId, targetUserId);
    }
    
    /**
     * Send group invitation notification
     * @param targetUserId The user being invited
     * @param groupId The group ID
     * @param groupName The group name
     * @param inviterName The name of the person inviting
     */
    public void sendGroupInvitationNotification(Long targetUserId, Long groupId, String groupName, String inviterName) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "GROUP_INVITATION");
        notification.put("groupId", groupId);
        notification.put("groupName", groupName);
        notification.put("inviterName", inviterName);
        notification.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend(
            "/user/" + targetUserId + "/queue/notifications", 
            notification
        );
        
        log.info("Group invitation notification sent to user {} for group {}", targetUserId, groupId);
    }
    
    /**
     * Send message notification
     * @param targetUserId The user to notify
     * @param groupId The group where message was sent
     * @param groupName The group name
     * @param senderName The message sender name
     * @param messagePreview Preview of the message
     */
    public void sendMessageNotification(Long targetUserId, Long groupId, String groupName, 
                                       String senderName, String messagePreview) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "NEW_MESSAGE");
        notification.put("groupId", groupId);
        notification.put("groupName", groupName);
        notification.put("senderName", senderName);
        notification.put("messagePreview", messagePreview);
        notification.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend(
            "/user/" + targetUserId + "/queue/notifications", 
            notification
        );
    }
    
    /**
     * Broadcast notification to multiple users
     * @param userIds List of user IDs to notify
     * @param title Notification title
     * @param message Notification message
     */
    public void broadcastNotification(Iterable<Long> userIds, String title, String message) {
        for (Long userId : userIds) {
            sendNotification(userId, title, message);
        }
    }
}
