# OpenMemory

OpenMemory 是您为大型语言模型（LLM）打造的个性化记忆层——私有、便携且开源。您的记忆数据完全本地存储，赋予您对数据的完全控制权。通过构建具备个性化记忆的AI应用，同时保障数据安全。

![OpenMemory](https://github.com/user-attachments/assets/3c701757-ad82-4afa-bfbe-e049c2b4320b)

## 快速部署

### 前置条件
- Docker环境
- OpenAI API密钥

可通过以下命令快速启动：
```bash
curl -sL https://raw.githubusercontent.com/mem0ai/mem0/main/openmemory/run.sh | bash
```

设置全局环境变量：
```bash
export OPENAI_API_KEY=your_api_key
```

或通过脚本参数传递：
```bash
curl -sL https://raw.githubusercontent.com/mem0ai/mem0/main/openmemory/run.sh | OPENAI_API_KEY=your_api_key bash
```

## 环境配置

### 必要条件
- Docker与Docker Compose
- 后端开发需Python 3.9+
- 前端开发需Node.js
- OpenAI API密钥（用于LLM交互，需创建`api/.env`文件并配置）

## 快速入门

### 1. 配置环境变量
通过以下任一方式配置：
- **手动创建**：在`/api/`和`/ui/`目录下分别创建`.env`文件
- **复制示例文件**：
  ```bash
  cp api/.env.example api/.env
  cp ui/.env.example ui/.env
  ```
- **使用Makefile**：
  ```bash
  make env
  ```

示例配置：
```env
# api/.env
OPENAI_API_KEY=sk-xxx
USER=<用户ID>  # 关联记忆的用户标识
```
```env
# ui/.env
NEXT_PUBLIC_API_URL=http://localhost:8765
NEXT_PUBLIC_USER_ID=<用户ID>  # 需与API配置一致
```

### 2. 构建与运行
执行以下命令：
```bash
make build  # 构建MCP服务器和前端
make up     # 启动服务
```

服务启动后：
- MCP服务器运行于：http://localhost:8765（API网页：http://localhost:8765/docs）
- 前端界面运行于：http://localhost:3000

**若前端启动失败**，可尝试手动启动：
```bash
cd ui
pnpm install
pnpm dev
```

### MCP客户端配置
使用`install-mcp`工具连接客户端：
```bash
npx install-mcp http://localhost:8765/mcp/<客户端名称>/sse/<用户ID> --client <客户端名称>
```
替换`<客户端名称>`和`<用户ID>`为实际值。

## 项目架构
- `api/` - 后端API服务及MCP协议服务器
- `ui/` - 基于React的前端管理界面

## 贡献指南
我们是一支由开发者组成的团队，致力于推动AI与开源软件的未来发展。我们相信社区驱动的力量，并致力于构建更易访问、更个性化的AI工具。

### 贡献方式
- 错误报告与功能建议
- 网页优化
- 代码贡献
- 测试反馈
- 社区支持

### 贡献流程
1. Fork仓库
2. 创建特性分支（`git checkout -b openmemory/feature/amazing-feature`）
3. 提交更改（`git commit -m '添加超棒的功能'`）
4. 推送分支（`git push origin openmemory/feature/amazing-feature`）
5. 提交Pull Request

加入我们，共同构建AI记忆管理的未来！您的每一份贡献都将推动OpenMemory的发展。

---

### 技术特性解析（基于搜索结果）
1. **本地优先设计**：所有记忆数据存储在本地设备，通过Postgres和Qdrant实现结构化存储与向量检索，确保数据主权
2. **跨工具协作**：支持Cursor、Claude Desktop等MCP协议客户端，实现记忆的无缝共享（如代码开发与网页生成的上下文延续）
3. **智能记忆管理**：采用GPT-4o进行语义分类，结合向量数据库实现相关性检索，支持情感标记和时间轴追踪
4. **安全架构**：通过Docker容器隔离运行环境，所有数据库操作使用参数化查询防止注入攻击，审计日志记录所有访问行为