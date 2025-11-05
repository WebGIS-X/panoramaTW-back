package com.panorama.backend.service.datasource;

import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.DefaultDataSource;
import com.panorama.backend.repository.LayerNodeRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description: 数据源辅助服务 - 用于获取和管理数据源信息
 * @date: 2025-11-03
 * @version: 1.0
 */
@Service
@Slf4j
public class DataSourceHelperService {

    private final DefaultDataSource defaultDataSource;
    private final LayerNodeRepo layerNodeRepo;

    @Autowired
    public DataSourceHelperService(DefaultDataSource defaultDataSource, LayerNodeRepo layerNodeRepo) {
        this.defaultDataSource = defaultDataSource;
        this.layerNodeRepo = layerNodeRepo;
    }

    /**
     * 获取默认数据源配置
     */
    public Map<String, String> getDefaultDataSource() {
        Map<String, String> dataSourceMap = new HashMap<>();
        dataSourceMap.put("url", defaultDataSource.getUrl());
        dataSourceMap.put("username", defaultDataSource.getUsername());
        dataSourceMap.put("password", defaultDataSource.getPassword());
        return dataSourceMap;
    }

    /**
     * 根据数据源ID获取数据源配置
     * 
     * @param dataSourceId 数据源ID（URL的hashCode）
     * @return 数据源配置 Map
     */
    public Map<String, String> getDataSourceById(String dataSourceId) {
        if (dataSourceId == null || "default".equals(dataSourceId)) {
            return getDefaultDataSource();
        }

        try {
            // 从 MongoDB 查询所有节点
            List<LayerNode> allNodes = layerNodeRepo.findAll();

            // 查找匹配的数据源
            for (LayerNode node : allNodes) {
                if (node.getDataSource() != null && node.getDataSource().get("url") != null) {
                    String url = node.getDataSource().get("url");
                    String id = String.valueOf(url.hashCode());

                    if (id.equals(dataSourceId)) {
                        log.info("找到数据源: {}", url);
                        return node.getDataSource();
                    }
                }
            }

            log.warn("未找到数据源 ID: {}, 使用默认数据源", dataSourceId);
            return getDefaultDataSource();

        } catch (Exception e) {
            log.error("获取数据源失败，使用默认数据源", e);
            return getDefaultDataSource();
        }
    }

    /**
     * 根据父节点或数据源ID决定目标数据源
     * 
     * @param parentNode         父节点
     * @param targetDataSourceId 目标数据源ID（可选）
     * @return 目标数据源配置
     */
    public Map<String, String> resolveTargetDataSource(LayerNode parentNode, String targetDataSourceId) {
        // 如果指定了目标数据源ID，使用它
        if (targetDataSourceId != null && !targetDataSourceId.isEmpty()) {
            log.info("使用指定的数据源 ID: {}", targetDataSourceId);
            return getDataSourceById(targetDataSourceId);
        }

        // 否则，使用父节点的数据源
        if (parentNode != null && parentNode.getDataSource() != null) {
            log.info("使用父节点的数据源");
            return parentNode.getDataSource();
        }

        // 如果都没有，使用默认数据源
        log.info("使用默认数据源");
        return getDefaultDataSource();
    }
}
