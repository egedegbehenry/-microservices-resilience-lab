# Windows laptop migration

## Repository and prerequisites

The existing remote is https://github.com/egedegbehenry/-microservices-resilience-lab (the repository name starts with a hyphen). Preserve the existing history.

Install Git for Windows, a JDK **21**, and IntelliJ IDEA with Java/Maven support. Set `JAVA_HOME` to the JDK directory and add its `bin` directory to PATH. Verify `git --version` and `java -version` in a new PowerShell window. Install Python 3 if running the cross-service acceptance scripts. Install Grafana k6 using the Windows installer linked from https://grafana.com/docs/k6/latest/set-up/install-k6/ and verify `k6 version`.

Clone into a normal local development directory, preferably outside OneDrive:

```powershell
git clone https://github.com/egedegbehenry/-microservices-resilience-lab.git microservices-resilience-lab
cd microservices-resilience-lab
git switch main
```

GitHub authentication is needed if repository access requires it. Sign in on the new laptop; never copy credentials into this repository.

## IntelliJ and Maven

Open the cloned root folder in IntelliJ. There is no root aggregator POM: these are two independent Maven projects. In the Maven tool window use **Add Maven Projects** to link both `booking-service/pom.xml` and `availability-service/pom.xml`. Alternatively open each POM as a project in separate windows. Set Project SDK and Maven importer/runner JDK to 21, choose Maven Wrapper, then reload all Maven projects. IntelliJ regenerates its local project files.

Each service includes `mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties`. Both use the Maven 3.10.0 **only-script** wrapper; no wrapper JAR is required. No global Maven installation is needed. The first run requires network access to download Maven, plugins and dependencies from Maven Central. Both POMs specify Spring Boot 4.1.2 and Java 21.

From the project root, build and run all automated tests:

```powershell
Push-Location booking-service
.\mvnw.cmd clean verify
Pop-Location
Push-Location availability-service
.\mvnw.cmd clean verify
Pop-Location
```

Check each command's exit code before continuing. JUnit reports are under each service's `target/surefire-reports`. Tests create local servers/stubs and need no external database or running peer service. Tests exercise both normal and lab configuration. Keep optional environment overrides unset when running the test suite.

## Run both services

No mandatory environment variables, external credentials, Docker, database, or external monitoring stack are required. No Docker/monitoring deployment files are present. Monitoring uses Actuator and Resilience4j endpoints.

Start Availability first in one root PowerShell terminal:

```powershell
java -jar availability-service/target/availability-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=lab
```

Start Booking in another root terminal:

```powershell
java -jar booking-service/target/booking-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=lab
```

Availability listens on **8082**, Booking on **8081**. For IntelliJ run the respective `AvailabilityServiceApplication` and `BookingServiceApplication` main classes using their matching modules and JDK 21; set Active profiles to `lab`, or add program argument `--spring.profiles.active=lab`. Keep both run configurations running.

The lab profile binds to localhost and enables fault controls and detailed monitoring. Use it only for local demonstrations.

```powershell
Invoke-RestMethod http://127.0.0.1:8082/actuator/health
Invoke-RestMethod http://127.0.0.1:8081/actuator/health
Invoke-RestMethod http://127.0.0.1:8081/api/bookings/availability
Invoke-RestMethod http://127.0.0.1:8081/actuator/circuitbreakers
Invoke-RestMethod http://127.0.0.1:8082/lab/state
```

## Configuration and demonstrations

The actual checked-in defaults differ between services:

| Optional variable | Booking | Availability |
|---|---|---|
| `RETRY_ATTEMPTS` | 3 | 3 |
| `RETRY_DELAY` | 500ms | 100ms |
| `RETRY_EXPONENTIAL` | false | false |
| `RATE_LIMIT` | 2 | 20 |
| `RATE_PERIOD` | 5s | 1s |
| `RATE_TIMEOUT` | 0ms | 0ms |
| `CB_WAIT` | 10s | 10s |

Booking also supports `AVAILABILITY_SERVICE_URL` (default `http://localhost:8082`), `CONNECT_TIMEOUT` (1s), and `READ_TIMEOUT` (8s). These are non-sensitive settings. No local secret configuration was found in the project audit. If introducing credentials later, use an external secure store or ignored local configuration and restore the real values separately.

**Before following DEMO.md or running the Python acceptance/k6 demonstrations**, set these overrides in **both service terminals**, then start/restart the applications using the commands above:

```powershell
$env:RATE_LIMIT='20'
$env:RATE_PERIOD='1s'
$env:RETRY_DELAY='100ms'
```

These match the demonstration assumptions in README/DEMO without changing application defaults. Without them Booking can reject the four calls needed to open its breaker. In IntelliJ add the same environment variables to each run configuration.

Follow [DEMO.md](DEMO.md) for CLOSED -> OPEN -> HALF_OPEN -> CLOSED, failed recovery, independent breaker pairs and retry demonstrations. Run scenarios sequentially against idle lab instances:

```powershell
python scripts/verify-lab.py
k6 run k6-tests/circuit-breaker.js
k6 run k6-tests/recovery.js
k6 run -e VUS=8 -e DURATION=10s k6-tests/rate-limiting.js
.\scripts\Run-K6.ps1
```

The Python suite requires both live services and takes several minutes. k6 needs the running lab services; expected 429/503 responses are part of fault demonstrations. Historical evidence is in `test-results/RESULTS.md` and the tracked JSON summaries; it is separate from migration build verification.

## Troubleshooting and excluded files

- If ports are occupied, stop your own previous runs or select alternate ports using `--server.port`. When changing Availability's port, update Booking's `AVAILABILITY_SERVICE_URL`. Pass matching `--upstream`/`--downstream` to Python or `-Upstream`/`-Downstream` to `Run-K6.ps1`.
- If Maven cannot clean `target`, stop relevant running apps/builds and check Windows file locks, antivirus or OneDrive synchronization. The old workspace encountered this during migration; a separate checkout is used to distinguish file-lock problems from source build failures. Do not delete source files to fix a clean failure.
- If dependencies fail to download, check network/proxy access and Maven Central availability. Any private Maven settings/credentials under your user profile must be recreated securely outside Git.
- If Java release 21 is unsupported, correct `JAVA_HOME`, PATH, Project SDK and Maven runner JDK. Reopen terminals after installing tools.
- If lab routes return 404, activate `lab` and restart. If calls return 429 unexpectedly, check the actual defaults above. Use the documented overrides for demonstrations.
- If PowerShell blocks the optional `Run-K6.ps1`, follow your organization's execution policy or invoke the individual `k6 run` commands.

Git intentionally excludes generated `target` files, IntelliJ metadata/caches, Python caches, runtime JAR copies, local environment/secret files and logs. Tracked Java/tests, POMs, wrapper scripts/properties, application YAML, k6/scripts, README/DEMO (including Mermaid diagrams), and useful JSON/results documentation rebuild the project. Generated service `HELP.md` files contain generic reference links only. No additional essential project files were found in ignored locations.

For personal historical records, optionally copy ignored raw logs and IDE workspace preferences to a separate private backup. They are not needed to rebuild. JDKs, tool installations, Maven caches, GitHub authentication, and files outside this project are not backed up by this repository. Keep the old laptop until the verified GitHub clone builds on the new one.
