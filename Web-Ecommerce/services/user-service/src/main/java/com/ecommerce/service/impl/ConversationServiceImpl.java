package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.entity.ChatMessage;
import com.ecommerce.entity.Conversation;
import com.ecommerce.entity.QuickReply;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.ChatMessageMapper;
import com.ecommerce.mapper.ConversationMapper;
import com.ecommerce.mapper.QuickReplyMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.ConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationMapper conversationMapper;
    @Autowired
    private ChatMessageMapper chatMessageMapper;
    @Autowired
    private QuickReplyMapper quickReplyMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Conversation createConversation(Long userId, Integer sourceType, Long sourceId,
                                            String sourceName, String firstMessage) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setUsername(user.getUsername());
        conv.setAvatar(user.getAvatar() != null ? user.getAvatar() : "");
        conv.setSubject(sourceName != null ? sourceName : "在线咨询");
        conv.setSourceType(sourceType != null ? sourceType : 3);
        conv.setSourceId(sourceId);
        conv.setSourceName(sourceName != null ? sourceName : "");
        conv.setStatus(1);
        conv.setLastMessage(firstMessage != null ? firstMessage : "");
        conv.setLastActive(LocalDateTime.now());
        conv.setUserUnread(0);
        conv.setUnreadCount(1);
        conversationMapper.insert(conv);

        if (firstMessage != null && !firstMessage.isBlank()) {
            ChatMessage msg = new ChatMessage();
            msg.setConversationId(conv.getId());
            msg.setSenderType(1);
            msg.setSenderId(userId);
            msg.setSenderName("");
            msg.setSenderAvatar("");
            msg.setContent(firstMessage.trim());
            msg.setContentType(1);
            msg.setIsRead(0);
            chatMessageMapper.insert(msg);
        }

        log.info("Conversation created: id={}, userId={}, sourceType={}", conv.getId(), userId, sourceType);
        return conv;
    }

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getLastActive);
        return conversationMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public List<ChatMessage> getMessages(Long conversationId, Long userId, boolean isAdmin) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && !conv.getUserId().equals(userId)) {
            throw new BusinessException("无权访问该会话");
        }

        // Mark opposite side's messages as read
        int targetSenderType = isAdmin ? 1 : 2;
        LambdaQueryWrapper<ChatMessage> unreadWrapper = new LambdaQueryWrapper<>();
        unreadWrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getSenderType, targetSenderType)
                .eq(ChatMessage::getIsRead, 0);
        List<ChatMessage> unreadMessages = chatMessageMapper.selectList(unreadWrapper);
        for (ChatMessage msg : unreadMessages) {
            msg.setIsRead(1);
            chatMessageMapper.updateById(msg);
        }

        // Reset unread counters
        if (isAdmin) {
            conv.setUnreadCount(0);
        } else {
            conv.setUserUnread(0);
        }
        conv.setLastActive(LocalDateTime.now());
        conversationMapper.updateById(conv);

        // Return all messages for this conversation
        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(msgWrapper);
    }

    @Override
    @Transactional
    public ChatMessage sendMessage(Long conversationId, Long senderId, Integer senderType,
                                    String senderName, String senderAvatar, String content,
                                    Integer contentType, String extraData) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("消息内容不能为空");
        }

        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        if (conv.getStatus() == 2) {
            throw new BusinessException("会话已关闭");
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(senderType);
        msg.setSenderId(senderId);
        msg.setSenderName(senderName != null ? senderName : "");
        msg.setSenderAvatar(senderAvatar != null ? senderAvatar : "");
        msg.setContent(content.trim());
        msg.setContentType(contentType != null ? contentType : 1);
        msg.setExtraData(extraData);
        msg.setIsRead(0);
        chatMessageMapper.insert(msg);

        // For product card, show friendly last message
        String lastMsg;
        if (contentType != null && contentType == 2 && extraData != null) {
            lastMsg = "[商品卡片] " + content.trim();
        } else {
            lastMsg = content.trim();
        }
        conv.setLastMessage(lastMsg.length() > 50 ? lastMsg.substring(0, 50) + "..." : lastMsg);
        conv.setLastActive(LocalDateTime.now());
        if (senderType == 1) {
            conv.setUnreadCount(conv.getUnreadCount() != null ? conv.getUnreadCount() + 1 : 1);
        } else {
            conv.setUserUnread(conv.getUserUnread() != null ? conv.getUserUnread() + 1 : 1);
        }
        conversationMapper.updateById(conv);

        return msg;
    }

    @Override
    public void closeConversation(Long conversationId, Long operatorId, boolean isAdmin) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && !conv.getUserId().equals(operatorId)) {
            throw new BusinessException("无权操作该会话");
        }
        conv.setStatus(2);
        conv.setCloseTime(LocalDateTime.now());
        conversationMapper.updateById(conv);
        log.info("Conversation closed: id={}", conversationId);
    }

    @Override
    public PageResult<Conversation> adminGetConversations(int page, int pageSize, Integer status, String keyword) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Conversation::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Conversation::getUsername, keyword)
                    .or().like(Conversation::getSourceName, keyword)
                    .or().like(Conversation::getLastMessage, keyword));
        }
        wrapper.orderByDesc(Conversation::getUnreadCount)
                .orderByDesc(Conversation::getLastActive);

        Page<Conversation> result = conversationMapper.selectPage(new Page<>(page, pageSize), wrapper);

        for (Conversation conv : result.getRecords()) {
            Long count = chatMessageMapper.selectCount(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, conv.getId()));
            conv.setTotalMessages(count.intValue());
        }

        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public boolean isCustomerServiceOnline() {
        LocalDateTime lastAdminMsg = conversationMapper.selectLastAdminMessageTime();
        if (lastAdminMsg == null) return false;
        return Duration.between(lastAdminMsg, LocalDateTime.now()).toMinutes() < 5;
    }

    @Override
    public List<QuickReply> getActiveQuickReplies() {
        LambdaQueryWrapper<QuickReply> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuickReply::getStatus, 1)
                .orderByAsc(QuickReply::getSortOrder);
        return quickReplyMapper.selectList(wrapper);
    }

    @Override
    public QuickReply createQuickReply(QuickReply reply) {
        quickReplyMapper.insert(reply);
        return reply;
    }

    @Override
    public void updateQuickReply(Long id, QuickReply reply) {
        QuickReply existing = quickReplyMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("快捷回复不存在");
        }
        existing.setTitle(reply.getTitle());
        existing.setContent(reply.getContent());
        existing.setSortOrder(reply.getSortOrder());
        quickReplyMapper.updateById(existing);
    }

    @Override
    public void deleteQuickReply(Long id) {
        quickReplyMapper.deleteById(id);
    }

    @Override
    public void toggleQuickReplyStatus(Long id, Integer status) {
        QuickReply reply = quickReplyMapper.selectById(id);
        if (reply == null) {
            throw new BusinessException("快捷回复不存在");
        }
        reply.setStatus(status);
        quickReplyMapper.updateById(reply);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoCloseIdleConversations() {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getStatus, 1)
                .lt(Conversation::getLastActive, threeHoursAgo);
        List<Conversation> idleList = conversationMapper.selectList(wrapper);
        for (Conversation conv : idleList) {
            conv.setStatus(2);
            conv.setCloseTime(LocalDateTime.now());
            conversationMapper.updateById(conv);
        }
        if (!idleList.isEmpty()) {
            log.info("Auto-closed {} idle conversations", idleList.size());
        }
    }
}
