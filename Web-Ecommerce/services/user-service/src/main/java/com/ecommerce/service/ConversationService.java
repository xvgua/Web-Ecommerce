package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.entity.ChatMessage;
import com.ecommerce.entity.Conversation;
import com.ecommerce.entity.QuickReply;

import java.util.List;

public interface ConversationService {
    // User-facing
    Conversation createConversation(Long userId, Integer sourceType, Long sourceId, String sourceName, String firstMessage);
    List<Conversation> getUserConversations(Long userId);
    List<ChatMessage> getMessages(Long conversationId, Long userId, boolean isAdmin);
    ChatMessage sendMessage(Long conversationId, Long senderId, Integer senderType, String senderName, String senderAvatar, String content, Integer contentType, String extraData);
    void closeConversation(Long conversationId, Long operatorId, boolean isAdmin);

    // Admin-facing
    PageResult<Conversation> adminGetConversations(int page, int pageSize, Integer status, String keyword);
    boolean isCustomerServiceOnline();

    // Quick replies
    List<QuickReply> getActiveQuickReplies();
    QuickReply createQuickReply(QuickReply reply);
    void updateQuickReply(Long id, QuickReply reply);
    void deleteQuickReply(Long id);
    void toggleQuickReplyStatus(Long id, Integer status);
}
