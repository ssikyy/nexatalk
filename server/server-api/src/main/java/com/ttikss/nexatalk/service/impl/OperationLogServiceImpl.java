package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.entity.OperationLog;
import com.ttikss.nexatalk.mapper.OperationLogMapper;
import com.ttikss.nexatalk.service.OperationLogService;
import com.ttikss.nexatalk.vo.OperationLogVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public OperationLogServiceImpl(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Override
    @Async
    public void log(Long userId, String username, String module, String operation,
                    String method, String ip, String params, String result,
                    Integer status, String errorMsg, Integer duration) {
        if (module == null) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username != null ? username : "");
        log.setModule(module != null ? module : "");
        log.setOperation(operation != null ? operation : "");
        log.setMethod(method);
        log.setIp(ip);
        log.setParams(truncateStr(params, 2000));
        log.setResult(truncateStr(result, 2000));
        log.setStatus(status);
        log.setErrorMsg(truncateStr(errorMsg, 512));
        log.setDuration(duration);
        operationLogMapper.insert(log);
    }

    @Override
    public IPage<OperationLogVO> listLogs(int page, int pageSize, String username, String module, String operation,
                                          Integer status, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(OperationLog::getUsername, username);
        }
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.like(OperationLog::getOperation, operation);
        }
        if (status != null) {
            wrapper.eq(OperationLog::getStatus, status);
        }
        if (StringUtils.hasText(startTime)) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime, TIME_FMT);
                wrapper.ge(OperationLog::getCreatedAt, start);
            } catch (Exception ignored) {}
        }
        if (StringUtils.hasText(endTime)) {
            try {
                LocalDateTime end = LocalDateTime.parse(endTime, TIME_FMT);
                wrapper.le(OperationLog::getCreatedAt, end);
            } catch (Exception ignored) {}
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> pageParam = new Page<>(page, pageSize);
        IPage<OperationLog> logPage = operationLogMapper.selectPage(pageParam, wrapper);

        return logPage.convert(OperationLogVO::from);
    }

    @Override
    public void clearAll() {
        operationLogMapper.delete(null);
    }

    private String truncateStr(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }
}
