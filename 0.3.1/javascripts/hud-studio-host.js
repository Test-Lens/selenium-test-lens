(function (window, document) {
  'use strict';

  var host = document.querySelector('[data-studio-host]');
  if (!host) return;

  var frame = host.querySelector('[data-studio-frame]');
  var expand = document.querySelector('[data-studio-expand]');
  var exit = document.querySelector('[data-studio-exit]');
  var fullscreen = document.querySelector('[data-studio-fullscreen]');
  var expandTrigger = null;
  var fullscreenTrigger = null;

  document.body.classList.add('tl-hud-studio-page');

  function notifyStudioResize() {
    window.dispatchEvent(new Event('resize'));
    if (frame && frame.contentWindow) {
      frame.contentWindow.postMessage({ type: 'studio-host-resize' }, '*');
    }
  }

  function setExpanded(active, restoreFocus) {
    document.body.classList.toggle('tl-studio-focus-mode', active);
    host.classList.toggle('is-expanded', active);
    expand.hidden = active;
    exit.hidden = !active;
    expand.setAttribute('aria-pressed', String(active));
    notifyStudioResize();
    if (!active && restoreFocus && expandTrigger) expandTrigger.focus();
  }

  expand.addEventListener('click', function () {
    expandTrigger = expand;
    setExpanded(true, false);
    exit.focus();
  });
  exit.addEventListener('click', function () { setExpanded(false, true); });

  var fullscreenAvailable = Boolean(document.fullscreenEnabled && host.requestFullscreen && document.exitFullscreen);
  if (!fullscreenAvailable) {
    fullscreen.hidden = true;
  } else {
    fullscreen.addEventListener('click', function () {
      fullscreenTrigger = fullscreen;
      try {
        var request = document.fullscreenElement === host ? document.exitFullscreen() : host.requestFullscreen();
        if (request && request.catch) request.catch(function () { fullscreen.disabled = true; });
      } catch (error) {
        fullscreen.disabled = true;
      }
    });
    document.addEventListener('fullscreenchange', function () {
      var active = document.fullscreenElement === host;
      fullscreen.setAttribute('aria-pressed', String(active));
      fullscreen.textContent = active ? 'Exit fullscreen' : 'Fullscreen';
      fullscreen.setAttribute('aria-label', active ? 'Exit browser fullscreen' : 'Open HUD Studio in browser fullscreen');
      notifyStudioResize();
      if (!active && fullscreenTrigger) fullscreenTrigger.focus();
    });
  }

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && document.body.classList.contains('tl-studio-focus-mode') && !document.fullscreenElement) {
      setExpanded(false, true);
    }
  });
})(window, document);
