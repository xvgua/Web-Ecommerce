package com.ecommerce.controller.admin;

import com.ecommerce.common.PageResult;
import com.ecommerce.common.Result;
import com.ecommerce.entity.ChatMessage;
import com.ecommerce.entity.Conversation;
import com.ecommerce.entity.QuickReply;
import com.ecommerce.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/conversations")
    public Result<PageResult<Conversation>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(conversationService.adminGetConversations(page, pageSize, status, keyword));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<ChatMessage>> messages(@PathVariable Long id) {
        return Result.success(conversationService.getMessages(id, 0L, true));
    }

    @PostMapping("/conversations/{id}/messages")
    public Result<ChatMessage> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        ChatMessage msg = conversationService.sendMessage(id, 0L, 2, "客服小二", "", content);
        return Result.success(msg);
    }

    @PutMapping("/conversations/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        conversationService.closeConversation(id, 0L, true);
        return Result.success();
    }

    @GetMapping("/quick-replies")
    public Result<List<QuickReply>> quickReplies() {
        return Result.success(conversationService.getActiveQuickReplies());
    }

    @PostMapping("/quick-replies")
    public Result<QuickReply> createQuickReply(@RequestBody QuickReply reply) {
        return Result.success(conversationService.createQuickReply(reply));
    }

    @PutMapping("/quick-replies/{id}")
    public Result<Void> updateQuickReply(@PathVariable Long id, @RequestBody QuickReply reply) {
        conversationService.updateQuickReply(id, reply);
        return Result.success();
    }

    @DeleteMapping("/quick-replies/{id}")
    public Result<Void> deleteQuickReply(@PathVariable Long id) {
        conversationService.deleteQuickReply(id);
        return Result.success();
    }

    @PutMapping("/quick-replies/{id}/status")
    public Result<Void> toggleQuickReplyStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        conversationService.toggleQuickReplyStatus(id, body.get("status"));
        return Result.success();
    }
}
