package com.ecommerce.feign;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", url = "${feign.user-service.url:http://localhost:8082}",
             fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/api/internal/addresses/{id}")
    Result<Address> getAddressById(@PathVariable("id") Long id);

    @GetMapping("/api/internal/users/{id}")
    Result<User> getUserById(@PathVariable("id") Long id);

    @GetMapping("/api/internal/addresses/batch")
    Result<List<Address>> batchGetAddresses(@RequestParam("ids") List<Long> ids);

    @GetMapping("/api/internal/users/all")
    Result<List<User>> getAllUsers();
}
