# CryptoDept Naming Conventions

## Architecture Layers
- **Data Layer**: 
    - Repositories: `XxxRepositoryImpl`
    - API Clients: `XxxApi`, `XxxService`
    - Room: `XxxDao`, `XxxEntity`
- **Domain Layer**:
    - Interfaces: `XxxRepository`
    - Use Cases: `XxxUseCase`, `XxxEngine`, `XxxCalculator`
    - Models: `Xxx` (Data classes)
- **Presentation Layer**:
    - ViewModels: `XxxViewModel`
    - Screens: `XxxScreen` (Compose)
    - Components: `XxxCard`, `XxxRow`, etc.
- **Infrastructure / Utils**:
    - Services: `XxxService` (Formerly `XxxManager`)
    - Extensions: `XxxExtensions.kt`

## Rules
1. Use `Service` suffix for long-running or global utility classes (e.g., `AnalyticsService`, `BillingService`).
2. Avoid `Manager` suffix.
3. UI components in Compose should end with `Screen` if they represent a full destination.
4. Business logic should be extracted to `UseCase` or `Engine` classes.
