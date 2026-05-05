package com.focusflow.server;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * FocusFlow Server 主启动类
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * 【架构说明】
 * 本服务采用 Spring Boot 3.x 纯净架构，不依赖任何第三方框架（如若依），
 * 确保代码的可读性和可维护性，适合毕业设计答辩展示。
 *
 * 【核心设计原则】
 * 1. 前后端完全分离：Android 端通过 RESTful API 与后端交互
 * 2. 高内聚低耦合：各模块职责单一，通过接口通信
 * 3. 约定优于配置：遵循 Spring Boot 最佳实践
 *
 * 【启动方式】
 * 方式一：IDE 中直接运行本类
 * 方式二：mvn spring-boot:run
 * 方式三：java -jar focusflow-server-1.0.0.jar
 *
 * @author FocusFlow Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.focusflow.server.mapper")
public class FocusFlowServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FocusFlowServerApplication.class, args);
        log.info("═══════════════════════════════════════════════════════════");
        log.info("   FocusFlow Server 启动成功！");
        log.info("   API 文档地址: http://localhost:8080/api/doc");
        log.info("═══════════════════════════════════════════════════════════");
    }
}
