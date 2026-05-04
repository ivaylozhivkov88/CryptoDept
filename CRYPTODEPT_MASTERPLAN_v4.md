# CryptoDept — MASTERPLAN v4.0 | WORLD #1 MODE
## Цел: Номер 1 крипто анализатор в света
### Версия 4.0 | Claude Sonnet 4.6 | 02.05.2026

---

> **КРИТИЧНО ПРАВИЛО:** Преди всеки промпт изчислявай нужните токени спрямо текущия си лимит. Ако операцията е рискова за квотата, спри и съветвай за изчакване.
> **DYNAMIC PLAN:** Изтривай всеки изпълнен промпт от този файл веднага след приключване.
> Изпълнявай пластовете подред. Започваме от Слой I — без него нищо друго не е стабилно.

---

# НОВО ПРАВИЛО (добавено 2026-05-04):

---

---

# ═══════════════════════════════════════
# СЛОЙ II — DATA & INTEL (INTELLIGENCE)
# ═══════════════════════════════════════

### ПРОМПТ #126 — Sentiment — Реална интеграция (CryptoPanic + Reddit)
```
ЗАДАЧА:Sentiment Analyzer с CryptoPanic RSS и Reddit r/CryptoCurrency. BULLISH/BEARISH word scoring.
Покажи SentimentResult в AnalysisScreen и Dashboard badge.
```

### ПРОМПТ #154 — Narrative AI Report Style (Crypto Apostles Style)
```
ЗАДАЧА: Рефакторирай AI Report Generator (в AnalysisViewModel), 
за да следва "Crypto Apostles" стила (Снимка 1).
- Промптът към Gemini трябва да изисква: гръмко заглавие с 🚀, 
провокативен увод, ➡️ секции (Bear Trap, Wall of Worry и др.), 
конкретна прогноза и "The CryptoDept Verdict". Когато променяме 
текстовия промпт за анализ, използваме вече наличните данни от всички 
анализи (името и данните на всеки анализ). Обобщаваме всички анализи 
като текстово съобщение, без да се изисква ново събиране на данни – 
просто агрегиране и форматиране на вече изчислените резултати.

- Добави Hashtags и Call to Action в края.
- Обнови UI на AlertDialog в AnalysisScreen, за да побира по-дългия и 
форматиран текст.
```

// PROMPT #131 removed: Whale Tracker feature deprecated per request.

### ПРОМПТ #132 — Crypto Correlation Matrix
```
ЗАДАЧА: Pearson Correlation матрица за последните 30 дни. heat map grid визуализация с Canvas.
Добави Screen.Correlation + "MATRIX" команда.
```

### ПРОМПТ #135 — Bitcoin Halving Cycle Analyzer
```
ЗАДАЧА: Анализ на Bitcoin halving cycles (Accumulation, Bull Early, Bull Late, Bear).
Нов екран SeasonalScreen с прогрес бар и препоръки.
```

### ПРОМПТ #136 — DeFi Protocol Monitor (DefiLlama)
```
ЗАДАЧА: Мониторинг на TVL и Yield от DefiLlama. Топ протоколи и APY възможности.
```

### ПРОМПТ #137 — Macro Correlation Dashboard
```
ЗАДАЧА: Корелация на BTC с DXY, SPX, GOLD, VIX чрез Alpha Vantage API.
```

### ПРОМПТ #138 — Advanced Signal Composer
```
ЗАДАЧА: Потребителски trading signals (RSI + MACD + Vol). Rule builder с drag-and-drop.
```

### ПРОМПТ #127 — Custom Composite Alerts
```
ЗАДАЧА: Alert система с AND/OR логика (Price + RSI + Volume).
```

---

# ═══════════════════════════════════════
# СЛОЙ III — ВИЗУАЛНА ДОМИНАЦИЯ (VISUALS)
# ═══════════════════════════════════════

### ПРОМПТ #125 — Adaptive Icon Optimization
```
ЗАДАЧА: Оптимизирай иконата за малки размери (simplified foreground). Обнови res/mipmap-anydpi-v26/ic_launcher.xml.
```

### ПРОМПТ #128 — Coin Comparison Mode
```
ЗАДАЧА: Side-by-side сравнение на 2 монети (Price, RSI, Funding, Correlation).
```

### ПРОМПТ #129 — Share Prediction Card (Image Export)
```
ЗАДАЧА: Експорт на прогнозното решение као PNG изображение чрез FileProvider и Canvas capture.
```

### ПРОМПТ #140 — Heatmap Screensaver (S&P Style)
```
ЗАДАЧА: Трети скрийнсейвър режим — Treemap heatmap на пазара (размер по Market Cap, цвят по 24h Change).
```

---

# ═══════════════════════════════════════
# СЛОЙ IV — USER EXPERIENCE DOMINATION
# ═══════════════════════════════════════

### ПРОМПТ #124 — App Shortcuts (Long-press Icon)
```
ЗАДАЧА: Добави статични shortcuts (Dashboard, Alerts, Risk) към иконата на приложението.
```

### ПРОМПТ #139 — AI Daily Market Summary (Push Notification)
```
ЗАДАЧА: AI Briefing Worker (08:00 сутрин). Gemini summary на пазара като нотификация.
```

### ПРОМПТ #141 — FIRST RUN EXPERIENCE (Onboarding)
```
ЗАДАЧА: 3-стъпков flow (Цели, Рисков профил, Опит). Авто-конфигурация на терминала според опита.
```

### ПРОМПТ #142 — “WHAT SHOULD I DO NOW?” BUTTON
```
ЗАДАЧА: Бутон, който анализира портфолио + рисков скор + сигнали и дава ACTION (Wait/Buy/Sell).
```

### ПРОМПТ #152 — Terminal Boot Sequence UI
```
ЗАДАЧА: Ретро Boot анимация с TypewriterText. Етапи: Initialization, Dashboard, AI Coach, Journal, Disclaimer.
```

### ПРОМПТ #133 — Tax Report Export (CSV)
```
ЗАДАЧА: Генерирай CSV отчет от Trade Journal за данъчни цели.
```

### ПРОМПТ #143 до #148 — Микро-интеракции и Доверие
```
#143: Personal Performance Tracker (Win rate, AI analysis)
#144: Trust Layer (Model accuracy stats)
#145: CRT Glitch effects при грешка, sound при alert.
#146: [WHY?] бутон за breakdown на всеки скор.
#147: Smart Push Notifications (Cooldown & High Conviction).
#148: Offline Mode (Last known market state).
```

### ПРОМПТ #149 & #150 — Архитектурен Одит и Унификация
```
#149: Memory Leak Одит (Lifecycle aware collectors, derivedStateOf).
#150: UI Унификация (0dp radius, JetBrains Mono, NumberFormatter Locale.US).
```

---

## Защо тези функции правят CryptoDept номер 1

Нито едно безплатно крипто приложение в Play Store не комбинира:
1. CRT терминален стил (визуална диференциация)
2. Prediction Ensemble с 6 модела + Accuracy Tracker
3. On-chain Whale Tracker в реално време
4. Bitcoin Halving Cycle Analyzer с исторически данни
5. Crypto Correlation Matrix (intermarket analysis)
6. Macro Correlation (крипто vs акции/злато/долар)
7. Signal Composer (custom trading signals)
8. AI Trade Coach с journal analysis (Gemini)
9. Strategy Backtester с equity curve
10. Tax Report Export

*Restructured by CryptoDept Senior Architect | 02.05.2026*
