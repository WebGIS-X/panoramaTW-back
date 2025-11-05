package com.panorama.backend.controller.node;

import com.panorama.backend.DTO.InfoDTO;
import com.panorama.backend.DTO.LayerNodeDTO;
import com.panorama.backend.model.node.LayerNode;
import com.panorama.backend.model.resource.GeneralResult;
import com.panorama.backend.service.node.LayerNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-14 15:18:33
 * @version: 1.0
 */
@RestController
@RequestMapping("api/v0/node/layerNode")
public class LayerNodeController {

    private LayerNodeService layerNodeService;

    @Autowired
    public void LayerNodeService(LayerNodeService layerNodeService) {
        this.layerNodeService = layerNodeService;
    }

    @GetMapping("/getLayerTree")
    public ResponseEntity<LayerNodeDTO> getAllInfo(){
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .body(layerNodeService.getLayerTree());
    }

    @PostMapping("/createCategory")
    public ResponseEntity<GeneralResult> createCategory(@RequestBody InfoDTO info) throws IOException {
        GeneralResult result = layerNodeService.createCategory(info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

//    @DeleteMapping("/deleteCategory/{id}")
//    public ResponseEntity<String> deleteCategory(@PathVariable String id) throws JsonProcessingException {
//        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
//        GeneralResult result = layerNodeService.deleteCategory(layerNode);
//        HttpHeaders headers = new HttpHeaders();
//        return ResponseEntity.ok().headers(headers)
//                .body(JsonUtil.serializeObject(result));
//    }

    @PutMapping("/updateCategory")
    public ResponseEntity<GeneralResult> updateCategory(@RequestBody InfoDTO info) throws IOException {
        GeneralResult result = layerNodeService.updateCategory(info);
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

    @GetMapping("/getNode/{id}")
    public ResponseEntity<LayerNodeDTO> getNodeById(@PathVariable String id) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        if (layerNode == null) {
            return ResponseEntity.notFound().build();
        }
        
        LayerNodeDTO dto = LayerNodeDTO.builder()
                .id(layerNode.getId())
                .tableName(layerNode.getTableName())
                .layerName(layerNode.getLayerName())
                .category(layerNode.getCategory())
                .usage(layerNode.getUsage())
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto);
    }

    @PutMapping("/updateNode/{id}")
    public ResponseEntity<GeneralResult> updateNode(@PathVariable String id, @RequestBody InfoDTO info) {
        LayerNode layerNode = layerNodeService.getLayerNodeById(id);
        if (layerNode == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(GeneralResult.builder()
                            .status("error")
                            .message("Node not found")
                            .build());
        }
        
        info.setId(id);
        boolean success = layerNodeService.updateLayer(layerNode, info);
        
        GeneralResult result = success 
            ? GeneralResult.builder().status("success").message("Node updated successfully").build()
            : GeneralResult.builder().status("error").message("Failed to update node").build();
        
        HttpHeaders headers = new HttpHeaders();
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result);
    }

}
