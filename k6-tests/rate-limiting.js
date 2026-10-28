import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('rate-limiting');
export function setup() { return setupFor('rate-limiting'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
