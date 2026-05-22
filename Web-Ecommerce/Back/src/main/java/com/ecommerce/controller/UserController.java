package com.ecommerce.controller;

import com.ecommerce.common.Result;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.User;
import com.ecommerce.security.UserContext;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AddressService addressService;

    @GetMapping("/info")
    public Result<User> getUserInfo() {
        return Result.success(userService.getUserInfo(UserContext.getUserId()));
    }

    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody User user) {
        return userService.updateUserInfo(UserContext.getUserId(), user);
    }

    @GetMapping("/addresses")
    public Result<List<Address>> getAddressList() {
        return Result.success(addressService.getAddressList(UserContext.getUserId()));
    }

    @PostMapping("/addresses")
    public Result<Address> createAddress(@RequestBody Address address) {
        return Result.success(addressService.create(UserContext.getUserId(), address));
    }

    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Address address) {
        addressService.update(UserContext.getUserId(), id, address);
        return Result.success();
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return Result.success();
    }
}
