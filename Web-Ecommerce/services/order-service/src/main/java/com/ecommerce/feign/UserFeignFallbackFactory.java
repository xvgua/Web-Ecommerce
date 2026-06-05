package com.ecommerce.feign;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("User service unavailable", cause);
        return new UserFeignClient() {
            @Override
            public Result<Address> getAddressById(Long id) {
                return Result.error(503, "用户服务暂不可用");
            }

            @Override
            public Result<User> getUserById(Long id) {
                return Result.error(503, "用户服务暂不可用");
            }

            @Override
            public Result<List<Address>> batchGetAddresses(List<Long> ids) {
                return Result.error(503, "用户服务暂不可用");
            }

            @Override
            public Result<List<User>> getAllUsers() {
                return Result.error(503, "用户服务暂不可用");
            }
        };
    }
}
