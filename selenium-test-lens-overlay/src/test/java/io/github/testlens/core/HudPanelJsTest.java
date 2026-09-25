package io.github.testlens.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPanelJsTest {

    @Test
    void initLoadsHudPanelResource() {
        assertFalse(HudPanelJs.INIT.isBlank());
        assertTrue(HudPanelJs.INIT.contains("__uiTestLens"));
        assertTrue(HudPanelJs.INIT.contains("modules.hud"));
        assertTrue(HudPanelJs.INIT.contains("init: init"));
        assertTrue(HudPanelJs.INIT.contains("setStep: setStep"));
        assertTrue(HudPanelJs.INIT.contains("log: log"));
        assertTrue(HudPanelJs.INIT.contains("clear: clear"));
        assertTrue(HudPanelJs.INIT.contains("remove: remove"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-bg"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-fg"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-accent"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-max-height"));
        assertTrue(HudPanelJs.INIT.contains("maxHeightPx"));
        assertTrue(HudPanelJs.INIT.contains("panel.style.backdropFilter = theme.backdropFilter"));
        assertTrue(HudPanelJs.INIT.contains("panel.style.webkitBackdropFilter = theme.backdropFilter"));
        assertTrue(HudPanelJs.INIT.contains("updateScrollableRegions"));
        assertTrue(HudPanelJs.INIT.contains("ensureScrollbarStyles"));
        assertTrue(HudPanelJs.INIT.contains("scrollbar-color"));
        assertTrue(HudPanelJs.INIT.contains("::-webkit-scrollbar-thumb:hover"));
        assertTrue(HudPanelJs.INIT.contains("data-test-lens-font-status"));
        assertTrue(HudPanelJs.INIT.contains("data-test-lens-font-reflow"));
        assertTrue(HudPanelJs.INIT.contains("sharedTypography.subscribe"));
        assertTrue(HudPanelJs.INIT.contains("source-navigation-active"));
        assertTrue(HudPanelJs.INIT.contains("panel.style.pointerEvents = 'auto'"));
        assertTrue(HudPanelJs.INIT.contains("logs.style.overscrollBehavior = 'contain'"));
        assertTrue(HudPanelJs.INIT.contains("window.addEventListener('blur'"));
        assertTrue(HudPanelJs.INIT.contains("cleanupSourceNavigation"));
        assertTrue(HudPanelJs.INIT.contains("__uiTestLensSourceNavigation"));
        assertTrue(HudPanelJs.INIT.contains("getModifierState('AltGraph')"));
        assertTrue(HudPanelJs.INIT.contains("event.repeat"));
        assertTrue(HudPanelJs.INIT.contains("editableKeyTarget"));
        assertTrue(HudPanelJs.INIT.contains("event.key === 'F8'"));
        assertTrue(HudPanelJs.INIT.contains("event.key === 'Escape'"));
        assertTrue(HudPanelJs.INIT.contains("source.setAttribute('href'"));
        assertTrue(HudPanelJs.INIT.contains("setSourceNavigationCompatibility"));
        assertTrue(HudPanelJs.INIT.contains("consumeSourceNavigationCompatibilityRetry"));
        assertTrue(HudPanelJs.INIT.contains("Source Navigation unavailable"));
        assertTrue(HudPanelJs.INIT.contains("Retry compatibility check"));
        assertTrue(HudPanelJs.INIT.contains("source.removeAttribute('href')"));
        assertFalse(HudPanelJs.INIT.contains("addEventListener('wheel'"));
        assertFalse(HudPanelJs.INIT.contains("setTimeout(function()"));
    }

    @Test
    void initContainsMinimalBrandingShell() {
        assertTrue(HudPanelJs.INIT.contains("stl-hud-shell"));
        assertFalse(HudPanelJs.INIT.contains("header.className = 'stl-hud-header'"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-brand-icon"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-side-rail"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-rail-brand"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-brand-text"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-main"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.flex = '0 0 ' + railWidth + 'px'"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.width = railWidth + 'px'"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.minWidth = railWidth + 'px'"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.padding = '0'"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.marginRight = '2px'"));
        assertTrue(HudPanelJs.INIT.contains("sideRail.style.marginLeft = 'calc(2px - var(--ui-test-lens-hud-padding-x, 10px))'"));
        assertFalse(HudPanelJs.INIT.contains("main.style.paddingLeft"));
        assertTrue(HudPanelJs.INIT.contains("railBrand.style.alignItems = 'center'"));
        assertTrue(HudPanelJs.INIT.contains("railBrand.style.justifyContent = 'center'"));
        assertTrue(HudPanelJs.INIT.contains("TEST LENS"));
        assertTrue(HudPanelJs.INIT.contains("<svg class=\"stl-hud-brand-icon-svg\" width=\"14\" height=\"14\""));
        assertTrue(HudPanelJs.INIT.contains("configureBrandContent(railBrand, config, false)"));
        assertFalse(HudPanelJs.INIT.contains("Selenium/WebDriver"));
    }

    @Test
    void initContainsCompactMetadataRows() {
        assertTrue(HudPanelJs.INIT.contains("function metadataRowMarkup"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-meta-row"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-meta-label"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-meta-value"));
        assertTrue(HudPanelJs.INIT.contains("display:flex;align-items:baseline;gap:4px"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-header-font-size"));
        assertTrue(HudPanelJs.INIT.contains("font-weight:400"));
        assertTrue(HudPanelJs.INIT.contains("color:var(--ui-test-lens-hud-success, #22c55e)"));
        assertTrue(HudPanelJs.INIT.contains("font-size:var(--ui-test-lens-hud-header-font-size, 10px)"));
        assertTrue(HudPanelJs.INIT.contains("text-overflow:ellipsis"));
        assertTrue(HudPanelJs.INIT.contains("white-space:nowrap"));
        assertTrue(HudPanelJs.INIT.contains("metadataRowMarkup('TEST', config.testName, 'stl-hud-test-row', '--ui-test-lens-hud-header-font-family')"));
        assertTrue(HudPanelJs.INIT.contains("metadataRowMarkup('STEP', stepDescription, 'stl-hud-step-row', '--ui-test-lens-hud-step-font-family')"));
        assertTrue(HudPanelJs.INIT.contains("metadataRowMarkup('PIPE'"));
        assertTrue(HudPanelJs.INIT.contains("showPipeline"));
        assertTrue(HudPanelJs.INIT.contains("function escapeHtml"));
        assertFalse(HudPanelJs.INIT.contains("grid-template-columns:52px"));
        assertFalse(HudPanelJs.INIT.contains(">Test</span>"));
        assertFalse(HudPanelJs.INIT.contains(">Step</span> <span>"));
    }

    @Test
    void initContainsAiryLogsAndWiderFallbackWidth() {
        assertTrue(HudPanelJs.INIT.contains("option(config, 'width', config.maxWidth || 520)"));
        assertTrue(HudPanelJs.INIT.contains("SAFE_MARGIN_PX"));
        assertTrue(HudPanelJs.INIT.contains("Math.min(configuredLogHeight, availableLogHeight)"));
        assertTrue(HudPanelJs.INIT.contains("logs.style.marginTop = '3px'"));
        assertTrue(HudPanelJs.INIT.contains("logs.style.paddingTop = '4px'"));
        assertTrue(HudPanelJs.INIT.contains("row.style.marginBottom = '5px'"));
        assertTrue(HudPanelJs.INIT.contains("row.style.lineHeight = '1.32'"));
    }

    @Test
    void initContainsSemanticVisibilityAndBrandingConfiguration() {
        assertTrue(HudPanelJs.INIT.contains("function eventVisible"));
        assertTrue(HudPanelJs.INIT.contains("NETWORK_"));
        assertTrue(HudPanelJs.INIT.contains("LOCATOR_RETRY"));
        assertTrue(HudPanelJs.INIT.contains("showAssertions"));
        assertTrue(HudPanelJs.INIT.contains("showEventLog"));
        assertTrue(HudPanelJs.INIT.contains("showTimestamps"));
        assertTrue(HudPanelJs.INIT.contains("SEMANTIC_CATEGORY_ICONS"));
        assertTrue(HudPanelJs.INIT.contains("ACTION:'\\ud83d\\uddb1\\ufe0f'"));
        assertTrue(HudPanelJs.INIT.contains("ASSERTION:'\\ud83e\\uddea'"));
        assertTrue(HudPanelJs.INIT.contains("LOCATOR:'\\ud83d\\udd0e'"));
        assertTrue(HudPanelJs.INIT.contains("ACTIONABILITY:'\\ud83d\\udee1\\ufe0f'"));
        assertTrue(HudPanelJs.INIT.contains("HIGHLIGHT:'\\u2728'"));
        assertTrue(HudPanelJs.INIT.contains("USER:'\\ud83d\\udcac'"));
        assertTrue(HudPanelJs.INIT.contains("SYSTEM:'\\u2139\\ufe0f'"));
        assertTrue(HudPanelJs.INIT.contains("RUNNING:'\\u23f3'"));
        assertTrue(HudPanelJs.INIT.contains("PASSED:'\\u2705'"));
        assertTrue(HudPanelJs.INIT.contains("RETRYING:'\\ud83d\\udd04'"));
        assertTrue(HudPanelJs.INIT.contains("WARNING:'\\u26a0\\ufe0f'"));
        assertTrue(HudPanelJs.INIT.contains("FAILED:'\\u274c'"));
        assertTrue(HudPanelJs.INIT.contains("INFO:'\\u2139\\ufe0f'"));
        assertFalse(HudPanelJs.INIT.contains("ASSERTION:'\\ud83d\\udd0e'"));
        assertFalse(HudPanelJs.INIT.contains("LOCATOR:'\\ud83d\\udd0d'"));
        assertFalse(HudPanelJs.INIT.contains("LOCATOR:'\\ud83c\\udfaf'"));
        assertFalse(HudPanelJs.INIT.contains("LOCATOR:'\\ud83d\\udccd'"));
        assertTrue(HudPanelJs.INIT.contains("data-operation-id"));
        assertTrue(HudPanelJs.INIT.contains("prefers-reduced-motion:reduce"));
        assertTrue(HudPanelJs.INIT.contains("aria-hidden"));
        assertFalse(HudPanelJs.INIT.contains("message.indexOf"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-custom-logo"));
        assertTrue(HudPanelJs.INIT.contains("customLogo.style.width = 'auto'"));
        assertTrue(HudPanelJs.INIT.contains("customLogo.style.height = horizontal ? '14px'"));
        assertTrue(HudPanelJs.INIT.contains("Math.max(12, Math.min(16, positiveNumber(option(config, 'railWidth', 16)) - 2))"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-header-font-family"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-step-font-family"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-event-font-family"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-meta-font-family"));
        assertTrue(HudPanelJs.INIT.contains("var typography = options.typography || {}"));
        assertTrue(HudPanelJs.INIT.contains("horizontal ? '56px' : '64px'"));
        assertTrue(HudPanelJs.INIT.contains("customLogo.style.objectFit = 'contain'"));
        assertTrue(HudPanelJs.INIT.contains("branding === 'NONE'"));
    }

    @Test
    void initDoesNotShortCircuitWhenHudModuleAlreadyExists() {
        assertFalse(HudPanelJs.INIT.contains("lens.modules.hud && lens.modules.hud.__uiTestLensHud === true"));
        assertTrue(HudPanelJs.INIT.contains("lens.modules.hud = {"));
    }

    @Test
    void initContainsLegacyHudContentMigration() {
        assertTrue(HudPanelJs.INIT.contains("function migrateHudContent"));
        assertTrue(HudPanelJs.INIT.contains("placeAfter(structure.contextHeader, title, null)"));
        assertTrue(HudPanelJs.INIT.contains("option(config, 'showPipeline', false)"));
        assertTrue(HudPanelJs.INIT.contains("placeAfter(structure.contextHeader, step, title)"));
        assertTrue(HudPanelJs.INIT.contains("structure.main.appendChild(logs)"));
    }

    @Test
    void initUpgradesExistingHudModuleAndRendersBrandingWhenNodeIsAvailable(@TempDir Path temp) throws Exception {
        Process process;
        try {
            Path validation = temp.resolve("hud-runtime-smoke.js");
            Files.writeString(validation, hudRuntimeSmokeScript(), StandardCharsets.UTF_8);
            process = new ProcessBuilder("node", validation.toString()).start();
        } catch (IOException ex) {
            throw new AssertionError("Node.js is required for HUD runtime smoke validation", ex);
        }

        boolean completed = process.waitFor(10, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String errors = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(completed, "Node HUD runtime smoke test timed out");
        assertEquals(0, process.exitValue(), output + errors);
    }

    @Test
    void bridgeScriptUsesPrimaryHudModule() {
        String script = HudPanelJs.bridgeScript();

        assertTrue(script.contains("modules.hud"));
    }

    private static String hudRuntimeSmokeScript() {
        return """
                function Style() {}
                Style.prototype.setProperty = function(name, value) { this[name] = String(value); };
                Style.prototype.removeProperty = function(name) { delete this[name]; };

                function Element(tag) {
                  this.tagName = tag;
                  this.children = [];
                  this.parentNode = null;
                  this.style = new Style();
                  this.attributes = {};
                  this.id = '';
                  this.className = '';
                  this._textContent = '';
                  this.innerHTML = '';
                  this.offsetHeight = 20;
                  this.offsetTop = 0;
                  var owner = this;
                  this.classList = {
                    add: function() { for (var i=0;i<arguments.length;i++) if ((' '+owner.className+' ').indexOf(' '+arguments[i]+' ')<0) owner.className += (owner.className?' ':'')+arguments[i]; },
                    remove: function() { for (var i=0;i<arguments.length;i++) owner.className=(' '+owner.className+' ').replace(' '+arguments[i]+' ',' ').trim(); }
                  };
                }
                Object.defineProperty(Element.prototype, 'textContent', {
                  get: function() {
                    if (this.tagName === '#text') return this._textContent;
                    if (this.children.length) return this.children.map(function(child){return child.textContent;}).join('');
                    return this._textContent;
                  },
                  set: function(value) { this._textContent = String(value); this.children = []; }
                });
                Element.prototype.appendChild = function(child) {
                  if (child.parentNode) {
                    var oldIndex = child.parentNode.children.indexOf(child);
                    if (oldIndex >= 0) child.parentNode.children.splice(oldIndex, 1);
                  }
                  child.parentNode = this;
                  this.children.push(child);
                  return child;
                };
                Element.prototype.insertBefore = function(child, next) {
                  if (child.parentNode) {
                    var oldIndex = child.parentNode.children.indexOf(child);
                    if (oldIndex >= 0) child.parentNode.children.splice(oldIndex, 1);
                  }
                  child.parentNode = this;
                  var index = next ? this.children.indexOf(next) : -1;
                  if (index < 0) this.children.push(child);
                  else this.children.splice(index, 0, child);
                  return child;
                };
                Element.prototype.setAttribute = function(name, value) {
                  this.attributes[name] = String(value);
                  if (name === 'id') this.id = String(value);
                  if (name === 'class') this.className = String(value);
                };
                Element.prototype.getBoundingClientRect = function() {
                  return { left: 0, top: 0, right: parseFloat(this.style.width) || 0,
                    bottom: this.offsetHeight || 0, width: parseFloat(this.style.width) || 0,
                    height: this.offsetHeight || 0 };
                };
                Element.prototype.matchesSelector = function(selector) {
                  if (selector.charAt(0) === '#') return this.id === selector.substring(1);
                  if (selector.charAt(0) === '.') return (' ' + this.className + ' ').indexOf(' ' + selector.substring(1) + ' ') >= 0;
                  return false;
                };
                Element.prototype.querySelector = function(selector) {
                  for (var i = 0; i < this.children.length; i++) {
                    var child = this.children[i];
                    if (child.matchesSelector(selector)) return child;
                    var nested = child.querySelector(selector);
                    if (nested) return nested;
                  }
                  return null;
                };
                Object.defineProperty(Element.prototype, 'previousSibling', {
                  get: function() {
                    if (!this.parentNode) return null;
                    var index = this.parentNode.children.indexOf(this);
                    return index > 0 ? this.parentNode.children[index - 1] : null;
                  }
                });
                Object.defineProperty(Element.prototype, 'nextSibling', {
                  get: function() {
                    if (!this.parentNode) return null;
                    var index = this.parentNode.children.indexOf(this);
                    return index >= 0 && index < this.parentNode.children.length - 1 ? this.parentNode.children[index + 1] : null;
                  }
                });

                function countByClass(node, className) {
                  var count = (' ' + node.className + ' ').indexOf(' ' + className + ' ') >= 0 ? 1 : 0;
                  for (var i = 0; i < node.children.length; i++) {
                    count += countByClass(node.children[i], className);
                  }
                  return count;
                }
                function assert(condition, message) {
                  if (!condition) throw new Error(message);
                }

                var root = new Element('root');
                var oldPanel = new Element('div');
                oldPanel.id = 'selenium-hud-panel';
                var oldStep = new Element('div');
                oldStep.id = 'selenium-hud-step';
                oldStep.innerHTML = '<b>Step:</b> old';
                oldPanel.appendChild(oldStep);
                root.appendChild(oldPanel);

                var window = {
                  innerWidth: 1024,
                  innerHeight: 768,
                  __seleniumOverlayRoot: root,
                  __uiTestLens: {
                    modules: {
                      hud: {
                        __uiTestLensHud: true,
                        init: function() { throw new Error('old hud module was not replaced'); }
                      }
                    },
                    state: { overlay: { root: root }, hud: {} }
                  },
                  getComputedStyle: function() { return { paddingTop: '0', paddingBottom: '0' }; }
                };
                var document = {
                  createElement: function(tag) { return new Element(tag); },
                  createTextNode: function(value) { var node=new Element('#text');node.textContent=value;return node; }
                };
                var runtime =\s""" + jsonString(HudPanelJs.INIT) + """
                ;

                eval(runtime);
                window.__uiTestLens.modules.hud.init({
                  testName: 'Checkout flow with a deliberately long test name that must stay on one line',
                  pipelineId: 'local',
                  maxWidth: 320,
                  theme: { maxHeightPx: 140 },
                  themeName: 'GLASS'
                });
                window.__uiTestLens.modules.hud.setStep('Pay <now> & confirm with a deliberately long current step name');
                window.__uiTestLens.modules.hud.log('Saved', 'info', 'now');
                window.__uiTestLens.modules.hud.init({
                  testName: 'Checkout flow with a deliberately long test name that must stay on one line',
                  pipelineId: 'local',
                  maxWidth: 320,
                  theme: { maxHeightPx: 140 },
                  themeName: 'GLASS'
                });

                assert(root.querySelector('.stl-hud-shell'), 'missing shell');
                assert(root.querySelector('.stl-hud-side-rail'), 'missing side rail');
                assert(root.querySelector('.stl-hud-rail-brand'), 'missing rail brand lockup');
                assert(root.querySelector('.stl-hud-main'), 'missing main');
                assert(root.querySelector('.stl-hud-brand-icon'), 'missing brand icon');
                assert(root.querySelector('.stl-hud-brand-icon').parentNode === root.querySelector('.stl-hud-rail-brand'), 'brand icon is not in rail lockup');
                assert(!root.querySelector('.stl-hud-header'), 'top header should not be rendered');
                assert(root.querySelector('.stl-hud-brand-text').textContent === 'TEST LENS', 'missing rail text');
                assert(root.querySelector('.stl-hud-side-rail').style.width === '16px', 'side rail width was not refined');
                assert(root.querySelector('.stl-hud-side-rail').style.minWidth === '16px', 'side rail minimum width was not refined');
                assert(root.querySelector('.stl-hud-side-rail').style.padding === '0', 'side rail padding was not removed');
                assert(root.querySelector('.stl-hud-side-rail').style.marginRight === '2px', 'side rail spacing is too wide');
                assert(root.querySelector('.stl-hud-side-rail').style.marginLeft === 'calc(2px - var(--ui-test-lens-hud-padding-x, 10px))', 'side rail does not reclaim panel padding');
                assert(!root.querySelector('.stl-hud-main').style.paddingLeft, 'main content should not add left padding');
                assert(root.querySelector('.stl-hud-rail-brand').style.transform === 'rotate(-90deg)', 'rail brand is not rotated');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('stl-hud-meta-row stl-hud-test-row') >= 0, 'test row missing');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('display:flex') >= 0, 'test row is not lightweight flex');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('font-weight:400') >= 0, 'test value is not normal weight');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('var(--ui-test-lens-hud-success, #22c55e)') >= 0, 'test value does not use the HUD success color');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('>TEST<') >= 0, 'test label missing');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('>Checkout flow with a deliberately long test name that must stay on one line<') >= 0, 'test value missing');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('text-overflow:ellipsis') >= 0, 'test value does not truncate');
                assert(root.querySelector('#selenium-hud-test').innerHTML.indexOf('title="Checkout flow with a deliberately long test name that must stay on one line"') >= 0, 'test value tooltip missing');
                assert(root.querySelector('#selenium-hud-step').innerHTML.indexOf('stl-hud-meta-row stl-hud-step-row') >= 0, 'step row missing');
                assert(root.querySelector('#selenium-hud-step').innerHTML.indexOf('>STEP<') >= 0, 'step label missing');
                assert(root.querySelector('#selenium-hud-step').innerHTML.indexOf('>Pay &lt;now&gt; &amp; confirm with a deliberately long current step name<') >= 0, 'step value was not escaped');
                assert(root.querySelector('#selenium-hud-step').innerHTML.indexOf('title="Pay &lt;now&gt; &amp; confirm with a deliberately long current step name"') >= 0, 'step tooltip was not escaped');
                assert(root.querySelector('#selenium-hud-step').parentNode === root.querySelector('.stl-hud-context-header'), 'legacy step was not migrated');
                assert(!root.querySelector('#selenium-hud-pipeline'), 'pipeline metadata must not be rendered');
                assert(root.querySelector('#selenium-hud-logs').parentNode === root.querySelector('.stl-hud-main'), 'logs are not in main');
                assert(root.querySelector('#selenium-hud-logs').children[0].style.marginBottom === '5px', 'log row spacing missing');
                assert(root.querySelector('#selenium-hud-logs').className.indexOf('stl-hud-scrollbar-subtle') >= 0, 'compact scrollbar is not subtle');
                assert(root.querySelector('#selenium-hud-logs').style['--ui-test-lens-scrollbar-width'] === '6px', 'compact scrollbar width missing');
                assert(root.querySelector('#selenium-hud-logs').style['--ui-test-lens-scrollbar-thumb'] === '#64748b', 'compact scrollbar thumb missing');
                assert(countByClass(root, 'stl-hud-side-rail') === 1, 'duplicated side rail');
                assert(countByClass(root, 'stl-hud-rail-brand') === 1, 'duplicated rail brand');
                assert(countByClass(root, 'stl-hud-brand-icon') === 1, 'duplicated brand icon');

                window.__uiTestLens.modules.hud.init({
                  testName: 'Debug', pipelineId: 'pipeline-7', theme: {},
                  hudOptions: { showTestName: true, showCurrentStep: true, showPipeline: true,
                    showTimestamps: true, showEventLog: true, showNetwork: false, showRetries: false,
                    showWaits: true, showAssertions: true, branding: 'NONE', width: 300, maxHeight: 220,
                    maxLogHeight: 100, position: 'TOP_LEFT', offsetX: 16, offsetY: 24,
                    fontPreset: 'MONOSPACE', typography: {header:'SYSTEM', eventLog:'UI_SANS'},
                    scrollbarStyle: 'STANDARD', scrollbarWidth: 8, scrollbarTrack: '#010203',
                    scrollbarThumb: '#040506', scrollbarThumbHover: '#070809',
                    baseFontSize: 12, headerFontSize: 9 }
                });
                assert(root.querySelector('#selenium-hud-pipeline'), 'configured pipeline missing');
                assert(root.querySelector('#selenium-hud-pipeline').innerHTML.indexOf('pipeline-7') >= 0, 'pipeline value missing');
                assert(!root.querySelector('.stl-hud-side-rail'), 'disabled branding must not remain in the DOM');
                assert(root.querySelector('#selenium-hud-panel').style.left === '16px', 'configured horizontal offset missing');
                assert(root.querySelector('#selenium-hud-panel').style.top === '24px', 'configured vertical offset missing');
                assert(root.querySelector('#selenium-hud-panel').style.maxHeight === 'var(--ui-test-lens-hud-max-height)', 'panel max height missing');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-font-size'] === '12px', 'base font size missing');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-header-font-size'] === '9px', 'header font size missing');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-header-font-family'].indexOf('system-ui') >= 0, 'header override missing');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-header-font-family'].indexOf('Test Lens Sora') < 0, 'SYSTEM header forced the UI font');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-step-font-family'].indexOf('ui-monospace') >= 0, 'step did not inherit global font');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-step-font-family'].indexOf('Test Lens Sora') < 0, 'MONOSPACE step forced the UI font');
                assert(root.querySelector('#selenium-hud-panel').style['--ui-test-lens-hud-event-font-family'].indexOf('Test Lens Sora') >= 0, 'event override missing');
                assert(root.querySelector('#selenium-hud-logs').className.indexOf('stl-hud-scrollbar-standard') >= 0, 'standard scrollbar class missing');
                assert(root.querySelector('#selenium-hud-logs').style['--ui-test-lens-scrollbar-width'] === '8px', 'custom scrollbar width missing');
                assert(root.querySelector('#selenium-hud-logs').style['--ui-test-lens-scrollbar-track'] === '#010203', 'custom scrollbar track missing');
                var rowsBefore = root.querySelector('#selenium-hud-logs').children.length;
                window.__uiTestLens.modules.hud.log('hidden network', 'info', 'now', 'NETWORK_RESPONSE_RECORDED');
                window.__uiTestLens.modules.hud.log('visible assertion', 'info', 'now', 'ASSERTION_PASSED');
                assert(root.querySelector('#selenium-hud-logs').children.length === rowsBefore + 1, 'semantic event filter failed');

                function lastLog() {
                  var values = root.querySelector('#selenium-hud-logs').children;
                  return values[values.length - 1];
                }
                function timestampConfig(format, zone, shown) {
                  window.__uiTestLens.modules.hud.init({testName:'Timestamps',theme:{},hudOptions:{
                    showEventLog:true,showTimestamps:shown,timestampFormat:format,timestampZone:zone,
                    branding:'NONE',showNetwork:true,showRetries:true,showWaits:true,showAssertions:true}});
                }
                timestampConfig('ISO_UTC', 'UTC', true);
                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('canonical','info','2026-01-15T12:34:56.123456789Z','GENERAL',null,null,'2026-01-15T12:34:56.123456789Z');
                assert(lastLog().querySelector('.stl-hud-timestamp').textContent === '[2026-01-15T12:34:56.123456789Z]', 'ISO UTC rendering differs');
                assert(lastLog().querySelector('.stl-hud-event-message').textContent === 'canonical', 'semantic message differs');
                assert(lastLog().attributes['data-category'] === 'SYSTEM' && lastLog().attributes['data-phase'] === 'INFO', 'legacy semantic fallback differs');
                assert(lastLog().attributes['data-test-lens-timestamp'] === '2026-01-15T12:34:56.123456789Z', 'canonical timestamp precision was not retained');
                timestampConfig('TIME_ONLY', 'Europe/Warsaw', true);
                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('winter','info','2026-01-15T22:59:59Z','GENERAL',null,null,'23:59:59');
                assert(lastLog().querySelector('.stl-hud-timestamp').textContent === '[23:59:59]', 'Warsaw winter offset differs');
                window.__uiTestLens.modules.hud.log('summer','info','2026-07-15T21:59:59Z','GENERAL',null,null,'23:59:59');
                assert(lastLog().querySelector('.stl-hud-timestamp').textContent === '[23:59:59]', 'Warsaw summer offset differs');
                timestampConfig('DATE_TIME', 'Europe/Warsaw', true);
                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('midnight','info','2026-07-15T22:00:00Z','GENERAL',null,null,'16.07.26 00:00:00');
                assert(lastLog().querySelector('.stl-hud-timestamp').textContent === '[16.07.26 00:00:00]', 'date rollover differs');
                var categories=['STEP','ACTION','HIGHLIGHT','WAIT','LOCATOR_RETRY','ASSERTION_PASSED','NETWORK_WAIT_STARTED','NETWORK_RESPONSE_RECORDED','AUTH_STATE_CREATED','SCREENSHOT_CAPTURE_PASSED','WARNING','ERROR','HUD'];
                var categoryStart=root.querySelector('#selenium-hud-logs').children.length;
                categories.forEach(function(type,index){window.__uiTestLens.modules.hud.log(type,'info','2026-07-15T22:00:00Z',type,null,null,'16.07.26 00:00:00');});
                assert(root.querySelector('#selenium-hud-logs').children.length-categoryStart === categories.length, 'a visible category was lost or duplicated');
                for(var categoryIndex=0;categoryIndex<categories.length;categoryIndex++) {
                  assert(root.querySelector('#selenium-hud-logs').children[categoryStart+categoryIndex].querySelector('.stl-hud-timestamp').textContent === '[16.07.26 00:00:00]', 'category timestamp missing');
                }
                ['','not-a-date','ui-test-lens',null,undefined].forEach(function(value){
                  window.__uiTestLens.modules.hud.log('fallback','info',value,'GENERAL');
                  var text=lastLog().textContent;
                  assert(/^\\[\\d{4}-\\d{2}-\\d{2}T.*Z\\]$/.test(lastLog().querySelector('.stl-hud-timestamp').textContent), 'invalid timestamp fallback missing');
                  assert(text.indexOf('Invalid Date')<0&&text.indexOf('undefined')<0&&text.indexOf('null')<0&&text.indexOf('ui-test-lens')<0&&text.indexOf('[]')<0, 'invalid timestamp leaked');
                });
                timestampConfig('DATE_TIME', 'Europe/Warsaw', false);
                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('hidden prefix','info',null,'GENERAL');
                assert(!lastLog().querySelector('.stl-hud-timestamp'), 'hidden timestamp was rendered');
                assert(lastLog().querySelector('.stl-hud-event-message').textContent === 'hidden prefix', 'hidden timestamp changed message');
                assert(/^\\d{4}-\\d{2}-\\d{2}T/.test(lastLog().attributes['data-test-lens-timestamp']), 'hidden timestamp was not assigned once');

                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('Click Save','info',null,'LOCATOR_ACTION_STARTED',null,null,null,
                  {category:'ACTION',phase:'RUNNING',operationId:'op-1',technical:false});
                window.__uiTestLens.modules.hud.log('Save clicked','info',null,'LOCATOR_ACTION_PASSED',null,null,null,
                  {category:'ACTION',phase:'PASSED',operationId:'op-1',durationMs:143,technical:false});
                assert(root.querySelector('#selenium-hud-logs').children.length === 1, 'operation created an orphan row');
                assert(lastLog().attributes['data-phase'] === 'PASSED', 'operation row did not become terminal');
                assert(lastLog().querySelector('.stl-hud-event-duration').textContent === '\u00b7 143 ms', 'operation duration missing');

                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.logBatch([
                  {message:'running',level:'info',timestamp:null,eventType:'LOCATOR_ACTION_STARTED',semantics:{category:'ACTION',phase:'RUNNING',operationId:'batch-1',technical:false}},
                  {message:'resolved',level:'debug',timestamp:null,eventType:'LOCATOR_RESOLVE_PASSED',semantics:{category:'LOCATOR',phase:'DEBUG',operationId:'batch-1',technical:false}},
                  {message:'<img src=x onerror=alert(1)>',level:'info',timestamp:null,eventType:'HUD',semantics:{category:'USER',phase:'INFO',operationId:'batch-user',technical:false,customIcon:'\ud83d\udc69\u200d\ud83d\udcbb'}},
                  {message:'passed',level:'info',timestamp:null,eventType:'LOCATOR_ACTION_PASSED',semantics:{category:'ACTION',phase:'PASSED',operationId:'batch-1',technical:false}}
                ]);
                var batchLogs=root.querySelector('#selenium-hud-logs');
                assert(batchLogs.children.length === 3, 'batch order/correlation changed row count');
                assert(batchLogs.children[0].querySelector('.stl-hud-event-message').textContent === 'passed', 'batch did not update the correlated action in order');
                assert(batchLogs.children[1].querySelector('.stl-hud-event-message').textContent === 'resolved', 'batch technical entry order changed');
                assert(batchLogs.children[2].querySelector('.stl-hud-event-message').textContent === '<img src=x onerror=alert(1)>', 'batch user message changed');
                assert(!batchLogs.children[2].querySelector('img'), 'batch rendered malicious message as markup');
                assert(batchLogs.children[2].querySelector('.stl-hud-event-category-icon').textContent === '\ud83d\udc69\u200d\ud83d\udcbb', 'batch changed compound custom icon');

                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('Value should match','info',null,'ASSERTION_STARTED',null,null,null,
                  {category:'ASSERTION',phase:'RUNNING',operationId:'assert-icon',technical:false});
                assert(lastLog().querySelector('.stl-hud-event-category-icon').textContent === '\ud83e\uddea', 'assertion category icon differs');
                assert(lastLog().querySelector('.stl-hud-event-status-icon').textContent === '\u23f3', 'running status icon differs');
                assert(lastLog().querySelector('.stl-hud-event-category').textContent === '[ASSERTION]', 'assertion category text missing');
                assert(lastLog().querySelector('.stl-hud-event-phase').textContent === 'RUNNING', 'running phase text missing');
                window.__uiTestLens.modules.hud.log('Resolved Save','debug',null,'LOCATOR_RESOLVE_PASSED',null,null,null,
                  {category:'LOCATOR',phase:'DEBUG',operationId:'locator-icon',technical:false});
                assert(lastLog().querySelector('.stl-hud-event-category-icon').textContent === '\ud83d\udd0e', 'locator category icon differs');
                assert(lastLog().querySelector('.stl-hud-event-status-icon').textContent === '\u00b7', 'debug status marker differs');
                assert(lastLog().querySelector('.stl-hud-event-category').textContent === '[LOCATOR]', 'locator category text missing');
                assert(lastLog().querySelector('.stl-hud-event-phase').textContent === 'DEBUG', 'debug phase text missing');

                window.__uiTestLens.modules.hud.log('Custom icon','info',null,'HUD',null,null,null,
                  {category:'USER',phase:'PASSED',operationId:'custom-icon',technical:false,customIcon:'\ud83d\udc69\u200d\ud83d\udcbb'});
                assert(lastLog().querySelector('.stl-hud-event-category-icon').textContent === '\ud83d\udc69\u200d\ud83d\udcbb', 'compound custom icon changed');
                assert(lastLog().querySelector('.stl-hud-event-status-icon').textContent === '\u2705', 'custom icon replaced status icon');
                assert(lastLog().querySelector('.stl-hud-event-category').textContent === '[USER]', 'custom icon changed category text');
                assert(lastLog().querySelector('.stl-hud-event-phase').textContent === 'PASSED', 'custom icon changed status text');
                window.__uiTestLens.modules.hud.log('Malicious icon','info',null,'HUD',null,null,null,
                  {category:'USER',phase:'INFO',operationId:'unsafe-icon',technical:false,customIcon:'<img src=x onerror=alert(1)>'});
                assert(lastLog().querySelector('.stl-hud-event-category-icon').textContent === '<img src=x onerror=alert(1)>', 'unsafe icon was not retained as text');
                assert(!lastLog().querySelector('img'), 'unsafe icon created markup');
                window.__uiTestLens.modules.hud.log('Blank icon','info',null,'HUD',null,null,null,
                  {category:'USER',phase:'INFO',operationId:'blank-icon',technical:false,customIcon:'  '});
                assert(lastLog().querySelector('.stl-hud-event-category-icon').textContent === '\ud83d\udcac', 'blank icon did not use USER default');

                window.__uiTestLens.modules.hud.clear();
                window.__uiTestLens.modules.hud.log('<img src=x onerror=alert(1)>','info',null,'HUD',null,null,null,
                  {category:'USER',phase:'INFO',operationId:'user-1',technical:false});
                assert(lastLog().querySelector('.stl-hud-event-message').textContent === '<img src=x onerror=alert(1)>', 'unsafe message was not retained as text');
                assert(!lastLog().querySelector('img'), 'unsafe message created markup');
                window.__uiTestLens.modules.hud.log('😀 🚀 ❤️ 👩‍💻 👨‍👩‍👧‍👦 🇵🇱 👍🏽 1️⃣ é','info',null,'HUD',null,null,null,
                  {category:'<script>alert(1)</script>',phase:'INFO',operationId:'unicode-1',technical:false});
                assert(lastLog().querySelector('.stl-hud-event-message').textContent === '😀 🚀 ❤️ 👩‍💻 👨‍👩‍👧‍👦 🇵🇱 👍🏽 1️⃣ é', 'Unicode message changed in DOM');
                assert(lastLog().attributes['data-category'] === 'SYSTEM', 'unsafe category did not use neutral fallback');
                assert(!lastLog().querySelector('script'), 'unsafe category created markup');

                [{w:1440,h:900},{w:1024,h:768},{w:768,h:700},{w:390,h:844}].forEach(function(viewport) {
                  ['TOP_LEFT', 'TOP_RIGHT', 'BOTTOM_LEFT', 'BOTTOM_RIGHT'].forEach(function(position) {
                    window.innerWidth = viewport.w;
                    window.innerHeight = viewport.h;
                    window.__uiTestLens.modules.hud.init({testName: 'Responsive', pipelineId: 'p', theme: {},
                      hudOptions: {position: position, offsetX: 40, offsetY: 40, width: 620,
                        maxHeight: 1000, maxLogHeight: 720, showTestName: true,
                        showCurrentStep: true, showPipeline: false, showEventLog: true,
                        branding: 'NONE'}});
                    var responsive = root.querySelector('#selenium-hud-panel');
                    var expectedWidth = Math.min(620, viewport.w - 20) + 'px';
                    var expectedX = viewport.w === 390 ? '10px' : '40px';
                    assert(responsive.style.width === expectedWidth, position + ' width was not clamped at ' + viewport.w);
                    assert(responsive.style.maxHeight === 'var(--ui-test-lens-hud-max-height)', position + ' max height missing');
                    assert(responsive.style['--ui-test-lens-hud-max-height'] === Math.min(1000, viewport.h - 20) + 'px', position + ' height was not clamped');
                    if (position.indexOf('LEFT') >= 0) assert(responsive.style.left === expectedX, position + ' X offset was not clamped');
                    else assert(responsive.style.right === expectedX, position + ' X offset was not clamped');
                    if (position.indexOf('TOP') === 0) assert(responsive.style.top === '40px', position + ' Y anchor missing');
                    else assert(responsive.style.bottom === '40px', position + ' Y anchor missing');
                  });
                });

                window.innerWidth = 1024; window.innerHeight = 768;
                window.__uiTestLens.modules.hud.init({testName:'Sized', theme:{}, hudOptions:{
                  maxHeight:280,maxLogHeight:180,showTestName:true,showCurrentStep:true,
                  showPipeline:false,showEventLog:true,branding:'NONE'}});
                assert(root.querySelector('#selenium-hud-logs').style.maxHeight === '180px', 'configured log limit was ignored');
                window.__uiTestLens.modules.hud.init({testName:'Short', theme:{}, hudOptions:{
                  maxHeight:120,maxLogHeight:720,showTestName:true,showCurrentStep:true,
                  showPipeline:false,showEventLog:true,branding:'NONE'}});
                assert(parseFloat(root.querySelector('#selenium-hud-logs').style.maxHeight) < 720, 'panel content limit was ignored');
                window.__uiTestLens.modules.hud.init({testName:'Log only',theme:{},hudOptions:{
                  maxLogHeight:100,showTestName:true,showCurrentStep:true,showEventLog:true,branding:'NONE'}});
                assert(root.querySelector('#selenium-hud-logs').style.maxHeight === '100px', 'log-only limit was ignored');
                window.__uiTestLens.modules.hud.init({testName:'Panel only',theme:{},hudOptions:{
                  maxHeight:280,showTestName:true,showCurrentStep:true,showEventLog:true,branding:'NONE'}});
                assert(parseFloat(root.querySelector('#selenium-hud-logs').style.maxHeight) <= 160, 'default log limit was ignored');
                window.__uiTestLens.modules.hud.init({testName:'Native',theme:{},hudOptions:{
                  showEventLog:true,branding:'NONE',scrollbarStyle:'NATIVE'}});
                assert(root.querySelector('#selenium-hud-logs').className.indexOf('stl-hud-custom-scrollbar') < 0, 'native scrollbar retained custom styling');
                """;
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                escaped.append('\\').append(c);
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c == '\r') {
                escaped.append("\\r");
            } else if (c == '\t') {
                escaped.append("\\t");
            } else {
                escaped.append(c);
            }
        }
        return escaped.append('"').toString();
    }
}

