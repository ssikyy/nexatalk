package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Follow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注关系数据访问层
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
