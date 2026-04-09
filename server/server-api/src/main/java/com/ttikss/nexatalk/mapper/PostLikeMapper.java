package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子点赞数据访问层
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
