# 测试脚本

Write-Host "=== AI HR 系统测试脚本 ==="
Write-Host "1. 编译项目"
Write-Host "2. 启动服务"
Write-Host "3. 测试API"
Write-Host "4. 退出"

$choice = Read-Host "请选择操作 (1-4)"

switch ($choice) {
    1 {
        Write-Host "正在编译项目..."
        mvn clean package
        Write-Host "编译完成！"
    }
    2 {
        Write-Host "请按顺序启动服务:"
        Write-Host "1. 配置中心 (ai-hr-config-server)"
        Write-Host "2. 服务注册中心 (ai-hr-eureka-server)"
        Write-Host "3. 认证服务 (ai-hr-auth)"
        Write-Host "4. 考勤服务 (ai-hr-attendance)"
        Write-Host "5. 薪酬服务 (ai-hr-salary)"
        Write-Host "6. AI核心服务 (ai-hr-ai-core)"
        Write-Host "7. 网关服务 (ai-hr-gateway)"
        Write-Host ""
        Write-Host "启动命令示例:"
        Write-Host "java -jar ai-hr-config-server\target\ai-hr-config-server-0.1.0-SNAPSHOT.jar"
    }
    3 {
        Write-Host "=== API测试 ==="
        Write-Host "1. 认证测试"
        Write-Host "2. 考勤测试"
        Write-Host "3. 薪酬测试"
        Write-Host "4. AI功能测试"
        
        $apiChoice = Read-Host "请选择测试类型 (1-4)"
        
        switch ($apiChoice) {
            1 {
                Write-Host "=== 认证测试 ==="
                Write-Host "获取Token:"
                Write-Host "curl -X POST http://localhost:9010/auth/login -H \"Content-Type: application/json\" -d '{\"username\": \"admin\", \"password\": \"admin123\"}'"
            }
            2 {
                Write-Host "=== 考勤测试 ==="
                Write-Host "获取打卡记录:"
                Write-Host "curl -X GET http://localhost:9000/api/attendance/records -H \"Authorization: Bearer {token}\""
                Write-Host "获取异常考勤:"
                Write-Host "curl -X GET http://localhost:9000/api/attendance/anomalies -H \"Authorization: Bearer {token}\""
            }
            3 {
                Write-Host "=== 薪酬测试 ==="
                Write-Host "获取薪资政策:"
                Write-Host "curl -X GET http://localhost:9000/api/salary/policies -H \"Authorization: Bearer {token}\""
                Write-Host "计算薪资:"
                Write-Host "curl -X POST http://localhost:9000/api/salary/calculate -H \"Content-Type: application/json\" -H \"Authorization: Bearer {token}\" -d '{\"payPeriod\": \"2024-03\", \"employeeIds\": [\"emp-001\"]}'"
            }
            4 {
                Write-Host "=== AI功能测试 ==="
                Write-Host "智能排班优化:"
                Write-Host "curl -X POST http://localhost:9000/api/ai/attendance/optimize-scheduling -H \"Content-Type: application/json\" -H \"Authorization: Bearer {token}\" -d '{\"startDate\": \"2024-04-01\", \"endDate\": \"2024-04-07\", \"employeeIds\": [\"emp-001\", \"emp-002\"]}'"
                Write-Host "薪酬调整建议:"
                Write-Host "curl -X POST http://localhost:9000/api/ai/salary/suggest-adjustment -H \"Content-Type: application/json\" -H \"Authorization: Bearer {token}\" -d '{\"employees\": [{\"employeeId\": \"emp-001\", \"currentSalary\": 10000, \"marketSalary\": 12000, \"performanceScore\": 4.5, \"tenureYears\": 3}], \"budgetLimit\": 5000}'"
            }
        }
    }
    4 {
        Write-Host "退出测试脚本"
        exit
    }
    default {
        Write-Host "无效选择"
    }
}
