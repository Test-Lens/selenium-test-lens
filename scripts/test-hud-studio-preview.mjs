import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'docs/demo/hud-studio/preview.js'), 'utf8');

function classList() {
  const values = new Set();
  return { add(value) { values.add(value); }, remove(value) { values.delete(value); }, contains(value) { return values.has(value); } };
}
function node(tag = 'div') {
  const listeners = new Map();
  return {
    tagName: tag.toUpperCase(), dataset: {}, style: {}, children: [], classList: classList(), className: '', textContent: '', hidden: false,
    appendChild(child) { this.children.push(child); child.parentNode = this; return child; },
    remove() {}, addEventListener(type, listener) { listeners.set(type, listener); },
    querySelector(selector) { return this.children.find(child => selector === `.${child.className}`) || null; }, querySelectorAll() { return []; },
    closest() { return null; }, setAttribute() {}, getAttribute(name) { return this.attributes?.[name] ?? null; },
    getBoundingClientRect() { return { left: 10, top: 10, right: 430, bottom: 290, width: 420, height: 280 }; }
  };
}

const panel = node(); panel.dataset.sourceNavigationActive = 'false';
const overlayRoot = node('root'); overlayRoot.querySelector = selector => selector === '#selenium-hud-panel' ? panel : null;
const status = node('p');
const order = node('button');
const result = node(); result.hidden = true;
const elements = { 'source-navigation-demo-status': status, 'preview-order': order, 'preview-result': result };
const document = { body: node('body'), documentElement: { clientWidth: 390 }, createElement: node, getElementById(id) { return elements[id]; } };
document.body.appendChild = () => {};

const logs = [];
const rows = new Map();
const hud = {
  remove() { rows.clear(); }, init(config) { this.config = config; }, setStep(value) { this.step = value; },
  setSourceNavigationCompatibility(...args) { this.compatibility = args; },
  log(...args) {
    logs.push(args);
    const semantics = args[7];
    rows.set(`${semantics.category}:${semantics.operationId}`, {
      message: args[0], sourceLabel: args[4], target: args[5], semantics
    });
  }, preset(name) { return { preset: name }; }
};
const highlights = [];
const highlight = { clear() {}, element(...args) { highlights.push(args); } };
const listeners = new Map();
const parent = { messages: [], postMessage(message) { this.messages.push(message); } };
let clock = 0;
let nextTimer = 1;
const scheduled = new Map();
function advanceTo(target) {
  while (true) {
    const due = [...scheduled.entries()].filter(([,timer]) => timer.due <= target).sort((a,b) => a[1].due-b[1].due)[0];
    if (!due) break;
    scheduled.delete(due[0]);
    clock = due[1].due;
    due[1].callback();
  }
  clock = target;
}
const window = {
  __uiTestLens: { modules: { hud, highlight } }, __seleniumOverlayRoot: overlayRoot, parent,
  innerWidth: 1024, innerHeight: 768,
  addEventListener(type, listener) { if (!listeners.has(type)) listeners.set(type, []); listeners.get(type).push(listener); },
  removeEventListener() {}, clearTimeout(id) { scheduled.delete(id); },
  setTimeout(callback, delay = 0) { const id=nextTimer++; scheduled.set(id,{callback,due:clock+delay}); return id; },
  dispatchEvent(event) {
    if (event.key === 'F8' && !event.repeat) panel.dataset.sourceNavigationActive = panel.dataset.sourceNavigationActive === 'true' ? 'false' : 'true';
    if (event.key === 'Escape') panel.dataset.sourceNavigationActive = 'false';
    (listeners.get(event.type) || []).forEach(listener => listener(event));
  }
};
class KeyboardEvent { constructor(type, init = {}) { this.type = type; Object.assign(this, init); } }

vm.runInContext(source, vm.createContext({ window, document, KeyboardEvent, console }), { filename: 'preview.js' });
assert.equal(parent.messages.at(-1).type, 'hud-ready');

const config = { sourceNavigationEnabled: true, sourceNavigationPreviewActive: true, sourceNavigationModifier: 'F8', sourceNavigationIde: 'INTELLIJ', timestampPreview: { events: [] }, highlight: {} };
const receive = (listeners.get('message') || [])[0];
receive({ source: parent, data: { type: 'hud-config', config } });
assert.equal(panel.dataset.sourceNavigationActive, 'true');
assert.equal(hud.compatibility[0], 'DEMO_SIMULATED');
assert.equal(hud.compatibility[1], 'VERIFIED');
assert.equal(hud.compatibility[2], true);
assert.equal(hud.config.hudOptions.width, 370);

const semantic = [...rows.values()];
const action = semantic.find(entry => entry.semantics.category === 'ACTION');
const assertion = semantic.find(entry => entry.semantics.category === 'ASSERTION' && entry.semantics.phase === 'PASSED');
const failed = semantic.find(entry => entry.semantics.category === 'ASSERTION' && entry.semantics.phase === 'FAILED');
assert.deepEqual([action.sourceLabel, assertion.sourceLabel, failed.sourceLabel], ['CheckoutPage.java:87', 'CheckoutTest.java:42', 'OrderAssertions.java:116']);
assert.ok([action.target, assertion.target, failed.target].every(target => target.startsWith('#demo-source-')));
assert.ok(semantic.some(entry => entry.semantics.category === 'USER' && entry.sourceLabel == null));
assert.ok(semantic.every(entry => !/JetBrains protocol handler is not registered/.test(entry.message)));
assert.match(source, /Preview source location/);
assert.match(source, /activate to simulate navigation/);

receive({ source: parent, data: { type: 'hud-replay' } });
const firstRun = panel.dataset.studioReplayId;
assert.match(firstRun, /^studio-run-/);
assert.equal(panel.dataset.studioReplayState, 'running');
assert.equal(rows.size, 3);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ACTION').semantics.phase, 'RUNNING');
assert.equal(hud.step, 'ACTION Place order');
assert.equal(result.hidden, true);

advanceTo(500);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ACTION').semantics.phase, 'PASSED');
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ACTION').sourceLabel, 'CheckoutPage.java:87');
advanceTo(750);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ASSERTION').semantics.phase, 'RUNNING');
const activeRows = [...rows.values()].map(entry => `${entry.semantics.category}:${entry.semantics.phase}:${entry.semantics.operationId}`);
receive({ source: parent, data: { type: 'hud-config', config: Object.assign({},config,{width:480}) } });
assert.deepEqual([...rows.values()].map(entry => `${entry.semantics.category}:${entry.semantics.phase}:${entry.semantics.operationId}`),activeRows);
assert.equal(panel.dataset.studioReplayId, firstRun);
advanceTo(1100);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ASSERTION').semantics.phase, 'RETRYING');
advanceTo(1500);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ASSERTION').semantics.phase, 'PASSED');
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ASSERTION').sourceLabel, 'CheckoutTest.java:42');
advanceTo(1750);
assert.equal([...rows.values()].filter(entry => entry.semantics.category === 'ASSERTION').length, 2);
assert.equal([...rows.values()].find(entry => entry.semantics.operationId.endsWith('assert-order-number')).semantics.phase, 'RUNNING');
advanceTo(2200);
assert.equal([...rows.values()].find(entry => entry.semantics.operationId.endsWith('assert-order-number')).semantics.phase, 'FAILED');
assert.equal([...rows.values()].find(entry => entry.semantics.operationId.endsWith('assert-order-number')).sourceLabel, 'OrderAssertions.java:116');
advanceTo(2450);
assert.equal(panel.dataset.studioReplayState, 'complete');
assert.equal(hud.step, 'Replay complete');
assert.equal(result.hidden, false);

receive({ source: parent, data: { type: 'hud-replay' } });
const interruptedRun = panel.dataset.studioReplayId;
advanceTo(2950);
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ACTION').semantics.phase, 'PASSED');
receive({ source: parent, data: { type: 'hud-replay' } });
const replacementRun = panel.dataset.studioReplayId;
assert.notEqual(replacementRun, interruptedRun);
assert.ok([...rows.values()].every(entry => entry.semantics.operationId.startsWith(replacementRun)));
advanceTo(6000);
assert.equal(panel.dataset.studioReplayState, 'complete');
assert.ok([...rows.values()].every(entry => entry.semantics.operationId.startsWith(replacementRun)));

const highlightCount = highlights.length;
const sourceOff = Object.assign({},config,{sourceNavigationEnabled:false,sourceNavigationPreviewActive:false,highlight:{enabled:false}});
receive({ source: parent, data: { type: 'hud-config', config: sourceOff } });
receive({ source: parent, data: { type: 'hud-replay' } });
const sourceOffRun = panel.dataset.studioReplayId;
advanceTo(9000);
assert.equal(panel.dataset.studioReplayState, 'complete');
assert.equal(highlights.length, highlightCount);
assert.ok([...rows.values()].every(entry => entry.semantics.operationId.startsWith(sourceOffRun)));
assert.equal([...rows.values()].find(entry => entry.semantics.category === 'ACTION').sourceLabel, 'CheckoutPage.java:87');

receive({ source: parent, data: { type: 'hud-config', config } });
window.__uiTestLensSourceNavigation(action.target);
assert.match(status.textContent, /would open CheckoutPage\.java:87 in IntelliJ/);
assert.match(status.textContent, /No external application was launched/);

panel.dataset.sourceNavigationActive = 'true';
window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', code: 'F8', repeat: false }));
advanceTo(clock);
assert.equal(parent.messages.at(-1).type, 'hud-source-navigation-state');
assert.equal(parent.messages.at(-1).active, false);
window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', code: 'F8', repeat: true }));
advanceTo(clock);
assert.equal(parent.messages.at(-1).active, false);

console.log('HUD Studio preview tests OK: timed semantic replay, cancellation, config preservation, source fixtures, safe dispatch, and F8 state synchronization.');
