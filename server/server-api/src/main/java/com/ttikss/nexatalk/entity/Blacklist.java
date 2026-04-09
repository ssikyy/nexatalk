package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 黑名单实体 - 用户屏蔽的好友/用户
 */
@Data
@TableName("blacklist")
public class Blacklist {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拉黑者用户ID */
    private Long userId;

    /** 被拉黑的用户ID */
    private Long blockedUserId;

    /** 拉黑时间 */
    private LocalDateTime createdAt;
}
