package com.panorama.backend.DTO;

import com.panorama.backend.DTO.base.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-25 10:52:32
 * @version: 1.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class InfoDTO extends BaseDTO {
    String parent_id;
    Map<String, Object> propertyType;
    String targetDataSourceId; // 目标数据源ID（可选）
}
