# 💰 CryptoDept Terminal — Pricing Protocol

This document defines the official pricing structure for the CryptoDept Intelligence Unit. These plans are designed to provide flexible access to high-performance quantitative tools while maintaining a 30-40% competitive advantage over current market standards.


---

### 🕹️ How to Fill in Google Play Console (Step-by-Step)

To ensure the code recognizes the products, you must set them up exactly as described below.

#### A. One-Time Access Passes (In-app products)
*Go to: Monetize -> Products -> In-app products*

 Product ID | Name in Console | Price (EUR) | Description |
 :--- | :--- |:------------| :--- |
 **`pro-1d`** | 1 Day Intelligence Pass | €0.99       | 24-hour full terminal access. |
 **`pro-3d`** | 3 Days Intelligence Pass | €1.49       | 72-hour full terminal access. |
 **`pro-7d`** | 7 Days Intelligence Pass | €2.99       | 1-week full terminal access. |

#### B. Recurring Subscriptions (Subscriptions)
*Go to: Monetize -> Products -> Subscriptions*

1. **Create Subscription:** Use the IDs below.
2. **Create Base Plan:** Set as "Auto-renewing".

 Subscription ID | Base Plan ID | Billing Period | Price (EUR) |
 :--- | :--- | :--- |:------------|
 **`pro-30d`** | `monthly-plan` | 1 Month | €9.99       |1 month full terminal access
 **`pro-90d`** | `quarterly-plan` | 3 Months | €19.99      |3 months full terminal access
 **`pro-1y`** | `yearly-plan` | 1 Year | €69.99      |1 year full terminal access

---

### 🛠️ Technical Implementation Notes

1.  **Product Type Sync:** Ensure `pro-1d`, `pro-3d`, and `pro-7d` are created as **"In-app products"** (One-time purchase).
2.  **Tax Inclusion:** Prices in EUR should include VAT where applicable (handled by Google).
3.  **Grace Period:** For subscriptions (`pro-30d`, etc.), enable a **7-day Grace Period** to reduce churn from payment failures.
4.  **Account Hold:** Enable **Account Hold** (30 days) for all subscriptions.

---
### 🛡️ Monetization Strategy & Notes

1.  **Direct Integration:** All payments are processed directly through **Google Play Billing**. No third-party intermediaries (like RevenueCat) are utilized to ensure maximum security and lower overhead.
2.  **Product IDs:** The IDs in Google Play Console must match the code logic: `pro-1d`, `pro-3d`, `pro-7d`, `pro-30d`, `pro-90d`, `pro-1y`.
3.  **Competitive Positioning:** Prices are calibrated to be significantly lower than TradingView (Pro), Glassnode (Advanced), and CoinStats (Premium).
4.  **Entry Hooks:** The **€0.99** daily tier and **€2.99** weekly tier are designed for "event-based" purchasing during high market volatility.

---
*CryptoDept Intelligence Unit — Financial Structure v1.2*
