package com.ttikss.nexatalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot 启动类（ServerApiApplication）
 *
 * 作用：
 * - 作为后端应用的入口（main 方法）
 * - 触发 Spring Boot 自动配置、组件扫描与内置 Tomcat 启动
 */
@SpringBootApplication(scanBasePackages = "com.ttikss.nexatalk")
@EnableAsync
public class ServerApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerApiApplication.class, args);
	}
}