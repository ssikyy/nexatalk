package com.ttikss.nexatalk.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ttikss.nexatalk.vo.OperationLogVO;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志
     *
     * @param userId    操作用户 ID
     * @param username  操作用户名
     * @param module    操作模块
     * @param operation 操作类型
     * @param method   请求方法
     * @param ip       客户端 IP
     * @param params   请求参数
     * @param result   返回结果
     * @param status   操作结果
     * @param errorMsg 错误信息
     * @param duration 耗时
     */
    void log(Long userId, String username, String module, String operation,
             String method, String ip, String params, String result,
             Integer status, String errorMsg, Integer duration);

    /**
     * 分页查询操作日志
     *
     * @param page      当前页码
     * @param pageSize  每页数量
     * @param username  用户名（可选）
     * @param module    模块（可选）
     * @param operation 操作类型（可选）
     * @param status    状态（可选）
     * @param startTime 起始时间（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @param endTime   结束时间（可选，格式 yyyy-MM-dd HH:mm:ss）
     * @return 分页日志列表
     */
    IPage<OperationLogVO> listLogs(int page, int pageSize, String username, String module, String operation,
                                   Integer status, String startTime, String endTime);

    /**
     * 清空所有操作日志
     */
    void clearAll();
}
