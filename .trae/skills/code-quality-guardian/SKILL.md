---
name: code-quality-guardian
description: Use when the user needs to check code quality, identify issues, and ensure best practices. This skill helps analyze code for potential problems, suggest improvements, and maintain code quality standards.
---

# Code Quality Guardian

A skill for checking and maintaining code quality standards.

## When to Use
- When reviewing code for quality issues
- When identifying potential bugs or vulnerabilities
- When ensuring adherence to coding standards
- When optimizing code performance
- When refactoring code to improve quality

## How to Use
1. **Analyze code**: Review the provided code for quality issues
2. **Identify problems**: Detect potential bugs, vulnerabilities, and anti-patterns
3. **Suggest improvements**: Provide actionable recommendations
4. **Verify fixes**: Ensure suggested changes address the issues
5. **Maintain standards**: Help enforce consistent coding practices

## Output Format
```markdown
# Code Quality Analysis

## Overview
- **File**: [File path]
- **Language**: [Programming language]
- **Total Issues**: [Number of issues found]

## Issues Found

### [Issue Type] - [Severity]
- **Location**: [File:line:column]
- **Description**: [Issue description]
- **Impact**: [Potential impact of the issue]
- **Recommendation**: [How to fix the issue]

## Code Quality Metrics
- **Cyclomatic Complexity**: [Value]
- **Code Coverage**: [Percentage]
- **Duplication Rate**: [Percentage]
- **Maintainability Index**: [Value]

## Suggested Improvements
1. [Improvement 1]
2. [Improvement 2]
3. [Improvement 3]

## Best Practices
- [Best practice 1]
- [Best practice 2]
- [Best practice 3]
```

## Example

**Input**: "Analyze this Java code for quality issues:

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int divide(int a, int b) {
        return a / b;
    }
}
```"

**Output**:
```markdown
# Code Quality Analysis

## Overview
- **File**: Calculator.java
- **Language**: Java
- **Total Issues**: 1

## Issues Found

### Bug - High
- **Location**: Calculator.java:7:16
- **Description**: Division by zero vulnerability
- **Impact**: Runtime exception when divisor is zero
- **Recommendation**: Add a check for zero divisor and throw an appropriate exception

## Code Quality Metrics
- **Cyclomatic Complexity**: Low (1 per method)
- **Code Coverage**: N/A (no tests provided)
- **Duplication Rate**: 0%
- **Maintainability Index**: High

## Suggested Improvements
1. Add input validation for divide method
2. Add Javadoc comments for all methods
3. Consider adding unit tests

## Best Practices
- Always validate inputs, especially for operations that can throw exceptions
- Document all public methods with Javadoc
- Write unit tests to ensure code correctness
```

## Supported Languages
- Java
- Python
- JavaScript/TypeScript
- C#
- C++
- Ruby
- Go
- PHP

## Quality Checks
- Syntax errors and warnings
- Potential bugs and vulnerabilities
- Code style violations
- Performance issues
- Security vulnerabilities
- Maintainability concerns
- Test coverage gaps

## Integration
This skill can be integrated into CI/CD pipelines to automatically check code quality before deployment.