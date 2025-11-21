# 🧬 Mutation Testing with PITest

## 📋 Overview

Mutation testing evaluates the quality of your tests by introducing small changes (mutations) to your code and checking if your tests catch them.

---

## 🚀 Quick Start

### Run All Mutation Tests
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

### Run for Specific Package
```bash
mvn org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses=com.example.hrcore.service.ProfileService
```

### Run with Specific Tests
```bash
mvn org.pitest:pitest-maven:mutationCoverage \
    -DtargetTests=ProfileControllerIntegrationTest
```

---

## �� Configuration

### Current Settings

| Setting | Value | Description |
|---------|-------|-------------|
| **Mutators** | `DEFAULTS` | Standard mutation operators |
| **Mutation Threshold** | 70% | Minimum mutation score to pass |
| **Coverage Threshold** | 60% | Minimum line coverage required |
| **Threads** | 2 | Parallel execution threads |
| **Timeout Factor** | 1.5 | Multiplier for test timeout |

### Target Classes
- ✅ `com.example.hrcore.service.*`
- ✅ `com.example.hrcore.controller.*`

### Excluded Classes
- ❌ JPA Metamodel classes (`*_`)
- ❌ QueryDSL classes (`Q*`)
- ❌ Configuration classes
- ❌ Main application class
- ❌ Generated code

---

## 🎯 What are Mutators?

### DEFAULTS Mutators Include:

| Mutator | Example | Description |
|---------|---------|-------------|
| **Conditionals Boundary** | `<` → `<=` | Changes boundary conditions |
| **Negate Conditionals** | `==` → `!=` | Inverts boolean conditions |
| **Math** | `+` → `-` | Changes arithmetic operators |
| **Increments** | `i++` → `i--` | Reverses increment/decrement |
| **Invert Negatives** | `-x` → `x` | Removes negation |
| **Return Values** | `return x` → `return x+1` | Modifies return values |
| **Void Method Calls** | Removes method calls | Removes void method calls |

### Example Mutations:

**Original Code:**
```java
if (user.getAge() >= 18) {
    return true;
}
return false;
```

**Possible Mutations:**
```java
// Mutation 1: Boundary change
if (user.getAge() > 18) { ... }

// Mutation 2: Negate conditional
if (user.getAge() < 18) { ... }

// Mutation 3: Return value change
return true; // Always return true
```

**Good tests will kill all these mutations!**

---

## 📈 Understanding Results

### Report Location
```
target/pit-reports/YYYYMMDDHHMI/index.html
```

### Mutation Score
```
Mutations: 100
Killed: 85
Survived: 10
No Coverage: 5

Mutation Score: 85% ✅ (threshold: 70%)
```

### Score Breakdown

| Status | Description | Desired |
|--------|-------------|---------|
| **Killed** | Test detected the mutation | ✅ High |
| **Survived** | Mutation not detected | ❌ Low |
| **No Coverage** | No test executed this code | ❌ Zero |
| **Timed Out** | Test took too long | ⚠️ Investigate |
| **Non Viable** | Mutation couldn't compile | ℹ️ Ignore |

---

## 🔍 Example Report

```
================================================================================
- Mutators
================================================================================
> com.example.hrcore.service.ProfileService
--------------------------------------------------------------------------------
Line 45: negated conditional → KILLED ✅
Line 47: removed method call → KILLED ✅
Line 52: changed conditional boundary → SURVIVED ❌
Line 67: replaced return value → KILLED ✅

>> Details for survived mutation:
   Line 52: if (userId > 0) changed to if (userId >= 0)
   
   This mutation survived because no test checks the boundary condition!
   
   Recommendation: Add test case with userId = 0
```

---

## 💡 Best Practices

### 1. Start Small
```bash
# Test one class first
mvn pitest:mutationCoverage \
    -DtargetClasses=com.example.hrcore.service.ProfileService \
    -DtargetTests=ProfileServiceTest
```

### 2. Incremental Improvement
- Don't aim for 100% immediately
- Focus on critical business logic first
- Current threshold: 70% is reasonable

### 3. Investigate Survivors
```java
// BAD: Mutation survives
if (user.getRole() == UserRole.ADMIN) {
    return true;
}
// If mutated to != ADMIN, and no test covers this...

// GOOD: Add test to kill mutation
@Test
void whenNotAdmin_shouldReturnFalse() {
    User user = new User();
    user.setRole(UserRole.EMPLOYEE);
    assertFalse(service.isAdmin(user));
}
```

### 4. Exclude What Makes Sense
- Configuration classes (not business logic)
- DTOs/Entities (getters/setters)
- Generated code
- Framework/library code

---

## 🛠️ Advanced Configuration

### Use Different Mutator Groups

**In pom.xml:**
```xml
<mutators>
    <!-- Conservative (fastest) -->
    <mutator>DEFAULTS</mutator>
    
    <!-- OR Stronger (slower, more thorough) -->
    <!-- <mutator>STRONGER</mutator> -->
    
    <!-- OR Specific mutators only -->
    <!-- <mutator>CONDITIONALS_BOUNDARY</mutator> -->
    <!-- <mutator>NEGATE_CONDITIONALS</mutator> -->
    <!-- <mutator>MATH</mutator> -->
</mutators>
```

### Adjust Thresholds

```xml
<!-- Stricter -->
<mutationThreshold>85</mutationThreshold>
<coverageThreshold>80</coverageThreshold>

<!-- Or more lenient -->
<mutationThreshold>60</mutationThreshold>
<coverageThreshold>50</coverageThreshold>
```

### Performance Tuning

```xml
<!-- More threads (faster, uses more CPU) -->
<threads>4</threads>

<!-- Longer timeout (for slow tests) -->
<timeoutFactor>2.0</timeoutFactor>
<timeoutConstant>6000</timeoutConstant>

<!-- Faster incremental runs -->
<withHistory>true</withHistory>
<historyInputFile>target/pit-history.txt</historyInputFile>
<historyOutputFile>target/pit-history.txt</historyOutputFile>
```

---

## 🎓 Real Example: ProfileService

### Code to Test
```java
public Optional<UserDto> getProfile(Long userId, Long currentUserId, UserRole role) {
    if (role == UserRole.SUPER_ADMIN) {
        return Optional.empty(); // SUPER_ADMIN should be ignored
    }
    
    return userRepository.findById(userId)
            .map(user -> maskSensitiveData(user, currentUserId, role));
}
```

### Possible Mutations
1. `role == UserRole.SUPER_ADMIN` → `role != UserRole.SUPER_ADMIN` ❌
2. `Optional.empty()` → `Optional.of(new UserDto())` ❌
3. Remove `maskSensitiveData` call ❌

### Tests to Kill Mutations
```java
@Test
void whenSuperAdmin_shouldReturnEmpty() {
    // Kills mutation #1
    Optional<UserDto> result = service.getProfile(1L, 1L, UserRole.SUPER_ADMIN);
    assertTrue(result.isEmpty());
}

@Test
void whenEmployee_shouldMaskSensitiveData() {
    // Kills mutation #3
    UserDto result = service.getProfile(2L, 1L, UserRole.EMPLOYEE).get();
    assertNull(result.getPhone()); // Sensitive data masked
}
```

---

## 📋 CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run Mutation Tests
  run: mvn org.pitest:pitest-maven:mutationCoverage
  
- name: Upload Mutation Report
  uses: actions/upload-artifact@v3
  with:
    name: pit-reports
    path: target/pit-reports/
```

### Fail Build on Low Score
```xml
<configuration>
    <mutationThreshold>70</mutationThreshold>
    <coverageThreshold>60</coverageThreshold>
    <failWhenNoMutations>true</failWhenNoMutations>
</configuration>
```

---

## 📚 Common Mutations to Watch

### 1. Boundary Conditions
```java
// Original
if (age >= 18) { ... }

// Mutation
if (age > 18) { ... }  // Misses age == 18

// Kill with test
@Test
void whenAgeIs18_shouldAllowAccess() { ... }
```

### 2. Return Value Changes
```java
// Original
return user != null;

// Mutation
return true; // Always true

// Kill with test
@Test
void whenUserIsNull_shouldReturnFalse() { ... }
```

### 3. Removed Method Calls
```java
// Original
validate(user);
save(user);

// Mutation
// validate(user); // Removed!
save(user);

// Kill with test
@Test
void whenInvalidUser_shouldThrowException() { ... }
```

---

## ✅ Current Status

**Configuration:**
- ✅ PITest Maven Plugin: `1.16.3`
- ✅ JUnit 5 Support: `1.2.1`
- ✅ Minimal mutators: `DEFAULTS`
- ✅ Reasonable thresholds: 70% mutation, 60% coverage
- ✅ Performance optimized: 2 threads, smart timeouts
- ✅ Excluded generated code

**Ready to run:**
```bash
mvn org.pitest:pitest-maven:mutationCoverage
```

**View reports:**
```bash
# After running, open:
target/pit-reports/*/index.html
```

---

## 🎯 Next Steps

1. **Run initial analysis:**
   ```bash
   mvn org.pitest:pitest-maven:mutationCoverage
   ```

2. **Review the report:**
   - Check overall mutation score
   - Identify survived mutations
   - Focus on critical business logic

3. **Improve tests:**
   - Add tests for survived mutations
   - Focus on edge cases and boundary conditions
   - Ensure all code paths are tested

4. **Iterate:**
   - Gradually increase thresholds
   - Run mutation tests regularly
   - Include in CI/CD pipeline

---

## 📖 Resources

- [PITest Official Docs](https://pitest.org/)
- [Mutation Testing Best Practices](https://pitest.org/quickstart/best_practices/)
- [Available Mutators](https://pitest.org/quickstart/mutators/)

**🧬 Happy Mutation Testing!**
