package com.example.aihr.aicore;

import com.example.aihr.aicore.config.DataSourceConfig;
import com.example.aihr.common.validation.InputValidator;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@SpringBootApplication
@EnableAutoConfiguration
public class AiCoreApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AiCoreApplication.class, args);
        Environment env = context.getEnvironment();
        String apiKey = env.getProperty("spring.ai.dashscope.api-key");
        System.out.println("spring.ai.dashscope.api-key: " + apiKey);
        System.out.println("spring.ai.dashscope.api-key is blank: " + (apiKey == null || apiKey.isBlank()));
        System.out.println("spring.ai.dashscope.api-key equals 'sk-placeholder': " + "sk-placeholder".equals(apiKey));
    }

    /**
     * 应用启动后执行的初始化任务。
     * <p>
     * 用于初始化数据源监控等功能。
     *
     * @param dataSource 数据源
     * @param dataSourceMonitor 数据源监控组件
     * @return ApplicationRunner
     */
    @Bean
    public ApplicationRunner initRunner(DataSource dataSource, DataSourceConfig.DataSourceMonitor dataSourceMonitor) {
        return args -> {
            // 监控数据源状态
            dataSourceMonitor.monitor(dataSource);
            System.out.println("DataSource monitoring initialized");
        };
    }

    @Bean
    public InputValidator inputValidator() {
        return new InputValidator();
    }
}
