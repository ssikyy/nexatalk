package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 私信会话数据访问层
 *
 * 说明：未读计数和 last_message 通过 updateById 统一维护，
 * 此处提供独立的清零方法供读取会话时调用。
 */
@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {

    /** 清零 user1 的未读计数（打开会话时调用） */
    @Update("UPDATE conversation SET user1_unread = 0 WHERE id = #{id}")
    int clearUser1Unread(Long id);

    /** 清零 user2 的未读计数（打开会话时调用） */
    @Update("UPDATE conversation SET user2_unread = 0 WHERE id = #{id}")
    int clearUser2Unread(Long id);

    /** 统计指定用户的未读消息总数（使用SQL聚合，更高效） */
    @Select("SELECT COALESCE(SUM(CASE WHEN user1_id = #{userId} THEN user1_unread ELSE user2_unread END), 0) " +
            "FROM conversation WHERE (user1_id = #{userId} OR user2_id = #{userId})")
    long countTotalUnread(Long userId);
}
