package com.ttikss.nexatalk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统操作日志实体
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID */
    private Long userId;

    /** 操作用户名 */
    private String username;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 客户端 IP */
    private String ip;

    /** 请求参数 */
    private String params;

    /** 返回结果 */
    private String result;

    /** 操作结果: 1=成功, 0=失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 操作耗时（毫秒） */
    private Integer duration;

    /** 操作时间 */
    private LocalDateTime createdAt;
}
