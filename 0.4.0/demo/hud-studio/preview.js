(function (window, document) {
  'use strict';
  var hud = window.__uiTestLens.modules.hud;
  var highlight = window.__uiTestLens.modules.highlight;
  var state = null;
  var generation = 0;
  var timers = [];

  function cancel() {
    generation += 1;
    timers.forEach(window.clearTimeout);
    timers = [];
    highlight.clear();
  }

  function later(callback, delay, run) {
    timers.push(window.setTimeout(function () { if (run === generation) callback(); }, delay));
  }

  function panel() { return window.__seleniumOverlayRoot.querySelector('#selenium-hud-panel'); }

  function ensureStudioControlStyles() {
    var root = window.__seleniumOverlayRoot;
    if (root.querySelector('style[data-studio-controls]')) return;
    var styles = document.createElement('style');
    styles.dataset.studioControls = 'true';
    styles.textContent =
      '.stl-studio-dragging{cursor:grabbing!important;outline:2px solid var(--ui-test-lens-hud-accent,#38bdf8)!important}' +
      '.stl-studio-drag-handle{position:absolute;top:2px;right:2px;z-index:4;width:auto;padding:2px 5px;border:1px solid rgba(248,250,252,.65);border-radius:3px;color:#f8fafc;background:rgba(15,23,42,.82);font:9px/1.2 system-ui,sans-serif;cursor:grab;pointer-events:auto}' +
      '.stl-studio-drag-handle:active{cursor:grabbing}' +
      '.stl-studio-resize{position:absolute;right:3px;bottom:3px;z-index:4;width:18px;height:18px;border:2px solid #f8fafc;border-radius:4px;background:var(--ui-test-lens-hud-accent,#38bdf8);box-shadow:0 1px 4px rgba(15,23,42,.5);cursor:nwse-resize;pointer-events:auto}';
    root.appendChild(styles);
  }

  function enhancePanel() {
    var value = panel();
    if (!value) return;
    ensureStudioControlStyles();
    value.dataset.studioInteractive = 'true';
    if (!value.querySelector('.stl-studio-drag-handle')) {
      var dragHandle = document.createElement('button');
      dragHandle.type = 'button';
      dragHandle.className = 'stl-studio-drag-handle';
      dragHandle.textContent = 'Drag HUD';
      dragHandle.setAttribute('aria-label', 'Drag HUD preview');
      value.appendChild(dragHandle);
      dragHandle.addEventListener('pointerdown', beginDrag);
    }
    if (!value.querySelector('.stl-studio-resize')) {
      var handle = document.createElement('span');
      handle.className = 'stl-studio-resize';
      handle.title = 'Resize HUD';
      value.appendChild(handle);
      handle.addEventListener('pointerdown', beginResize);
    }
    if (!value.__studioBound) {
      value.__studioBound = true;
      value.addEventListener('click', selectSection);
    }
  }

  function render() {
    if (!state) return;
    hud.remove();
    hud.init({testName:'Checkout creates an order',pipelineId:'studio-preview',offsetX:state.offsetX,offsetY:state.offsetY,maxWidth:state.width,themeName:'CUSTOM',hudOptions:state});
    hud.setStep('Checkout semantic event preview');
    var times=state.timestampPreview&&state.timestampPreview.events||[];
    function event(message,level,type,category,phase,id,index,technical,attempt,duration){
      hud.log(message,level,'2026-07-15T22:00:0'+index+'.123456789Z',type,null,null,times.length ? times[index%times.length] : null,
        {category:category,phase:phase,operationId:id,technical:!!technical,attempt:attempt||0,durationMs:duration||0,severity:String(level).toUpperCase()});
    }
    event('Place order','info','LOCATOR_ACTION_STARTED','ACTION','RUNNING','preview-action-running',0,false);
    event('Place order','info','LOCATOR_ACTION_PASSED','ACTION','PASSED','preview-action-passed',1,false,0,143);
    event('Confirmation should be visible','info','ASSERTION_STARTED','ASSERTION','RUNNING','preview-assert-running',2,false);
    event('Previous value was hidden','warn','ASSERTION_RETRY','ASSERTION','RETRYING','preview-assert-retry',3,false,2);
    event('Confirmation is visible','info','ASSERTION_PASSED','ASSERTION','PASSED','preview-assert-passed',4,false,0,281);
    event('Order number should exist','error','ASSERTION_FAILED','ASSERTION','FAILED','preview-assert-failed',5,false,0,500);
    event('JetBrains protocol handler is not registered','warn','GENERAL','SYSTEM','WARNING','preview-warning',6,false);
    event('Checkpoint: payment fixture prepared','info','HUD','USER','INFO','preview-user',7,false);
    event('Resolved Place order button','info','LOCATOR_RESOLVE_PASSED','LOCATOR','DEBUG','preview-locator',8,true);
    event('Rendered success-state outline','info','HIGHLIGHT','HIGHLIGHT','DEBUG','preview-highlight',9,true);
    enhancePanel();
    if (state.sourceNavigationEnabled) {
      var sourcePreviewActive=!!state.sourceNavigationPreviewActive;
      if (state.sourceNavigationModifier === 'CTRL_ALT') {
        window.dispatchEvent(new KeyboardEvent(sourcePreviewActive?'keydown':'keyup',{key:sourcePreviewActive?'Alt':'Shift',ctrlKey:sourcePreviewActive,altKey:sourcePreviewActive,bubbles:true}));
      } else if ((panel().dataset.sourceNavigationActive === 'true') !== sourcePreviewActive) {
        window.dispatchEvent(new KeyboardEvent('keydown',{key:'F8',code:'F8',bubbles:true}));
      }
    }
    if (panel()) window.parent.postMessage({type:'hud-rendered'},'*');
  }

  function replay() {
    cancel();
    var run = generation;
    var target = document.getElementById('preview-order');
    document.getElementById('preview-result').hidden = true;
    render();
    var configured=state.highlight||{},base={borderWidth:configured.borderWidthPx,showLabel:configured.showLabels},previewOperation=0;
    function show(label,visual,color,automatic){if(configured.enabled!==false&&(!automatic||configured.automaticFeedback!==false)){var key=visual+'DurationMs',explicit=Object.prototype.hasOwnProperty.call(configured,key),duration=explicit?configured[key]:configured.durationMs;highlight.element(target,label,Object.assign({},base,{state:visual,color:color,duration:duration,suppress:explicit&&duration===0,sessionId:'hud-studio',operationId:'preview-'+(++previewOperation),standalone:true}));}}
    show('ACTION Place order','action',configured.actionColor,false);
    later(function () { hud.setStep('WAIT checkout ready');show('WAIT checkout ready','waiting',configured.waitingColor,true); }, 350, run);
    later(function () { hud.setStep('RETRY checkout ready');show('RETRY checkout ready','retry',configured.retryColor,true); }, 800, run);
    later(function () { document.getElementById('preview-result').hidden=false;hud.setStep('PASSED');show('SUCCESS Place order','success',configured.successColor,true); }, 1400, run);
    later(function () { show('FAILURE demo','failure',configured.failureColor,true); }, 2300, run);
  }

  function beginDrag(event) {
    if (event.button !== 0) return;
    var value = panel();
    var rect = value.getBoundingClientRect();
    var startX = event.clientX, startY = event.clientY;
    value.classList.add('stl-studio-dragging');
    var previousOutline=value.style.outline,previousCursor=value.style.cursor;
    value.style.outline='2px solid var(--ui-test-lens-hud-accent,#38bdf8)';value.style.cursor='grabbing';
    var snap = document.createElement('div'); snap.className='stl-studio-snap'; document.body.appendChild(snap);
    function move(moveEvent) {
      var left = Math.max(0, Math.min(window.innerWidth-rect.width, rect.left+moveEvent.clientX-startX));
      var top = Math.max(0, Math.min(window.innerHeight-rect.height, rect.top+moveEvent.clientY-startY));
      value.style.left=left+'px';value.style.top=top+'px';value.style.right='auto';value.style.bottom='auto';
    }
    function finish(upEvent) {
      value.classList.remove('stl-studio-dragging');value.style.outline=previousOutline;value.style.cursor=previousCursor;snap.remove();
      window.removeEventListener('pointermove',move);window.removeEventListener('pointerup',finish);window.removeEventListener('pointercancel',finish);
      var finalRect=value.getBoundingClientRect();
      var left=finalRect.left,top=finalRect.top,right=window.innerWidth-finalRect.right,bottom=window.innerHeight-finalRect.bottom;
      var horizontal=left<=right?'LEFT':'RIGHT',vertical=top<=bottom?'TOP':'BOTTOM';
      window.parent.postMessage({type:'hud-change',changes:{position:vertical+'_'+horizontal,offsetX:Math.min(500,Math.round(Math.max(0,horizontal==='LEFT'?left:right))),offsetY:Math.min(500,Math.round(Math.max(0,vertical==='TOP'?top:bottom)))}},'*');
      upEvent.preventDefault();
    }
    window.addEventListener('pointermove',move);window.addEventListener('pointerup',finish);window.addEventListener('pointercancel',finish);event.preventDefault();
  }

  function beginResize(event) {
    event.stopPropagation();event.preventDefault();
    var value=panel(),rect=value.getBoundingClientRect(),startX=event.clientX,startY=event.clientY;
    var nextWidth=state.width,nextHeight=state.maxHeight;
    function move(moveEvent){nextWidth=Math.max(240,Math.min(960,state.width+moveEvent.clientX-startX));nextHeight=Math.max(120,Math.min(1000,state.maxHeight+moveEvent.clientY-startY));value.style.width=nextWidth+'px';value.style.height=nextHeight+'px';value.style.maxHeight=nextHeight+'px';}
    function finish(){window.removeEventListener('pointermove',move);window.removeEventListener('pointerup',finish);window.removeEventListener('pointercancel',finish);window.parent.postMessage({type:'hud-change',changes:{width:Math.round(nextWidth),maxHeight:Math.round(nextHeight)}},'*');}
    window.addEventListener('pointermove',move);window.addEventListener('pointerup',finish);window.addEventListener('pointercancel',finish);
  }

  function selectSection(event) {
    var section='appearance';
    if (event.target.closest('.stl-hud-side-rail,.stl-hud-header-brand')) section='branding';
    else if (event.target.closest('#selenium-hud-logs')) section='content';
    else if (event.target.closest('#selenium-hud-test,#selenium-hud-step,#selenium-hud-pipeline')) section='typography';
    window.parent.postMessage({type:'hud-select',section:section},'*');
  }

  window.addEventListener('message', function (event) {
    if (event.source !== window.parent || !event.data) return;
    if (event.data.type === 'hud-config') { state=event.data.config; render(); }
    if (event.data.type === 'hud-replay') replay();
  });
  window.parent.postMessage({type:'hud-ready',presets:{MINIMAL:hud.preset('MINIMAL'),COMPACT:hud.preset('COMPACT'),STANDARD:hud.preset('STANDARD'),DEBUG:hud.preset('DEBUG')}},'*');
})(window,document);
