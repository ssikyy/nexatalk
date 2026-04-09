package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 私信消息数据访问层
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 将某会话中发给指定用户的所有未读消息标记为已读
     */
    @Update("UPDATE message SET is_read = 1 WHERE conversation_id = #{conversationId} AND receiver_id = #{userId} AND is_read = 0")
    int markConversationRead(Long conversationId, Long userId);
}
