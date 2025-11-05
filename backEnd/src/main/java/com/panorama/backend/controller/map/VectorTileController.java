package com.panorama.backend.controller.map;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.map.VectorTileService;
import com.panorama.backend.service.node.LayerNodeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
            @PathVariable String id, @PathVariable int z, @PathVariable int x, @PathVariable int y) {
        try {
            LayerNode layerNode = layerNodeService.getLayerNodeById(id);
            if (layerNode == null) {
                return ResponseEntity.noContent().build();
            }

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
        } catch (Exception e) {
            // 记录错误日志，但返回 204 而不是 500
            return ResponseEntity.noContent().build();
        }
    }

    @GetMapping("/getDetailInfo/{id}/{ogc_fid}")
    public ResponseEntity<String> getDetailInfo(@PathVariable String id, @PathVariable int ogc_fid) {

        LayerNode layerNode = layerNodeService.getLayerNodeById(id);

        JsonNode detailInfo = vectorTileService.getDetailInfo(layerNode, ogc_fid);

        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.valueOf("application/json"))
                .body(detailInfo.toString());
    }

    @PostMapping("/upload/json")
    public ResponseEntity<GeneralResult> uploadVectorLayer(@RequestPart("file") MultipartFile file,
            @RequestPart("info") String infoJson) throws IOException {
        // 手动解析 JSON 字符串为 InfoDTO 对象
        ObjectMapper objectMapper = new ObjectMapper();
        InfoDTO info = objectMapper.readValue(infoJson, InfoDTO.class);

        LayerNode parentNode;
        if (info.getParent_id() == null || info.getParent_id().isEmpty()) {
            // 如果没有提供 parent_id，使用根节点
            parentNode = layerNodeService.getLayerNodeByTableName("layerNode");
        } else {
            parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        }
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
    public ResponseEntity<GeneralResult> storeShpLayer(@RequestPart("path") String path,
            @RequestPart("info") String infoJson) throws IOException, InterruptedException, FactoryException {
        // 手动解析 JSON 字符串为 InfoDTO 对象
        ObjectMapper objectMapper = new ObjectMapper();
        InfoDTO info = objectMapper.readValue(infoJson, InfoDTO.class);

        LayerNode parentNode;
        if (info.getParent_id() == null || info.getParent_id().isEmpty()) {
            // 如果没有提供 parent_id，使用根节点
            parentNode = layerNodeService.getLayerNodeByTableName("layerNode");
        } else {
            parentNode = layerNodeService.getLayerNodeById(info.getParent_id());
        }
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
