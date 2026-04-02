import os
import json
import time
import importlib
from datetime import datetime

# ===================== 【全局配置】 =====================
# 技能配置文件路径（支持环境变量覆盖）
SKILLS_CONFIG_PATH = os.getenv('SKILLS_CONFIG_PATH', 
                               os.path.join(os.path.dirname(__file__), 'skills_config.json'))

class SkillManager:
    """技能管理器：加载并管理全局技能配置"""
    def __init__(self):
        self.skills = {}
        self.load_skills()
    
    def load_skills(self):
        """从配置文件加载技能配置"""
        if os.path.exists(SKILLS_CONFIG_PATH):
            try:
                with open(SKILLS_CONFIG_PATH, 'r', encoding='utf-8') as f:
                    config = json.load(f)
                    self.skills = config.get('skills', {})
            except Exception as e:
                print(f"⚠️ 技能配置加载失败: {str(e)}")
        else:
            # 创建默认技能配置
            self.skills = {
                "task-decomposer": {
                    "name": "任务拆分",
                    "module": "skills.task_decomposer",
                    "class": "TaskDecomposer"
                },
                "test-driven-development": {
                    "name": "测试驱动开发",
                    "module": "skills.tdd",
                    "class": "TDDHandler"
                },
                "ai-code-generator": {
                    "name": "AI代码生成",
                    "module": "skills.ai_generator",
                    "class": "AICodeGenerator"
                }
            }
            self.save_default_config()
    
    def save_default_config(self):
        """保存默认配置到文件"""
        with open(SKILLS_CONFIG_PATH, 'w', encoding='utf-8') as f:
            json.dump({"skills": self.skills}, f, ensure_ascii=False, indent=2)
    
    def get_skill(self, skill_id):
        """获取技能配置"""
        return self.skills.get(skill_id)

# ===================== 【流程编排核心】 =====================
class WorkflowOrchestrator:
    """工作流编排器：通用流程编排逻辑"""
    def __init__(self):
        self.skill_manager = SkillManager()
        self.requirements_dir = os.path.join(os.path.dirname(__file__), 'requirements')
        os.makedirs(self.requirements_dir, exist_ok=True)
        
        # 动态加载技能映射规则
        self.SKILL_MAPPING = self.load_skill_mapping()
        
        # 状态跟踪
        self.missing_skills = set()
        self.executed_skills = set()
    
    def load_skill_mapping(self):
        """加载技能映射规则（支持动态配置）"""
        return {
            "开发": ["task-decomposer", "test-driven-development", "ai-code-generator"],
            "实现": ["task-decomposer", "test-driven-development", "ai-code-generator"],
            "创建": ["task-decomposer", "ai-code-generator"],
            "生成": ["ai-code-generator"]
        }
    
    def _log(self, content):
        """日志记录"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_line = f"[{timestamp}] {content}\n"
        print(log_line.strip())
        
        # 写入日志文件
        log_file = os.path.join(self.requirements_dir, f"workflow_{datetime.now().strftime('%Y%m%d')}.log")
        with open(log_file, "a", encoding="utf-8") as f:
            f.write(log_line)
    
    def identify_requirement(self, user_input):
        """需求识别"""
        req_id = f"req_{int(time.time())}"
        requirement = {
            "id": req_id,
            "title": user_input[:50],
            "description": user_input,
            "skills_required": self._identify_required_skills(user_input),
            "created_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        }
        self._log(f"📌 识别需求：ID={req_id}，内容={user_input[:50]}...")
        return requirement
    
    def _identify_required_skills(self, user_input):
        """识别所需技能"""
        skills = []
        for keyword, skill_list in self.SKILL_MAPPING.items():
            if keyword in user_input:
                skills.extend(skill_list)
        unique_skills = list(set(skills)) if skills else ["task-decomposer", "test-driven-development"]
        self._log(f"🔍 匹配所需技能：{unique_skills}")
        return unique_skills
    
    def stage1_requirement_clarification(self, requirement):
        """阶段1：需求澄清"""
        self._log("\n===== 阶段1：需求澄清 =====")
        print(f"需求ID: {requirement['id']}")
        print(f"需求标题: {requirement['title']}")
        print(f"需求描述: {requirement['description']}")
        print(f"所需技能: {requirement['skills_required']}")
        
        # 保存需求文件
        req_path = os.path.join(self.requirements_dir, f"{requirement['id']}.json")
        with open(req_path, "w", encoding="utf-8") as f:
            json.dump(requirement, f, ensure_ascii=False, indent=2)
        self._log(f"📄 需求文件已保存：{req_path}")
        return requirement
    
    def stage2_task_decomposition(self, requirement):
        """阶段2：任务分解"""
        self._log("\n===== 阶段2：任务分解 =====")
        
        # 动态选择任务拆分技能
        task_decomposer = self.skill_manager.get_skill("task-decomposer")
        if task_decomposer:
            self._log(f"✅ 使用任务拆分技能：{task_decomposer['name']}")
            # 实际项目中这里会调用技能的方法
            return [{"id": "t1", "title": "动态生成任务", "description": "根据需求生成具体任务", "skill": "task-decomposer"}]
        else:
            self.missing_skills.add("task-decomposer")
            self._log("⚠️ 任务拆分技能未配置，使用默认逻辑")
            return [
                {"id": "t1", "title": "需求分析", "description": "明确实现范围", "skill": "writing-plans"},
                {"id": "t2", "title": "代码生成", "description": "生成核心代码", "skill": "ai-code-generator"},
                {"id": "t3", "title": "测试验证", "description": "验证功能正确性", "skill": "test-driven-development"}
            ]
    
    def _execute_skill(self, skill_id, **kwargs):
        """通用技能执行逻辑"""
        skill_config = self.skill_manager.get_skill(skill_id)
        if not skill_config:
            self._log(f"⚠️ 技能未配置：{skill_id}")
            self.missing_skills.add(skill_id)
            return {"success": False, "reason": "skill_not_configured"}
        
        try:
            self._log(f"▶ 执行技能：{skill_config['name']} ({skill_id})")
            # 动态加载技能模块
            module = importlib.import_module(skill_config["module"])
            handler = getattr(module, skill_config["class"])()
            # 调用技能方法
            result = handler.execute(**kwargs)
            self.executed_skills.add(skill_id)
            self._log(f"✅ 技能执行成功：{skill_config['name']}")
            return {"success": True, "data": result}
        except Exception as e:
            self._log(f"❌ 技能执行失败：{skill_config['name']} | 错误：{str(e)}")
            self.missing_skills.add(skill_id)
            return {"success": False, "reason": "execution_error", "error": str(e)}
    
    def run_workflow(self, user_input):
        """运行完整工作流"""
        self._log("="*50)
        self._log("🚀 启动通用工作流编排")
        self._log("="*50)
        
        # 执行四阶段流程
        req = self.identify_requirement(user_input)
        self.stage1_requirement_clarification(req)
        tasks = self.stage2_task_decomposition(req)
        
        # 阶段3：执行任务
        all_passed = True
        self._log("\n===== 阶段3：任务执行 =====")
        for task in tasks:
            self._log(f"\n📌 执行任务：{task['title']}")
            result = self._execute_skill(task["skill"], task=task)
            if not result["success"]:
                all_passed = False
                self._log(f"⚠️ 任务执行失败：{task['title']}")
        
        # 阶段4：生成交付文档
        self._log("\n===== 阶段4：生成交付文档 =====")
        doc_content = f"""# 需求交付报告
## 需求信息
- 标题：{req['title']}
- ID：{req['id']}
- 创建时间：{req['created_at']}

## 执行结果
- 已执行技能：{list(self.executed_skills)}
- 缺失技能：{list(self.missing_skills) if self.missing_skills else '无'}
- 任务完成状态：{'成功' if all_passed else '部分失败'}
"""
        doc_path = os.path.join(self.requirements_dir, f"{req['id']}_delivery.md")
        with open(doc_path, "w", encoding="utf-8") as f:
            f.write(doc_content)
        self._log(f"📄 交付文档已生成：{doc_path}")
        
        # 最终日志
        self._log("\n🎉 工作流执行完成")
        self._log(f"已执行技能：{list(self.executed_skills)}")
        self._log(f"缺失技能：{list(self.missing_skills) if self.missing_skills else '无'}")

# ===================== 【技能示例实现】 =====================
# 示例技能实现（实际项目中会放在单独文件中）
class TaskDecomposer:
    def execute(self, **kwargs):
        return {"tasks": [{"id": "t1", "title": "示例任务", "description": "这是一个示例任务"}]}

class TDDHandler:
    def execute(self, **kwargs):
        return {"tests": [{"description": "验证用户登录功能", "status": "通过"}]}

class AICodeGenerator:
    def execute(self, **kwargs):
        return {"code": "print('Hello World!')", "language": "Python"}

# ===================== 【主入口】 =====================
if __name__ == "__main__":
    # 初始化编排器
    orchestrator = WorkflowOrchestrator()
    
    # 用户输入
    user_input = "开发一个用户管理系统，包含登录和权限控制"
    
    # 运行工作流
    orchestrator.run_workflow(user_input)
