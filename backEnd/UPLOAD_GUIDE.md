# 后端代码 Git 上传指南

## 1. 初始化 Git 仓库

```bash
cd panoramaTW-back/backEnd
git init
```

## 2. 创建 .gitignore 文件

```bash
cat > .gitignore << 'EOF'
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties
.mvn/wrapper/maven-wrapper.jar

# Eclipse
.classpath
.project
.settings/
bin/

# IntelliJ IDEA
.idea/
*.iml
*.iws
*.ipr
out/

# NetBeans
nbproject/
build/
nbbuild/
dist/
nbdist/
.nb-gradle/

# VS Code
.vscode/

# Log files
*.log

# Spring Boot
/BOOT-INF/
/META-INF/

# Application properties (含敏感信息)
application-*.yml
!application.yml
*.properties
!application.properties

# OS
.DS_Store
Thumbs.db
*~

# Temporary files
*.tmp
*.bak
*.swp
EOF
```

## 3. 创建新分支并提交代码

```bash
# 添加所有文件
git add .

# 查看将要提交的文件
git status

# 提交代码
git commit -m "feat: 添加 Dataset Node 编辑 API

- 新增 getNode 和 updateNode 接口
- 支持更新 LayerNode 的 usage 字段
- 实现完整替换逻辑，支持删除和修改
- 优化 updateLayer 方法"

# 创建并切换到新分支
git checkout -b feature/dataset-node-edit-api
```

## 4. 关联远程仓库并推送

```bash
# 关联远程仓库（替换为你的 GitHub 仓库地址）
git remote add origin https://github.com/your-username/panoramaTW-back.git

# 推送到远程仓库
git push -u origin feature/dataset-node-edit-api

# 如果是第一次推送，也可以推送到 main 分支
git branch -M main
git push -u origin main
```

## 5. 重要提示

⚠️ **注意事项**：

1. **配置文件安全**：

   - 不要上传包含数据库密码的配置文件
   - 创建 `application.yml.example` 作为模板
   - 在 README 中说明如何配置

2. **敏感信息**：

   - 删除所有硬编码的密码
   - 使用环境变量或配置管理

3. **数据文件**：
   - 不要上传数据库文件
   - 不要上传日志文件

## 6. 创建配置文件模板

```bash
# 创建配置文件示例
cp src/main/resources/application-dmk.yml src/main/resources/application.yml.example

# 编辑 application.yml.example，移除敏感信息
```

示例 `application.yml.example`：

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
      driver-class-name: org.postgresql.Driver
      url: jdbc:postgresql://localhost:5432/panoramaTW
      username: your_username
      password: your_password # 修改为你的密码
```

## 常用命令

```bash
# 查看当前分支
git branch

# 切换分支
git checkout branch-name

# 查看提交历史
git log --oneline

# 查看远程仓库
git remote -v

# 拉取最新代码
git pull origin main

# Maven 打包（提交前测试）
./mvnw clean package
```

