package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 评论数据访问层
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 评论点赞数 +1 / -1
     */
    @Update("UPDATE comment SET like_count = GREATEST(0, like_count + #{delta}) WHERE id = #{id}")
    int updateLikeCount(Long id, int delta);

    /**
     * 一级评论的回复数 +1 / -1
     */
    @Update("UPDATE comment SET reply_count = GREATEST(0, reply_count + #{delta}) WHERE id = #{id}")
    int updateReplyCount(Long id, int delta);
}
