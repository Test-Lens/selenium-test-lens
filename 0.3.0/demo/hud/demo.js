(function (window, document) {
  'use strict';

  var hud = window.__uiTestLens.modules.hud;
  var highlight = window.__uiTestLens.modules.highlight;
  var scrollArrow = window.__uiTestLens.modules.scrollArrow;
  var motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)');
  var reducedMotion = motionPreference.matches;
  var runSequence = 0;
  var replayTimer = null;
  var pendingDelays = new Set();
  var activeScroll = null;
  var viewportVisible = window.parent === window;
  var active = false;
  var elapsed = 0;
  var selectedPreset = 'COMPACT';
  var configurableHud = typeof hud.preset === 'function';

  var email = document.getElementById('email');
  var continueButton = document.getElementById('continue');
  var placeOrder = document.getElementById('place-order');
  var confirmation = document.getElementById('confirmation');
  var replayButton = document.getElementById('replay-demo');
  var accessibleStatus = document.getElementById('demo-status');

  function timestamp() {
    var seconds = Math.floor(elapsed / 1000);
    var tenths = Math.floor((elapsed % 1000) / 100);
    return '00:' + String(seconds).padStart(2, '0') + '.' + tenths;
  }

  function delay(milliseconds, sequence) {
    var duration = reducedMotion ? Math.min(milliseconds, 180) : milliseconds;
    return new Promise(function (resolve) {
      if (sequence !== runSequence || !active) {
        resolve(false);
        return;
      }
      var pending = { timer: null, resolve: resolve };
      pending.timer = window.setTimeout(function () {
        pendingDelays.delete(pending);
        if (sequence !== runSequence || !active) {
          resolve(false);
          return;
        }
        elapsed += duration;
        resolve(true);
      }, duration);
      pendingDelays.add(pending);
    });
  }

  function setStep(value) {
    hud.setStep(value);
    accessibleStatus.textContent = value;
    document.body.dataset.demoStep = value;
  }
  function eventType(value) {
    if (value.indexOf('Request recorded') === 0 || value.indexOf('Response recorded') === 0) return 'NETWORK_RESPONSE_RECORDED';
    if (value.indexOf('WAIT') === 0) return 'WAIT';
    if (value.indexOf('ASSERT') === 0) return 'ASSERTION_PASSED';
    return 'ACTION';
  }
  function log(value, level) { hud.log(value, level || 'info', timestamp(), eventType(value)); }

  function presetOptions() {
    if (!configurableHud) return {};
    var options = hud.preset(selectedPreset);
    if (!options) throw new Error('HUD preset is unavailable: ' + selectedPreset);
    return Object.assign(options, {
      preset: selectedPreset,
      position: 'BOTTOM_RIGHT',
      width: Math.max(240, Math.min(options.width, window.innerWidth - 24)),
      branding: 'TEST_LENS'
    });
  }

  function decorate(target, label, duration) {
    highlight.clear();
    highlight.element(target, label, {
      duration: reducedMotion ? 500 : duration,
      color: '#ffeb3b'
    });
  }

  function scrollToOrder(sequence) {
    return new Promise(function (resolve) {
      if (reducedMotion) {
        window.scrollTo(0, placeOrder.getBoundingClientRect().top + window.scrollY - 180);
        resolve(sequence === runSequence);
        return;
      }

      var originalRequestAnimationFrame = window.requestAnimationFrame;
      var frameIds = new Set();
      function trackedRequestAnimationFrame(callback) {
        var frameId = originalRequestAnimationFrame.call(window, function (timestamp) {
          frameIds.delete(frameId);
          callback(timestamp);
        });
        frameIds.add(frameId);
        return frameId;
      }
      function finishScroll(completed) {
        if (!activeScroll || activeScroll.sequence !== sequence) return;
        window.requestAnimationFrame = originalRequestAnimationFrame;
        activeScroll = null;
        resolve(completed && sequence === runSequence && active);
      }
      activeScroll = {
        sequence: sequence,
        frameIds: frameIds,
        originalRequestAnimationFrame: originalRequestAnimationFrame,
        resolve: resolve
      };
      window.requestAnimationFrame = trackedRequestAnimationFrame;
      scrollArrow.scrollToElementWithArrow(placeOrder, 900, 'CENTER', 'CENTER', function () {
        finishScroll(true);
      });
    });
  }

  function cancelScroll() {
    if (!activeScroll) return;
    var scroll = activeScroll;
    activeScroll = null;
    scroll.frameIds.forEach(function (frameId) {
      window.cancelAnimationFrame(frameId);
    });
    window.requestAnimationFrame = scroll.originalRequestAnimationFrame;
    scroll.resolve(false);
  }

  function cancelScheduledWork() {
    runSequence += 1;
    if (replayTimer !== null) {
      window.clearTimeout(replayTimer);
      replayTimer = null;
    }
    pendingDelays.forEach(function (pending) {
      window.clearTimeout(pending.timer);
      pending.resolve(false);
    });
    pendingDelays.clear();
    cancelScroll();
    highlight.clear();
    scrollArrow.clear();
  }

  function resetVisuals() {
    hud.clear();
    window.scrollTo(0, 0);
    email.value = '';
    confirmation.hidden = true;
    elapsed = 0;
    hud.init({
      testName: 'Checkout creates an order',
      pipelineId: 'docs-demo',
      position: 'BOTTOM_RIGHT',
      offsetX: 12,
      offsetY: 12,
      maxWidth: Math.max(260, Math.min(370, window.innerWidth - 24)),
      themeName: 'DARK',
      hudOptions: presetOptions()
    });
    setStep('Preparing checkout');
    document.body.dataset.demoState = 'running';
  }

  async function play(sequence) {
    resetVisuals();

    setStep('WAIT page ready');
    log('WAIT document ready state — STARTED', 'info');
    if (!await delay(800, sequence)) return;
    log('WAIT document ready state — PASSED', 'success');

    setStep('FILL Email');
    decorate(email, 'FILL Email', 1150);
    if (!await delay(850, sequence)) return;
    email.value = 'qa@example.test';
    email.dispatchEvent(new Event('input', { bubbles: true }));
    log('FILL Email — PASSED', 'success');
    if (!await delay(750, sequence)) return;

    setStep('CLICK Continue');
    decorate(continueButton, 'CLICK Continue', 1100);
    log('CLICK Continue — STARTED', 'info');
    if (!await delay(850, sequence)) return;
    log('CLICK Continue — PASSED', 'success');
    if (!await delay(550, sequence)) return;

    setStep('SCROLL checkout');
    log('SCROLL checkout — STARTED', 'info');
    if (!await scrollToOrder(sequence)) return;
    log('SCROLL checkout — PASSED', 'success');

    setStep('CLICK Place order');
    decorate(placeOrder, 'CLICK Place order', 1250);
    log('CLICK Place order — STARTED', 'info');
    if (!await delay(850, sequence)) return;
    log('CLICK Place order — PASSED', 'success');

    setStep('Observe order request');
    log('Request recorded: POST /api/orders', 'royal');
    if (!await delay(850, sequence)) return;
    log('Response recorded: 200 /api/orders', 'success');
    if (!await delay(650, sequence)) return;

    confirmation.hidden = false;
    setStep('ASSERT confirmation visible');
    decorate(confirmation, 'ASSERT visible', 1400);
    log('ASSERT confirmation visible — STARTED', 'info');
    if (!await delay(850, sequence)) return;
    log('ASSERT confirmation visible — PASSED', 'success');
    setStep('PASSED');
    log('SESSION PASSED', 'success');
    document.body.dataset.demoState = 'passed';

    if (!reducedMotion && active && sequence === runSequence) {
      replayTimer = window.setTimeout(function () {
        replayTimer = null;
        startScenario();
      }, 2200);
    }
  }

  function startScenario() {
    cancelScheduledWork();
    var sequence = runSequence;
    document.body.dataset.demoRun = String(sequence);
    play(sequence);
  }

  function updateActivity() {
    var nextActive = !document.hidden && viewportVisible;
    if (nextActive === active) return;
    active = nextActive;
    document.body.dataset.demoActive = String(active);
    if (!active) {
      cancelScheduledWork();
      document.body.dataset.demoState = 'paused';
      return;
    }
    startScenario();
  }

  replayButton.addEventListener('click', function () {
    if (active) startScenario();
  });
  document.querySelectorAll('[data-hud-preset]').forEach(function (button) {
    button.addEventListener('click', function () {
      selectedPreset = button.getAttribute('data-hud-preset');
      document.querySelectorAll('[data-hud-preset]').forEach(function (candidate) {
        candidate.setAttribute('aria-pressed', String(candidate === button));
      });
      if (active) startScenario();
    });
  });
  if (!configurableHud) {
    var presetControl = document.querySelector('.preset-control');
    if (presetControl) presetControl.hidden = true;
  }
  document.addEventListener('visibilitychange', updateActivity);
  window.addEventListener('message', function (event) {
    if (event.source !== window.parent || !event.data || event.data.type !== 'test-lens-demo-visibility') return;
    viewportVisible = event.data.visible === true;
    updateActivity();
  });
  if (typeof motionPreference.addEventListener === 'function') {
    motionPreference.addEventListener('change', function (event) {
      reducedMotion = event.matches;
      if (active) startScenario();
    });
  }
  updateActivity();
  if (window.parent !== window) {
    window.parent.postMessage({ type: 'test-lens-demo-ready' }, '*');
  }
})(window, document);
