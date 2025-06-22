package Study.Assistant.Studia.controller;

import Study.Assistant.Studia.dto.GroupMessageRequest;
import Study.Assistant.Studia.dto.GroupMessageResponse;
import Study.Assistant.Studia.domain.entity.GroupMessage;
import Study.Assistant.Studia.domain.entity.User;
import Study.Assistant.Studia.service.StudyGroupService;
import Study.Assistant.Studia.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);
    
    private final StudyGroupService studyGroupService;
    private final AuthenticationService authenticationService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat/{groupId}")
    @SendTo("/topic/group/{groupId}")
    public GroupMessageResponse sendGroupMessage(
            @DestinationVariable Long groupId,
            GroupMessageRequest request,
            Authentication authentication) {
        
        try {
            User sender = authenticationService.getCurrentUser(authentication.getName());
            
            // Save message to database
            GroupMessage message = studyGroupService.sendMessageForWebSocket(
                    groupId, 
                    sender.getId(), 
                    request.getContent()
            );
            
            // Create response
            GroupMessageResponse response = new GroupMessageResponse();
            response.setId(message.getId());
            response.setContent(message.getContent());
            response.setSenderName(sender.getName());
            response.setSenderId(sender.getId());
            response.setTimestamp(message.getSentAt());
            
            return response;
        } catch (Exception e) {
            log.error("Error sending group message", e);
            throw new RuntimeException("Failed to send message");
        }
    }
    
    @MessageMapping("/notification/{userId}")
    public void sendNotification(
            @DestinationVariable Long userId,
            Map<String, Object> notification) {
        
        // Send notification to specific user
        messagingTemplate.convertAndSend(
                "/user/" + userId + "/queue/notifications", 
                notification
        );
    }
    
    @MessageMapping("/friend-request/{userId}")
    public void notifyFriendRequest(
            @DestinationVariable Long userId,
            Authentication authentication) {
        
        try {
            User sender = authenticationService.getCurrentUser(authentication.getName());
            
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "FRIEND_REQUEST");
            notification.put("senderId", sender.getId());
            notification.put("senderName", sender.getName());
            notification.put("timestamp", LocalDateTime.now());
            
            // Send notification to target user
            messagingTemplate.convertAndSend(
                    "/user/" + userId + "/queue/notifications", 
                    notification
            );
        } catch (Exception e) {
            log.error("Error sending friend request notification", e);
        }
    }
    
    @MessageMapping("/typing/{groupId}")
    @SendTo("/topic/group/{groupId}/typing")
    public Map<String, Object> notifyTyping(
            @DestinationVariable Long groupId,
            Map<String, Boolean> typing,
            Authentication authentication) {
        
        try {
            User user = authenticationService.getCurrentUser(authentication.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("userName", user.getName());
            response.put("isTyping", typing.get("isTyping"));
            
            return response;
        } catch (Exception e) {
            log.error("Error sending typing notification", e);
            return new HashMap<>();
        }
    }
}
