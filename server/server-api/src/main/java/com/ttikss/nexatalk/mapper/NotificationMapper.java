package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Notification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 通知数据访问层
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 将指定用户的所有未读通知标记为已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllRead(Long userId);

    /**
     * 将指定用户某类型的未读通知标记为已读
     */
    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0 AND type = #{type}")
    int markAllReadByType(@Param("userId") Long userId, @Param("type") Integer type);

    /**
     * 批量插入通知
     */
    int batchInsert(@Param("list") List<Notification> notifications);

    /**
     * 按广播去重分页查询系统通知（每条广播只返回一条代表记录）
     */
    @Select("SELECT n.* FROM notification n INNER JOIN (SELECT COALESCE(broadcast_id, id) AS bid, MIN(id) AS mid FROM notification WHERE type = 10 GROUP BY COALESCE(broadcast_id, id)) t ON n.id = t.mid WHERE n.type = 10 ORDER BY n.is_pinned DESC, n.created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<Notification> selectDistinctSystemNotifications(@Param("offset") long offset, @Param("pageSize") long pageSize);

    /**
     * 统计系统通知广播数（去重）
     */
    @Select("SELECT COUNT(*) FROM (SELECT 1 FROM notification WHERE type = 10 GROUP BY COALESCE(broadcast_id, id)) t")
    long countDistinctSystemNotifications();

    /**
     * 按广播 ID 批量更新内容（同一次发布的多条记录一起更新）
     */
    @Update("UPDATE notification SET title=#{n.title}, content=#{n.content}, content_type=#{n.contentType}, image_url=#{n.imageUrl}, is_pinned=#{n.isPinned}, is_bold=#{n.isBold} WHERE broadcast_id=#{broadcastId}")
    int updateByBroadcastId(@Param("broadcastId") String broadcastId, @Param("n") Notification n);

    /**
     * 按广播 ID 批量删除（同一次发布的多条记录一起删除）
     */
    @Delete("DELETE FROM notification WHERE broadcast_id=#{broadcastId}")
    int deleteByBroadcastId(@Param("broadcastId") String broadcastId);
}
