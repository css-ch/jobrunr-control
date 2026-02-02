# Release to Maven Central

This document describes how to release the JobRunr Control Extension to Maven Central.

> **🚀 Quick Start:** For a condensed setup guide, see [Release Quick Start](RELEASE_QUICKSTART.md)

## Prerequisites

### 1. Maven Central Account Setup

1. Create an account at [Maven Central](https://central.sonatype.com/)
2. Verify your namespace (e.g., `ch.css.jobrunr`)
3. Generate user token for publishing

### 2. GPG Key Setup

Generate a GPG key pair for signing artifacts:

```bash
# Generate key
gpg --full-generate-key
# Use RSA and RSA, 4096 bits, no expiration
# Use your email address

# List keys
gpg --list-secret-keys --keyid-format=long

# Export private key (replace KEY_ID with your key ID)
gpg --armor --export-secret-keys KEY_ID

# Publish public key to key server
gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID
```

### 3. GitHub Secrets Configuration

Configure the following secrets in your GitHub repository (Settings → Secrets and variables → Actions):

| Secret Name              | Description                     | How to Obtain                                |
|--------------------------|---------------------------------|----------------------------------------------|
| `MAVEN_CENTRAL_USERNAME` | Maven Central username/token    | From Maven Central account settings          |
| `MAVEN_CENTRAL_PASSWORD` | Maven Central password/token    | From Maven Central account settings          |
| `GPG_PRIVATE_KEY`        | GPG private key (ASCII armored) | `gpg --armor --export-secret-keys KEY_ID`    |
| `GPG_PASSPHRASE`         | GPG key passphrase              | The passphrase you set when creating the key |
| `JOBRUNR_USERNAME`       | JobRunr Pro repository username | From JobRunr Pro account                     |
| `JOBRUNR_PASSWORD`       | JobRunr Pro repository password | From JobRunr Pro account                     |
| `JOBRUNR_PRO_LICENSE`    | JobRunr Pro license key         | From JobRunr Pro license                     |

**📖 For detailed step-by-step instructions, see [GitHub Secrets Setup Guide](GITHUB_SECRETS_SETUP.md)**

## Release Process

### Option 1: Manual Workflow Dispatch (Recommended)

1. Go to GitHub Actions → Release to Maven Central
2. Click "Run workflow"
3. Enter the release version (e.g., `1.0.0`)
4. Click "Run workflow"

The workflow will:

- Update all POM versions to the release version
- Build and test the project
- Sign artifacts with GPG
- Deploy to Maven Central
- Create a Git tag
- Create a GitHub release
- Update POMs to next development version (e.g., `1.0.1-SNAPSHOT`)
- Push changes back to the repository

### Option 2: GitHub Release

1. Create a new release in GitHub
2. Create a new tag (e.g., `v1.0.0`)
3. Publish the release

The workflow will automatically:

- Extract version from the tag name
- Deploy to Maven Central
- Update POMs to next development version

## Release Workflow Details

### What Gets Published

The following artifacts are deployed to Maven Central:

- `jobrunr-control-extension` (runtime) - JAR, sources, javadoc
- `jobrunr-control-extension-deployment` - JAR, sources, javadoc

**Note:** The parent POMs and the example module are NOT published to Maven Central.

### Version Management

- **Release versions**: `1.0.0`, `2.0.0`, etc. (no SNAPSHOT suffix)
- **Development versions**: `1.0.1-SNAPSHOT`, `2.0.1-SNAPSHOT`, etc.

The workflow automatically:

1. Sets release version for deployment
2. After successful deployment, increments patch version and adds `-SNAPSHOT`
3. Commits and pushes the next development version

### Signing and Publishing

All artifacts are:

- Signed with GPG (required by Maven Central)
- Published with source and javadoc JARs
- Automatically released to Maven Central (no manual staging)

## Verifying the Release

### 1. Check Maven Central

Search for your artifact at: https://central.sonatype.com/

It may take a few minutes to appear after the workflow completes.

### 2. Check GitHub Release

The workflow creates a GitHub release with auto-generated release notes.

### 3. Test the Release

Add the dependency to a test project:

```xml

<dependency>
    <groupId>ch.css.jobrunr</groupId>
    <artifactId>jobrunr-control-extension</artifactId>
    <version>1.0.0</version>
</dependency>
```

Run `mvn dependency:resolve` to verify it downloads from Maven Central.

## Troubleshooting

### GPG Signing Fails

**Error:** `gpg: signing failed: Inappropriate ioctl for device`

**Solution:** Ensure `--pinentry-mode loopback` is configured in the POM (already done).

### Maven Central Deployment Fails

**Error:** `401 Unauthorized`

**Solution:**

- Verify `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` secrets are correct
- Ensure you're using a user token, not your account password
- Check namespace verification in Maven Central

### Missing Required Metadata

**Error:** `Missing required metadata: description, url, licenses, developers, scm`

**Solution:** All required metadata is configured in the parent POM. Ensure child modules inherit from the parent.

### Artifact Already Exists

**Error:** `A component with the same coordinates already exists`

**Solution:** Maven Central does not allow overwriting released versions. You must:

1. Increment the version number
2. Release with the new version

## Release Checklist

Before releasing:

- [ ] All tests pass
- [ ] Documentation is up to date
- [ ] CHANGELOG is updated (if applicable)
- [ ] Version number follows semantic versioning
- [ ] GitHub secrets are configured
- [ ] GPG key is published to key server
- [ ] Maven Central namespace is verified

## Manual Release (Alternative)

If you prefer to release manually from your local machine:

```bash
# Set release version
mvn versions:set -DnewVersion=1.0.0

# Build and deploy
mvn clean deploy -P release \
  -Dgpg.passphrase=YOUR_PASSPHRASE

# Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Set next development version
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
git add pom.xml */pom.xml */*/pom.xml
git commit -m "Prepare next development iteration"
git push
```

**Note:** You must configure Maven settings.xml with your credentials for manual releases.

## Post-Release

After a successful release:

1. **Announce**: Update documentation, notify users
2. **Monitor**: Watch for issues on GitHub and Maven Central
3. **Update Examples**: Update example projects to use the new version
4. **Documentation**: Update README and version badges

## Support

For issues with:

- **Maven Central**: https://central.sonatype.org/support/
- **GPG**: https://gnupg.org/documentation/
- **GitHub Actions**: Check workflow logs in the Actions tab
