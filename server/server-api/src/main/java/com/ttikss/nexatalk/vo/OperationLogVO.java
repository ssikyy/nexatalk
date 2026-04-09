package com.ttikss.nexatalk.vo;

import com.ttikss.nexatalk.entity.OperationLog;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志视图对象
 */
@Data
public class OperationLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String method;

    private String ip;

    private String params;

    private String result;

    private Integer status;

    private String errorMsg;

    private Integer duration;

    private LocalDateTime createdAt;

    public static OperationLogVO from(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setUsername(log.getUsername());
        vo.setModule(log.getModule());
        vo.setOperation(log.getOperation());
        vo.setMethod(log.getMethod());
        vo.setIp(log.getIp());
        vo.setParams(log.getParams());
        vo.setResult(log.getResult());
        vo.setStatus(log.getStatus());
        vo.setErrorMsg(log.getErrorMsg());
        vo.setDuration(log.getDuration());
        vo.setCreatedAt(log.getCreatedAt());
        return vo;
    }
}
