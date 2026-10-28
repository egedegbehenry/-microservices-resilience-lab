import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('simultaneous');
export function setup() { return setupFor('simultaneous'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
