package com.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT MAX(c.create_time) FROM chat_message c WHERE c.sender_type = 2")
    LocalDateTime selectLastAdminMessageTime();
}
