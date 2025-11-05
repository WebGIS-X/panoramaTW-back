# panoramaTW-back 配置说明

**注意**：本文档仅用于说明后端配置，实际使用中**请勿修改后端代码和配置文件**。

前端代码已完全适配现有的后端配置（`application-dmk.yml`），无需修改后端。

## 当前后端配置

后端使用 `application-dmk.yml` 配置文件：

- **端口**：8765
- **API 前缀**：/api/v0
- **数据库**：PostgreSQL (panoramaTW) + MongoDB (panoramaTW)
- **CORS**：已配置允许所有来源

## 前端适配

前端通过环境变量连接到后端：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8765
```

如果后端配置发生变化，只需更新前端的 `.env.local` 文件即可。

## 环境变量配置

### 方式一：使用 .env 文件（推荐）

1. 复制 `.env.example` 为 `.env`：

   ```bash
   cp .env.example .env
   ```

2. 编辑 `.env` 文件，设置你的配置值

3. 在启动应用前加载环境变量（IDE 或命令行）

### 方式二：直接设置环境变量

#### Windows (PowerShell)

```powershell
$env:SERVER_PORT="8765"
$env:DATASOURCE_PASSWORD="your_password"
# ... 其他变量
```

#### Windows (CMD)

```cmd
set SERVER_PORT=8765
set DATASOURCE_PASSWORD=your_password
# ... 其他变量
```

#### Linux/Mac

```bash
export SERVER_PORT=8765
export DATASOURCE_PASSWORD=your_password
# ... 其他变量
```

### 方式三：IDE 配置（IntelliJ IDEA）

1. 打开 `Run` → `Edit Configurations`
2. 选择你的运行配置
3. 在 `Environment variables` 中添加变量，格式：`KEY1=value1;KEY2=value2`

## 必需配置项

### 数据库配置

#### PostgreSQL

```yaml
spring:
  datasource:
    default:
      url: jdbc:postgresql://localhost:5432/panoramaTW
      username: postgres
      password: your_password
```

#### MongoDB

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/panoramaTW
      database: panoramaTW
```

### 文件路径配置

确保以下目录存在或配置为可访问的路径：

- `path.temp` - 临时文件目录
- `path.rasterTile` - 栅格瓦片存储目录
- `path.rasterFile` - 栅格文件存储目录
- `path.static` - 静态文件存储目录
- `path.3DTile` - 3D 瓦片存储目录

**注意**: Windows 路径使用反斜杠 `\`，Linux/Mac 使用正斜杠 `/`

## 运行应用

### 开发环境

默认使用 `dev` profile：

```bash
mvn spring-boot:run
```

或指定 profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产环境

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

或打包后运行：

```bash
mvn clean package
java -jar target/backEnd-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 端口配置

默认端口：`8765`

可通过环境变量 `SERVER_PORT` 或配置文件修改。

## CORS 配置

后端已配置 CORS，允许所有来源访问（开发环境）。生产环境建议限制允许的来源。

配置文件位置：`src/main/java/com/panorama/backend/config/CorsConfig.java`

## 常见问题

### 1. 数据库连接失败

- 检查 PostgreSQL/MongoDB 服务是否运行
- 验证连接 URL、用户名、密码是否正确
- 检查防火墙设置

### 2. 文件路径错误

- 确保配置的路径存在
- Windows 路径使用双反斜杠或正斜杠：`H:\\data\\temp` 或 `H:/data/temp`
- 确保应用有权限访问这些目录

### 3. 端口被占用

- 修改 `SERVER_PORT` 环境变量
- 或修改配置文件中的端口号

### 4. 文件上传大小限制

默认最大文件大小为 1GB，可通过 `MULTIPART_MAX_FILE_SIZE` 和 `MULTIPART_MAX_REQUEST_SIZE` 调整。

## 环境变量列表

| 变量名                       | 说明                | 默认值                                      |
| ---------------------------- | ------------------- | ------------------------------------------- |
| `SERVER_PORT`                | 服务器端口          | 8765                                        |
| `SPRING_PROFILES_ACTIVE`     | Spring Profile      | dev                                         |
| `MONGODB_DATABASE`           | MongoDB 数据库名    | panoramaTW                                  |
| `MONGODB_AUTH_DATABASE`      | MongoDB 认证数据库  | admin                                       |
| `MONGODB_URI`                | MongoDB 连接 URI    | mongodb://localhost:27017/panoramaTW        |
| `DATASOURCE_URL`             | PostgreSQL 连接 URL | jdbc:postgresql://localhost:5432/panoramaTW |
| `DATASOURCE_USERNAME`        | PostgreSQL 用户名   | postgres                                    |
| `DATASOURCE_PASSWORD`        | PostgreSQL 密码     | 无默认值                                    |
| `MULTIPART_MAX_FILE_SIZE`    | 最大文件大小        | 1GB                                         |
| `MULTIPART_MAX_REQUEST_SIZE` | 最大请求大小        | 1GB                                         |
| `PATH_TEMP`                  | 临时文件目录        | ./data/temp                                 |
| `PATH_RASTER_TILE`           | 栅格瓦片目录        | ./data/raster/rasterTile                    |
| `PATH_RASTER_FILE`           | 栅格文件目录        | ./data/raster/rasterFile                    |
| `PATH_STATIC`                | 静态文件目录        | ./data/static                               |
| `PATH_3DTILE`                | 3D 瓦片目录         | ./data/3DTile                               |

## 与前端集成

前端 web-front 需要配置以下环境变量：

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8765
```

确保后端端口与前端配置的端口一致。
