package com.ecommerce.service;

import com.ecommerce.entity.Address;
import java.util.List;

public interface AddressService {
    List<Address> getAddressList(Long userId);
    Address create(Long userId, Address address);
    void update(Long userId, Long addressId, Address address);
    void delete(Long userId, Long addressId);
}
