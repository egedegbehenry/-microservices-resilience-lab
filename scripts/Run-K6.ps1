param(
    [string]$K6 = 'k6',
    [string]$Upstream = 'http://127.0.0.1:8081',
    [string]$Downstream = 'http://127.0.0.1:8082',
    [string]$Duration = '10s'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$resultRoot = Join-Path $projectRoot 'test-results/k6'
New-Item -ItemType Directory -Force $resultRoot | Out-Null
foreach ($scenario in @('normal','heavy','rate-limiting','circuit-breaker','retry','dependency-failure','recovery','upstream','downstream','simultaneous','slow')) {
    Write-Host "Running $scenario"
    $scriptPath = Join-Path $projectRoot "k6-tests/$scenario.js"
    $summaryPath = Join-Path $resultRoot "$scenario.json"
    # k6 writes informational console logs to stderr by default. Route them to stdout
    # so Windows PowerShell 5 does not treat successful native output as an error.
    & $K6 --log-output=stdout run -e "UPSTREAM_URL=$Upstream" -e "DOWNSTREAM_URL=$Downstream" -e "DURATION=$Duration" -e "SUMMARY_FILE=$summaryPath" $scriptPath *> (Join-Path $resultRoot "$scenario.log")
    if ($LASTEXITCODE -ne 0) { throw "k6 $scenario failed; inspect $resultRoot/$scenario.log" }
}
