package com.ttikss.nexatalk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ttikss.nexatalk.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 操作日志数据访问层
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 删除指定时间之前的日志
     *
     * @param cutoffTime 截止时间
     * @return 删除的记录数
     */
    @Update("DELETE FROM operation_log WHERE created_at < #{cutoffTime}")
    int deleteOldLogs(@Param("cutoffTime") LocalDateTime cutoffTime);
}
