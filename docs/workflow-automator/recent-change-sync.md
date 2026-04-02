# 最近变更文档同步

- 需求：在 irsAi 项目中将 Redis 的使用替换为 Redisson，并输出完整流程看板
- 选择方案：分阶段替换
- 生成时间：2026-03-31 18:38:24

## 最近变更概览
- 依赖与构建变更：ai-hr-ai-core/pom.xml, ai-hr-attendance/pom.xml, ai-hr-common/pom.xml, ai-hr-salary/pom.xml, pom.xml, i-hr-ai-core/pom.xml
- 配置与部署变更：.github/workflows/ci-cd.yml, .trae/skills/code-simplifier/.claude-plugin/plugin.json, .trae/skills/ralph-loop/.claude-plugin/plugin.json, .trae/skills/ralph-loop/hooks/hooks.json, ai-hr-ai-core/src/main/resources/application.yml, github/workflows/ci-cd.yml
- 代码实现变更：.trae/skills/code-simplifier/LICENSE, .trae/skills/docx/LICENSE.txt, .trae/skills/docx/scripts/__init__.py, .trae/skills/docx/scripts/accept_changes.py, .trae/skills/docx/scripts/comment.py, .trae/skills/docx/scripts/office/helpers/__init__.py
- 测试变更：ai-hr-ai-core/src/test/java/com/example/aihr/aicore/service/AttendanceAIServiceImplTest.java, ai-hr-ai-core/src/test/java/com/example/aihr/aicore/service/SalaryAIServiceImplTest.java, ai-hr-ai-core/src/test/java/com/example/aihr/aicore/service/SimpleTest.java, ai-hr-ai-core/src/test/java/com/example/aihr/aicore/tools/ToolInvokerTest.java, ai-hr-attendance/src/test/java/com/example/aihr/attendance/service/AttendanceServiceTest.java, i-hr-ai-core/src/test/java/com/example/aihr/aicore/service/AttendanceAIServiceImplTest.java
- 已有文档变更：.trae/skills/code-quality-guardian/SKILL.md, .trae/skills/code-simplifier/agents/code-simplifier.md, .trae/skills/coverage-enhancer/SKILL.md, .trae/skills/doc-coauthoring/SKILL.md, .trae/skills/docx/SKILL.md, .trae/skills/find-skills/SKILL.md

## 文档同步说明
- 已扫描到 1 个候选文档，优先同步到 README 或 docs 目录。
- 这份内容由 workflow-automator 自动生成，用来说明最近变更对依赖、配置、代码和测试的影响。
- 如果后续确认某些对外行为或部署步骤发生变化，建议再补充到正式业务文档中。
