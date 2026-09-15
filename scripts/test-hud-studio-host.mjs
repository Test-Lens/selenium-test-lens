import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'docs/javascripts/hud-studio-host.js'), 'utf8');

function classList() {
  const names = new Set();
  return {
    add(name) { names.add(name); },
    contains(name) { return names.has(name); },
    toggle(name, active) { active ? names.add(name) : names.delete(name); }
  };
}

function target(properties = {}) {
  const listeners = new Map();
  return Object.assign({
    hidden: false,
    disabled: false,
    attributes: {},
    classList: classList(),
    addEventListener(type, listener) { listeners.set(type, listener); },
    dispatch(type, event = {}) { listeners.get(type)?.(event); },
    setAttribute(name, value) { this.attributes[name] = String(value); },
    focus() { document.activeElement = this; }
  }, properties);
}

const frameWindow = { messages: [], postMessage(message) { this.messages.push(message); } };
const frame = target({ contentWindow: frameWindow });
const expand = target();
const exit = target({ hidden: true });
const fullscreen = target();
const elements = {
  '[data-studio-frame]': frame,
  '[data-studio-expand]': expand,
  '[data-studio-exit]': exit,
  '[data-studio-fullscreen]': fullscreen
};
const documentListeners = new Map();
const host = target({
  querySelector(selector) { return elements[selector]; },
  requestFullscreen() {
    document.fullscreenElement = host;
    documentListeners.get('fullscreenchange')?.();
    return Promise.resolve();
  }
});
const document = {
  activeElement: null,
  fullscreenEnabled: true,
  fullscreenElement: null,
  body: { classList: classList() },
  querySelector(selector) { return selector === '[data-studio-host]' ? host : null; },
  addEventListener(type, listener) { documentListeners.set(type, listener); },
  exitFullscreen() {
    this.fullscreenElement = null;
    documentListeners.get('fullscreenchange')?.();
    return Promise.resolve();
  }
};
let resizeEvents = 0;
const window = { dispatchEvent(event) { if (event.type === 'resize') resizeEvents += 1; } };
class Event { constructor(type) { this.type = type; } }

vm.runInContext(source, vm.createContext({ window, document, Event, Boolean }), { filename: 'hud-studio-host.js' });
assert.ok(document.body.classList.contains('tl-hud-studio-page'));

expand.dispatch('click');
assert.ok(document.body.classList.contains('tl-studio-focus-mode'));
assert.equal(expand.hidden, true);
assert.equal(exit.hidden, false);
assert.equal(document.activeElement, exit);
assert.equal(frameWindow.messages.at(-1).type, 'studio-host-resize');

exit.dispatch('click');
assert.equal(document.body.classList.contains('tl-studio-focus-mode'), false);
assert.equal(document.activeElement, expand);

fullscreen.dispatch('click');
assert.equal(document.fullscreenElement, host);
assert.equal(fullscreen.attributes['aria-pressed'], 'true');
assert.equal(fullscreen.textContent, 'Exit fullscreen');
document.exitFullscreen();
assert.equal(fullscreen.attributes['aria-pressed'], 'false');
assert.equal(document.activeElement, fullscreen);

expand.dispatch('click');
documentListeners.get('keydown')({ key: 'Escape' });
assert.equal(document.body.classList.contains('tl-studio-focus-mode'), false);
assert.equal(document.activeElement, expand);
assert.ok(resizeEvents >= 5);

console.log('HUD Studio host tests OK: expand/exit, Escape, fullscreen state, focus return, and resize signaling.');
