# LingoFlix AI Project Instructions

## 1. Core Values & Priorities
- **Efficiency (Surgical Edits):** Prefer `replace_file_content` over `write_file`. Only rewrite the whole file if structural changes are massive.
- **Developer Autonomy:** Build tools that allow the "Main Developer" to manage content (Recommendations, Databases) through the UI/simple config without deep code changes.
- **High Technical Standard:** Maintain extreme clarity in naming, logs, and error handling.
- **Verification First:** Never report a task as complete without running Gradle checks/tests.

## 2. Coding Standards
- **Single Responsibility Principle (SRP):** Each class/function must do one thing.
- **File Length Limit:** Maximum 200 lines per file.
- **Naming Convention:** Descriptive (7-8 words if needed). Clarity > Brevity.
- **Error Handling:** `try-catch` blocks on all IO/Network/DB operations.
- **Logging:** Centralized `LingoLog`. Log every entry, exit, and exception.
- **Type Safety:** Explicit type hints everywhere.
- **Language:** UI/Comments in Hebrew; Code/Docs in English.

## 2. Architecture (MVVM + Clean)
- **UI:** Jetpack Compose only.
- **ViewModel:** One per screen.
- **Repository:** Responsible for data fetching and error mapping.
- **Dependency Injection:** Manual DI for now, transitioning to Hilt/Koin if complexity grows.

## 3. Testing & Verification
- **Unit Tests:** Mandatory for all Repositories and ViewModels.
- **Simulated Validation:** Use Gradle build and Unit Tests to verify changes before reporting success.

## 4. Work Protocol
1. **Plan:** Outline the change and impact.
2. **Execute:** Implement with logs and try-catches.
3. **Verify:** Run relevant tests and `compileDebugKotlin`.
4. **Report:** Summarize changes and verification results.
