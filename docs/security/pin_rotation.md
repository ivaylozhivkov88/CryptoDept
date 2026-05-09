# Certificate Pinning & Rotation Strategy

## Overview
CryptoDept uses Certificate Pinning to prevent Man-in-the-Middle (MitM) attacks. This is implemented in `NetworkModule.kt` using OkHttp's `CertificatePinner`.

## Current Hosts Pinned
- `api.coingecko.com`
- `api.binance.com`
- `generativelanguage.googleapis.com` (Google Gemini)

## Rotation Process
Every time an SSL certificate is renewed by the provider, the SHA-256 hash of the public key might change. To prevent app breakage:

1. **Primary Pin**: The current active certificate hash.
2. **Backup Pin**: The hash of the next/alternate certificate or the Root CA.

### How to get new pins:
Use the following shell command (requires openssl):
```bash
openssl s_client -servername api.coingecko.com -connect api.coingecko.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
```

### Emergency Fallback:
If all pins fail, a new version of the app must be pushed immediately with updated pins in `NetworkModule.kt`. 

**DO NOT** disable pinning in production unless it's a critical emergency.
