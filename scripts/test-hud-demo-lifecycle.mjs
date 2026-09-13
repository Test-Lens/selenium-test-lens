import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'docs/demo/hud/demo.js'), 'utf8');

function eventTarget(properties = {}) {
  const listeners = new Map();
  return Object.assign(properties, {
    addEventListener(type, listener) {
      const registered = listeners.get(type) || [];
      registered.push(listener);
      listeners.set(type, registered);
    },
    dispatch(type, event = {}) {
      for (const listener of [...(listeners.get(type) || [])]) listener(event);
    }
  });
}

function harness({ reducedMotion }) {
  let timerId = 0;
  let frameId = 0;
  const timers = new Map();
  const frames = new Map();
  const calls = { hudInit: 0, logs: [], highlight: 0, scrollArrow: 0, scrollTo: 0 };
  const parent = { postMessage() {} };
  const elements = new Map();
  const presetButtons = ['COMPACT', 'MINIMAL', 'DEBUG'].map(value => eventTarget({
    attributes: { 'data-hud-preset': value, 'aria-pressed': value === 'COMPACT' ? 'true' : 'false' },
    getAttribute(name) { return this.attributes[name]; },
    setAttribute(name, value) { this.attributes[name] = String(value); }
  }));

  function element(id) {
    const value = eventTarget({
      id,
      value: '',
      hidden: id === 'confirmation',
      textContent: '',
      dataset: {},
      getBoundingClientRect() { return { top: id === 'place-order' ? 900 : 100, left: 20, width: 200, height: 40 }; },
      dispatchEvent() {}
    });
    elements.set(id, value);
    return value;
  }

  for (const id of ['email', 'continue', 'place-order', 'confirmation', 'replay-demo', 'demo-status']) element(id);

  const document = eventTarget({
    hidden: false,
    body: { dataset: {} },
    getElementById(id) { return elements.get(id); },
    querySelectorAll(selector) { return selector === '[data-hud-preset]' ? presetButtons : []; }
  });
  const motionPreference = eventTarget({ matches: reducedMotion });
  const window = eventTarget({
    parent,
    innerWidth: 900,
    scrollY: 0,
    setTimeout(callback) {
      const id = ++timerId;
      timers.set(id, callback);
      return id;
    },
    clearTimeout(id) { timers.delete(id); },
    requestAnimationFrame(callback) {
      const id = ++frameId;
      frames.set(id, callback);
      return id;
    },
    cancelAnimationFrame(id) { frames.delete(id); },
    scrollTo(x, y) {
      calls.scrollTo += 1;
      window.scrollY = y;
    },
    matchMedia() { return motionPreference; }
  });
  window.window = window;
  window.document = document;
  window.__uiTestLens = {
    modules: {
      hud: {
        init() { calls.hudInit += 1; },
        setStep() {},
        log(message) { calls.logs.push(message); },
        clear() {},
        preset(name) {
          const values = {
            MINIMAL: { width: 280, maxHeight: 180, maxLogHeight: 80, showTestName: false, showCurrentStep: true, showPipeline: false, showTimestamps: false, showEventLog: false, showNetwork: false, showRetries: false, showWaits: true, showAssertions: true, fontPreset: 'UI_SANS', baseFontSize: 9, headerFontSize: 10 },
            COMPACT: { width: 420, maxHeight: 280, maxLogHeight: 180, showTestName: true, showCurrentStep: true, showPipeline: false, showTimestamps: false, showEventLog: true, showNetwork: true, showRetries: true, showWaits: true, showAssertions: true, fontPreset: 'UI_SANS', baseFontSize: 10, headerFontSize: 10 },
            DEBUG: { width: 620, maxHeight: 520, maxLogHeight: 360, showTestName: true, showCurrentStep: true, showPipeline: true, showTimestamps: true, showEventLog: true, showNetwork: true, showRetries: true, showWaits: true, showAssertions: true, fontPreset: 'MONOSPACE', baseFontSize: 10, headerFontSize: 11 }
          };
          return Object.assign({}, values[name]);
        }
      },
      highlight: {
        element() { calls.highlight += 1; },
        clear() {}
      },
      scrollArrow: {
        scrollToElementWithArrow(target, duration, elementEdge, viewportEdge, done) {
          calls.scrollArrow += 1;
          window.requestAnimationFrame(done);
        },
        clear() {}
      }
    }
  };

  const context = vm.createContext({ window, document, Event: class Event {}, performance: { now: () => 0 }, Set });
  vm.runInContext(source, context, { filename: 'demo.js' });

  async function settle() {
    await Promise.resolve();
    await Promise.resolve();
  }
  async function runNextTimer() {
    const next = timers.entries().next();
    assert.equal(next.done, false, 'expected a pending demo timer');
    const [id, callback] = next.value;
    timers.delete(id);
    callback();
    await settle();
  }
  async function drain() {
    for (let guard = 0; (timers.size || frames.size) && guard < 100; guard += 1) {
      if (timers.size) {
        await runNextTimer();
      } else {
        const [id, callback] = frames.entries().next().value;
        frames.delete(id);
        callback(1000);
        await settle();
      }
    }
    assert.equal(timers.size + frames.size, 0, 'demo lifecycle did not drain');
  }
  function setIntersection(visible) {
    window.dispatch('message', { source: parent, data: { type: 'test-lens-demo-visibility', visible } });
  }

  return { calls, document, elements, frames, motionPreference, presetButtons, setIntersection, timers, window, runNextTimer, drain, settle };
}

{
  const demo = harness({ reducedMotion: true });
  assert.equal(demo.calls.hudInit, 0, 'sandboxed iframe must wait for parent intersection state');

  demo.setIntersection(true);
  assert.equal(demo.document.body.dataset.demoActive, 'true');
  assert.equal(demo.calls.hudInit, 1);

  demo.elements.get('replay-demo').dispatch('click');
  demo.elements.get('replay-demo').dispatch('click');
  demo.elements.get('replay-demo').dispatch('click');
  assert.equal(demo.calls.hudInit, 4, 'each explicit replay must start one replacement generation');
  assert.equal(demo.timers.size, 1, 'rapid replay must leave only one scenario timer');
  await demo.drain();
  assert.equal(demo.document.body.dataset.demoState, 'passed');
  assert.equal(demo.calls.logs.filter(message => message === 'SESSION PASSED').length, 1);
  assert.equal(demo.calls.scrollArrow, 0, 'reduced motion must avoid animated scroll-arrow travel');
  assert.ok(demo.calls.scrollTo > 0, 'reduced motion must still reach the checkout content');
  assert.ok(demo.calls.highlight > 0, 'reduced motion must retain static target highlights');
  assert.equal(demo.timers.size, 0, 'reduced motion must not schedule automatic replay');

  demo.presetButtons[1].dispatch('click');
  assert.equal(demo.presetButtons[1].attributes['aria-pressed'], 'true', 'preset switch must update its pressed state');
  assert.equal(demo.timers.size, 1, 'preset switch must replace rather than duplicate the active run');

  demo.document.hidden = true;
  demo.document.dispatch('visibilitychange');
  assert.equal(demo.document.body.dataset.demoState, 'paused');
  demo.document.hidden = false;
  demo.document.dispatch('visibilitychange');
  assert.equal(demo.calls.hudInit, 6, 'returning to a visible tab must restart cleanly');

  demo.setIntersection(false);
  assert.equal(demo.document.body.dataset.demoState, 'paused');
  assert.equal(demo.timers.size, 0);
  demo.setIntersection(true);
  assert.equal(demo.calls.hudInit, 7, 're-entering the viewport must restart cleanly');
}

{
  const demo = harness({ reducedMotion: false });
  demo.setIntersection(true);
  for (let index = 0; index < 5; index += 1) await demo.runNextTimer();
  assert.equal(demo.calls.scrollArrow, 1, 'normal motion must use the runtime scroll-arrow operation');
  assert.equal(demo.frames.size, 1, 'scroll animation must have one pending frame');
  demo.setIntersection(false);
  await demo.settle();
  assert.equal(demo.frames.size, 0, 'leaving the viewport must cancel the pending scroll frame');
  assert.equal(demo.timers.size, 0, 'leaving the viewport must cancel scenario timers');
  assert.equal(demo.document.body.dataset.demoState, 'paused');
}

console.log('HUD demo lifecycle tests OK: sandbox activation, replay generations, visibility/intersection restart, reduced motion, and scroll cancellation.');
