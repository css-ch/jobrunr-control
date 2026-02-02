# GitHub Secrets Setup Guide

This guide explains how to configure the required GitHub secrets for releasing to Maven Central.

## Required Secrets

### 1. GPG_PRIVATE_KEY

**What it is:** Your GPG private key in ASCII-armored format

**How to obtain:**

```bash
# List your GPG keys
gpg --list-secret-keys --keyid-format=long

# You'll see output like:
# sec   rsa4096/ABCD1234EFGH5678 2026-02-02 [SC]
# The key ID is: ABCD1234EFGH5678

# Export the private key (replace with your key ID)
gpg --armor --export-secret-keys ABCD1234EFGH5678

# Copy the ENTIRE output including:
# -----BEGIN PGP PRIVATE KEY BLOCK-----
# ...content...
# -----END PGP PRIVATE KEY BLOCK-----
```

**🧪 Test your key before adding to GitHub:**

Run the test script from the repository root:

```bash
./test-gpg-key.sh
```

This script will validate that your key is properly formatted and can be imported successfully.

**Add to GitHub:**

1. Copy the entire output (including BEGIN and END lines)
2. Go to repository Settings → Secrets and variables → Actions
3. Click "New repository secret"
4. Name: `GPG_PRIVATE_KEY`
5. Paste the entire key block
6. Click "Add secret"

### 2. GPG_PASSPHRASE

**What it is:** The passphrase you used when creating your GPG key

**How to obtain:** This is the passphrase you entered during `gpg --full-generate-key`

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `GPG_PASSPHRASE`
4. Value: Your GPG passphrase
5. Click "Add secret"

### 3. MAVEN_CENTRAL_USERNAME

**What it is:** Your Maven Central user token username

**How to obtain:**

1. Log in to [Maven Central](https://central.sonatype.com/)
2. Click on your profile (top right)
3. Go to "View Account"
4. Click "Generate User Token"
5. Copy the **username** part

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `MAVEN_CENTRAL_USERNAME`
4. Value: Your Maven Central token username
5. Click "Add secret"

### 4. MAVEN_CENTRAL_PASSWORD

**What it is:** Your Maven Central user token password

**How to obtain:**

1. Same as above - when you generate a user token
2. Copy the **password** part

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `MAVEN_CENTRAL_PASSWORD`
4. Value: Your Maven Central token password
5. Click "Add secret"

### 5. JOBRUNR_USERNAME

**What it is:** Your JobRunr Pro repository username

**How to obtain:** From your JobRunr Pro account

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `JOBRUNR_USERNAME`
4. Value: Your JobRunr username
5. Click "Add secret"

### 6. JOBRUNR_PASSWORD

**What it is:** Your JobRunr Pro repository password

**How to obtain:** From your JobRunr Pro account

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `JOBRUNR_PASSWORD`
4. Value: Your JobRunr password
5. Click "Add secret"

### 7. JOBRUNR_PRO_LICENSE

**What it is:** Your JobRunr Pro license key

**How to obtain:** From your JobRunr Pro license file or email

**Add to GitHub:**

1. Go to repository Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `JOBRUNR_PRO_LICENSE`
4. Value: Your JobRunr Pro license key
5. Click "Add secret"

## Creating a GPG Key (If You Don't Have One)

```bash
# Generate a new GPG key
gpg --full-generate-key

# Follow the prompts:
# 1. Select: (1) RSA and RSA
# 2. Key size: 4096
# 3. Expiration: 0 (does not expire) or choose a date
# 4. Enter your name and email
# 5. Enter a strong passphrase (you'll need this for GPG_PASSPHRASE)

# Publish your public key to a key server
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# You can also publish to other key servers:
gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
gpg --keyserver pgp.mit.edu --send-keys YOUR_KEY_ID
```

## Verifying Your Setup

After adding all secrets, verify they're configured correctly:

1. Go to repository Settings → Secrets and variables → Actions
2. You should see all 7 secrets listed:
    - `GPG_PASSPHRASE`
    - `GPG_PRIVATE_KEY`
    - `JOBRUNR_PASSWORD`
    - `JOBRUNR_PRO_LICENSE`
    - `JOBRUNR_USERNAME`
    - `MAVEN_CENTRAL_PASSWORD`
    - `MAVEN_CENTRAL_USERNAME`

## Testing

To test your setup without doing a real release:

1. Create a test branch
2. Modify the release workflow to use `-DaltDeploymentRepository=local::file:./target/staging`
3. Run the workflow manually
4. Check the workflow logs for errors

## Troubleshooting

### GPG Key Issues

**Problem:** "gpg: signing failed: No secret key"

**Solution:** Ensure you exported the correct key ID and the entire key block is in the secret.

### Maven Central Authentication Issues

**Problem:** "401 Unauthorized" from Maven Central

**Solution:**

- Verify you're using a **user token**, not your account password
- Regenerate the token if needed
- Ensure the token has publishing permissions

### Passphrase Issues

**Problem:** "gpg: signing failed: Inappropriate ioctl for device"

**Solution:** This is usually handled by the `--pinentry-mode loopback` setting in the POM. If issues persist, verify
your passphrase is correct.

## Security Best Practices

1. **Never commit secrets to the repository**
2. **Use user tokens instead of passwords** for Maven Central
3. **Rotate tokens regularly** (at least annually)
4. **Use a strong passphrase** for your GPG key
5. **Backup your GPG key** in a secure location
6. **Limit repository access** to trusted maintainers

## Support

- **GPG Help:** https://gnupg.org/documentation/
- **Maven Central:** https://central.sonatype.org/support/
- **GitHub Secrets:** https://docs.github.com/en/actions/security-guides/encrypted-secrets
