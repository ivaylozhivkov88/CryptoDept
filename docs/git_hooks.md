# Git Hooks Setup

To enable automated pre-commit checks, run the following command in your terminal:

```bash
git config core.hooksPath scripts/
```

## Hook: pre-commit
This hook performs the following checks:
1. **Security**: Scans staged changes for hardcoded Gemini API keys.
2. **Quality**: Runs `detekt` static analysis.

If an API key is detected OR if `detekt` finds issues, the commit will be blocked.
