# Migration validation — 2026-11-11

- Original repository: `main`, initial commit `645fcde071f8d67e3e3ccd7be11570b1a2f0ae54`; fetched GitHub `origin/main` matched before changes.
- Remote: https://github.com/egedegbehenry/-microservices-resilience-lab.git (existing repository, leading hyphen preserved).
- Both POMs specify Java 21. Validation used Java 21.0.12.1 and each service's Maven 3.10.0 wrapper.
- Original workspace `clean verify` could not delete existing `target/classes` subdirectories on Windows. No source changes were made to address this local filesystem problem.
- A separate temporary checkout containing only committed project files passed both complete wrapper `clean verify` commands: Booking **18 tests**, Availability **14 tests**, zero failures/errors/skips. Both executable Spring Boot JARs were packaged successfully. Normal/lab Spring contexts and YAML were exercised by the tests. Maven used the existing user dependency cache and configured repositories; this was not an empty-cache installation test.
- Both independent Maven projects, all Java source/tests, POMs, wrapper scripts/properties, four application YAML files, k6 scripts, helper scripts, Mermaid diagrams in README, DEMO, and historical JSON/results documentation are tracked. No nested Git repositories/submodules, separate diagram assets, Docker files or external monitoring deployment configuration were found.
- Secret-pattern review found no project credentials requiring a safe configuration template. Wrapper credential variable references are generic support code, not stored credentials.
- Reviewed ignored files: generated build output, IntelliJ metadata, Python caches, generic Spring Initializr HELP references, raw logs and runtime JAR copies. No essential unique source/configuration was found there. Existing structured test evidence is retained; optional raw-log/IDE-preference archival is a private manual backup choice.
- Application behavior is unchanged. Service ignore rules now permit a wrapper JAR should one be needed in future; the current only-script wrappers do not require one.
- Migration guide documents actual Booking defaults (2 permits/5s, 500ms retry delay) and explicit demonstration overrides, since older README prose assumes 20 permits/1s and 100ms.
- This validation reran Maven/JUnit. Historical Python acceptance and k6 results were preserved, not rerun or represented as newly executed migration checks. They need two live lab services.

Push/remote commit equality and a fresh remote clone are verified separately after committing; this file does not assert a push happened before it was created.
