package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 帖子数据访问层
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 浏览量 +1（使用数据库原子操作，防并发覆盖）
     */
    @Update("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    /**
     * 点赞数 +1 / -1（used_delta = 1 或 -1）
     */
    @Update("UPDATE post SET like_count = GREATEST(0, like_count + #{delta}) WHERE id = #{id}")
    int updateLikeCount(Long id, int delta);

    /**
     * 评论数 +1 / -1
     */
    @Update("UPDATE post SET comment_count = GREATEST(0, comment_count + #{delta}) WHERE id = #{id}")
    int updateCommentCount(Long id, int delta);

    /**
     * 收藏数 +1 / -1
     */
    @Update("UPDATE post SET favorite_count = GREATEST(0, favorite_count + #{delta}) WHERE id = #{id}")
    int updateFavoriteCount(Long id, int delta);

    /**
     * 分享数 +1
     */
    @Update("UPDATE post SET share_count = share_count + 1 WHERE id = #{id}")
    int incrementShareCount(Long id);
}
