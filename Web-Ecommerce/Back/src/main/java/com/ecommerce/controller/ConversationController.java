package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.ChatMessage;
import com.ecommerce.entity.Conversation;
import com.ecommerce.entity.User;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.ConversationService;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;
    @Autowired
    private UserService userService;

    @PostMapping
    public Result<Conversation> create(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        Integer sourceType = body.get("sourceType") != null ? ((Number) body.get("sourceType")).intValue() : 3;
        Long sourceId = body.get("sourceId") != null ? ((Number) body.get("sourceId")).longValue() : null;
        String sourceName = (String) body.getOrDefault("sourceName", "");
        String firstMessage = (String) body.getOrDefault("firstMessage", "");
        Conversation conv = conversationService.createConversation(userId, sourceType, sourceId, sourceName, firstMessage);
        return Result.success(conv);
    }

    @GetMapping
    public Result<List<Conversation>> list() {
        Long userId = UserContext.getUserId();
        return Result.success(conversationService.getUserConversations(userId));
    }

    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return Result.success(conversationService.getMessages(id, userId, false));
    }

    @PostMapping("/{id}/messages")
    public Result<ChatMessage> sendMessage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = UserContext.getUserId();
        String content = (String) body.get("content");
        Integer contentType = body.get("contentType") != null ? ((Number) body.get("contentType")).intValue() : 1;
        String extraData = (String) body.get("extraData");
        User user = userService.getUserById(userId);
        String senderName = user.getUsername();
        ChatMessage msg = conversationService.sendMessage(id, userId, 1, senderName,
                user.getAvatar() != null ? user.getAvatar() : "", content, contentType, extraData);
        return Result.success(msg);
    }

    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        conversationService.closeConversation(id, userId, false);
        return Result.success();
    }
}
