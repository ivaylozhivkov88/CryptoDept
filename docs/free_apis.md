# Free API Strategy & Limits

## Overview
CryptoDept aims to provide elite analytics using free API tiers where possible. This document tracks limits and required integration changes.

## Etherscan API (Ethereum)
**Status:** Active (Free Tier)
**Usage:** Gas prices, Whale tracking.

### Critical Changes (Effective July 1, 2026)
1. **Result Limit Reduction**: Maximum records per request reduced from **10,000 to 1,000**.
   - **Impact**: Whale tracking logic MUST use pagination if requesting large transaction lists.
   - **Action**: Update any `txlist` calls to include `offset=1000` and handle multi-page fetching.
2. **Internal Transactions**: `getinternaltransactionsbyblockrange` moving to **PRO PLAN ONLY**.
   - **Impact**: Do NOT use this endpoint for free users.
   - **Action**: Use standard `txlist` or individual address internal tx queries if still available on free tier.

## Helius API (Solana)
**Status:** Active (Free Tier)
**Usage:** Solana whale tracking.
- **Limit**: 100,000 requests/month.
- **Strategy**: Filter for transactions > $1M USD.

## Mempool.space (Bitcoin)
**Status:** Active (Public / No Key)
**Usage:** BTC whale tracking.
- **Limit**: Rate limited by IP, but generous.
- **Strategy**: Scrape recently mined blocks for high-value outputs.

## CoinGecko API
**Status:** Active (Demo Plan)
**Usage:** Market prices, metadata.
- **Limit**: 30 calls/minute.
- **Strategy**: Cache aggressively (5-10 mins).

## Binance API (Public)
**Status:** Active
**Usage:** Real-time prices, Funding rates.
- **Limit**: Weight-based (generous for public data).

## Alternative.me (Fear & Greed)
**Status:** Active (Public)
**Usage:** Market sentiment index.

## CryptoPanic API
**Status:** Active (Free Tier)
**Usage:** News aggregation.
- **Limit**: Rate limited.
