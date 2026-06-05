package com.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.BusinessException;
import com.ecommerce.entity.Address;
import com.ecommerce.mapper.AddressMapper;
import com.ecommerce.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getAddressList(Long userId) {
        return addressMapper.selectList(
                new LambdaQueryWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .orderByDesc(Address::getIsDefault));
    }

    @Override
    public Address create(Long userId, Address address) {
        address.setUserId(userId);
        long count = addressMapper.selectCount(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (count == 0 || (address.getIsDefault() != null && address.getIsDefault() == 1)) {
            clearDefault(userId);
            address.setIsDefault(1);
        }
        addressMapper.insert(address);
        return address;
    }

    @Override
    public void update(Long userId, Long addressId, Address address) {
        Address dbAddr = addressMapper.selectById(addressId);
        if (dbAddr == null || !dbAddr.getUserId().equals(userId)) {
            throw new BusinessException(404, "地址不存在");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(userId);
        }
        address.setId(addressId);
        addressMapper.updateById(address);
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address addr = addressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException(404, "地址不存在");
        }
        addressMapper.deleteById(addressId);
        // If only one address remains, make it the default
        List<Address> remaining = addressMapper.selectList(
                new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));
        if (remaining.size() == 1) {
            Address sole = remaining.get(0);
            sole.setIsDefault(1);
            addressMapper.updateById(sole);
        }
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null,
                new LambdaUpdateWrapper<Address>()
                        .eq(Address::getUserId, userId)
                        .eq(Address::getIsDefault, 1)
                        .set(Address::getIsDefault, 0));
    }
}
