import http from 'k6/http';
import { sleep, check } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const unexpected = new Rate('unexpected_failures');
const statuses = new Counter('response_status');
const limited = new Counter('rate_limit_rejections');
const open = new Counter('circuit_open_rejections');
const latency = new Trend('operation_duration', true);
const transitions = new Counter('observed_state_transitions');
const attempts = new Counter('observed_dependency_attempts');
const retryEvents = new Counter('observed_retry_events');
const A = __ENV.UPSTREAM_URL || 'http://127.0.0.1:8081';
const B = __ENV.DOWNSTREAM_URL || 'http://127.0.0.1:8082';
const local = url => /^http:\/\/(127\.0\.0\.1|localhost):\d+$/.test(url);
if (!local(A) || !local(B)) throw new Error('Only local lab URLs are allowed');

export function optionsFor(name) {
  const heavy = ['heavy', 'rate-limiting', 'simultaneous'].includes(name);
  const vus = Number(__ENV.VUS || (heavy ? 8 : 1));
  const duration = __ENV.DURATION || '10s';
  if (!Number.isInteger(vus) || vus < 1 || vus > 50 || !/^\d+s$/.test(duration) || parseInt(duration) < 1 || parseInt(duration) > 120)
    throw new Error('Bounded load required: VUS 1..50; DURATION 1s..120s');
  return { vus, duration, thresholds: { unexpected_failures: ['rate<0.01'],
    'response_status{status:200}': ['count>=0'], 'response_status{status:429}': ['count>=0'],
    'response_status{status:503}': ['count>=0'], 'response_status{status:504}': ['count>=0'] },
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)'] };
}
function control(base, path, data) {
  const r = data === undefined ? http.get(base + path, { tags: { kind: 'control' } }) :
    http.post(base + path, JSON.stringify(data), { headers: { 'Content-Type': 'application/json' }, tags: { kind: 'control' } });
  if (r.status !== 200) throw new Error(`Control ${path}: ${r.status} ${r.body}`);
  return r.json();
}
function configure(base, mode, failures = 0, delayMs = 0) {
  control(base, '/lab/config', { mode, failures, delayMs });
}
export function setupFor(name) {
  for (const base of [A, B]) control(base, '/lab/reset', {});
  sleep(1.1);
  if (name === 'retry') configure(B, 'transient', 2);
  if (name === 'dependency-failure') configure(B, 'failure');
  if (name === 'circuit-breaker' || name === 'recovery') configure(A, 'failure');
  if (name === 'slow') configure(B, 'delay', 0, Number(__ENV.DELAY_MS || 500));
  if (name === 'recovery') {
    for (let i = 0; i < 4; i++) http.get(A + '/lab/operation');
    const state = control(A, '/lab/state');
    if (state.state !== 'OPEN') throw new Error(`Expected OPEN, got ${state.state}`);
    configure(A, 'success'); sleep(Number(__ENV.OPEN_WAIT_SECONDS || 10.5));
    if (control(A, '/lab/state').state !== 'HALF_OPEN') throw new Error('Expected HALF_OPEN');
  }
  return { name, beforeA: control(A, '/lab/state'), beforeB: control(B, '/lab/state') };
}
export function run(data) {
  const targets = data.name === 'downstream' ? [B] : data.name === 'simultaneous' ? [A, B] : [A];
  const responses = http.batch(targets.map(base => ['GET', base + '/lab/operation', null, {
    tags: { kind: 'operation', service: base === A ? 'upstream' : 'downstream' },
    responseCallback: http.expectedStatuses(200, 429, 503, 504), timeout: '30s'
  }]));
  for (const r of responses) {
    const expected = r.status === 200 ||
      (r.status === 429 && (['heavy', 'rate-limiting', 'simultaneous'].includes(data.name) || __ENV.EXPECT_RATE_LIMIT === 'true')) ||
      (['dependency-failure', 'circuit-breaker'].includes(data.name) && r.status === 503) ||
      (data.name === 'slow' && r.status === 504);
    unexpected.add(!expected); statuses.add(1, { status: String(r.status) }); latency.add(r.timings.duration);
    if (r.status === 429) limited.add(1);
    if (r.status === 503 && String(r.body).includes('CIRCUIT_OPEN')) open.add(1);
    check(r, { 'expected scenario status': () => expected });
  }
  sleep(Number(__ENV.PACE_SECONDS || (['heavy', 'rate-limiting', 'simultaneous'].includes(data.name) ? 0.01 : 0.25)));
}
export function teardownFor(data) {
  for (const [base, before] of [[A, data.beforeA], [B, data.beforeB]]) {
    const after = control(base, '/lab/state');
    attempts.add(Math.max(0, after.attempts - before.attempts), { service: after.breaker });
    transitions.add(Math.max(0, after.stateTransitions - before.stateTransitions), { service: after.breaker });
    retryEvents.add(Math.max(0, after.retryAttempts - before.retryAttempts), { service: after.breaker });
    console.log(JSON.stringify({ service: after.breaker, state: after.state, attempts: after.attempts, events: after.events }));
    control(base, '/lab/reset', {});
  }
}
export function summary(data) {
  return { stdout: JSON.stringify(data, null, 2) + '\n', [__ENV.SUMMARY_FILE || 'k6-summary.json']: JSON.stringify(data, null, 2) };
}
