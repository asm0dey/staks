# Publishing to Maven Central

This project is set up to publish to Maven Central via GitHub Actions. This document explains how to set up the necessary secrets and how to trigger a new release.

## Setting Up GitHub Secrets

To publish to Maven Central, you need to set up the following secrets in your GitHub repository:

1. Go to your repository on GitHub
2. Click on "Settings" > "Secrets and variables" > "Actions"
3. Add the following secrets:

| Secret Name | Description |
|-------------|-------------|
| `OSSRH_USERNAME` | Your Sonatype OSSRH username |
| `OSSRH_PASSWORD` | Your Sonatype OSSRH password |
| `SIGNING_KEY` | Your GPG private key in ASCII-armored format |
| `SIGNING_PASSWORD` | The passphrase for your GPG key |

### Getting Your GPG Key in ASCII-armored Format

If you already have a GPG key, you can export it in ASCII-armored format with:

```bash
gpg --export-secret-keys --armor YOUR_KEY_ID
```

If you don't have a GPG key yet, you can create one with:

```bash
gpg --gen-key
```

Follow the prompts to create your key, then export it as shown above.

## Triggering a Release

The publishing workflow is triggered automatically when you create a new release on GitHub:

1. Go to your repository on GitHub
2. Click on "Releases" > "Create a new release"
3. Enter a tag version (e.g., `v1.0.0`)
4. Enter a release title and description
5. Click "Publish release"

The GitHub Actions workflow will automatically build, test, and publish the library to Maven Central.

## Manual Publishing

You can also trigger the publishing workflow manually:

1. Go to your repository on GitHub
2. Click on "Actions" > "Publish to Maven Central"
3. Click "Run workflow" > "Run workflow"

## Local Publishing

For testing purposes, you can also publish locally:

1. Add your Sonatype credentials and GPG key information to your `~/.gradle/gradle.properties` file:

```properties
ossrhUsername=your-username
ossrhPassword=your-password
signingKey=your-gpg-key
signingPassword=your-gpg-passphrase
```

2. Run the publish task:

```bash
./gradlew publish
```

## Verifying the Publication

After publishing, you can verify that your artifact is available on Maven Central by searching for it at:

https://search.maven.org/search?q=g:com.github.asm0dey%20AND%20a:staks

Note that it may take some time for the artifact to appear on Maven Central after publishing.