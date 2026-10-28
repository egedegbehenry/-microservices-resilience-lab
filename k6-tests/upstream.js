import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('upstream');
export function setup() { return setupFor('upstream'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
