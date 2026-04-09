package com.ttikss.nexatalk.config;

import com.ttikss.nexatalk.mapper.OperationLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志自动清理任务
 *
 * 作用：
 * - 每天凌晨 3 点自动清理 90 天前的操作日志
 * - 防止日志表数据无限增长
 */
@Component
@ConditionalOnProperty(name = "app.log.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class OperationLogCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(OperationLogCleanupTask.class);
    private static final int RETENTION_DAYS = 90;

    private final OperationLogMapper operationLogMapper;

    public OperationLogCleanupTask(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨 3 点执行
    public void cleanup() {
        log.info("开始清理 {} 天前的操作日志...", RETENTION_DAYS);
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(RETENTION_DAYS);
            int deletedCount = operationLogMapper.deleteOldLogs(cutoffTime);
            log.info("清理完成，共删除 {} 条旧日志", deletedCount);
        } catch (Exception e) {
            log.error("清理操作日志失败: {}", e.getMessage(), e);
        }
    }
}
