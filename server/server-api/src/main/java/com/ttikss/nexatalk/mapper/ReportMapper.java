package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 举报数据访问层
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
