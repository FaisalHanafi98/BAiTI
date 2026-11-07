# 8 November 2026 — P05d platform access

Added platform login and the protected tenant list/create routes. The platform
chain uses a dedicated JWT audience and purpose with the existing signing key.
Onboarding uses the existing privileged JDBC pool with a transaction-local
tenant setting. The full native suite passed 311 tests, including I-9 rollback.
Next is first-use password enforcement, async audit and the full I-suite.

[Test evidence](../06_TESTING_EVIDENCE/2026-11-08_platform_onboarding.md)
