package com.ecommerce.controller.internal;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.AddressMapper;
import com.ecommerce.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
public class InternalUserController {

    @Autowired
    private AddressMapper addressMapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping("/addresses/{id}")
    public Result<Address> getAddressById(@PathVariable Long id) {
        Address address = addressMapper.selectById(id);
        if (address == null) {
            return Result.error(404, "地址不存在");
        }
        return Result.success(address);
    }

    @GetMapping("/users/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @GetMapping("/addresses/batch")
    public Result<List<Address>> batchGetAddresses(@RequestParam("ids") List<Long> ids) {
        List<Address> addresses = addressMapper.selectBatchIds(ids);
        return Result.success(addresses);
    }

    @GetMapping("/users/all")
    public Result<List<User>> getAllUsers() {
        List<User> users = userMapper.selectList(null);
        for (User u : users) u.setPassword(null);
        return Result.success(users);
    }
}
