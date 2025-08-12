package com.panorama.backend.service.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.annotation.DynamicNodeData;
import com.panorama.backend.mapper.VectorTileMapper;
import com.panorama.backend.model.Constant.GenerateResultStatus;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.DefaultDataSource;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import com.panorama.backend.util.FileUtil;
import com.panorama.backend.util.JsonUtil;
import com.panorama.backend.util.ProcessUtil;
import com.panorama.backend.util.ShapeFileUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-02 09:46:30
 * @version: 1.0
 */
@Service
@Slf4j
public class VectorTileService {
    private VectorTileMapper vectorTileMapper;

    @Value("${path.temp}")
    private String temp;

    private DefaultDataSource defaultDataSource;

    private LayerNodeService layerNodeService;

    @Autowired
    public void setVectorTileMapper(VectorTileMapper vectorTileMapper, LayerNodeService layerNodeService, DefaultDataSource defaultDataSource) {
        this.vectorTileMapper = vectorTileMapper;
        this.layerNodeService = layerNodeService;
        this.defaultDataSource = defaultDataSource;
    }

    @DynamicNodeData
    public byte[] getVectorTile(LayerNode layerNode, int z, int x, int y) {
        String tableName = layerNode.getTableName();
        String[] visualizationFieldsList = layerNode.getUsage().get("visualizationField").split(",");
        byte[] tile = (byte[]) vectorTileMapper.getVectorTile(tableName, z, x, y, visualizationFieldsList);
        return tile;
    }

    @DynamicNodeData
    public JsonNode getDetailInfo(LayerNode layerNode, int ogc_fid) {
        String tableName = layerNode.getTableName();
        String[] detailFieldsList = layerNode.getUsage().get("detailField").split(",");
        return JsonUtil.mapToJson(vectorTileMapper.getDetailInfo(tableName, ogc_fid, detailFieldsList));
    }

    @DynamicNodeData
    public GeneralResult uploadJSONLayer(LayerNode parentNode, MultipartFile multipartFile, InfoDTO infoDTO) throws IOException {

        //M2F
        File file = FileUtil.convertMultipartFileToFile(multipartFile, temp);

        try {
            //解析json
            JsonNode features = JsonUtil.parseJson(file).get("features");
            //建表，指定表名、字段名、字段类型
            String tableName = infoDTO.getTableName();
            String layerName = infoDTO.getLayerName();

            int sameCount = vectorTileMapper.getSameCount(tableName);
            String uniqueTableName;
            if (sameCount == 0){
                uniqueTableName = tableName;
            }else {
                uniqueTableName = tableName + "_" + sameCount;
            }

            vectorTileMapper.createTable(uniqueTableName, infoDTO.getPropertyType(), infoDTO.getUsage().get("type"), Integer.parseInt(infoDTO.getUsage().get("srid")));
            //插入每个要素
            for (JsonNode feature : features) {
                String geometry = feature.get("geometry").toString();
                vectorTileMapper.insertGeoJsonFeature(uniqueTableName, geometry, JsonUtil.jsonToMap(feature.get("properties")));
            }

            String path = layerNodeService.getNodePath(parentNode);

            Map<String, String> dataSourceMap = getDataSourceMap();

            LayerNode newLayerNode = LayerNode.builder()
                    .tableName(uniqueTableName).layerName(layerName)
                    .category("vector").usage(infoDTO.getUsage())
                    .path(path).dataSource(dataSourceMap)
                    .build();

            layerNodeService.saveLayerNode(newLayerNode);
            FileUtil.deleteDirectory(Path.of(file.getParent()));

            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("upload json successfully").build();

        }catch (Exception e){
            vectorTileMapper.deleteTable(infoDTO.getTableName());
            FileUtil.deleteDirectory(Path.of(file.getParent()));
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("invalid json").build();
        }
    }

    public GeneralResult parseShpLayer(MultipartFile multipartFile) throws IOException {

        //M2F
        File zipFile = FileUtil.convertMultipartFileToFile(multipartFile, temp);
        String path = zipFile.getParent();
        //解压
        List<String> list = FileUtil.unZipFiles(zipFile, path);
        boolean result = zipFile.delete();
        if (!result) {
            log.error("failed to delete zip file");
        }
        //找到shp文件
        String shapefilePath = FileUtil.findFileWithExtension(list, ".shp");
        if (shapefilePath.isEmpty()){
            return GeneralResult.builder().status("error").message("shapefile not found").build();
        }else {
            log.info("shp路径为：{}", shapefilePath);
            try {
                Map<String, Object> info = new HashMap<>();
                Map<String, String> fields = ShapeFileUtil.parseShapefile(new File(shapefilePath));
                Map<String, String> propertyType = new HashMap<>(fields);
                propertyType.remove("the_geom");
                info.put("geom", fields.get("the_geom"));
                info.put("propertyType", propertyType);
                info.put("path", path);
                return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message(info).build();
            } catch (Exception e) {
                return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to parse shp").build();
            }
        }
    }

    @DynamicNodeData
    public GeneralResult storeShpLayer(LayerNode parentNode, String path, InfoDTO infoDTO) throws IOException, InterruptedException, FactoryException {
        String shpPath = FileUtil.findFileWithExtension(path, ".shp");
        String prjPath = FileUtil.findFileWithExtension(path, ".prj");

        //获取srid，默认为4326
        String wkt;
        int srid;
        if (prjPath != null){
            wkt = new String(Files.readAllBytes(Paths.get(prjPath)));
            CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
            // 获取SRID
            srid = CRS.lookupEpsgCode(crs, false);
        }else {
            srid = 4326;
        }

        String tableName = infoDTO.getTableName();
        int sameCount = vectorTileMapper.getSameCount(tableName);
        String uniqueTableName;
        if (sameCount == 0){
            uniqueTableName = tableName;
        }else {
            uniqueTableName = tableName + "_" + sameCount;
        }

        if (ProcessUtil.shp2pgProcess(shpPath, uniqueTableName, defaultDataSource, srid)){
            Map<String, String> dataSourceMap = getDataSourceMap();
            Map<String, String> usage = infoDTO.getUsage();
            usage.put("srid", String.valueOf(srid));
            LayerNode newLayerNode = LayerNode.builder()
                    .tableName(uniqueTableName).layerName(infoDTO.getLayerName())
                    .category("vector").usage(usage)
                    .path(layerNodeService.getNodePath(parentNode)).dataSource(dataSourceMap)
                    .build();
            layerNodeService.saveLayerNode(newLayerNode);
            FileUtil.deleteDirectory(Path.of(path));
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("store shp successfully").build();
        }else {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to store shp").build();
        }
    }

    @DynamicNodeData
    public GeneralResult deleteVectorLayer(LayerNode layerNode){
        try{
            vectorTileMapper.deleteTable(layerNode.getTableName());
            layerNodeService.deleteLayerNode(layerNode);
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("delete vector layer successfully").build();
        }catch (Exception e){
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to delete vector layer").build();
        }
    }

    @DynamicNodeData
    public GeneralResult updateVectorLayer(LayerNode layerNode, InfoDTO infoDTO){
        if (layerNodeService.updateLayer(layerNode, infoDTO)){
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message("update vector layer successfully").build();
        }else{
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to update vector layer").build();
        }
    }

    @DynamicNodeData
    public GeneralResult getGeojsonByTableName(LayerNode layerNode, String tableName) {
        try {
            return GeneralResult.builder().status(GenerateResultStatus.SUCCESS).message(vectorTileMapper.getGeojsonByTableName(tableName)).build();
        } catch (Exception e) {
            return GeneralResult.builder().status(GenerateResultStatus.ERROR).message("failed to get geojson").build();
        }
    }

    // 新增：获取所有列
    @DynamicNodeData
    public List<String> getAllColumns(LayerNode layerNode) {
        return vectorTileMapper.getAllColumns(layerNode.getTableName());
    }

    // 新增：分页获取属性
    @DynamicNodeData
    public List<Map<String, Object>> getAttributes(LayerNode layerNode, List<String> columns, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        return vectorTileMapper.getAttributes(layerNode.getTableName(), columns, offset, size);
    }

    // 新增：总数
    @DynamicNodeData
    public int getRowCount(LayerNode layerNode) {
        return vectorTileMapper.getRowCount(layerNode.getTableName());
    }
    
    // 临时：调试用
    public List<String> findTablesLikePort() {
        return vectorTileMapper.findTablesLikePort();
    }

    private Map<String, String> getDataSourceMap() {
        Map<String, String> dataSourceMap = new HashMap<>();
        dataSourceMap.put("url", defaultDataSource.getUrl());
        dataSourceMap.put("username", defaultDataSource.getUsername());
        dataSourceMap.put("password", defaultDataSource.getPassword());
        return dataSourceMap;
    }

}
