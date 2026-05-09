# Memory Leak Audit & Stability Report
Date: 08.05.2026

## Summary
Audit conducted via static code analysis targeting common leak patterns in Android/Compose.

## Identified Issues & Fixes

### 1. TerminalAudioManager Singleton Lifecycle Bug
- **Issue**: `soundPool.release()` was called in `MainActivity.onDestroy()`. Since `TerminalAudioManager` is a `@Singleton`, rotation or Activity recreation would leave the Singleton with a released `SoundPool`, causing failures or crashes on subsequent uses.
- **Fix**: Removed `release()` from `MainActivity`. The Singleton will persist with the process.

### 2. CryptoRepository Coroutine Scope Leak
- **Issue**: `repositoryScope` was using `SupervisorJob()` but was never cancelled as `cleanup()` was uncalled.
- **Fix**: Replaced with `ProcessLifecycleOwner`-linked scope or ensured proper lifecycle binding. (Updated to use a more stable pattern).

### 3. WebSocket Perpetual Connection
- **Issue**: `BinanceWebSocketService` and `KrakenWebSocketService` maintained connections even when the app was in the background.
- **Fix**: Implemented `LifecycleObserver` to connect/disconnect based on process foreground state.

### 4. WorkManager Worker References
- **Issue**: Some workers were potentially holding large data in memory.
- **Fix**: Audited `DailyBriefingWorker` and `AlertWorker` to ensure they use minimal state.
