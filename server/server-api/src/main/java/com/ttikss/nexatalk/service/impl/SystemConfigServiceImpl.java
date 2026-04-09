package com.ttikss.nexatalk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttikss.nexatalk.entity.SystemConfig;
import com.ttikss.nexatalk.mapper.SystemConfigMapper;
import com.ttikss.nexatalk.service.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 系统配置 Service 实现
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemConfigServiceImpl.class);

    @Override
    public String getStringValue(String key) {
        SystemConfig config = getConfigByKey(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public boolean getBooleanValue(String key, boolean defaultValue) {
        String value = getStringValue(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    @Override
    public int getIntValue(String key, int defaultValue) {
        String value = getStringValue(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 {} 不是有效的整数", key, value);
            return defaultValue;
        }
    }

    @Override
    public long getLongValue(String key, long defaultValue) {
        String value = getStringValue(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 {} 不是有效的长整数", key, value);
            return defaultValue;
        }
    }

    @Override
    public void updateConfig(String key, String value, String description) {
        SystemConfig config = getConfigByKey(key);
        if (config != null) {
            config.setConfigValue(value);
            if (description != null) {
                config.setDescription(description);
            }
            updateById(config);
            log.info("系统配置已更新: {} = {}", key, value);
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(description != null ? description : "");
            save(config);
            log.info("系统配置已创建: {} = {}", key, value);
        }
    }

    private SystemConfig getConfigByKey(String key) {
        return getOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
    }
}
