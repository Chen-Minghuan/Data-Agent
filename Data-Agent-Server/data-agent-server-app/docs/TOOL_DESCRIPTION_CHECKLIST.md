# 工具描述检查清单

与《系统提示词与工具设计方法论》中的工具描述范式对齐。每个工具在 @Tool 描述及本清单中应满足以下项。

## 检查项

- **能力句**：首句一句话说清「这个工具做什么」。
- **When to Use**：何时应调用（1～2 句）。
- **与其它工具关系**：优先顺序或替代关系（如「先 X 再本工具」「不要用本工具替代 Y」）。
- **参数约定**：必填/可选、路径/范围（如 connectionId → databaseName → schemaName、objectNamePattern 的 `%`）、默认与上限。
- **示例**（可选）：易误用工具可带 1 个 Good/BAD 示例或简短 reasoning。

## 与 prompt 的交叉对照

- `<tool-mastery>` / `<tool-usage>` 中的决策路径、使用顺序应与各工具的 When to Use 及关系一致。
- 参数层级约定（connectionId → databaseName → schemaName、objectNamePattern）在 prompt 的 `<tool-usage>` 或 `<tool-mastery>` 中统一写一次，工具描述中可引用。

## 工具逐项对照表

| 工具名 | 能力句 | When to Use | 与它工具关系 | 参数约定 |
|--------|--------|-------------|--------------|----------|
| getEnvironmentOverview | ✓ | 环境未知、新请求开始时 | 发现入口；先于 searchObjects | 无参数 |
| searchObjects | ✓ | 按名称/模式找表或对象 | 在 getEnvironmentOverview 之后缩小范围；先于 getObjectDetail | objectNamePattern 用 %；层级 connectionId→databaseName→schemaName |
| getObjectDetail | ✓ | 写 SQL 前获取 DDL/行数/索引 | 在 searchObjects 或确认目标后；多对象一次传入 | objects 列表；每项含 connectionId,databaseName,schemaName,objectName,objectType |
| executeSelectSql | ✓ | 执行只读 SQL | 先 getObjectDetail 再写 SQL；多语句一次传 | connectionId,databaseName,schemaName,sqls |
| executeNonSelectSql | ✓ | 执行写 SQL（已确认） | 必须且仅在 askUserConfirm 之后 | 同上 + 需用户确认 |
| askUserQuestion | ✓ | 多候选需用户选择时 | 在 searchObjects 发现多候选后 | questions 列表，每项 2～3 选项 |
| askUserConfirm | ✓ | 任何写操作执行前 | 写操作前必须调用；之后才能 executeNonSelectSql | sql, connectionId, databaseName, schemaName, explanation |
| todoWrite | ✓ | 多步任务（3+ 步） | 与 workflow 进度同步 | action, todoId, items |
| enterPlanMode | ✓ | 复杂/写/多表需规划时 | 规划阶段用；之后 exitPlanMode 交付 | reason, triggerSignal |
| exitPlanMode | ✓ | 规划完成交付计划时 | 与 enterPlanMode 配对 | title, steps |
| renderChart | ✓ | 数据就绪需可视化时 | 在 executeSelectSql 有结果之后；图表即终局 | chartType, optionJson, description |
| activateSkill | ✓ | 某能力首次使用前加载规则 | 如 chart 制图前 | skillName（如 chart） |

更新 @Tool 描述或 prompt 工具块时，请同步更新本表。
