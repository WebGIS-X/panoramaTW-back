package com.panorama.backend.AOP;

import com.panorama.backend.config.DataSourceContextHolder;
import com.panorama.backend.config.DynamicDataSource;
import com.panorama.backend.model.node.LayerNode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: DMK
 * @date: 2024-09-18 18:52:08
 * @description:
 * @version: 1.0
 */
@Slf4j
@Aspect
@Component
public class DynamicDataSourceAspect {

    private DynamicDataSource dynamicDataSource;

    //数据库对应驱动
    private final Map<String, String> datasourceDriverMapper = new HashMap<>(){{
        put("mysql", "com.mysql.cj.jdbc.Driver");
        put("oracle", "oracle.jdbc.driver.OracleDriver");
        put("postgresql", "org.postgresql.Driver");
        put("sqlite", "org.sqlite.JDBC");
        put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }};

    @Qualifier("dynamicDataSource")
    @Autowired
    public void setDynamicDataSource(DynamicDataSource dynamicDataSource) {
        this.dynamicDataSource = dynamicDataSource;
    }

    //切换数据源
    @Around("@annotation(com.panorama.backend.annotation.DynamicNodeData)")
    public Object switchDataSource(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        LayerNode layerNode = (LayerNode) args[0];

        try {
            // 如果 layerNode 为 null 或没有 dataSource，使用默认数据源
            if (layerNode == null || layerNode.getDataSource() == null || 
                layerNode.getDataSource().get("url") == null) {
                log.info("LayerNode is null or has no dataSource, using default");
                DataSourceContextHolder.setDataSourceKey("default");
                Object result = joinPoint.proceed();
                DataSourceContextHolder.setDataSourceKey("default");
                return result;
            }
            
            String url = layerNode.getDataSource().get("url");
            log.info("Creating datasource for URL: {}", url);
            String hashCode4JDBC= String.valueOf(url.hashCode());
            //判断数据源是否已存在，若不存在则新建
            if (!dynamicDataSource.containDataSourceKey(hashCode4JDBC)) {
                DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
                dataSourceBuilder.url(url);
                //根据驱动map得到对应数据库的驱动
                String driver = datasourceDriverMapper.get(url.split(":")[1]);
                log.info("Using driver: {}", driver);
                dataSourceBuilder.driverClassName(driver);
                if(layerNode.getDataSource().containsKey("username")) {
                    dataSourceBuilder.username(layerNode.getDataSource().get("username"));
                }
                if(layerNode.getDataSource().containsKey("password")) {
                    dataSourceBuilder.password(layerNode.getDataSource().get("password"));
                }
                // 设置连接超时和测试配置
                dataSourceBuilder.type(com.zaxxer.hikari.HikariDataSource.class);
                DataSource source = dataSourceBuilder.build();
                if (source instanceof com.zaxxer.hikari.HikariDataSource) {
                    com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) source;
                    hikariDataSource.setConnectionTimeout(5000); // 5秒连接超时
                    hikariDataSource.setValidationTimeout(3000); // 3秒验证超时
                    hikariDataSource.setInitializationFailTimeout(5000); // 5秒初始化失败超时
                    hikariDataSource.setConnectionTestQuery("SELECT 1"); // 连接测试查询
                    hikariDataSource.setMaximumPoolSize(5); // 限制连接池大小
                    log.info("HikariCP timeout configured, maxPoolSize: 5");
                }
                log.info("DataSource built successfully, adding to pool");
                dynamicDataSource.addDataSource(hashCode4JDBC, source);
                
                // 测试连接
                try {
                    java.sql.Connection conn = source.getConnection();
                    if (conn != null) {
                        log.info("Test connection successful!");
                        conn.close();
                    }
                } catch (Exception e) {
                    log.error("Test connection failed: {}", e.getMessage());
                }
            } else {
                log.info("DataSource already exists, reusing");
            }
            // 切换数据源
            DataSourceContextHolder.setDataSourceKey(hashCode4JDBC);
        } catch (Exception e) {
            log.error("Failed to switch datasource: {}", e.getMessage(), e);
            log.error("Exception details: ", e);
            DataSourceContextHolder.setDataSourceKey("default");
        }
        log.info(DataSourceContextHolder.getDataSourceKey());

        Object result = joinPoint.proceed();

        DataSourceContextHolder.setDataSourceKey("default");

        return result;
    }

}
