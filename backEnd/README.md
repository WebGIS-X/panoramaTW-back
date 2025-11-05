# PanoramaTW Backend

PanoramaTW 后端服务，基于 Spring Boot，提供 WebGIS 数据管理和地理空间服务。

## 目录

- [快速开始](#快速开始)
- [本次更新内容](#本次更新内容)
- [Dataset Node 编辑 API](#dataset-node-编辑-api)
- [技术栈](#技术栈)
- [数据库设计](#数据库设计)
- [故障排查](#故障排查)

---

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- MongoDB 4.4+
- PostgreSQL 12+ (with PostGIS)

### 启动步骤

```bash
# 1. 启动 MongoDB
mongod --dbpath C:\data\db

# 2. 启动 PostgreSQL（通常作为服务自动启动）

# 3. 配置文件（如需自定义）
cd src/main/resources
copy application-dmk.yml application-yourname.yml
# 编辑 application-yourname.yml，修改数据库密码和路径

# 4. 启动应用
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dmk
```

服务启动在 `http://localhost:8765`

### 验证

```bash
curl http://localhost:8765/api/v0/node/layerNode/getLayerTree
```

---

## 本次更新内容

### 🎯 新增功能

#### Dataset Node 编辑 API

- ✅ 获取单个节点的详细信息
- ✅ 更新节点的 usage 字段（元数据）
- ✅ 支持完整替换，正确处理删除和修改 key 操作
- ✅ 自动保存到 MongoDB

### 📝 修改的文件清单

| 文件路径                                   | 修改内容                | 说明                      |
| ------------------------------------------ | ----------------------- | ------------------------- |
| `controller/node/LayerNodeController.java` | 新增 2 个 API 端点      | `getNode` 和 `updateNode` |
| `service/node/LayerNodeService.java`       | 优化 `updateLayer` 方法 | 改为完整替换 usage        |

### 🔑 新增 API 端点

#### 1. 获取节点信息

```java
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

    return ResponseEntity.ok(dto);
}
```

#### 2. 更新节点信息

```java
@PutMapping("/updateNode/{id}")
public ResponseEntity<GeneralResult> updateNode(
        @PathVariable String id,
        @RequestBody InfoDTO info) {
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
        ? GeneralResult.builder()
            .status("success")
            .message("Node updated successfully")
            .build()
        : GeneralResult.builder()
            .status("error")
            .message("Failed to update node")
            .build();

    return ResponseEntity.ok(result);
}
```

### 🔧 优化的 Service 方法

#### LayerNodeService.java

```java
public boolean updateLayer(LayerNode layerNode, InfoDTO infoDTO) {
    try {
        // 更新 layerName
        String layerName = infoDTO.getLayerName();
        if (layerName != null && !layerName.isEmpty()) {
            layerNode.setLayerName(layerName);
        }

        // 更新 usage - 关键改动：完整替换
        // 前端会发送完整的 usage（系统字段 + 用户字段）
        Map<String, String> usage = infoDTO.getUsage();
        if (usage != null) {
            layerNode.setUsage(usage);  // ← 直接替换，支持删除
        }

        saveLayerNode(layerNode);  // 保存到 MongoDB
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

**修改前**（有问题）：

```java
existingUsage.putAll(usage);  // ❌ 只能添加/更新，无法删除旧 key
```

**修改后**（正确）：

```java
layerNode.setUsage(usage);    // ✅ 完整替换，支持删除和修改 key
```

---

## Dataset Node 编辑 API

### API 详情

#### 获取节点

```http
GET /api/v0/node/layerNode/getNode/{id}
```

**响应**：

```json
{
  "id": "node-id",
  "layerName": "My Dataset",
  "tableName": "table_name",
  "category": "category_name",
  "usage": {
    "type": "vector",
    "srid": "4326",
    "customField": "customValue"
  }
}
```

#### 更新节点

```http
PUT /api/v0/node/layerNode/updateNode/{id}
Content-Type: application/json

{
  "usage": {
    "type": "vector",
    "srid": "4326",
    "newField": "newValue"
  }
}
```

**响应**：

```json
{
  "status": "success",
  "message": "Node updated successfully"
}
```

### 数据流程

```
前端请求
    ↓
LayerNodeController.updateNode()
    ↓
LayerNodeService.updateLayer()
    ↓
layerNode.setUsage(usage)  ← 完整替换
    ↓
layerNodeRepo.save(layerNode)
    ↓
MongoDB 更新
```

---

## 技术栈

### 核心技术

- **Spring Boot** 3.x
- **MongoDB** - 元数据存储
- **PostgreSQL + PostGIS** - 地理数据存储
- **MyBatis** - SQL 映射
- **Maven** - 构建工具
- **Lombok** - 代码简化

### 依赖管理

主要依赖（pom.xml）：

- spring-boot-starter-web
- spring-boot-starter-data-mongodb
- spring-boot-starter-jdbc
- postgresql
- mybatis-spring-boot-starter
- lombok

---

## 数据库设计

### MongoDB - layerNode 集合

```javascript
{
  "_id": "67ce47fd71dbf9e10feb40f4",
  "layerName": "Beijing Roads",
  "tableName": "beijing_roads",
  "category": "Transportation",
  "path": "root,china,beijing,",
  "dataSource": {
    "type": "postgresql",
    "database": "panoramaTW"
  },
  "usage": {
    // 系统字段
    "type": "vector",
    "srid": "4326",
    "visualizationField": "name,type",
    "detailField": "name,type,length",
    // 用户自定义字段
    "dataSource": "OpenStreetMap",
    "lastUpdate": "2024-01-15",
    "accuracy": "High"
  }
}
```

**字段说明**：

| 字段        | 类型                | 说明            |
| ----------- | ------------------- | --------------- |
| `_id`       | String              | 节点唯一标识    |
| `layerName` | String              | 显示名称        |
| `tableName` | String              | PostgreSQL 表名 |
| `category`  | String              | 分类名称        |
| `path`      | String              | 树形路径        |
| `usage`     | Map<String, String> | 元数据和配置    |

### PostgreSQL - 动态表

根据上传的数据动态创建：

```sql
CREATE TABLE beijing_roads (
    ogc_fid SERIAL PRIMARY KEY,
    geom GEOMETRY(LineString, 4326),
    name VARCHAR(255),
    type VARCHAR(100),
    -- 其他属性...
);

CREATE INDEX idx_geom ON beijing_roads USING GIST(geom);
```

---

## 配置说明

### application.yml

```yaml
spring:
  profiles:
    active: dmk # 使用 application-dmk.yml
```

### application-dmk.yml

```yaml
server:
  port: 8765

spring:
  data:
    mongodb:
      database: panoramaTW
      uri: mongodb://localhost:27017/panoramaTW
  datasource:
    default:
      url: jdbc:postgresql://localhost:5432/panoramaTW
      username: postgres
      password: your_password
  servlet:
    multipart:
      max-file-size: 1GB
      max-request-size: 1GB

path:
  temp: C:\path\to\data\temp
  static: C:\path\to\data\static
  3DTile: C:\path\to\data\3DTile
```

---

## 故障排查

### 1. 端口被占用

```
Port 8765 is already in use
```

**解决**：

```bash
netstat -ano | findstr :8765
taskkill /PID <PID> /F
```

### 2. MongoDB 连接失败

```
MongoTimeoutException: Timed out after 30000 ms
```

**解决**：

- 检查 MongoDB 是否运行：`mongo`
- 检查连接字符串：`mongodb://localhost:27017/panoramaTW`

### 3. PostgreSQL 连接失败

```
PSQLException: Connection refused
```

**解决**：

- 检查 PostgreSQL 是否运行：`psql -U postgres`
- 确认数据库已创建：`CREATE DATABASE panoramaTW;`
- 启用 PostGIS：`CREATE EXTENSION postgis;`

---

## 部署

### 生产构建

```bash
./mvnw clean package -DskipTests
```

### 运行

```bash
java -jar target/panoramaTW-back-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8765
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 许可证

MIT License
