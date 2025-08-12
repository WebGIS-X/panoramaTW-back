package com.panorama.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-02 14:30:43
 * @version: 1.0
 */
@Mapper
public interface VectorTileMapper {

    Object getVectorTile(String tableName, int z, int x, int y, String[] visualizationFieldsList);

    Map<String, Object> getDetailInfo(String tableName, int ogc_fid, String[] detailFieldsList);

    void insertGeoJsonFeature(String tableName, String geometry, Map<String, Object> properties);

    void createTable(String tableName, Map<String, Object> propertyType, String type, int srid);

    void deleteTable(String tableName);

    int findSRIDByWKT(String wkt);

    int getSameCount(String tableName);

    String getGeojsonByTableName(String tableName);

    // 新增：获取所有字段（不含geom）
    List<String> getAllColumns(String tableName);

    // 新增：分页获取属性（按指定字段列表）
    List<Map<String, Object>> getAttributes(@Param("tableName") String tableName, 
                                           @Param("columnsList") List<String> columnsList, 
                                           @Param("offset") int offset, 
                                           @Param("limit") int limit);

    // 新增：统计总行数
    int getRowCount(String tableName);
    
    // 临时：调试用 - 查看所有表名
    @org.apache.ibatis.annotations.Select("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name LIKE '%port%'")
    List<String> findTablesLikePort();
}