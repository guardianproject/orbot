## CI/CD and Building Orbot

[![Build Status](https://app.bitrise.io/app/0e76c31b8e7e1801/status.svg?token=S2weJXueO3AvrDUrrd85SA&branch=master)](https://app.bitrise.io/app/0e76c31b8e7e1801)

We use [bitrise](https://app.bitrise.io/app/0e76c31b8e7e1801) for CI.  Pull requests from within 
the project (and pushes to `master`) are automatically built, while PRs from forks must be approved to protect
secrets and prevent abusing the build server.

A build will do a few things:
    * Build a universal APK and make it available for download (`nightlyDebug`).
    * Run tests
        * Unit tests are run (and results are available) right in bitrise.
        * Espresso tests are run in Browserstack App Automate on real devices.
    * Make the app available for testing on real devices via Browserstack.
        * Accessing this requires a login to our browserstack account.

### Secrets

Bitrise builds can require secrets.  In our case, the secrets are our Browserstack credentials.  Thus, before kicking off a build for a PR, be sure that PR isn't trying to log our credentials from environment variables.