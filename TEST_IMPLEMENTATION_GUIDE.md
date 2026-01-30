# Test Coverage Improvement - Implementation Guide

## 📖 Overview

This guide provides everything needed to implement comprehensive test coverage for the JobRunr Control extension, with a
focus on **quality over quantity**.

## 🎯 Goals

- Increase test coverage from **~10% to 75%+**
- Add **~160 high-quality tests** (from 15)
- Focus on **critical business logic** first
- Maintain **fast test suite** (< 10 seconds total)
- Tests as **living documentation**

## 📚 Documentation Structure

### 1. [TEST_COVERAGE_PLAN.md](./TEST_COVERAGE_PLAN.md) - Comprehensive Plan

**What**: Detailed 7-week implementation plan
**Who**: Technical leads, architects
**When**: Planning phase

**Contains**:

- Executive summary with current state and goals
- Phase-by-phase breakdown (Weeks 1-7)
- Detailed test specifications for each component
- Test type distribution and coverage targets
- Testing infrastructure and tools
- Success metrics and best practices
- Complete test templates and examples

### 2. [TEST_COVERAGE_QUICK_REF.md](./TEST_COVERAGE_QUICK_REF.md) - Quick Reference

**What**: One-page summary for daily use
**Who**: All developers
**When**: Daily development

**Contains**:

- Current status at a glance
- Weekly implementation checklist
- Test type distribution
- Testing principles (DO/DON'T)
- Quick start guide
- Success criteria

### 3. [runtime/src/test/java/README.md](./jobrunr-control-extension-parent/runtime/src/test/java/README.md) - Testing Guidelines

**What**: Practical testing guide
**Who**: Developers writing tests
**When**: Writing tests

**Contains**:

- Test directory structure
- Test types and when to use each
- Naming conventions
- Code organization patterns
- Testing tools and frameworks
- Best practices with examples
- Running tests and viewing coverage

### 4. [CreateScheduledJobUseCaseTest.java](./jobrunr-control-extension-parent/runtime/src/test/java/ch/css/jobrunr/control/application/scheduling/CreateScheduledJobUseCaseTest.java) - Example Test

**What**: Production-ready test example
**Who**: Developers learning by example
**When**: Starting new tests

**Contains**:

- Comprehensive use case test with ~20 test methods
- Nested test classes for organization
- Proper mocking and verification
- Edge case testing
- Test data builders
- Clear documentation

## 🚀 Getting Started

### For Project Managers / Tech Leads

1. **Review** [TEST_COVERAGE_PLAN.md](./TEST_COVERAGE_PLAN.md) for full scope
2. **Allocate** 2-3 hours per day for 7 weeks
3. **Track progress** using weekly checklists in Quick Reference
4. **Monitor** coverage reports weekly

### For Developers

1. **Read** [TEST_COVERAGE_QUICK_REF.md](./TEST_COVERAGE_QUICK_REF.md) (5 min)
2. **Study
   ** [CreateScheduledJobUseCaseTest.java](./jobrunr-control-extension-parent/runtime/src/test/java/ch/css/jobrunr/control/application/scheduling/CreateScheduledJobUseCaseTest.java)
   example (15 min)
3. **Review** [Testing Guidelines](./jobrunr-control-extension-parent/runtime/src/test/java/README.md) (10 min)
4. **Start writing tests** following the example
5. **Refer to** [TEST_COVERAGE_PLAN.md](./TEST_COVERAGE_PLAN.md) for detailed specifications

## 📅 Implementation Timeline

### Week 1: Critical Scheduling Use Cases

**Focus**: CreateScheduledJobUseCase, UpdateScheduledJobUseCase, DeleteScheduledJobUseCase, ExecuteScheduledJobUseCase
**Tests**: ~40-45 tests
**Coverage**: Core scheduling logic

### Week 2: Templates, Parameters & Validation

**Focus**: Template management, parameter handling, validation
**Tests**: ~35-40 tests
**Coverage**: Template cloning, parameter resolution, input validation

### Week 3: Scheduler & Storage Adapters

**Focus**: JobRunr integration, database operations
**Tests**: ~20-25 tests
**Coverage**: External integrations

### Week 4: Execution & Filters

**Focus**: Job execution monitoring, cleanup filters
**Tests**: ~15-20 tests
**Coverage**: Job lifecycle management

### Week 5: Domain Layer

**Focus**: Domain models and business rules
**Tests**: ~20-25 tests
**Coverage**: Core domain logic

### Week 6: Controller Integration Tests

**Focus**: HTTP/HTMX endpoints
**Tests**: ~30-35 tests
**Coverage**: API contract testing

### Week 7: Utilities & Cleanup

**Focus**: Helper classes, final coverage gaps
**Tests**: ~15-20 tests
**Coverage**: Supporting utilities

## 📊 Tracking Progress

### Daily Checklist

- [ ] Pick untested component from current week's focus
- [ ] Write tests (happy path + edge cases)
- [ ] Run tests and verify they pass
- [ ] Check coverage: `./mvnw verify jacoco:report`
- [ ] Review code quality
- [ ] Commit with descriptive message

### Weekly Review

- [ ] Compare coverage against targets
- [ ] Review test quality (speed, clarity, value)
- [ ] Update Quick Reference checklist
- [ ] Identify blockers or challenges
- [ ] Plan next week's priorities

### Coverage Targets by Week

| Week | Focus Area           | Tests Added | Coverage Target |
|------|----------------------|-------------|-----------------|
| 1    | Scheduling Use Cases | 40-45       | 20% → 35%       |
| 2    | Templates/Parameters | 35-40       | 35% → 50%       |
| 3    | Adapters             | 20-25       | 50% → 60%       |
| 4    | Execution/Filters    | 15-20       | 60% → 65%       |
| 5    | Domain Layer         | 20-25       | 65% → 70%       |
| 6    | Controllers          | 30-35       | 70% → 73%       |
| 7    | Utilities            | 15-20       | 73% → 75%+      |

## 🎓 Testing Philosophy

### Quality Indicators

✅ **High-Quality Test**:

- Clear, descriptive name explaining what is tested
- Single, focused assertion
- Tests behavior, not implementation
- Fast (< 100ms for unit tests)
- Independent (no shared state)
- Provides value (not testing trivial code)

❌ **Low-Quality Test**:

- Generic name like "testMethod1"
- Multiple unrelated assertions
- Tests internal implementation details
- Slow or flaky
- Depends on other tests running first
- Tests framework code or getters/setters

### Example: Good vs Bad

```java
// ❌ BAD: Generic name, tests implementation
@Test
void test1() {
    service.processJob();
    verify(repository).save(any()); // Testing how, not what
}

// ✅ GOOD: Clear name, tests behavior
@Test
@DisplayName("should save job to database when processing succeeds")
void execute_SuccessfulProcessing_SavesJobToDatabase() {
    // Arrange
    Job job = createValidJob();

    // Act
    service.process(job);

    // Assert
    assertThat(service.getProcessedJobs())
            .contains(job); // Testing what, not how
}
```

## 🛠️ Tools & Setup

### Required Dependencies

Already configured in `pom.xml`:

- JUnit 5
- Mockito
- AssertJ
- Quarkus Test
- H2 Database (for integration tests)

### IDE Setup

1. **IntelliJ IDEA**: Right-click test → Run with Coverage
2. **Coverage Report**: View inline coverage in editor
3. **Hotkey**: Ctrl+Shift+F10 (Win) / Cmd+Shift+R (Mac) to run test

### Coverage Reports

```bash
# Generate coverage report
./mvnw clean verify jacoco:report

# Open report (macOS)
open target/site/jacoco/index.html

# Open report (Linux)
xdg-open target/site/jacoco/index.html

# Open report (Windows)
start target/site/jacoco/index.html
```

## 📞 Support & Questions

### Common Questions

**Q: How much time will this take?**
A: ~2-3 hours per day for 7 weeks. Can be spread over longer period.

**Q: Should we use TDD (Test-Driven Development)?**
A: Recommended for new features. For existing code, write tests first, then refactor if needed.

**Q: What if I find bugs while writing tests?**
A: Good! Write a failing test first, then fix the bug. This prevents regression.

**Q: Should we aim for 100% coverage?**
A: No. 75% is a good target. Focus on critical paths, not trivial code.

**Q: How do I test private methods?**
A: Don't. Test through public API. If you feel the need, consider extracting to a separate class.

### Getting Help

1. **Review examples** in TEST_COVERAGE_PLAN.md
2. **Check** CreateScheduledJobUseCaseTest.java for patterns
3. **Read** Testing Guidelines in test/java/README.md
4. **Consult** JUnit 5 / Mockito documentation

## 🎯 Success Metrics

### Code Quality

- [ ] 75%+ overall coverage
- [ ] 80%+ use case coverage
- [ ] 90%+ domain coverage
- [ ] 70%+ adapter coverage
- [ ] Unit tests run in < 5 seconds
- [ ] All tests run in < 10 seconds
- [ ] 0 flaky tests

### Process Quality

- [ ] Tests written before fixing bugs
- [ ] TDD used for new features
- [ ] All tests pass on every commit
- [ ] Coverage reports reviewed weekly
- [ ] Tests serve as documentation

### Team Quality

- [ ] All developers comfortable writing tests
- [ ] Test quality reviewed in PRs
- [ ] Testing best practices followed
- [ ] No skipped or ignored tests (without good reason)

## 🎓 Next Steps

1. **Week 0 (Preparation)**:
    - [ ] All developers read Quick Reference
    - [ ] Study example test file
    - [ ] Review Testing Guidelines
    - [ ] Set up coverage reporting

2. **Week 1 (Start Implementation)**:
    - [ ] Begin with CreateScheduledJobUseCaseTest
    - [ ] Follow test template from plan
    - [ ] Review coverage after each test class
    - [ ] Share learnings with team

3. **Ongoing**:
    - [ ] Daily: Write tests, review coverage
    - [ ] Weekly: Team review, adjust plan if needed
    - [ ] Monthly: Assess progress against goals

## 📝 Conclusion

This test coverage improvement plan is designed to:

- **Systematically** increase coverage from 10% to 75%+
- **Prioritize** critical business logic first
- **Maintain** high code quality throughout
- **Provide** clear guidance and examples
- **Enable** sustainable testing practices

**Remember**: Quality over quantity. Every test should provide real value.

---

*Good tests are the best documentation. They show how the system works and prevent regressions.*

**Start Date**: January 30, 2026
**Target Completion**: March 20, 2026 (7 weeks)
**Status**: 📋 Ready to implement
