package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏数据访问层
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
