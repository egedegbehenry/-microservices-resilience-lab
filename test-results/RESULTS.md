# Executed validation results

Generated from local test evidence; no projected performance numbers.

Environment: Windows, Java 21.0.12.1, Spring Boot 4.1.2, Resilience4j 2.4.0. Full services on loopback ports 18081/18082; existing IntelliJ processes on 8081/8082 were preserved. k6 v2.3.0 was downloaded to a temporary directory, not installed globally.

## JUnit and builds

| Service | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| booking | 18 | 0 | 0 | 0 |
| availability | 14 | 0 | 0 | 0 |

Both Maven package builds completed with exit code 0. See local `booking-build.log` and `availability-build.log` (logs are ignored by Git). JUnit reports remain under each service’s `target/surefire-reports`. Tests include AOP interception, original upstream service proxying, absence of lab operations without the lab profile, actual Actuator HTTP responses, both breaker recovery paths, retry counts, concurrent probes, status 429 policy and limiter configurations.

## HTTP acceptance evidence

Executed: 2026-10-27T10:21:13+0000. Raw evidence: [acceptance.json](acceptance.json).

- preserved five-second delay through upstream with default eight-second read timeout: 5.034
- 1 normal HTTP communication: A=CLOSED, B=CLOSED
- 2 downstream independently open: A=CLOSED, B=OPEN
- 3 upstream independently open: A=OPEN, B=CLOSED
- 4 both open: A=OPEN, B=OPEN
- 5 upstream probes: A=HALF_OPEN, B=CLOSED
- upstream successful recovery: A=CLOSED, B=CLOSED
- 6 downstream probes: A=CLOSED, B=HALF_OPEN
- downstream successful recovery: A=CLOSED, B=CLOSED
- 7 simultaneous half open: A=HALF_OPEN, B=HALF_OPEN
- both recovered through HTTP: A=CLOSED, B=CLOSED
- failed half open recovery: state=OPEN, attempts=18
- failed half open recovery: state=OPEN, attempts=18
- retry succeeds on third attempt: state=CLOSED, attempts=3
- retry exhausted at three attempts: state=CLOSED, attempts=3
- retry succeeds on third attempt: state=CLOSED, attempts=3
- retry exhausted at three attempts: state=CLOSED, attempts=3
- eight simultaneous upstream requests: [200, 200, 200, 200, 200, 200, 200, 200]
- bounded rate limit burst: state=CLOSED, attempts=20
- bounded rate limit burst: state=CLOSED, attempts=20

All 22 additional Actuator endpoint/metric checks returned HTTP 200; raw evidence: [monitoring.json](monitoring.json). A separate Booking process with a 3-second read timeout and one attempt returned HTTP 504 in approximately 3 seconds against the preserved 5-second delay; raw evidence: [timeout.json](timeout.json). The default 8-second read timeout succeeded through the original Booking delay endpoint.

## k6 measurements

Default quotas: 20 requests/second at both services, zero permit wait, three retry attempts, 100ms fixed delay, 10-second OPEN wait. Each scenario uses 10 seconds of load; recovery additionally waits during setup. Normal/failure/recovery/retry/upstream/downstream/slow use one VU at 250ms pacing. Heavy/rate/simultaneous use eight VUs at 10ms pacing. Simultaneous issues a batch to both services.

| Scenario | Operations | Rate/s¹ | Average ms | p95 ms | 200 | 429 | 503 | Unexpected | Transitions² | Extra retries² |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| circuit-breaker | 36 | 3.21 | 26.67 | 224.58 | 0 | 0 | 36 | 0.00% | 1 | 8 |
| dependency-failure | 32 | 2.87 | 60.25 | 346.99 | 0 | 0 | 32 | 0.00% | 2 | 16 |
| downstream | 40 | 3.55 | 2.48 | 3.48 | 40 | 0 | 0 | 0.00% | 0 | 0 |
| heavy | 4307 | 385.20 | 6.67 | 20.74 | 218 | 4089 | 0 | 0.00% | 0 | 0 |
| normal | 39 | 3.43 | 11.09 | 14.20 | 39 | 0 | 0 | 0.00% | 0 | 0 |
| rate-limiting | 5461 | 489.50 | 3.48 | 13.74 | 220 | 5241 | 0 | 0.00% | 0 | 0 |
| recovery | 39 | 1.73 | 6.88 | 9.39 | 39 | 0 | 0 | 0.00% | 1 | 0 |
| retry | 38 | 3.23 | 26.20 | 61.12 | 38 | 0 | 0 | 0.00% | 0 | 2 |
| simultaneous | 5620 | 503.48 | 8.92 | 28.18 | 220 | 5400 | 0 | 0.00% | 0 | 0 |
| slow | 13 | 1.16 | 518.69 | 527.77 | 13 | 0 | 0 | 0.00% | 0 | 0 |
| upstream | 40 | 3.52 | 5.62 | 7.06 | 40 | 0 | 0 | 0.00% | 0 | 0 |

¹ k6 counter rates use its reported run interval, including setup/teardown overhead. Latency is from operation calls only. Full JSON summaries are in [k6/](k6/). Counts of 504, actual attempts, circuit-open responses and all checks are retained in those summaries.

² Transition and retry totals are summed across both service counters after setup. Recovery setup observes OPEN then HALF_OPEN; its measured transition is HALF_OPEN → CLOSED. Counts include additional retry events, not initial attempts. Counters persist independently of the bounded event history.

High traffic produces fast expected 429 responses; low aggregate latency does not imply high successful throughput. Continuous failure opens breakers and replaces actual operation attempts with fast rejections. Retry recovery produces two extra B attempts and then successful calls. Slow dependency calls increase operation latency. These are short local runs with other desktop processes present, not a production capacity assessment.

Quota 5/10/20, fixed zero wait, 100ms refresh with waiting, and insufficient timeout were separately exercised by JUnit. Full end-to-end performance comparisons for every quota/backoff/refresh combination are not claimed. Use the README matrix to collect further measurements.

## Limits and next steps

- The original five-second delay endpoints remain lab-only. The new combined delay uses a bounded configurable plan; timeout behavior is mapped to 504.
- Lab reset/configuration is intended for quiescent local demonstrations; no distributed control plane is implemented.
- The second dependency is simulated inside Availability, as required; no third process or database was introduced.
- Original IntelliJ instances need a Maven reload/restart with the lab profile to serve the changed code on ports 8081/8082.
- No Git commits, pushes, branch changes, merges or remote creation were performed. Review the source and guide with ChatGPT before version-control work.
