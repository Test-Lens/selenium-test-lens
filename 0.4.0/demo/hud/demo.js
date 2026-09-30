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
  var selectedScenario = 'lifecycle';
  var configurableHud = typeof hud.preset === 'function';

  var email = document.getElementById('email');
  var continueButton = document.getElementById('continue');
  var placeOrder = document.getElementById('place-order');
  var confirmation = document.getElementById('confirmation');
  var replayButton = document.getElementById('replay-demo');
  var accessibleStatus = document.getElementById('demo-status');
  var scenarioSelect = document.getElementById('scenario');
  var clickCover = document.getElementById('click-cover');

  function timestamp() {
    var seconds = Math.floor(elapsed / 1000);
    var tenths = Math.floor((elapsed % 1000) / 100);
    return '00:' + String(seconds).padStart(2, '0') + '.' + tenths;
  }

  function delay(milliseconds, sequence) {
    var duration = reducedMotion ? Math.min(milliseconds, 180) : milliseconds;
    return new Promise(function (resolve) {
      if (sequence !== runSequence || !active) { resolve(false); return; }
      var pending = { timer: null, resolve: resolve };
      pending.timer = window.setTimeout(function () {
        pendingDelays.delete(pending);
        if (sequence !== runSequence || !active) { resolve(false); return; }
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

  function log(value, level, eventType, category, phase, operationId, technical) {
    hud.log(value, level || 'info', timestamp(), eventType || category || 'HUD', null, null, null, {
      category: category || 'SYSTEM', phase: phase || 'INFO', operationId: operationId || null,
      technical: technical === true
    });
  }

  function presetOptions() {
    if (!configurableHud) return {};
    var options = hud.preset(selectedPreset);
    if (!options) throw new Error('HUD preset is unavailable: ' + selectedPreset);
    return Object.assign(options, {
      preset: selectedPreset, position: 'BOTTOM_RIGHT',
      width: Math.max(240, Math.min(options.width, window.innerWidth - 24)), branding: 'TEST_LENS'
    });
  }

  function decorate(target, label, duration, state) {
    var colors = { action: '#ffeb3b', waiting: '#2196f3', retry: '#ff9800', success: '#4caf50', failure: '#f44336' };
    var effective = state || 'action';
    highlight.clear();
    highlight.element(target, label, {
      duration: reducedMotion ? 500 : duration, color: colors[effective], state: effective,
      borderWidth: 3, showLabel: true
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
        var frameId = originalRequestAnimationFrame.call(window, function (time) { frameIds.delete(frameId); callback(time); });
        frameIds.add(frameId);
        return frameId;
      }
      function finishScroll(completed) {
        if (!activeScroll || activeScroll.sequence !== sequence) return;
        window.requestAnimationFrame = originalRequestAnimationFrame;
        activeScroll = null;
        resolve(completed && sequence === runSequence && active);
      }
      activeScroll = { sequence: sequence, frameIds: frameIds, originalRequestAnimationFrame: originalRequestAnimationFrame, resolve: resolve };
      window.requestAnimationFrame = trackedRequestAnimationFrame;
      scrollArrow.scrollToElementWithArrow(placeOrder, 900, 'CENTER', 'CENTER', function () { finishScroll(true); });
    });
  }

  function cancelScroll() {
    if (!activeScroll) return;
    var scroll = activeScroll;
    activeScroll = null;
    scroll.frameIds.forEach(function (frameId) { window.cancelAnimationFrame(frameId); });
    window.requestAnimationFrame = scroll.originalRequestAnimationFrame;
    scroll.resolve(false);
  }

  function cancelScheduledWork() {
    runSequence += 1;
    if (replayTimer !== null) { window.clearTimeout(replayTimer); replayTimer = null; }
    pendingDelays.forEach(function (pending) { window.clearTimeout(pending.timer); pending.resolve(false); });
    pendingDelays.clear();
    cancelScroll();
    highlight.clear();
    scrollArrow.clear();
  }

  function scenarioName() {
    if (scenarioSelect && scenarioSelect.options && scenarioSelect.selectedIndex >= 0) {
      return scenarioSelect.options[scenarioSelect.selectedIndex].text;
    }
    return 'Action + assertion';
  }

  function resetVisuals() {
    hud.clear();
    window.scrollTo(0, 0);
    email.value = '';
    confirmation.hidden = true;
    if (clickCover) clickCover.hidden = true;
    elapsed = 0;
    hud.init({
      testName: '0.4.0 · ' + scenarioName(), pipelineId: 'docs-demo', position: 'BOTTOM_RIGHT',
      offsetX: 12, offsetY: 12, maxWidth: Math.max(260, Math.min(370, window.innerWidth - 24)),
      themeName: 'DARK', hudOptions: presetOptions()
    });
    setStep('Preparing ' + scenarioName());
    document.body.dataset.demoState = 'running';
    document.body.dataset.demoMode = 'default';
  }

  async function finishScenario(sequence) {
    setStep('PASSED');
    log('SESSION PASSED', 'success', 'STEP_PASSED', 'SYSTEM', 'PASSED', 'session');
    document.body.dataset.demoState = 'passed';
    if (!reducedMotion && active && sequence === runSequence) {
      replayTimer = window.setTimeout(function () { replayTimer = null; startScenario(); }, 2400);
    }
  }

  async function playLifecycle(sequence) {
    setStep('WAIT page ready');
    log('Document ready state', 'info', 'WAIT', 'ACTION', 'RUNNING', 'wait-ready');
    if (!await delay(800, sequence)) return;
    log('Document ready state', 'success', 'WAIT', 'ACTION', 'PASSED', 'wait-ready');

    setStep('FILL Email');
    decorate(email, 'ACTION · Fill Email', 1150, 'action');
    log('Fill Email', 'info', 'LOCATOR_ACTION_STARTED', 'ACTION', 'RUNNING', 'fill-email');
    if (!await delay(850, sequence)) return;
    email.value = 'qa@example.test';
    email.dispatchEvent(new Event('input', { bubbles: true }));
    log('Fill Email', 'success', 'LOCATOR_ACTION_PASSED', 'ACTION', 'PASSED', 'fill-email');
    if (!await delay(750, sequence)) return;

    setStep('CLICK Continue');
    decorate(continueButton, 'ACTION · Click Continue', 1100, 'action');
    log('Click Continue', 'info', 'LOCATOR_ACTION_STARTED', 'ACTION', 'RUNNING', 'click-continue');
    if (!await delay(850, sequence)) return;
    decorate(continueButton, 'SUCCESS · Click Continue', 1000, 'success');
    log('Click Continue', 'success', 'LOCATOR_ACTION_PASSED', 'ACTION', 'PASSED', 'click-continue');
    if (!await delay(550, sequence)) return;

    setStep('SCROLL checkout');
    log('Scroll checkout', 'info', 'ACTION', 'ACTION', 'RUNNING', 'scroll-checkout');
    if (!await scrollToOrder(sequence)) return;
    log('Scroll checkout', 'success', 'ACTION', 'ACTION', 'PASSED', 'scroll-checkout');

    setStep('CLICK Place order');
    decorate(placeOrder, 'ACTION · Click Place order', 1250, 'action');
    log('Click Place order', 'info', 'LOCATOR_ACTION_STARTED', 'ACTION', 'RUNNING', 'click-order');
    if (!await delay(850, sequence)) return;
    decorate(placeOrder, 'SUCCESS · Click Place order', 1000, 'success');
    log('Click Place order', 'success', 'LOCATOR_ACTION_PASSED', 'ACTION', 'PASSED', 'click-order');

    setStep('Observe order request');
    log('Request recorded: POST /api/orders', 'royal', 'NETWORK_REQUEST_RECORDED', 'SYSTEM', 'INFO', 'request-order');
    if (!await delay(850, sequence)) return;
    log('Response recorded: 200 /api/orders', 'success', 'NETWORK_RESPONSE_RECORDED', 'SYSTEM', 'PASSED', 'request-order');
    if (!await delay(650, sequence)) return;

    confirmation.hidden = false;
    setStep('ASSERT confirmation visible');
    decorate(confirmation, 'WAITING · Confirmation visible', 1400, 'waiting');
    log('Confirmation is visible', 'info', 'ASSERTION_STARTED', 'ASSERTION', 'RUNNING', 'assert-confirmation');
    if (!await delay(850, sequence)) return;
    decorate(confirmation, 'SUCCESS · Confirmation visible', 1800, 'success');
    log('Confirmation is visible', 'success', 'ASSERTION_PASSED', 'ASSERTION', 'PASSED', 'assert-confirmation');
    await finishScenario(sequence);
  }

  async function playHighlights(sequence) {
    var states = [['ACTION', 'action', 'Preparing click'], ['WAITING', 'waiting', 'Waiting for target'], ['RETRY', 'retry', 'Retry after interception'], ['SUCCESS', 'success', 'Click completed'], ['FAILURE', 'failure', 'Assertion example']];
    for (var index = 0; index < states.length; index += 1) {
      var item = states[index];
      setStep(item[0] + ' highlight');
      decorate(continueButton, item[0] + ' · ' + item[2], 1300, item[1]);
      log(item[2], item[1] === 'failure' ? 'error' : 'info', 'HIGHLIGHT', 'HIGHLIGHT', item[0] === 'RETRY' ? 'RETRYING' : (item[0] === 'FAILURE' ? 'FAILED' : item[0] === 'SUCCESS' ? 'PASSED' : 'RUNNING'), 'highlight-demo');
      if (!await delay(1200, sequence)) return;
    }
    setStep('Highlight preview complete');
    document.body.dataset.demoState = 'passed';
    if (!reducedMotion && active && sequence === runSequence) {
      replayTimer = window.setTimeout(function () { replayTimer = null; startScenario(); }, 2400);
    }
  }

  async function playObservability(sequence) {
    setStep('DEFAULT · live presentation');
    decorate(email, 'ACTION · DEFAULT is visible', 1600, 'action');
    log('DEFAULT: action shown live', 'info', 'LOCATOR_ACTION_STARTED', 'ACTION', 'RUNNING', 'default-action');
    if (!await delay(1800, sequence)) return;
    log('DEFAULT: action shown live', 'success', 'LOCATOR_ACTION_PASSED', 'ACTION', 'PASSED', 'default-action');
    if (!await delay(1400, sequence)) return;
    highlight.clear();
    hud.clear();
    accessibleStatus.textContent = 'FAST keeps structured summary and failure evidence; live presentation is disabled';
    document.body.dataset.demoMode = 'fast';
    if (!await delay(2200, sequence)) return;
    resetVisuals();
    log('FAST run retained its structured summary', 'success', 'STEP_PASSED', 'SYSTEM', 'PASSED', 'fast-summary');
    await finishScenario(sequence);
  }

  async function playNative(sequence) {
    setStep('Ordinary Selenium · sendKeys');
    decorate(email, 'ACTION · WebElement.sendKeys', 1400, 'action');
    log('Native sendKeys on Email', 'info', 'ACTION', 'ACTION', 'RUNNING', 'native-sendkeys');
    if (!await delay(1000, sequence)) return;
    email.value = '•••••••••••••••';
    log('Native sendKeys on Email (value redacted)', 'success', 'ACTION', 'ACTION', 'PASSED', 'native-sendkeys');
    if (!await delay(1200, sequence)) return;
    setStep('Ordinary Selenium · click exactly once');
    decorate(continueButton, 'ACTION · WebElement.click', 1400, 'action');
    log('Native click on Continue', 'info', 'ACTION', 'ACTION', 'RUNNING', 'native-click');
    if (!await delay(1000, sequence)) return;
    decorate(continueButton, 'SUCCESS · executed once', 1700, 'success');
    log('Native click on Continue · executed once', 'success', 'ACTION', 'ACTION', 'PASSED', 'native-click');
    await finishScenario(sequence);
  }

  async function playSmartClick(sequence) {
    window.scrollTo(0, placeOrder.getBoundingClientRect().top + window.scrollY - 180);
    if (clickCover) clickCover.hidden = false;
    setStep('Smart Click · shared deadline');
    decorate(placeOrder, 'ACTION · Click Place order', 1600, 'action');
    log('Click Place order', 'info', 'LOCATOR_ACTION_STARTED', 'ACTION', 'RUNNING', 'smart-click');
    if (!await delay(1100, sequence)) return;
    log('NATIVE · intercepted', 'debug', 'ACTIONABILITY_NOT_READY', 'ACTIONABILITY', 'DEBUG', 'smart-click', true);
    if (!await delay(900, sequence)) return;
    log('ACTIONS · hit-test mismatch', 'debug', 'ACTIONABILITY_NOT_READY', 'ACTIONABILITY', 'DEBUG', 'smart-click', true);
    if (!await delay(900, sequence)) return;
    log('POINT · no valid point', 'debug', 'ACTIONABILITY_NOT_READY', 'ACTIONABILITY', 'DEBUG', 'smart-click', true);
    if (!await delay(900, sequence)) return;
    log('JS · dispatched once (fallback enabled)', 'debug', 'ACTIONABILITY_READY', 'ACTIONABILITY', 'DEBUG', 'smart-click', true);
    if (clickCover) clickCover.hidden = true;
    decorate(placeOrder, 'SUCCESS · Click Place order', 1800, 'success');
    log('Click Place order', 'success', 'LOCATOR_ACTION_PASSED', 'ACTION', 'PASSED', 'smart-click');
    await finishScenario(sequence);
  }

  async function play(sequence) {
    resetVisuals();
    if (selectedScenario === 'highlights') return playHighlights(sequence);
    if (selectedScenario === 'observability') return playObservability(sequence);
    if (selectedScenario === 'native') return playNative(sequence);
    if (selectedScenario === 'smart-click') return playSmartClick(sequence);
    return playLifecycle(sequence);
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
    if (!active) { cancelScheduledWork(); document.body.dataset.demoState = 'paused'; return; }
    startScenario();
  }

  replayButton.addEventListener('click', function () { if (active) startScenario(); });
  if (scenarioSelect) {
    var scenarioMatch = window.location && /[?&]scenario=([^&]+)/.exec(window.location.search || '');
    if (scenarioMatch) {
      var requestedScenario = decodeURIComponent(scenarioMatch[1]);
      var knownScenario = Array.prototype.some.call(scenarioSelect.options, function (option) { return option.value === requestedScenario; });
      if (knownScenario) scenarioSelect.value = requestedScenario;
    }
    selectedScenario = scenarioSelect.value;
    scenarioSelect.addEventListener('change', function () { selectedScenario = scenarioSelect.value; if (active) startScenario(); });
  }
  var presetMatch = window.location && /[?&]preset=([^&]+)/.exec(window.location.search || '');
  if (presetMatch && /^(COMPACT|MINIMAL|DEBUG)$/.test(decodeURIComponent(presetMatch[1]).toUpperCase())) {
    selectedPreset = decodeURIComponent(presetMatch[1]).toUpperCase();
  }
  document.querySelectorAll('[data-hud-preset]').forEach(function (button) {
    button.setAttribute('aria-pressed', String(button.getAttribute('data-hud-preset') === selectedPreset));
    button.addEventListener('click', function () {
      selectedPreset = button.getAttribute('data-hud-preset');
      document.querySelectorAll('[data-hud-preset]').forEach(function (candidate) { candidate.setAttribute('aria-pressed', String(candidate === button)); });
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
    motionPreference.addEventListener('change', function (event) { reducedMotion = event.matches; if (active) startScenario(); });
  }
  updateActivity();
  if (window.parent !== window) window.parent.postMessage({ type: 'test-lens-demo-ready' }, '*');
})(window, document);
