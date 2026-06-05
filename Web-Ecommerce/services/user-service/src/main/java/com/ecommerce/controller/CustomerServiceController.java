package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        boolean online = conversationService.isCustomerServiceOnline();
        return Result.success(Map.of("online", online));
    }
}
