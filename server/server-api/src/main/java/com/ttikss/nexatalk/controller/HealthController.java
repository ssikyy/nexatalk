package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查控制器
 *
 * 用途：
 * 1. 用于运维/部署时判断服务是否存活
 * 2. 不承载任何业务逻辑
 * 3. 永远不抛业务异常
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        // 只要服务能响应，说明系统是健康的
        return Result.ok("OK");
    }
}