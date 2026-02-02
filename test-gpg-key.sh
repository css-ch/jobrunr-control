#!/bin/bash

# Script to test if your GPG key is properly formatted for GitHub Secrets
# Usage: ./test-gpg-key.sh

set -e

echo "🔍 GPG Key Validation Test"
echo "=========================="
echo ""

# Check if GPG is installed
if ! command -v gpg &> /dev/null; then
    echo "❌ Error: GPG is not installed"
    echo "Install with: brew install gnupg (macOS) or apt-get install gnupg (Linux)"
    exit 1
fi

echo "✅ GPG is installed"
echo ""

# List available keys
echo "📋 Available GPG keys:"
gpg --list-secret-keys --keyid-format=long

echo ""
echo "Enter your GPG Key ID (the part after 'rsa4096/'):"
read -r KEY_ID

if [ -z "$KEY_ID" ]; then
    echo "❌ Error: Key ID cannot be empty"
    exit 1
fi

echo ""
echo "🔑 Exporting private key for Key ID: $KEY_ID"
echo ""

# Export the key
EXPORTED_KEY=$(gpg --armor --export-secret-keys "$KEY_ID")

if [ -z "$EXPORTED_KEY" ]; then
    echo "❌ Error: Failed to export key. Check if the Key ID is correct."
    exit 1
fi

echo "✅ Key successfully exported"
echo ""

# Test import in a temporary GPG home
TEMP_GPG_HOME=$(mktemp -d)
export GNUPGHOME="$TEMP_GPG_HOME"

echo "🧪 Testing key import in temporary keyring..."
echo "$EXPORTED_KEY" | gpg --batch --import 2>&1

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ SUCCESS! Your GPG key is properly formatted"
    echo ""
    echo "📋 Next steps:"
    echo "1. Copy the key to clipboard:"
    echo "   gpg --armor --export-secret-keys $KEY_ID | pbcopy"
    echo ""
    echo "2. Add to GitHub Secrets:"
    echo "   - Go to: Repository → Settings → Secrets and variables → Actions"
    echo "   - Click: New repository secret"
    echo "   - Name: GPG_PRIVATE_KEY"
    echo "   - Value: Paste from clipboard"
    echo ""
    echo "3. Also add GPG_PASSPHRASE secret with your key's passphrase"
else
    echo ""
    echo "❌ FAILED! There's an issue with your GPG key export"
    echo "Please check the key ID and try again"
fi

# Cleanup
rm -rf "$TEMP_GPG_HOME"
