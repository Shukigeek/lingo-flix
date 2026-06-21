# LingoFlix AI Project Instructions

## 1. Coding Standards
- **Single Responsibility Principle (SRP):** Each class/function must do one thing.
- **File Length Limit:** Maximum 200 lines per file. Split components into sub-files if they exceed this.
- **Naming Convention:** Use descriptive variable and function names (even 7-8 words) to ensure absolute clarity.
- **Error Handling:** Every major operation must be wrapped in `try-catch` blocks with specific error handling.
- **Logging:** Use the centralized `LingoLog` utility for all logs. Every entry/exit point and error must be logged.
- **Type Safety:** Use explicit type hints everywhere. Avoid `Any` or implicit types.
- **Language:** UI/Comments for user in Hebrew; Code, variable names, and internal documentation in English.

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
