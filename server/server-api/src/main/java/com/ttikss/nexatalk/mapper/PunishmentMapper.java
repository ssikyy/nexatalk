package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Punishment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 处罚记录数据访问层
 */
@Mapper
public interface PunishmentMapper extends BaseMapper<Punishment> {
}
