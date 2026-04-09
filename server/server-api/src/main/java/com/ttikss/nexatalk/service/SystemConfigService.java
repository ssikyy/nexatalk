package com.ttikss.nexatalk.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttikss.nexatalk.entity.SystemConfig;

/**
 * 系统配置 Service 接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取字符串值
     */
    String getStringValue(String key);

    /**
     * 根据配置键获取布尔值
     */
    boolean getBooleanValue(String key, boolean defaultValue);

    /**
     * 根据配置键获取整数值
     */
    int getIntValue(String key, int defaultValue);

    /**
     * 根据配置键获取长整数值
     */
    long getLongValue(String key, long defaultValue);

    /**
     * 更新配置值
     */
    void updateConfig(String key, String value, String description);
}
