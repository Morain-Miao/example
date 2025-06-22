# OpenMemory API

本目录包含基于 FastAPI 和 SQLAlchemy 构建的 OpenMemory 后端 API 服务，同时运行 Mem0 MCP 服务器，支持与 MCP 客户端进行记忆交互。

## 快速启动（推荐 Docker 方式）
### 前置条件
- Docker 环境
- Docker Compose

通过以下命令快速启动：
```bash
make build    # 构建容器镜像
make env      # 生成环境配置文件
make up       # 启动服务
```
启动后 API 服务将运行在：http://localhost:8765

## 常用 Docker 命令
| 命令          | 功能说明                     |
|---------------|----------------------------|
| `make logs`   | 查看服务日志                 |
| `make shell`  | 进入容器终端                 |
| `make migrate`| 执行数据库迁移               |
| `make test`   | 运行测试套件                 |
| `make down`   | 停止并移除容器               |

## API 网页
服务启动后可通过以下方式访问：
- Swagger 网页版：http://localhost:8765/docs
- ReDoc 网页版：http://localhost:8765/redoc

## 项目结构
```bash
├── app/                # 主应用代码
│   ├── models.py       # 数据库模型定义
│   ├── database.py     # 数据库连接配置
│   ├── routers/        # API 路由处理
│   ├── migrations/     # 数据库迁移文件
│   └── tests/          # 测试用例
├── alembic/            # Alembic 迁移配置
└── main.py             # 应用入口文件
```

## 开发规范
1. **代码风格**：遵循 PEP 8 编码规范
2. **类型系统**：使用 Python 类型提示
3. **测试覆盖**：新增功能需编写单元测试
4. **网页同步**：代码变更同步更新 API 网页
5. **数据库变更**：结构修改需通过 Alembic 迁移

## 技术特性（基于搜索结果）
1. **本地化记忆存储**：所有数据通过 Docker 容器运行在本地，采用 PostgreSQL 结构化存储 + Qdrant 向量数据库实现记忆持久化
2. **跨工具协作**：支持 MCP 协议客户端（如 Cursor/Claude/Windsurf）实现记忆共享，突破传统 AI 工具的会话隔离限制
3. **安全架构**：通过参数化查询防止 SQL 注入，审计日志记录所有记忆操作，支持用户级访问控制
4. **智能检索**：基于 GPT-4o 的语义分类 + 向量相似度搜索，实现精准记忆定位
5. **扩展能力**：提供 REST API 和 MCP 协议双接口，支持自定义记忆分类标签和情感标记