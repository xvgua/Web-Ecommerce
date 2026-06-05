package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.BusinessException;
import com.ecommerce.common.PageResult;
import com.ecommerce.dto.FeedbackQuery;
import com.ecommerce.dto.FeedbackReplyRequest;
import com.ecommerce.dto.FeedbackSubmitRequest;
import com.ecommerce.entity.Feedback;
import com.ecommerce.entity.FeedbackStatus;
import com.ecommerce.entity.FeedbackType;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.FeedbackMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FeedbackServiceImpl implements FeedbackService {

    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public void submit(Long userId, FeedbackSubmitRequest req) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(req.getType());
        feedback.setTitle(req.getTitle());
        feedback.setContent(req.getContent());
        feedback.setContact(req.getContact());
        feedback.setStatus(FeedbackStatus.PENDING);
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            feedback.setImages(String.join(",", req.getImages()));
        }
        feedbackMapper.insert(feedback);
        log.info("Feedback submitted: userId={}, type={}, title={}", userId, req.getType(), req.getTitle());
    }

    @Override
    public PageResult<Feedback> getMyPage(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Feedback::getUserId, userId)
               .orderByDesc(Feedback::getCreateTime);

        Page<Feedback> mpPage = new Page<>(page, pageSize);
        Page<Feedback> result = feedbackMapper.selectPage(mpPage, wrapper);

        fillTextFields(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Override
    public Feedback getMyDetail(Long userId, Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null || !feedback.getUserId().equals(userId)) {
            throw new BusinessException("反馈不存在");
        }
        fillTextFields(List.of(feedback));
        return feedback;
    }

    @Override
    public PageResult<Feedback> adminGetPage(FeedbackQuery query) {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (query.getType() != null) {
            wrapper.eq(Feedback::getType, query.getType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Feedback::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.like(Feedback::getTitle, query.getKeyword());
        }
        wrapper.orderByAsc(Feedback::getStatus)
               .orderByDesc(Feedback::getCreateTime);

        Page<Feedback> mpPage = new Page<>(query.getPage(), query.getPageSize());
        Page<Feedback> result = feedbackMapper.selectPage(mpPage, wrapper);

        fillTextFields(result.getRecords());
        fillUserInfo(result.getRecords());
        return PageResult.of(result.getRecords(), result.getTotal(), query.getPage(), query.getPageSize());
    }

    @Override
    public Feedback adminGetDetail(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        fillTextFields(List.of(feedback));
        fillUserInfo(List.of(feedback));
        return feedback;
    }

    @Override
    public void reply(Long adminId, Long id, FeedbackReplyRequest req) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        feedback.setStatus(req.getStatus());
        feedback.setAdminReply(req.getAdminReply());
        feedback.setAdminId(adminId);
        feedback.setHandleTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
        log.info("Feedback replied: id={}, adminId={}, status={}", id, adminId, req.getStatus());
    }

    @Override
    public void delete(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException("反馈不存在");
        }
        feedbackMapper.deleteById(id);
        log.info("Feedback deleted: id={}", id);
    }

    private void fillTextFields(List<Feedback> list) {
        for (Feedback f : list) {
            f.setTypeText(FeedbackType.getText(f.getType()));
            f.setStatusText(FeedbackStatus.getText(f.getStatus()));
        }
    }

    private void fillUserInfo(List<Feedback> list) {
        List<Long> userIds = list.stream()
                .map(Feedback::getUserId)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) return;

        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        for (Feedback f : list) {
            User u = userMap.get(f.getUserId());
            if (u != null) {
                f.setUsername(u.getUsername());
                f.setUserEmail(u.getEmail());
            }
        }
    }
}
