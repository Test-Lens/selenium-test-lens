package io.github.testlens.core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        assertTrue(HudPanelJs.INIT.contains("updateScrollableRegions"));
    }

    @Test
    void initContainsMinimalBrandingShell() {
        assertTrue(HudPanelJs.INIT.contains("stl-hud-shell"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-header"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-brand-icon"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-side-rail"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-side-rail-text"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-main"));
        assertTrue(HudPanelJs.INIT.contains("TEST LENS"));
        assertTrue(HudPanelJs.INIT.contains("<svg class=\"stl-hud-brand-icon-svg\""));
        assertFalse(HudPanelJs.INIT.contains("Selenium/WebDriver"));
        assertFalse(HudPanelJs.INIT.contains("Test Lens"));
    }

    @Test
    void initDoesNotShortCircuitWhenHudModuleAlreadyExists() {
        assertFalse(HudPanelJs.INIT.contains("lens.modules.hud && lens.modules.hud.__uiTestLensHud === true"));
        assertTrue(HudPanelJs.INIT.contains("lens.modules.hud = {"));
    }

    @Test
    void initContainsLegacyHudContentMigration() {
        assertTrue(HudPanelJs.INIT.contains("function migrateHudContent"));
        assertTrue(HudPanelJs.INIT.contains("placeAfter(structure.main, title, previous)"));
        assertTrue(HudPanelJs.INIT.contains("placeAfter(structure.main, pipeline, previous)"));
        assertTrue(HudPanelJs.INIT.contains("placeAfter(structure.main, step, previous)"));
        assertTrue(HudPanelJs.INIT.contains("structure.main.appendChild(logs)"));
    }

    @Test
    void initUpgradesExistingHudModuleAndRendersBrandingWhenNodeIsAvailable() throws Exception {
        Process process;
        try {
            process = new ProcessBuilder("node", "-e", hudRuntimeSmokeScript()).start();
        } catch (IOException ex) {
            Assumptions.abort("Node.js is not available for HUD runtime smoke test");
            return;
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
                  this.textContent = '';
                  this.innerHTML = '';
                  this.offsetHeight = 20;
                  this.offsetTop = 0;
                }
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
                var document = { createElement: function(tag) { return new Element(tag); } };
                var runtime =\s""" + jsonString(HudPanelJs.INIT) + """
                ;

                eval(runtime);
                window.__uiTestLens.modules.hud.init({
                  testName: 'Checkout',
                  pipelineId: 'local',
                  maxWidth: 320,
                  theme: { maxHeightPx: 140 },
                  themeName: 'GLASS'
                });
                window.__uiTestLens.modules.hud.setStep('Pay');
                window.__uiTestLens.modules.hud.log('Saved', 'info', 'now');
                window.__uiTestLens.modules.hud.init({
                  testName: 'Checkout',
                  pipelineId: 'local',
                  maxWidth: 320,
                  theme: { maxHeightPx: 140 },
                  themeName: 'GLASS'
                });

                assert(root.querySelector('.stl-hud-shell'), 'missing shell');
                assert(root.querySelector('.stl-hud-side-rail'), 'missing side rail');
                assert(root.querySelector('.stl-hud-main'), 'missing main');
                assert(root.querySelector('.stl-hud-brand-icon'), 'missing brand icon');
                assert(root.querySelector('.stl-hud-side-rail-text').textContent === 'TEST LENS', 'missing rail text');
                assert(root.querySelector('#selenium-hud-step').parentNode === root.querySelector('.stl-hud-main'), 'legacy step was not migrated');
                assert(root.querySelector('#selenium-hud-logs').parentNode === root.querySelector('.stl-hud-main'), 'logs are not in main');
                assert(countByClass(root, 'stl-hud-side-rail') === 1, 'duplicated side rail');
                assert(countByClass(root, 'stl-hud-brand-icon') === 1, 'duplicated brand icon');
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

