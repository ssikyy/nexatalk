package com.ttikss.nexatalk.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库连接自检（启动时执行）
 *
 * 作用：
 * - 项目启动后立刻执行一条最简单的 SQL：SELECT 1
 * - 如果数据库连不上（库不存在、密码错误、MySQL 没启动），应用会直接启动失败
 * - 这样我们就能 100% 确认当前环境数据库连接正确
 *
 * 注意：
 * - 这是开发阶段的“自检工具”，后续稳定后可以保留或改成更正式的健康检查
 */
@Component
public class DbConnectionCheckRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DbConnectionCheckRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        System.out.println("[DB CHECK] MySQL connection OK, SELECT 1 = " + one);
    }
}