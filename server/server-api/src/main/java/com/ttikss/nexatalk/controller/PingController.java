package com.ttikss.nexatalk.controller;

import com.ttikss.nexatalk.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例业务控制器（PingController）
 *
 * 这个类的作用：
 * 1. 演示一个“标准业务接口”应该如何编写
 * 2. 区分于 HealthController（健康检查）
 * 3. 以后所有真实业务接口（用户、帖子等）都按这个结构来
 */
@RestController
@RequestMapping("/api")
public class PingController {

    /**
     * 测试接口
     *
     * 访问路径：GET /api/ping
     * 用途：验证业务接口是否正常工作
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.ok("pong");
    }
}