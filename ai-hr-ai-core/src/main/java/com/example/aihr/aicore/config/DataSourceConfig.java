package com.example.aihr.aicore.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

/**
 * 数据源配置类，用于配置和监控HikariCP连接池。
 * <p>
 * 配置了HikariCP连接池的各种参数，提高数据库访问性能。
 */
@Configuration
public class DataSourceConfig {
    
    /**
     * 配置HikariCP连接池参数。
     * <p>
     * 从application.yml中读取配置参数。
     * 
     * @return HikariConfig
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        return new HikariConfig();
    }

    /**
     * 创建数据源。
     * <p>
     * 使用配置的HikariCP连接池参数创建数据源。
     * 
     * @param hikariConfig HikariCP配置
     * @return 数据源
     */
    @Bean
    public DataSource dataSource(HikariConfig hikariConfig, Environment environment) {
        if (hikariConfig.getJdbcUrl() == null || hikariConfig.getJdbcUrl().isBlank()) {
            hikariConfig.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        }
        if (hikariConfig.getUsername() == null || hikariConfig.getUsername().isBlank()) {
            hikariConfig.setUsername(environment.getProperty("spring.datasource.username"));
        }
        if (hikariConfig.getPassword() == null || hikariConfig.getPassword().isBlank()) {
            hikariConfig.setPassword(environment.getProperty("spring.datasource.password"));
        }
        if (hikariConfig.getDriverClassName() == null || hikariConfig.getDriverClassName().isBlank()) {
            String driver = environment.getProperty("spring.datasource.driver-class-name");
            if (driver == null || driver.isBlank()) {
                String url = hikariConfig.getJdbcUrl();
                if (url != null && url.startsWith("jdbc:mysql:")) {
                    driver = "com.mysql.cj.jdbc.Driver";
                }
            }
            if (driver != null && !driver.isBlank()) {
                hikariConfig.setDriverClassName(driver);
            }
        }
        return new HikariDataSource(hikariConfig);
    }
    
    /**
     * 创建数据库连接池监控组件。
     * <p>
     * 用于监控HikariCP连接池的运行状态，包括连接数、活跃连接数等指标。
     * 
     * @param meterRegistry 指标注册表
     * @return 数据库连接池监控组件
     */
    @Bean
    public DataSourceMonitor dataSourceMonitor(MeterRegistry meterRegistry) {
        return new DataSourceMonitor(meterRegistry);
    }
    
    /**
     * 数据库连接池监控类，用于监控HikariCP连接池的运行状态。
     */
    public static class DataSourceMonitor {
        
        private final MeterRegistry meterRegistry;
        
        public DataSourceMonitor(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }
        
        /**
         * 监控数据源状态。
         * 
         * @param dataSource 数据源
         */
        public void monitor(DataSource dataSource) {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                
                // 监控连接池大小
                Gauge.builder("aihr.datasource.pool.size", hikariDataSource, ds -> ds.getHikariPoolMXBean().getTotalConnections())
                        .tag("pool", hikariDataSource.getPoolName())
                        .register(meterRegistry);
                
                // 监控活跃连接数
                Gauge.builder("aihr.datasource.pool.active", hikariDataSource, ds -> ds.getHikariPoolMXBean().getActiveConnections())
                        .tag("pool", hikariDataSource.getPoolName())
                        .register(meterRegistry);
                
                // 监控空闲连接数
                Gauge.builder("aihr.datasource.pool.idle", hikariDataSource, ds -> ds.getHikariPoolMXBean().getIdleConnections())
                        .tag("pool", hikariDataSource.getPoolName())
                        .register(meterRegistry);
                
                // 监控等待连接数
                // Gauge.builder("aihr.datasource.pool.pending", hikariDataSource, ds -> ds.getHikariPoolMXBean().getQueueLength())
                //         .tag("pool", hikariDataSource.getPoolName())
                //         .register(meterRegistry);
            }
        }
    }
}
