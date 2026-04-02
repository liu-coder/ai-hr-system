import json
import uuid
from datetime import datetime

# 流程定义
workflows = {
    "industrial": {
        "name": "工业级开发流程",
        "steps": [
            {
                "step_id": "req-mgmt",
                "step_name": "需求管理",
                "skill_name": "requirement-management",
                "params": {}
            },
            {
                "step_id": "task-decomp",
                "step_name": "任务拆分",
                "skill_name": "task-decomposer",
                "params": {}
            },
            {
                "step_id": "doc-req",
                "step_name": "需求文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "req"}
            },
            {
                "step_id": "arch-design",
                "step_name": "架构设计",
                "skill_name": "architecture-design",
                "params": {}
            },
            {
                "step_id": "doc-arch",
                "step_name": "架构文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "arch"}
            },
            {
                "step_id": "db-design",
                "step_name": "数据库设计",
                "skill_name": "database-design",
                "params": {}
            },
            {
                "step_id": "doc-db",
                "step_name": "数据库文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "db"}
            },
            {
                "step_id": "tdd",
                "step_name": "测试驱动开发",
                "skill_name": "test-driven-development",
                "params": {}
            },
            {
                "step_id": "doc-code",
                "step_name": "代码文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "code"}
            },
            {
                "step_id": "code-quality",
                "step_name": "代码质量检查",
                "skill_name": "code-quality-guardian",
                "params": {}
            },
            {
                "step_id": "security-audit",
                "step_name": "安全审计",
                "skill_name": "security-audit",
                "params": {}
            },
            {
                "step_id": "perf-tuning",
                "step_name": "性能调优",
                "skill_name": "performance-tuning",
                "params": {}
            },
            {
                "step_id": "doc-final",
                "step_name": "最终文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "final"}
            },
            {
                "step_id": "verification",
                "step_name": "完成前验证",
                "skill_name": "verification-before-completion",
                "params": {}
            }
        ]
    },
    "lightweight": {
        "name": "轻量开发流程",
        "steps": [
            {
                "step_id": "req-mgmt",
                "step_name": "需求管理",
                "skill_name": "requirement-management",
                "params": {}
            },
            {
                "step_id": "task-decomp",
                "step_name": "任务拆分",
                "skill_name": "task-decomposer",
                "params": {}
            },
            {
                "step_id": "tdd",
                "step_name": "测试驱动开发",
                "skill_name": "test-driven-development",
                "params": {}
            },
            {
                "step_id": "code-quality",
                "step_name": "代码质量检查",
                "skill_name": "code-quality-guardian",
                "params": {}
            },
            {
                "step_id": "doc-final",
                "step_name": "最终文档",
                "skill_name": "doc-coauthoring",
                "params": {"type": "final"}
            },
            {
                "step_id": "verification",
                "step_name": "完成前验证",
                "skill_name": "verification-before-completion",
                "params": {}
            }
        ]
    }
}

def analyze_requirement_complexity(requirement_description):
    """
    自动分析需求复杂性
    :param requirement_description: 需求描述
    :return: 需求类型：complex 或 simple
    """
    # 复杂需求关键词
    complex_keywords = [
        "开发", "新功能", "系统", "架构", "设计", "数据库", "集成",
        "重构", "优化", "升级", "迁移", "部署", "安全", "性能"
    ]
    
    # 简单需求关键词
    simple_keywords = [
        "修复", "bug", "错误", "问题", "缺陷", "修正", "调整",
        "更新", "修改", "改进", "优化", "完善"
    ]
    
    description = requirement_description.lower()
    
    # 计算关键词匹配数
    complex_count = sum(1 for keyword in complex_keywords if keyword in description)
    simple_count = sum(1 for keyword in simple_keywords if keyword in description)
    
    # 长度分析
    length = len(description)
    
    # 综合判断
    if complex_count > simple_count or length > 100:
        return "complex"
    else:
        return "simple"

def execute_workflow(requirement_description):
    """
    执行流程编排
    :param requirement_description: 需求描述
    :return: 执行结果
    """
    # 生成流程ID
    workflow_id = f"wf-{uuid.uuid4()}"
    
    # 自动分析需求复杂性
    requirement_type = analyze_requirement_complexity(requirement_description)
    print(f"自动分析需求类型: {requirement_type}")
    
    # 根据需求类型选择流程
    workflow_type = "industrial" if requirement_type == "complex" else "lightweight"
    workflow = workflows[workflow_type]
    
    print(f"开始执行 {workflow['name']} (ID: {workflow_id})")
    print(f"需求描述: {requirement_description}")
    
    # 执行流程步骤
    steps_executed = []
    overall_status = "success"
    
    for step in workflow['steps']:
        print(f"\n执行步骤: {step['step_name']} ({step['skill_name']})")
        
        # 构建技能执行参数
        skill_params = {
            "requirement_description": requirement_description,
            "workflow_id": workflow_id,
            "step_id": step['step_id']
        }
        skill_params.update(step['params'])
        
        try:
            # 这里模拟技能执行
            # 实际应用中，应该调用Trae的技能执行API
            result = {
                "status": "success",
                "message": f"执行 {step['skill_name']} 成功",
                "data": {
                    "executed_at": datetime.now().isoformat(),
                    "params": skill_params
                }
            }
            
            step_result = {
                "step_id": step['step_id'],
                "step_name": step['step_name'],
                "skill_name": step['skill_name'],
                "status": "success",
                "result": result
            }
            
            print(f"步骤执行成功: {step['step_name']}")
            
        except Exception as e:
            # 处理执行失败的情况
            result = {
                "status": "failed",
                "message": f"执行 {step['skill_name']} 失败: {str(e)}"
            }
            
            step_result = {
                "step_id": step['step_id'],
                "step_name": step['step_name'],
                "skill_name": step['skill_name'],
                "status": "failed",
                "result": result
            }
            
            overall_status = "failed"
            print(f"步骤执行失败: {step['step_name']} - {str(e)}")
        
        steps_executed.append(step_result)
    
    # 构建输出结果
    output = {
        "workflow_id": workflow_id,
        "workflow_type": workflow_type,
        "steps_executed": steps_executed,
        "overall_status": overall_status
    }
    
    print(f"\n流程执行完成，状态: {overall_status}")
    return output

def main(input_data):
    """
    主函数
    :param input_data: 输入数据
    :return: 输出结果
    """
    try:
        # 解析输入数据
        requirement_description = input_data.get("requirement_description")
        
        if not requirement_description:
            return {
                "error": "Missing required parameter: requirement_description"
            }
        
        # 执行流程
        result = execute_workflow(requirement_description)
        return result
        
    except Exception as e:
        return {
            "error": f"Error executing workflow: {str(e)}"
        }

if __name__ == "__main__":
    # 测试代码 - 复杂需求
    test_input_complex = {
        "requirement_description": "开发一个新的员工考勤管理系统，包含打卡、请假、排班等功能"
    }
    
    print("=== 测试复杂需求 ===")
    result_complex = main(test_input_complex)
    print(json.dumps(result_complex, indent=2, ensure_ascii=False))
    
    # 测试代码 - 简单需求
    test_input_simple = {
        "requirement_description": "修复员工考勤系统中的打卡记录显示错误"
    }
    
    print("\n=== 测试简单需求 ===")
    result_simple = main(test_input_simple)
    print(json.dumps(result_simple, indent=2, ensure_ascii=False))
