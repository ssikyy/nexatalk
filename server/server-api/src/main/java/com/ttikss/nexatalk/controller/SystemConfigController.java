package com.ttikss.nexatalk.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ttikss.nexatalk.common.Result;
import com.ttikss.nexatalk.entity.SystemConfig;
import com.ttikss.nexatalk.security.CurrentUser;
import com.ttikss.nexatalk.security.RequireAdmin;
import com.ttikss.nexatalk.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置管理接口
 */
@RestController
@RequestMapping("/api/admin/system")
@Tag(name = "系统配置管理", description = "系统配置的增删改查接口")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/configs")
    @Operation(summary = "获取系统配置列表", description = "分页获取所有系统配置")
    @RequireAdmin
    public Result<Page<SystemConfig>> getConfigList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "配置键关键字搜索") @RequestParam(required = false) String keyword) {
        
        Page<SystemConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SystemConfig::getConfigKey, keyword)
                   .or()
                   .like(SystemConfig::getDescription, keyword);
        }
        
        wrapper.orderByDesc(SystemConfig::getUpdatedAt);
        Page<SystemConfig> result = systemConfigService.page(page, wrapper);
        return Result.ok(result);
    }

    @GetMapping("/configs/all")
    @Operation(summary = "获取所有系统配置", description = "获取所有系统配置（不分页）")
    @RequireAdmin
    public Result<List<SystemConfig>> getAllConfigs() {
        List<SystemConfig> configs = systemConfigService.list(
            new LambdaQueryWrapper<SystemConfig>()
                .orderByDesc(SystemConfig::getUpdatedAt)
        );
        return Result.ok(configs);
    }

    @GetMapping("/configs/{id}")
    @Operation(summary = "获取单个配置详情")
    @RequireAdmin
    public Result<SystemConfig> getConfigById(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        SystemConfig config = systemConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.ok(config);
    }

    @GetMapping("/configs/key/{key}")
    @Operation(summary = "根据键名获取配置")
    @RequireAdmin
    public Result<SystemConfig> getConfigByKey(
            @Parameter(description = "配置键") @PathVariable String key) {
        SystemConfig config = systemConfigService.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
        );
        if (config == null) {
            return Result.error("配置不存在");
        }
        return Result.ok(config);
    }

    @PostMapping("/configs")
    @Operation(summary = "创建系统配置")
    @RequireAdmin
    public Result<SystemConfig> createConfig(
            @CurrentUser Long userId,
            @RequestBody SystemConfig config) {
        
        // 检查配置键是否已存在
        SystemConfig existing = systemConfigService.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, config.getConfigKey())
        );
        if (existing != null) {
            return Result.error("配置键已存在，请使用更新接口");
        }
        
        // 设置默认值
        if (config.getConfigType() == null || config.getConfigType().isEmpty()) {
            config.setConfigType("string");
        }
        if (config.getDescription() == null) {
            config.setDescription("");
        }
        
        systemConfigService.save(config);
        return Result.ok(config);
    }

    @PutMapping("/configs/{id}")
    @Operation(summary = "更新系统配置")
    @RequireAdmin
    public Result<SystemConfig> updateConfig(
            @Parameter(description = "配置ID") @PathVariable Long id,
            @RequestBody SystemConfig config) {
        
        SystemConfig existing = systemConfigService.getById(id);
        if (existing == null) {
            return Result.error("配置不存在");
        }
        
        // 如果更新了配置键，检查是否冲突
        if (config.getConfigKey() != null && !config.getConfigKey().equals(existing.getConfigKey())) {
            SystemConfig conflict = systemConfigService.getOne(
                new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getConfigKey, config.getConfigKey())
            );
            if (conflict != null) {
                return Result.error("配置键已存在");
            }
        }
        
        // 更新非空字段
        if (config.getConfigKey() != null) {
            existing.setConfigKey(config.getConfigKey());
        }
        if (config.getConfigValue() != null) {
            existing.setConfigValue(config.getConfigValue());
        }
        if (config.getConfigType() != null) {
            existing.setConfigType(config.getConfigType());
        }
        if (config.getDescription() != null) {
            existing.setDescription(config.getDescription());
        }
        
        systemConfigService.updateById(existing);
        return Result.ok(existing);
    }

    @PutMapping("/configs/key/{key}")
    @Operation(summary = "根据键名更新配置", description = "更便捷的根据配置键更新配置值")
    @RequireAdmin
    public Result<String> updateConfigByKey(
            @Parameter(description = "配置键") @PathVariable String key,
            @RequestBody SystemConfig config) {
        
        SystemConfig existing = systemConfigService.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
        );
        
        if (existing == null) {
            return Result.error("配置不存在");
        }
        
        if (config.getConfigValue() != null) {
            existing.setConfigValue(config.getConfigValue());
        }
        if (config.getDescription() != null) {
            existing.setDescription(config.getDescription());
        }
        
        systemConfigService.updateById(existing);
        return Result.ok("配置更新成功");
    }

    @DeleteMapping("/configs/{id}")
    @Operation(summary = "删除系统配置")
    @RequireAdmin
    public Result<Void> deleteConfig(
            @Parameter(description = "配置ID") @PathVariable Long id) {
        
        SystemConfig config = systemConfigService.getById(id);
        if (config == null) {
            return Result.error("配置不存在");
        }
        
        // 禁止删除关键系统配置
        if ("ai_enabled".equals(config.getConfigKey()) ||
            "ai_base_url".equals(config.getConfigKey()) ||
            "ai_api_key".equals(config.getConfigKey()) ||
            "ai_model".equals(config.getConfigKey()) ||
            "ai_timeout_ms".equals(config.getConfigKey())) {
            return Result.error("禁止删除系统关键配置");
        }
        
        systemConfigService.removeById(id);
        return Result.ok(null);
    }

    @DeleteMapping("/configs/key/{key}")
    @Operation(summary = "根据键名删除配置")
    @RequireAdmin
    public Result<Void> deleteConfigByKey(
            @Parameter(description = "配置键") @PathVariable String key) {
        
        SystemConfig config = systemConfigService.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
        );
        
        if (config == null) {
            return Result.error("配置不存在");
        }
        
        // 禁止删除关键系统配置
        if ("ai_enabled".equals(key) ||
            "ai_base_url".equals(key) ||
            "ai_api_key".equals(key) ||
            "ai_model".equals(key) ||
            "ai_timeout_ms".equals(key)) {
            return Result.error("禁止删除系统关键配置");
        }
        
        systemConfigService.removeById(config.getId());
        return Result.ok(null);
    }

    @PostMapping("/configs/batch")
    @Operation(summary = "批量更新配置", description = "根据键名批量更新多个配置")
    @RequireAdmin
    public Result<String> batchUpdateConfigs(@RequestBody List<SystemConfig> configs) {
        for (SystemConfig config : configs) {
            if (config.getConfigKey() == null || config.getConfigKey().isEmpty()) {
                continue;
            }
            systemConfigService.updateConfig(
                config.getConfigKey(),
                config.getConfigValue(),
                config.getDescription()
            );
        }
        return Result.ok("批量更新成功");
    }
}
