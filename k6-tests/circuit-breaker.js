import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('circuit-breaker');
export function setup() { return setupFor('circuit-breaker'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
