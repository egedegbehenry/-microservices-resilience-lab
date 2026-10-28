import { optionsFor, setupFor, run, teardownFor, summary } from './lib.js';
export const options = optionsFor('normal');
export function setup() { return setupFor('normal'); }
export default function(data) { run(data); }
export function teardown(data) { teardownFor(data); }
export const handleSummary = summary;
