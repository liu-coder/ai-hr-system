---
name: coverage-enhancer
description: Use when the user needs to improve test coverage for their codebase. This skill helps identify untested code, generate test cases, and increase overall test coverage.
---

# Coverage Enhancer

A skill for improving test coverage in codebases.

## When to Use
- When test coverage is low or insufficient
- When identifying untested code paths
- When generating test cases for uncovered code
- When optimizing test suites for better coverage
- When ensuring code quality through comprehensive testing

## How to Use
1. **Analyze coverage**: Review current test coverage metrics
2. **Identify gaps**: Detect untested code paths and branches
3. **Generate tests**: Create test cases for uncovered code
4. **Validate coverage**: Verify that new tests increase coverage
5. **Optimize tests**: Refine test suite for maximum coverage

## Output Format
```markdown
# Test Coverage Analysis

## Overview
- **Project**: [Project name]
- **Language**: [Programming language]
- **Current Coverage**: [Percentage]
- **Target Coverage**: [Percentage]
- **Coverage Gap**: [Percentage]

## Uncovered Code

### [File path]
- **Uncovered Lines**: [Line numbers]
- **Uncovered Branches**: [Branch information]
- **Complexity**: [Cyclomatic complexity]

## Recommended Test Cases

### Test Case 1: [Test name]
- **Target**: [File:line]
- **Purpose**: [Test purpose]
- **Test Code**:
```[language]
[Test code]
```

## Coverage Enhancement Plan
1. [Step 1]
2. [Step 2]
3. [Step 3]

## Expected Results
- **Estimated Coverage After**: [Percentage]
- **Additional Tests Needed**: [Number]
- **Time Estimate**: [Hours]
```

## Example

**Input**: "Improve test coverage for this Java class:

```java
public class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int subtract(int a, int b) {
        return a - b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
    
    public int divide(int a, int b) {
        if (b == 0) {
            throw new IllegalArgumentException("Divisor cannot be zero");
        }
        return a / b;
    }
}
```"

**Output**:
```markdown
# Test Coverage Analysis

## Overview
- **Project**: Calculator
- **Language**: Java
- **Current Coverage**: 0%
- **Target Coverage**: 100%
- **Coverage Gap**: 100%

## Uncovered Code

### Calculator.java
- **Uncovered Lines**: 3-18
- **Uncovered Branches**: 11-13 (if condition)
- **Complexity**: Low

## Recommended Test Cases

### Test Case 1: Test add method
- **Target**: Calculator.java:3-5
- **Purpose**: Test addition functionality
- **Test Code**:
```java
@Test
public void testAdd() {
    Calculator calculator = new Calculator();
    assertEquals(5, calculator.add(2, 3));
    assertEquals(0, calculator.add(0, 0));
    assertEquals(-1, calculator.add(1, -2));
}
```

### Test Case 2: Test subtract method
- **Target**: Calculator.java:7-9
- **Purpose**: Test subtraction functionality
- **Test Code**:
```java
@Test
public void testSubtract() {
    Calculator calculator = new Calculator();
    assertEquals(1, calculator.subtract(3, 2));
    assertEquals(0, calculator.subtract(5, 5));
    assertEquals(-3, calculator.subtract(2, 5));
}
```

### Test Case 3: Test multiply method
- **Target**: Calculator.java:11-13
- **Purpose**: Test multiplication functionality
- **Test Code**:
```java
@Test
public void testMultiply() {
    Calculator calculator = new Calculator();
    assertEquals(6, calculator.multiply(2, 3));
    assertEquals(0, calculator.multiply(5, 0));
    assertEquals(-6, calculator.multiply(2, -3));
}
```

### Test Case 4: Test divide method (normal case)
- **Target**: Calculator.java:15-18
- **Purpose**: Test division functionality
- **Test Code**:
```java
@Test
public void testDivide() {
    Calculator calculator = new Calculator();
    assertEquals(2, calculator.divide(6, 3));
    assertEquals(0, calculator.divide(0, 5));
    assertEquals(-2, calculator.divide(6, -3));
}
```

### Test Case 5: Test divide method (zero divisor)
- **Target**: Calculator.java:16-17
- **Purpose**: Test division by zero exception
- **Test Code**:
```java
@Test
public void testDivideByZero() {
    Calculator calculator = new Calculator();
    assertThrows(IllegalArgumentException.class, () -> {
        calculator.divide(5, 0);
    });
}
```

## Coverage Enhancement Plan
1. Add all recommended test cases
2. Run tests to verify coverage
3. Analyze any remaining gaps
4. Add additional tests if needed

## Expected Results
- **Estimated Coverage After**: 100%
- **Additional Tests Needed**: 0
- **Time Estimate**: 30 minutes
```

## Supported Languages
- Java (JUnit, TestNG)
- Python (pytest, unittest)
- JavaScript/TypeScript (Jest, Mocha)
- C# (NUnit, xUnit)
- Go (testing package)

## Coverage Tools Integration
- JaCoCo (Java)
- Coverage.py (Python)
- Istanbul (JavaScript)
- Coverlet (.NET)
- Go Cover (Go)

## Best Practices
- Focus on critical code paths first
- Test both positive and negative scenarios
- Include edge cases and boundary conditions
- Maintain test readability and maintainability
- Regularly monitor coverage metrics

## Integration
This skill can be integrated into CI/CD pipelines to automatically check and enforce coverage thresholds.