package com.panorama.backend.controller.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.VectorTileService;
import com.panorama.backend.service.node.LayerNodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-02 10:05:38
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/resource/vector")
public class VectorTileController {

    private VectorTileService vectorTileService;
    private LayerNodeService layerNodeService;

    @Autowired
    public void setVectorTileService(VectorTileService vectorTileService, LayerNodeService layerNodeService) {
        this.vectorTileService = vectorTileService;
        this.layerNodeService = layerNodeService;
    }

    @GetMapping("/getMVT/{id}/{z}/{x}/{y}")
    public ResponseEntity<byte[]> getVectorTile(
            @PathVariable String id,@PathVariable int z, @PathVariable int x, @PathVariable int y) {

        LayerNode layerNode = layerNodeService.getLayerNodeById(id);

        byte[] tileData = vectorTileService.getVectorTile(layerNode, z, x, y);
        if (tileData == null || tileData.length == 0) {
            return ResponseEntity.noContent().build();
        }
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.valueOf("application/vnd.mapbox-vector-tile"))
                .contentLength(tileData.length)
                .body(tileData);
    }

    @GetMapping("/getDetailInfo/{id}/{ogc_fid}")
    public ResponseEntity<String> getDetailInfo(@PathVariable String id, @PathVariable int ogc_fid){

        LayerNode layerNode = layerNodeService.getLayerNodeById(id);

        JsonNode detailInfo = vectorTileService.getDetailInfo(layerNode, ogc_fid);

        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.valueOf("application/json"))
                .body(detailInfo.toString());
    }

    // 测试接口
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        System.out.println("测试接口被调用了！");
        return ResponseEntity.ok("VectorTile Controller 工作正常");
    }
    
    // 新增：返回所有列名（不含geom）
    @GetMapping("/columns/{id}")
    public ResponseEntity<List<String>> getColumns(@PathVariable String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        System.out.println("获取列名 - 图层ID: " + id + ", 表名: " + layerNode.getTableName());
        System.out.println("LayerNode详细信息: " + layerNode.toString());
        
        // 临时调试：查看数据库中实际的表名
        List<String> portTables = vectorTileService.findTablesLikePort();
        System.out.println("数据库中包含'port'的表名: " + (portTables != null ? String.join(", ", portTables) : "无"));
        
        List<String> cols = vectorTileService.getAllColumns(layerNode);
        System.out.println("查询到的列名数量: " + (cols != null ? cols.size() : 0));
        if (cols != null && !cols.isEmpty()) {
            System.out.println("列名列表: " + String.join(", ", cols));
        } else {
            System.out.println("警告：未查询到任何列名！可能表名不存在或无权限访问");
        }
        return ResponseEntity.ok(cols);
    }

    // 新增：分页返回属性数据
    @PostMapping("/attributes/{id}")
    public ResponseEntity<List<Map<String, Object>>> getAttributes(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        LayerNode ln = layerNodeService.getLayerNodeById(id);
        @SuppressWarnings("unchecked")
        List<String> columns = (List<String>) body.get("columns");
        int page = body.get("page") == null ? 1 : (int) body.get("page");
        int size = body.get("size") == null ? 50 : (int) body.get("size");
        
        System.out.println("获取属性数据 - 表名: " + ln.getTableName() + ", 列数: " + (columns != null ? columns.size() : 0) + 
                          ", 页码: " + page + ", 每页: " + size);
        if (columns != null) {
            System.out.println("请求的列: " + String.join(", ", columns));
        }
        
        List<Map<String, Object>> rows = vectorTileService.getAttributes(ln, columns, page, size);
        System.out.println("返回数据行数: " + (rows != null ? rows.size() : 0));
        return ResponseEntity.ok(rows);
    }

    // 新增：总数
    @GetMapping("/rowCount/{id}")
    public ResponseEntity<Integer> getRowCount(@PathVariable String id) {
        LayerNode ln = layerNodeService.getLayerNodeById(id);
        return ResponseEntity.ok(vectorTileService.getRowCount(ln));
    }

    @PostMapping("/upload/json")
    public ResponseEntity<GeneralResult> uploadVectorLayer(@RequestPart("file") MultipartFile file, @RequestPart("info") InfoDTO info) throws IOException {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        GeneralResult result = vectorTileService.uploadJSONLayer(parentNode, file, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/upload/shp/parse")
    public ResponseEntity<GeneralResult> parseShpLayer(@RequestParam("file") MultipartFile file) throws IOException {
        GeneralResult result = vectorTileService.parseShpLayer(file);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PostMapping("/upload/shp/store")
    public ResponseEntity<GeneralResult> storeShpLayer(@RequestPart("path") String path, @RequestPart("info") InfoDTO info) throws IOException, InterruptedException, FactoryException {
        LayerNode parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        GeneralResult result = vectorTileService.storeShpLayer(parentNode, path, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<GeneralResult> deleteVectorLayer(@PathVariable String id) throws JsonProcessingException {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        GeneralResult result = vectorTileService.deleteVectorLayer(layerNode);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @PutMapping("/update")
    public ResponseEntity<GeneralResult> updateVectorLayer(@RequestBody InfoDTO info) throws IOException {
        LayerNode layerNode = layerNodeService.getLayerNodeById(info.getId());
        GeneralResult result = vectorTileService.updateVectorLayer(layerNode, info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }
}
