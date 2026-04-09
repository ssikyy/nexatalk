package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Blacklist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 黑名单 Mapper
 */
@Mapper
public interface BlacklistMapper extends BaseMapper<Blacklist> {

    /**
     * 检查用户是否在黑名单中
     * @param userId 当前用户ID（被拉黑者）
     * @param blockedUserId 拉黑者用户ID
     * @return 存在返回1，否则返回0
     */
    @Select("SELECT COUNT(*) FROM blacklist WHERE user_id = #{userId} AND blocked_user_id = #{blockedUserId}")
    int isBlocked(Long userId, Long blockedUserId);
}
