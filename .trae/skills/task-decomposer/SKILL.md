---
name: task-decomposer
description: Use when the user needs to break down complex tasks into smaller, manageable steps. This skill helps create structured task breakdowns, identify dependencies, and prioritize work.
---

# Task Decomposer

A skill for breaking down complex tasks into smaller, manageable steps.

## When to Use
- When the user has a complex task that needs to be broken down
- When planning a project or workflow
- When identifying dependencies between tasks
- When prioritizing work items
- When creating a structured approach to problem-solving

## How to Use
1. **Analyze the task**: Understand the overall goal and requirements
2. **Break down into subtasks**: Create smaller, actionable steps
3. **Identify dependencies**: Determine relationships between subtasks
4. **Prioritize**: Order tasks based on importance and dependencies
5. **Estimate effort**: Provide rough estimates for each subtask

## Output Format
```markdown
# Task Breakdown: [Task Name]

## Overview
[Brief description of the task]

## Subtasks

### 1. [Subtask 1]
- **Description**: [What needs to be done]
- **Dependencies**: [List of dependent subtasks]
- **Priority**: [High/Medium/Low]
- **Estimated Effort**: [Time estimate]

### 2. [Subtask 2]
- **Description**: [What needs to be done]
- **Dependencies**: [List of dependent subtasks]
- **Priority**: [High/Medium/Low]
- **Estimated Effort**: [Time estimate]

## Implementation Plan
[Step-by-step plan for executing the subtasks]

## Success Criteria
[What defines completion of the task]
```

## Example

**Input**: "I need to build a new attendance tracking system"

**Output**:
```markdown
# Task Breakdown: Build New Attendance Tracking System

## Overview
Create a comprehensive attendance tracking system that records employee check-ins/outs, tracks overtime, and generates reports.

## Subtasks

### 1. Requirements Gathering
- **Description**: Collect and document business requirements for the attendance system
- **Dependencies**: None
- **Priority**: High
- **Estimated Effort**: 2 days

### 2. System Design
- **Description**: Design the system architecture, database schema, and user interface
- **Dependencies**: 1. Requirements Gathering
- **Priority**: High
- **Estimated Effort**: 3 days

### 3. Database Implementation
- **Description**: Create database tables and relationships
- **Dependencies**: 2. System Design
- **Priority**: High
- **Estimated Effort**: 2 days

### 4. Backend Development
- **Description**: Implement API endpoints and business logic
- **Dependencies**: 3. Database Implementation
- **Priority**: High
- **Estimated Effort**: 5 days

### 5. Frontend Development
- **Description**: Build user interface components and views
- **Dependencies**: 2. System Design
- **Priority**: Medium
- **Estimated Effort**: 4 days

### 6. Integration Testing
- **Description**: Test the complete system functionality
- **Dependencies**: 4. Backend Development, 5. Frontend Development
- **Priority**: Medium
- **Estimated Effort**: 3 days

### 7. Deployment
- **Description**: Deploy the system to production
- **Dependencies**: 6. Integration Testing
- **Priority**: Medium
- **Estimated Effort**: 1 day

### 8. User Training
- **Description**: Train users on how to use the new system
- **Dependencies**: 7. Deployment
- **Priority**: Low
- **Estimated Effort**: 1 day

## Implementation Plan
1. Complete requirements gathering and system design
2. Implement database and backend in parallel with frontend development
3. Conduct integration testing once all components are ready
4. Deploy the system and provide user training

## Success Criteria
- System accurately tracks employee attendance
- Overtime calculations are correct
- Reports can be generated on demand
- Users can easily navigate and use the system
```