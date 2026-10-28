"""Create a reviewable report from executed JUnit, acceptance and k6 evidence."""
from pathlib import Path
import json, xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
out=root/'test-results'; out.mkdir(exist_ok=True)
lines=['# Executed validation results','','Generated from local test evidence; no projected performance numbers.','',
       'Environment: Windows, Java 21.0.12.1, Spring Boot 4.1.2, Resilience4j 2.4.0. Full services on loopback ports 18081/18082; existing IntelliJ processes on 8081/8082 were preserved. k6 v2.3.0 was downloaded to a temporary directory, not installed globally.','',
       '## JUnit and builds','','| Service | Tests | Failures | Errors | Skipped |','|---|---:|---:|---:|---:|']
for service in ['booking','availability']:
    counts=[0]*4
    for file in (root/f'{service}-service/target/surefire-reports').glob('TEST-*.xml'):
        report=ET.parse(file).getroot()
        for i,key in enumerate(['tests','failures','errors','skipped']): counts[i]+=int(report.attrib[key])
    lines.append(f'| {service} | '+' | '.join(map(str,counts))+' |')
lines+=['','Both Maven package builds completed with exit code 0. See local `booking-build.log` and `availability-build.log` (logs are ignored by Git). JUnit reports remain under each service’s `target/surefire-reports`. Tests include AOP interception, original upstream service proxying, absence of lab operations without the lab profile, actual Actuator HTTP responses, both breaker recovery paths, retry counts, concurrent probes, status 429 policy and limiter configurations.','',
        '## HTTP acceptance evidence','']
if (out/'acceptance.json').exists():
    acceptance=json.loads((out/'acceptance.json').read_text())
    lines+=[f"Executed: {acceptance['timestamp']}. Raw evidence: [acceptance.json](acceptance.json).",'']
    for result in acceptance['results']:
        if 'A' in result: detail=f"A={result['A']['state']}, B={result['B']['state']}"
        elif 'state' in result: detail=f"state={result['state']['state']}, attempts={result['state']['attempts']}"
        else: detail=str(result.get('elapsedSeconds',result.get('statuses','')))
        lines.append(f"- {result['scenario']}: {detail}")
lines+=['','All 22 additional Actuator endpoint/metric checks returned HTTP 200; raw evidence: [monitoring.json](monitoring.json). A separate Booking process with a 3-second read timeout and one attempt returned HTTP 504 in approximately 3 seconds against the preserved 5-second delay; raw evidence: [timeout.json](timeout.json). The default 8-second read timeout succeeded through the original Booking delay endpoint.','',
        '## k6 measurements','',
        'Default quotas: 20 requests/second at both services, zero permit wait, three retry attempts, 100ms fixed delay, 10-second OPEN wait. Each scenario uses 10 seconds of load; recovery additionally waits during setup. Normal/failure/recovery/retry/upstream/downstream/slow use one VU at 250ms pacing. Heavy/rate/simultaneous use eight VUs at 10ms pacing. Simultaneous issues a batch to both services.','',
        '| Scenario | Operations | Rate/s¹ | Average ms | p95 ms | 200 | 429 | 503 | Unexpected | Transitions² | Extra retries² |',
        '|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|']
def metric(metrics,name,key): return metrics.get(name,{}).get('values',{}).get(key,0)
for file in sorted((out/'k6').glob('*.json')):
    data=json.loads(file.read_text(encoding='utf-8-sig')); m=data['metrics']
    lines.append(f"| {file.stem} | {metric(m,'response_status','count')} | {metric(m,'response_status','rate'):.2f} | "
                 f"{metric(m,'operation_duration','avg'):.2f} | {metric(m,'operation_duration','p(95)'):.2f} | "
                 f"{metric(m,'response_status{status:200}','count')} | {metric(m,'response_status{status:429}','count')} | "
                 f"{metric(m,'response_status{status:503}','count')} | {metric(m,'unexpected_failures','rate'):.2%} | "
                 f"{metric(m,'observed_state_transitions','count')} | {metric(m,'observed_retry_events','count')} |")
lines+=['', '¹ k6 counter rates use its reported run interval, including setup/teardown overhead. Latency is from operation calls only. Full JSON summaries are in [k6/](k6/). Counts of 504, actual attempts, circuit-open responses and all checks are retained in those summaries.','',
        '² Transition and retry totals are summed across both service counters after setup. Recovery setup observes OPEN then HALF_OPEN; its measured transition is HALF_OPEN → CLOSED. Counts include additional retry events, not initial attempts. Counters persist independently of the bounded event history.','',
        'High traffic produces fast expected 429 responses; low aggregate latency does not imply high successful throughput. Continuous failure opens breakers and replaces actual operation attempts with fast rejections. Retry recovery produces two extra B attempts and then successful calls. Slow dependency calls increase operation latency. These are short local runs with other desktop processes present, not a production capacity assessment.','',
        'Quota 5/10/20, fixed zero wait, 100ms refresh with waiting, and insufficient timeout were separately exercised by JUnit. Full end-to-end performance comparisons for every quota/backoff/refresh combination are not claimed. Use the README matrix to collect further measurements.','',
        '## Limits and next steps','',
        '- The original five-second delay endpoints remain lab-only. The new combined delay uses a bounded configurable plan; timeout behavior is mapped to 504.',
        '- Lab reset/configuration is intended for quiescent local demonstrations; no distributed control plane is implemented.',
        '- The second dependency is simulated inside Availability, as required; no third process or database was introduced.',
        '- Original IntelliJ instances need a Maven reload/restart with the lab profile to serve the changed code on ports 8081/8082.',
        '- No Git commits, pushes, branch changes, merges or remote creation were performed. Review the source and guide with ChatGPT before version-control work.','']
(out/'RESULTS.md').write_text('\n'.join(lines),encoding='utf-8')

modified=['.gitignore','booking-service/src/main/resources/application.yaml','availability-service/src/main/resources/application.yaml',
          'availability-service/pom.xml']
for s in ['booking','availability']:
    prefix=f'{s}-service/src/'
    modified += [prefix+f'main/java/com/henry/resilience/{s}/controller/{s.title()}Controller.java',
                 prefix+f'test/java/com/henry/resilience/{s}/{s.title()}ServiceApplicationTests.java']
modified+=['booking-service/src/main/java/com/henry/resilience/booking/service/BookingService.java']
created=['README.md','DEMO.md','scripts/Run-K6.ps1','scripts/verify-lab.py','scripts/summarize-results.py']
for s in ['booking','availability']:
    prefix=f'{s}-service/src/'
    created += [prefix+'main/resources/application-lab.yaml',
                prefix+f'main/java/com/henry/resilience/{s}/controller/{s.title()}LabController.java',
                prefix+f'main/java/com/henry/resilience/{s}/exception/ResilienceExceptionHandler.java']
    created += [str(p.relative_to(root)).replace('\\','/') for p in (root/prefix/f'main/java/com/henry/resilience/{s}/lab').glob('*.java')]
    created += [prefix+f'test/java/com/henry/resilience/{s}/'+name+'.java' for name in ['ResilienceIntegrationTests','RateLimitIntegrationTests','RateLimiterPolicyTests']]
created+=['booking-service/src/test/java/com/henry/resilience/booking/PatternOrderingTests.java',
          'booking-service/src/test/java/com/henry/resilience/booking/RetryPolicyTests.java']
created += [str(p.relative_to(root)).replace('\\','/') for p in (root/'k6-tests').glob('*.js')]
created += [str(p.relative_to(root)).replace('\\','/') for p in out.rglob('*') if p.is_file() and p.suffix in ['.json','.md']]
created+=['test-results/FILES.md']
manifest=['# Files changed by this assignment','','Existing files modified:','']+[f'- `{p}`' for p in sorted(modified)]
manifest+=['','Files created:','']+[f'- `{p}`' for p in sorted(set(created))]
manifest+=['','Generated Maven `target` files, ignored runtime jar copies, Python caches and ignored `.log` files are not listed. Existing application classes, RestClient config and booking POM were inspected and preserved unless listed above. No unrelated source was removed.','']
(out/'FILES.md').write_text('\n'.join(manifest),encoding='utf-8')
