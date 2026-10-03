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
    querySelector(selector) { return this.children.find(child => selector === `.${child.className}`) || null; },
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
const document = { body: node('body'), createElement: node, getElementById(id) { return elements[id]; } };
document.body.appendChild = () => {};

const logs = [];
const hud = {
  remove() {}, init(config) { this.config = config; }, setStep() {},
  setSourceNavigationCompatibility(...args) { this.compatibility = args; },
  log(...args) { logs.push(args); }, preset(name) { return { preset: name }; }
};
const highlight = { clear() {}, element() {} };
const listeners = new Map();
const parent = { messages: [], postMessage(message) { this.messages.push(message); } };
const window = {
  __uiTestLens: { modules: { hud, highlight } }, __seleniumOverlayRoot: overlayRoot, parent,
  innerWidth: 1024, innerHeight: 768,
  addEventListener(type, listener) { if (!listeners.has(type)) listeners.set(type, []); listeners.get(type).push(listener); },
  removeEventListener() {}, clearTimeout() {},
  setTimeout(callback) { callback(); return 1; },
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
(listeners.get('message') || [])[0]({ source: parent, data: { type: 'hud-config', config } });
assert.equal(panel.dataset.sourceNavigationActive, 'true');
assert.equal(hud.compatibility[0], 'DEMO_SIMULATED');
assert.equal(hud.compatibility[1], 'VERIFIED');
assert.equal(hud.compatibility[2], true);

const semantic = logs.map(args => ({ message: args[0], sourceLabel: args[4], target: args[5], semantics: args[7] }));
const action = semantic.find(entry => entry.semantics.category === 'ACTION' && entry.semantics.phase === 'PASSED');
const assertion = semantic.find(entry => entry.semantics.category === 'ASSERTION' && entry.semantics.phase === 'PASSED');
const failed = semantic.find(entry => entry.semantics.category === 'ASSERTION' && entry.semantics.phase === 'FAILED');
assert.deepEqual([action.sourceLabel, assertion.sourceLabel, failed.sourceLabel], ['CheckoutPage.java:87', 'CheckoutTest.java:42', 'OrderAssertions.java:116']);
assert.ok([action.target, assertion.target, failed.target].every(target => target.startsWith('#demo-source-')));
assert.ok(semantic.some(entry => entry.semantics.category === 'USER' && entry.sourceLabel == null));
assert.ok(semantic.every(entry => !/JetBrains protocol handler is not registered/.test(entry.message)));

window.__uiTestLensSourceNavigation(action.target);
assert.match(status.textContent, /would open CheckoutPage\.java:87 in IntelliJ/);
assert.match(status.textContent, /No external application was launched/);

panel.dataset.sourceNavigationActive = 'true';
window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', code: 'F8', repeat: false }));
assert.equal(parent.messages.at(-1).type, 'hud-source-navigation-state');
assert.equal(parent.messages.at(-1).active, false);
window.dispatchEvent(new KeyboardEvent('keydown', { key: 'F8', code: 'F8', repeat: true }));
assert.equal(parent.messages.at(-1).active, false);

console.log('HUD Studio preview tests OK: semantic source fixtures, simulated compatibility, safe dispatch, and F8 state synchronization.');
