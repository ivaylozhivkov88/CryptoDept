# CryptoDept — Ефективен работен промпт за Gemini Agent
## Версия: TOKEN-SAVER MODE (v2.1)

---

## ЗАДЪЛЖИТЕЛНИ ПРАВИЛА ЗА АГЕНТА — ПРОЧЕТИ ВЕДНЪЖ, СЛЕДВАЙ ВИНАГИ

### Режим на комуникация — СТРОГО
- Отговаряй САМО с код и минимален коментар
- БЕЗ обяснения как работи кодът освен ако не питам
- БЕЗ увод, БЕЗ заключение, БЕЗ "Ето как...", БЕЗ "Страхотно!"
- При грешка: 1 ред причина + fix. Нищо повече.
- При завършена стъпка: само `[DONE] #N — filename.kt`
- При въпрос към мен: максимум 1 изречение
- Код блоковете да са пълни и готови за copy-paste
- НЕ повтаряй код който вече съществува — само новото/променeното
- Използвай `// ...existing code...` за да прескочиш непроменени части

### Формат на работния лог (само това, нищо повече)
```
[DONE] #42 BootSequenceScreen.kt
[DONE] #43 CRTOverlay.kt
[NEXT] #44 PriceText.kt
```

---

## КОНТЕКСТ — НЕ ОБЯСНЯВАЙ, САМО ПРОЧЕТИ

- Проект: CryptoDept Android (Kotlin + Compose + Hilt + Room)
- Тема: "Wall Street Terminal 90s" — черен фон, зелен (#00FF41) и кехлибарен (#FFB000) текст, моноширинен шрифт JetBrains Mono
- Архитектура: Clean Architecture + MVVM + Repository
- v1 е завършена и работи
- Правила: виж по-долу

---

## 🎨 ВИЗУАЛЕН СТАНДАРТ

### Забранени елементи
- ❌ borderRadius > 2dp
- ❌ Dynamic Material You colors  
- ❌ Sans-serif шрифтове за данни
- ❌ Светли фонове
- ❌ Shadow/elevation
- ❌ Анимации > 600ms

### Задължителни елементи
- ✅ JetBrains Mono за всички данни
- ✅ Квадратни ъгли на картите
- ✅ Border 1dp #1A2E1A
- ✅ Padding 12dp навсякъде
- ✅ CRTOverlay на всички екрани
- ✅ PhosphorMode от DataStore

---

## 📋 РАБОТЕН ЛОГ — Фаза 3 & 4
#,Стъпка,Файлове засегнати,Приоритет,Статус
67,Prediction Ensemble Engine (Type Fix),domain/usecase/prediction/PredictionEnsembleEngine.kt,🔴 Висок,✅
68,DataStore Extension (Notifications),data/datastore/PreferencesManager.kt,🔴 Висок,✅
69,CRT/White Mode Implementation,"ui/theme/Color.kt, ui/theme/Theme.kt",🔴 Висок,✅
70,Centralized Theme Model,ui/theme/Color.kt (TerminalColorSet),🔴 Висок,✅
71,NavGraph Injection & Routes,ui/navigation/NavGraph.kt,🔴 Висок,✅
72,MainActivity Screensaver Logic,MainActivity.kt,🟡 Среден,✅
73,Settings Screen Integration,ui/settings/SettingsScreen.kt,🔴 Висок,✅
74,Legacy Naming Compatibility,ui/theme/Color.kt (WallStreet aliases),🟡 Среден,✅
---

## ПРИОРИТИЗИРАН РЕД НА ИЗПЪЛНЕНИЕ
