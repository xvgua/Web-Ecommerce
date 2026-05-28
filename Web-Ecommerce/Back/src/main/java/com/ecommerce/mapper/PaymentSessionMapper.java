package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.PaymentSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentSessionMapper extends BaseMapper<PaymentSession> {
}
