# workflow-automator 技能

## 功能描述

workflow-automator 是一个流程编排自动化工具，根据需求类型自动选择工业级或轻量流程，并按顺序执行相关技能。

## 支持的流程

### 工业级流程（适用于复杂需求）
1. requirement-management（需求管理）
2. task-decomposer（任务拆分）
3. doc-coauthoring:req（需求文档）
4. architecture-design（架构设计）
5. doc-coauthoring:arch（架构文档）
6. database-design（数据库设计）
7. doc-coauthoring:db（数据库文档）
8. test-driven-development（测试驱动开发）
9. doc-coauthoring:code（代码文档）
10. code-quality-guardian（代码质量检查）
11. security-audit（安全审计）
12. performance-tuning（性能调优）
13. doc-coauthoring:final（最终文档）
14. verification-before-completion（完成前验证）

### 轻量流程（适用于Bug修复和小功能）
1. requirement-management（需求管理）
2. task-decomposer（任务拆分）
3. test-driven-development（测试驱动开发）
4. code-quality-guardian（代码质量检查）
5. doc-coauthoring:final（最终文档）
6. verification-before-completion（完成前验证）

## 输入参数

| 参数名 | 类型 | 描述 | 必填 |
|--------|------|------|------|
| requirement_type | string | 需求类型：complex（复杂需求）或 simple（简单需求/Bug修复） | 是 |
| requirement_description | string | 需求描述 | 是 |

## 输出结果

| 字段名 | 类型 | 描述 |
|--------|------|------|
| workflow_id | string | 流程ID |
| workflow_type | string | 流程类型：industrial（工业级）或 lightweight（轻量） |
| steps_executed | array | 执行的步骤列表 |
| overall_status | string | 整体执行状态：success 或 failed |

## 使用示例

### 复杂需求示例

输入：
```json
{
  "requirement_type": "complex",
  "requirement_description": "开发一个新的员工考勤管理系统，包含打卡、请假、排班等功能"
}
```

输出：
```json
{
  "workflow_id": "wf-12345678-1234-1234-1234-1234567890ab",
  "workflow_type": "industrial",
  "steps_executed": [
    {
      "step_id": "req-mgmt",
      "step_name": "需求管理",
      "skill_name": "requirement-management",
      "status": "success",
      "result": {
        "status": "success",
        "message": "执行 requirement-management 成功",
        "data": {
          "executed_at": "2026-03-30T12:00:00",
          "params": {
            "requirement_description": "开发一个新的员工考勤管理系统，包含打卡、请假、排班等功能",
            "workflow_id": "wf-12345678-1234-1234-1234-1234567890ab",
            "step_id": "req-mgmt"
          }
        }
      }
    },
    // 其他步骤...
  ],
  "overall_status": "success"
}
```

### 简单需求示例

输入：
```json
{
  "requirement_description": "修复员工考勤系统中的打卡记录显示错误"
}
```

输出：
```json
{
  "workflow_id": "wf-87654321-4321-4321-4321-ba0987654321",
  "workflow_type": "lightweight",
  "steps_executed": [
    {
      "step_id": "req-mgmt",
      "step_name": "需求管理",
      "skill_name": "requirement-management",
      "status": "success",
      "result": {
        "status": "success",
        "message": "执行 requirement-management 成功",
        "data": {
          "executed_at": "2026-03-30T12:00:00",
          "params": {
            "requirement_description": "修复员工考勤系统中的打卡记录显示错误",
            "workflow_id": "wf-87654321-4321-4321-4321-ba0987654321",
            "step_id": "req-mgmt"
          }
        }
      }
    },
    // 其他步骤...
  ],
  "overall_status": "success"
}
```

## 安装和使用

1. 将此技能目录复制到 Trae 的技能目录中
2. 在 Trae 中激活此技能
3. 通过 Trae 的 API 或界面调用此技能

## 注意事项

1. 此技能需要依赖其他相关技能才能正常执行，包括：
   - requirement-management
   - task-decomposer
   - test-driven-development
   - doc-coauthoring
   - verification-before-completion
   - architecture-design（工业级流程）
   - database-design（工业级流程）
   - code-quality-guardian
   - security-audit（工业级流程）
   - performance-tuning（工业级流程）

2. 实际执行时，技能会调用 Trae 的技能执行 API 来执行各个步骤

3. 流程执行过程中会记录详细的日志，便于追踪和调试
