# 工具分类与模式 × 工具矩阵（v2 — 多 Agent 架构）

本文档是 data-agent-server-app 工具体系的单一事实来源：**Agent 类型 × 工具归属**、**模式过滤矩阵**，与 `AgentToolConfig` 保持一致。

> v2 架构说明：MainAgent 不再直接使用 DiscoveryTool，而是通过 `callingExplorerSubAgent` 和 `callingSqlPlannerSubAgent` 编排 SubAgent。
> MainAgent Prompt 不再包含 `<tool-mastery>` 块，工具在决策阶段中自然出现。

## 1. 多 Agent 架构概览

| Agent 类型 | 角色 | 职责 |
|-----------|------|------|
| **MainAgent** | 编排者 | 理解用户意图，编排 SubAgent，执行 SQL，用户交互 |
| **Explorer** | SubAgent | 探索数据库结构，输出 SchemaSummary |
| **SqlPlanner** | SubAgent | 接收 SchemaSummary，生成 SqlPlan |

## 2. 工具归属矩阵（AgentType × Tool）

### 2.1 MainAgent 工具

| 分类 | 工具名 | 说明 |
|------|--------|------|
| **编排** | callingExplorerSubAgent | 调用 Explorer 检索 schema |
| **编排** | callingSqlPlannerSubAgent | 调用 SqlPlanner 生成 SQL 计划 |
| **执行** | executeSelectSql | 执行只读 SQL（SELECT） |
| **执行** | executeNonSelectSql | 执行写操作；必须先 askUserConfirm |
| **用户交互** | askUserQuestion | 向用户提问并给出选项 |
| **用户交互** | askUserConfirm | 写操作前的强制确认 |
| **推理与任务** | todoWrite | 多步任务进度追踪 |
| **计划** | enterPlanMode | 进入 Plan 模式 |
| **计划** | exitPlanMode | 退出 Plan 模式并交付计划 |
| **呈现** | renderChart | 渲染图表 |
| **技能** | activateSkill | 加载专家规则（chart, sql-optimization） |

### 2.2 Explorer SubAgent 工具

| 分类 | 工具名 | 说明 |
|------|--------|------|
| **发现** | getEnvironmentOverview | 获取所有连接、数据库、schema 全景 |
| **发现** | searchObjects | 按模式搜索表/视图/函数等 |
| **发现** | getObjectDetail | 批量获取对象 DDL、行数、索引 |

### 2.3 SqlPlanner SubAgent 工具

| 分类 | 工具名 | 说明 |
|------|--------|------|
| **任务** | todoWrite | SQL 生成步骤追踪 |
| **技能** | activateSkill | 加载 sql-optimization 规则 |

## 3. 模式 × 工具过滤矩阵（仅 MainAgent）

模式过滤仅作用于 MainAgent，SubAgent 不受模式影响。

过滤逻辑见 `AgentToolConfig.filterTools(agentTools, mode)`：按**工具类**禁用。

### 3.1 AGENT 模式（默认）

- **可用**：除 exitPlanMode 外的所有 MainAgent 工具
- **禁用**：exitPlanMode

### 3.2 PLAN 模式

- **可用**：callingExplorerSubAgent, callingSqlPlannerSubAgent, askUserQuestion, todoWrite, enterPlanMode, exitPlanMode, activateSkill
- **禁用**（整类移除）：
  - ExecuteSqlTool：executeSelectSql, executeNonSelectSql
  - ChartTool：renderChart
  - AskUserConfirmTool：askUserConfirm

### 3.3 矩阵速查

| 工具名 | Agent 归属 | AGENT 模式 | PLAN 模式 |
|--------|-----------|-----------|----------|
| callingExplorerSubAgent | MainAgent | ✓ | ✓ |
| callingSqlPlannerSubAgent | MainAgent | ✓ | ✓ |
| executeSelectSql | MainAgent | ✓ | ✗ |
| executeNonSelectSql | MainAgent | ✓ | ✗ |
| askUserQuestion | MainAgent | ✓ | ✓ |
| askUserConfirm | MainAgent | ✓ | ✗ |
| todoWrite | MainAgent / SqlPlanner | ✓ | ✓ |
| enterPlanMode | MainAgent | ✓ | ✓ |
| exitPlanMode | MainAgent | ✗ | ✓ |
| renderChart | MainAgent | ✓ | ✗ |
| activateSkill | MainAgent / SqlPlanner | ✓ | ✓ |
| getEnvironmentOverview | Explorer | — | — |
| searchObjects | Explorer | — | — |
| getObjectDetail | Explorer | — | — |

> Explorer 和 SqlPlanner 的工具不受 AGENT/PLAN 模式过滤，因为它们由 MainAgent 内部编排调用。

## 4. SubAgent 编排规则

SubAgent 调用通过 `callingExplorerSubAgent` 和 `callingSqlPlannerSubAgent` 两个工具，规则定义在 main-agent.xml 的 `<subAgent-calling-rule>` 中。

| 规则项 | EXPLORER | SQL_PLANNER |
|--------|----------|-------------|
| 触发条件 | 需要了解数据库结构 | 已有 SchemaSummary + 需生成 SQL |
| 禁止条件 | 已有充分 schema 信息 | 无 SchemaSummary |
| 依赖链 | 无前置依赖 | 依赖 Explorer 输出 |
| 循环上限 | maxExplorerLoop (默认 3) | — |
| 并发限制 | 最多 2 个 (connectionIds) | 不支持并发 |
| 降级策略 | 超时/失败 → 告知用户 | 超时/失败 → 告知用户 |

## 5. 与代码的对应关系

| 配置项 | 代码位置 |
|--------|---------|
| Agent 类型定义 | `AgentTypeEnum` |
| 按 AgentType 过滤工具 | `AgentToolConfig.filterToolsByAgentType()` |
| 按 AgentMode 过滤工具 | `AgentToolConfig.filterTools()` |
| SubAgent 配置 | `SubAgentProperties` |
| 链路追踪 | `SubAgentTraceContext` / `SubAgentSpan` |
| SubAgent 调用规则 | `main-agent.xml` 内 `<subAgent-calling-rule>` |
| MainAgent Prompt | `prompt/main-agent.xml`（无 `<tool-mastery>`） |
| Explorer Prompt | `prompt/explorer.xml` |
| SqlPlanner Prompt | `prompt/sql-planner.xml` |

更新本文档时请同步更新 `AgentToolConfig` 与 Prompt 文件。
