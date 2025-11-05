package com.panorama.backend.controller.datasource;

import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.repository.LayerNodeRepo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author: DMK
 * @description: DataSource Controller - 管理可用的数据源
 * @date: 2025-11-03
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/datasource")
@Slf4j
public class DataSourceController {

    private final LayerNodeRepo layerNodeRepo;

    @Autowired
    public DataSourceController(LayerNodeRepo layerNodeRepo) {
        this.layerNodeRepo = layerNodeRepo;
    }

    /**
     * 获取所有可用的数据源列表（仅PostgreSQL）
     */
    @GetMapping("/list")
    public ResponseEntity<List<DataSourceInfo>> listDataSources() {
        try {
            // 从 MongoDB 查询所有 LayerNode
            List<LayerNode> allNodes = layerNodeRepo.findAll();

            // 提取唯一的数据源
            Map<String, DataSourceInfo> uniqueSources = new HashMap<>();

            // 首先添加默认数据源
            uniqueSources.put("default", DataSourceInfo.builder()
                    .id("default")
                    .displayName("默认数据库")
                    .url("jdbc:postgresql://localhost:5432/panoramaTW")
                    .database("panoramaTW")
                    .build());

            // 然后添加从节点中提取的数据源
            for (LayerNode node : allNodes) {
                if (node.getDataSource() != null && node.getDataSource().get("url") != null) {
                    String url = node.getDataSource().get("url");

                    // ✨ 只处理 JDBC PostgreSQL URL
                    if (!url.startsWith("jdbc:postgresql://")) {
                        continue; // 跳过 HTTP URL、MySQL URL 等
                    }

                    String id = String.valueOf(url.hashCode());

                    if (!uniqueSources.containsKey(id)) {
                        // 从 URL 提取数据库名
                        String dbName = extractDatabaseName(url);

                        DataSourceInfo info = DataSourceInfo.builder()
                                .id(id)
                                .displayName(dbName)
                                .url(maskPassword(url)) // 隐藏密码
                                .database(dbName)
                                .build();

                        uniqueSources.put(id, info);
                    }
                }
            }

            log.info("Found {} unique PostgreSQL datasources", uniqueSources.size());
            return ResponseEntity.ok(new ArrayList<>(uniqueSources.values()));

        } catch (Exception e) {
            log.error("Failed to list datasources", e);
            // 返回默认数据源
            return ResponseEntity.ok(Collections.singletonList(
                    DataSourceInfo.builder()
                            .id("default")
                            .displayName("默认数据库")
                            .url("jdbc:postgresql://localhost:5432/panoramaTW")
                            .database("panoramaTW")
                            .build()));
        }
    }

    /**
     * 从 JDBC URL 提取数据库名
     */
    private String extractDatabaseName(String jdbcUrl) {
        // jdbc:postgresql://localhost:5432/database_name
        try {
            String[] parts = jdbcUrl.split("/");
            String dbPart = parts[parts.length - 1];
            // 移除可能的参数 (如 ?param=value)
            return dbPart.split("\\?")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 隐藏 URL 中的密码
     */
    private String maskPassword(String url) {
        // 如果 URL 包含密码 (jdbc:postgresql://user:pass@host:port/db)
        // 替换为 jdbc:postgresql://user:****@host:port/db
        return url.replaceAll(":[^:@]+@", ":****@");
    }
}

/**
 * 数据源信息 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DataSourceInfo {
    private String id; // hashCode 或 "default"
    private String displayName; // 显示名称
    private String url; // JDBC URL（已脱敏）
    private String database; // 数据库名
}
