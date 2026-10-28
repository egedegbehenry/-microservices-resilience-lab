import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('retry');
export function setup() { return setupFor('retry'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
