import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'docs/javascripts/ai-integration-builder.js'), 'utf8');
const page = fs.readFileSync(path.join(root, 'docs/ai-assisted-integration.md'), 'utf8');
const context = { globalThis: {} };
vm.runInNewContext(source, context, { filename: 'ai-integration-builder.js' });
const builder = context.globalThis.TestLensAiIntegrationBuilder;

assert.ok(builder, 'builder API must be exported');
assert.equal(builder.VERSION, '0.4.0');
assert.deepEqual(Array.from(builder.BIDI_MINIMUM), [4, 39, 0]);

const mandatory = [
  'Mandatory project discovery before changes',
  'effective/resolved Selenium Java version',
  'Non-negotiable lifecycle invariants',
  'Preserve CI and project execution',
  'Required validation',
  'Final report',
  'finishPassed()',
  'driver.quit()',
  'Never hard-code secrets',
  'Do not claim a command, browser, provider or workflow passed unless it was actually executed'
];

for (const [preset, capabilities] of Object.entries(builder.PRESETS)) {
  const prompt = builder.buildPrompt({ style: 'auto', capabilities, docsBase: 'https://docs.example/0.4.0/' });
  for (const contract of mandatory) assert.match(prompt, new RegExp(contract.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `${preset}: ${contract}`);
  assert.match(prompt, /Lifecycle model: inspect and choose/);
  assert.match(prompt, /https:\/\/docs\.example\/0\.4\.0\/getting-started\//);
}

const featureContracts = {
  nativeObservation: ['Native Selenium observation', 'observeDriver()', 'exactly once'],
  uiLocator: ['Lens-native interactions', 'UiLocator.click()', 'NATIVE -> ACTIONS -> POINT'],
  hud: ['HUD and live diagnostics', 'MINIMAL, COMPACT (default), STANDARD and DEBUG', 'ObservabilityMode.DEFAULT and FAST'],
  reportsEvidence: ['Reports and evidence', 'evidence bundle/ZIP', 'Do not describe Test Lens as a video recorder'],
  reportUpload: ['Explicit report upload', 'finish -> synchronous HTTP upload -> quit', 'never sends anything automatically'],
  allure: ['Allure coexistence', 'selenium-test-lens-allure:0.4.0', 'does not replace Allure'],
  bidiNetwork: ['WebDriver BiDi network diagnostics', '4.39.0 or newer', 'not interception, blocking, mocking'],
  visualRedaction: ['Visual redaction', 'VisualRedactionOptions', 'STRICT'],
  managedAuth: ['Managed Auth State', 'AuthStateRequest', 'same-origin'],
  managedTestState: ['Managed Test State & Resources', 'scenarioState()', 'exactly-once LIFO'],
  reactSpa: ['React & SPA resilience', 'selenium-test-lens-react:0.4.0', 'Do not add it merely because'],
  applicationWaits: ['Application-aware waits', 'waitForNetworkIdle', 'not browser-wide network idle']
};

for (const feature of builder.FEATURE_ORDER) {
  const prompt = builder.buildPrompt({ style: 'manual', capabilities: [feature], docsBase: 'https://docs.example/0.4.0/' });
  for (const contract of featureContracts[feature]) assert.ok(prompt.includes(contract), `${feature}: ${contract}`);
}

const reportUpload = Array.from(builder.normalizedCapabilities(['reportUpload']));
assert.deepEqual(reportUpload, ['reportsEvidence', 'reportUpload']);
const allure = Array.from(builder.normalizedCapabilities(['allure']));
assert.deepEqual(allure, ['reportsEvidence', 'allure']);

const below = builder.seleniumAssessment('4.38.2', true);
assert.equal(below.kind, 'blocked');
assert.equal(below.message, 'Test Lens BiDi/network diagnostics require Selenium Java 4.39.0 or newer. This project currently resolves Selenium 4.38.2, so Test Lens BiDi-based functionality cannot be enabled with the current dependency set.');
assert.equal(builder.seleniumAssessment('4.39.0', true).kind, 'compatible-version');
assert.equal(builder.seleniumAssessment('4.40.1', true).kind, 'compatible-version');
assert.equal(builder.seleniumAssessment('4.38.2', false).kind, 'inactive');

const blockedPrompt = builder.buildPrompt({ style: 'manual', capabilities: ['bidiNetwork'], seleniumVersion: '4.38.2' });
assert.ok(blockedPrompt.includes(below.message));
assert.match(blockedPrompt, /integrate core Lens without BiDi/);
const compatiblePrompt = builder.buildPrompt({ style: 'manual', capabilities: ['bidiNetwork'], seleniumVersion: '4.39.0' });
assert.match(compatiblePrompt, /Browser, session, transport and provider compatibility still require runtime verification/);
const newerPrompt = builder.buildPrompt({ style: 'manual', capabilities: ['bidiNetwork'], seleniumVersion: '4.41.0' });
assert.match(newerPrompt, /Browser, session, transport and provider compatibility still require runtime verification/);
const noBidiPrompt = builder.buildPrompt({ style: 'manual', capabilities: [], seleniumVersion: '4.38.2' });
assert.doesNotMatch(noBidiPrompt, /WebDriver BiDi network diagnostics and mandatory compatibility gate/);

for (const [style, heading] of Object.entries({ auto: 'inspect and choose', manual: 'existing/custom WebDriver', junit5: 'JUnit 5 adapter', testng: 'TestNG adapter' })) {
  assert.ok(builder.buildPrompt({ style, capabilities: [] }).includes(`Lifecycle model: ${heading}`));
}

const allPrompt = builder.buildPrompt({ style: 'testng', capabilities: builder.FEATURE_ORDER, docsBase: 'https://docs.example/latest/' });
for (const contracts of Object.values(featureContracts)) assert.ok(allPrompt.includes(contracts[0]));
assert.doesNotMatch(allPrompt, /selenium-test-lens-selector-(?:engine|tooling|live|lab)/);
assert.doesNotMatch(allPrompt, /selenium-test-lens-(?:compatibility|migration)-/);
assert.doesNotMatch(allPrompt, /CDP fallback|network mocking|record video/);

assert.equal((page.match(/data-copy-prompt/g) || []).length, 1);
assert.equal((page.match(/data-generated-prompt/g) || []).length, 1);
assert.equal((page.match(/data-feature=/g) || []).length, builder.FEATURE_ORDER.length);
assert.doesNotMatch(page, /name="(?:detail|prompt-level|verbosity)"/i);
assert.doesNotMatch(page, />\s*(?:Short prompt|Basic prompt|Advanced prompt)\s*</i);

console.log('AI Integration Builder behavior OK: presets, modules, dependencies, Selenium boundary and full-prompt contract.');
