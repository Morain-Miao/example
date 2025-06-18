# 中国导游助手

中国导游助手是一个面向导游和游客的智能导览平台，集成了智能问答、行程推荐、地图服务等功能，旨在提升旅游体验和导游服务效率。

---

## 项目简介

本项目致力于为中国旅游行业提供一站式智能导游解决方案，支持景点讲解、路线规划、实时问答等多种场景，适用于导游、游客及相关从业者。

---

## 主要功能

- 智能聊天问答（AI导游）
- 景点信息查询与讲解
- 行程路线智能推荐
- 地图与导航服务集成
- 用户注册与登录

---

## 技术栈

- **前端**：React、TypeScript、Ant Design X
- **后端**：Spring Boot、Java
- **数据库**：MySQL
- **地图服务**：百度地图API

---

## 项目结构

### 目录说明

#### 后端目录 (`backend/`)
- `modules/`: 核心业务模块
  - `bom/`: 基础对象模型模块
  - `chat/`: 聊天服务模块
    - `domain/`: 领域模型层
      - `entity/`: 数据库实体对象
      - `bo/`: 业务对象和值对象
      - `service/`: 领域服务接口
    - `Infrastructure/`: 基础设施层
      - `repository/`: 数据访问层
      - `external/`: 外部服务集成
  - `dependencies/`: 项目依赖管理
  - `parent/`: 父级模块配置

- `services/`: 应用服务层
  - `chat-agent/`: 聊天代理服务
    - `Application/`: 应用服务实现
    - `Controller/`: API控制器
    - `request/`: 请求对象
    - `dto/`: 数据传输对象

#### 前端目录 (`web/`)
- `public/`: 静态资源目录
- `src/`: 源代码目录
  - `apis/`: API接口定义
  - `app/`: 应用模块
    - `(auth)/`: 认证相关模块
      - `api/`: 认证API
      - `login/`: 登录页面
      - `register/`: 注册页面
    - `(chat)/`: 聊天相关模块
      - `api/`: 聊天API
      - `chat/`: 聊天页面
  - `components/`: 公共组件
    - `provider/`: 上下文提供者
  - `utils/`: 工具函数

```text
├─ backend                # 后端App目录
│  ├─ modules             # 后端模块
│  │  ├─ bom              # 基础对象模型模块
│  │  ├─ chat             # 聊天功能核心模块
│  │  │   ├─ domain              # 领域模型
│  │  │   │   ├─ entity/         # 数据库对象实体
│  │  │   │   ├─ bo/             # 领域内的业务对象/值对象
│  │  │   │   └─ service/        # 领域内的repository的Service
│  │  │   │       └─ impl/       # service的实现类
│  │  │   └─ infrastructure/     # 基础设施层
│  │  │       ├─ repository      # Jpa的repository对象
│  │  │       └─ external        # 外部服务集成，如百度地图
│  │  ├─ dependencies            # 依赖管理
│  │  └─ parent                  # 公共父级模块
│  └─ services                   # 应用服务模块
│      └─ chat-agent             # 聊天Agent服务
│           ├─ Application/      # 应用层代码
│           ├─ Controller/       # 控制层代码
│           ├─ request/          # 请求参数
│           └─ dto/              # 传输对象
└─ web                    # 前端app目录
    ├─ public                    # 静态资源目录
    └─ src
        ├─ apis                     # API接口定义
        ├─ app                      # 应用模块
        │   ├─ (auth)               # 认证模块
        │   │   ├─ api              # 认证API
        │   │   │   └─ auth         # 认证
        │   │   │       └─ login    # 登录认证
        │   │   ├─ login            # 登录页面
        │   │   └─ register         # 注册
        │   └─ (chat)               # 聊天模块
        │       ├─ api              # 聊天API
        │       │   ├─ chat         # 聊天接口
        │       │   └─ message      # 消息接口
        │       └─ chat             # 聊天页面
        ├─ components               # 前端组件
        │   └─ provider
        └─ utils                    # 工具模块
```

---

## 快速开始

### 1. 克隆项目
```bash
git clone <项目地址>
```

### 2. 启动后端
```bash
cd backend
# 根据实际情况启动 Spring Boot 项目
```

### 3. 启动前端
```bash
cd web
npm install
npm start
```

---

## 联系方式

如有问题或建议，欢迎联系项目维护者：
- 邮箱：your_email@example.com
- GitHub Issues

---

## License

MIT




