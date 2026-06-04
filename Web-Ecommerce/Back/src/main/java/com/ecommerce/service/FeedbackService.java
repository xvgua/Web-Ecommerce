package com.ecommerce.service;

import com.ecommerce.common.PageResult;
import com.ecommerce.dto.FeedbackQuery;
import com.ecommerce.dto.FeedbackReplyRequest;
import com.ecommerce.dto.FeedbackSubmitRequest;
import com.ecommerce.entity.Feedback;

public interface FeedbackService {
    void submit(Long userId, FeedbackSubmitRequest req);
    PageResult<Feedback> getMyPage(Long userId, int page, int pageSize);
    Feedback getMyDetail(Long userId, Long id);
    PageResult<Feedback> adminGetPage(FeedbackQuery query);
    Feedback adminGetDetail(Long id);
    void reply(Long adminId, Long id, FeedbackReplyRequest req);
    void delete(Long id);
}
