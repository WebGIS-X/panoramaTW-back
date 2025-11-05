# panoramaTW-back

panoramaTW 后端服务，基于 Spring Boot 3.3.2。

## 快速开始

### 前置要求

- Java 17+
- Maven 3.6+
- PostgreSQL 数据库
- MongoDB 数据库

### 配置

1. 查看 `README_CONFIG.md` 了解详细配置说明
2. 复制 `env.example` 中的环境变量配置
3. 根据你的环境设置环境变量或修改配置文件

### 运行

```bash
# 开发环境
mvn spring-boot:run

# 或指定 profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

默认端口：`8765`

## 配置说明

详细配置说明请查看 [README_CONFIG.md](./README_CONFIG.md)

## API 文档

后端提供以下主要 API：

- `/api/v0/node/layerNode/*` - 图层节点管理
- `/api/v0/resource/vector/*` - 矢量瓦片服务
- `/api/v0/resource/raster/*` - 栅格瓦片服务
- `/api/v0/resource/static/*` - 静态文件服务
- `/api/v0/resource/poi/*` - POI 管理
- `/api/v0/workflow/*` - 工作流服务

## 前端集成

前端 web-front 已配置为连接此后端，确保：

1. 后端运行在端口 8765（或配置前端环境变量 `NEXT_PUBLIC_API_BASE_URL`）
2. 数据库服务正常运行
3. CORS 已正确配置（开发环境允许所有来源）
