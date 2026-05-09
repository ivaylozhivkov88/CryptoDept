#!/bin/sh
echo "Running pre-commit checks..."

# Check for hardcoded Gemini API keys (typical pattern)
if git diff --cached --name-only | xargs grep -l "AIzaSy[A-Za-z0-9_-]\{30,\}" 2>/dev/null; then
    echo "ERROR: API key detected in staged changes!"
    exit 1
fi

# Run Detekt
./gradlew detekt --quiet
if [ $? -ne 0 ]; then
    echo "ERROR: Detekt found issues. Commit blocked."
    exit 1
fi

echo "OK"
