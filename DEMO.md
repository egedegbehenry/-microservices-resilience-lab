# Instructor demonstration (PowerShell)

Start both packaged services with the `lab` profile as in README. Run the following from the project root. Allow about five minutes for the full automated acceptance sequence and selected load demonstrations. Commands assume ports 8081/8082; substitute 18081/18082 for isolated instances.

```powershell
$A='http://127.0.0.1:8081'
$B='http://127.0.0.1:8082'
function Plan($base,$mode='success',$failures=0,$delayMs=0) {
  $body=@{mode=$mode;failures=$failures;delayMs=$delayMs}|ConvertTo-Json
  Invoke-RestMethod "$base/lab/config" -Method Post -ContentType application/json -Body $body | Out-Null
}
function Reset-Lab {
  foreach($base in @($A,$B)) { Invoke-RestMethod "$base/lab/reset" -Method Post -ContentType application/json -Body '{}' | Out-Null }
  Start-Sleep -Milliseconds 1100
}
function Show-States {
  foreach($base in @($A,$B)) {
    Invoke-RestMethod "$base/lab/state" | Select-Object breaker,state,attempts,retryAttempts,stateTransitions,failedCalls
  }
}
function Call-Lab($base) { curl.exe -s -i "$base/lab/operation" }
function Open-Breaker($base) { Plan $base failure; 1..4 | ForEach-Object { Call-Lab $base }; Show-States }
```

1. Show the fixed monitoring and normal communication:

```powershell
Reset-Lab
Invoke-RestMethod "$A/actuator"
Invoke-RestMethod "$A/actuator/circuitbreakers"
Invoke-RestMethod "$B/actuator/circuitbreakers"
Call-Lab $A
Show-States
```

2. Demonstrate CLOSED → OPEN → HALF_OPEN → CLOSED:

```powershell
Reset-Lab
Open-Breaker $A
Call-Lab $A # CIRCUIT_OPEN; attempts remains 12, B attempts remains 0
Show-States
Plan $A success
Start-Sleep -Seconds 11
Show-States # HALF_OPEN / CLOSED
Call-Lab $A
Show-States # One logical probe, A still HALF_OPEN
Call-Lab $A
Show-States # CLOSED / CLOSED
Invoke-RestMethod "$A/actuator/circuitbreakerevents" | ConvertTo-Json -Depth 10
```

3. Demonstrate failed HALF_OPEN recovery:

```powershell
Reset-Lab
Open-Breaker $A
Start-Sleep -Seconds 11
Show-States
Call-Lab $A
Call-Lab $A
Show-States # A OPEN again; two logical probes, six actual attempts
```

4. Demonstrate independent breakers and all seven combinations:

```powershell
Reset-Lab
Open-Breaker $B # A CLOSED / B OPEN
Reset-Lab
Open-Breaker $A
Open-Breaker $B # OPEN / OPEN
Plan $A success
Plan $B success
Start-Sleep -Seconds 11
Show-States # HALF_OPEN / HALF_OPEN
Call-Lab $A
Call-Lab $A
Show-States # Both CLOSED
# Automated evidence for the complete table and both recovery paths:
python scripts/verify-lab.py
```

5. Demonstrate Retry recovery and exhaustion:

```powershell
Reset-Lab
Plan $B transient 2
Call-Lab $A
Show-States # A attempts=1, B attempts=3, B retryAttempts=2
Invoke-RestMethod "$B/actuator/retryevents" | ConvertTo-Json -Depth 10
Reset-Lab
Plan $A failure
Call-Lab $A
Show-States # A attempts=3; one failed logical breaker call
```

6. Demonstrate excessive traffic and combined resilience:

```powershell
Reset-Lab
k6 run -e VUS=8 -e DURATION=10s k6-tests/rate-limiting.js
k6 run k6-tests/circuit-breaker.js
k6 run k6-tests/recovery.js
k6 run k6-tests/simultaneous.js
```

Show `operation_duration` average/p95, status distribution, `unexpected_failures`, rate rejections and actual attempt/transition counters. Expected 429/503 responses are part of these experiments. For a waiting demonstration restart one service with `RATE_LIMIT=5`, `RATE_PERIOD=1s`, `RATE_TIMEOUT=1500ms`; repeat and compare latency and rejections. Use separate terminals to set upstream/downstream values independently. Do not run the strict acceptance suite with altered retry counts, long limiter periods or aspect orders; its expected counts assume defaults.

7. Show the architecture and ordering tests, then measured results in `test-results/RESULTS.md`. Explain that A wraps a real HTTP dependency and B wraps an in-process simulated dependency. OPEN states are read from the actual registries and events; the reset endpoint is used only to prepare a fresh scenario.
