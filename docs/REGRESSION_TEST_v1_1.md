# v1.1 Regression Test Checklist

Execute all sections on a REAL device (not emulator).

## Pre-test setup

- [ ] Clear app data: Settings → Apps → CryptoDept → Storage → Clear data
- [ ] Have 3 test accounts ready:
  - Free user: any non-admin gmail
  - Admin user: ivaylozhivkov14@gmail.com OR condignia@gmail.com OR test-reviewer@cryptodept.com
  - (Optional) Pro user via test purchase

## SECTION A: Auth & Tier Detection

- [ ] Cold start → onboarding completes
- [ ] Boot Sequence + Risk Disclaimer flow
- [ ] Google Sign-In with NON-admin email
  - [ ] Login succeeds
  - [ ] PreferencesService.isAdmin() returns FALSE
  - [ ] TierAccessManager.currentTier = FREE
- [ ] Sign out, sign in with ADMIN email
  - [ ] Login succeeds
  - [ ] isAdmin() returns TRUE
  - [ ] currentTier = ADMIN
- [ ] Verify admin features visible (Content Studio link)
- [ ] Sign out → currentTier resets to FREE

## SECTION B: Dashboard Adaptive

- [ ] Login as Free user
- [ ] Dashboard shows:
  - [ ] Price ticker (top 5)
  - [ ] AI Pulse SHORT card
  - [ ] Today's Movers
  - [ ] Whale INSIGHT card (processed signal)
  - [ ] Daily AI Pick card
  - [ ] Fear & Greed Gauge
- [ ] Verify NO "Live Whale Feed" visible
- [ ] Verify NO "Full AI Narrative" visible
- [ ] Login as Pro user (or admin)
- [ ] Dashboard shows:
  - [ ] Full AI Narrative (streaming)
  - [ ] Live Whale Feed
  - [ ] Sentiment Matrix
- [ ] Verify Pro tier still shows Daily AI Pick card (Admin sees full predictions)
- [ ] Login as Admin email
- [ ] Verify Predict Engine available in Tools Hub and results show 6 models

## SECTION C: Free Tier Limits

- [ ] Login as Free user
- [ ] Dashboard shows ONLY top 3 tracked coins (even if more are marked in Markets)
- [ ] Try to create 4 alerts
  - [ ] 1st, 2nd, 3rd succeed
  - [ ] 4th triggers limit dialog
  - [ ] Tap "Upgrade to Pro" navigates to paywall
- [ ] Watchlist limit: Add 11th coin to watchlist (star in Markets)
  - [ ] 11th triggers limit dialog
- [ ] Markets screen shows top 50, ends with "Upgrade" banner
- [ ] Login as Pro user
- [ ] Verify no limits (Dashboard shows all tracked coins, can add 10+ coins to watchlist)

## SECTION D: Glossary & Help Icons

- [ ] Navigate Settings → Crypto Glossary
- [ ] 45 entries visible
- [ ] Detailed descriptions shown (Bitcoin, ETH, RSI, etc.)
- [ ] Search "RSI" → only RSI entry
- [ ] Filter by category → entries filter
- [ ] Tap entry → expands
- [ ] Tap "?" icon на Dashboard near AI Pulse
  - [ ] Dialog opens with detailed description
- [ ] Verify "?" icons on at least 15 sections/tools
- [ ] Verify "?" icon size is large and clickable (18dp)

## SECTION E: AccuracyBadge

- [ ] Open Daily AI Pick card
  - [ ] If 10+ predictions verified: shows "X% accuracy"
  - [ ] Otherwise: shows "INSUFFICIENT DATA"
- [ ] Navigate to Analysis screen for BTC
  - [ ] Prediction shows accuracy badge
  - [ ] Disclaimer "Past performance ≠ future results" visible

## SECTION F: Sanity Check

- [ ] Open Position Sizer
- [ ] Enter: Account $1000, Risk 50%, Entry $100, Stop $95
  - [ ] Tap "RUN SANITY CHECK"
  - [ ] CRITICAL warning appears
- [ ] Tap "I understand" → result shows
- [ ] Open Trade Planner
- [ ] Set R:R below 1:1
  - [ ] Tap "ANALYZE SETUP"
  - [ ] WARNING dialog appears

## SECTION G: Paywall

- [ ] Tap any "[UNLOCK]" button
  - [ ] Paywall opens with personalized title
  - [ ] Pricing plans visible
  - [ ] "Free tier remains powerful" section visible
  - [ ] "What we won't do" section visible

## SECTION H: Admin Features

- [ ] Login as admin email
- [ ] Verify Content Studio link visible in Tools Hub
- [ ] Open Content Studio
- [ ] Generate prompt for "Daily Recap" + "Day Trader"
  - [ ] Prompt generates
- [ ] Sign out → log in as non-admin
- [ ] Verify Content Studio link HIDDEN

## SECTION I: Release Build

- [ ] ./gradlew bundleRelease succeeds
- [ ] Install signed APK
- [ ] Verify TestModeFlag.SHOW_TEST_PURCHASE_BUTTON is false in release
