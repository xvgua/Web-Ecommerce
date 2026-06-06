package com.ecommerce.config;

import com.ecommerce.common.Result;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public Result<Void> handleFeignException(FeignException e) {
        log.error("Feign call failed: status={}, message={}", e.status(), e.getMessage());
        return Result.error(503, "服务暂时不可用，请稍后重试");
    }
}
