import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const source = fs.readFileSync(path.join(root, 'docs/javascripts/ai-integration-builder.js'), 'utf8');
const page = fs.readFileSync(path.join(root, 'docs/ai-assisted-integration.md'), 'utf8');
const context = { globalThis: {}, URL };
vm.runInNewContext(source, context, { filename: 'ai-integration-builder.js' });
const builder = context.globalThis.TestLensAiIntegrationBuilder;

assert.ok(builder, 'builder API must be exported');
assert.equal(builder.VERSION_TOKEN, '{{TEST_LENS_TARGET_RELEASE}}');
assert.deepEqual(Array.from(builder.BIDI_MINIMUM), [4, 39, 0]);

const release = {
  targetRelease: '0.5.0',
  publicDocsRoot: 'https://test-lens.github.io/selenium-test-lens/',
  sourceKind: 'release'
};
const promptFor = options => builder.buildPrompt({ ...release, style: 'manual', capabilities: [], ...options });

const latestTarget = builder.resolveDocumentationTarget({ ...release, sourceRevision: 'abc123' }, 'https://test-lens.github.io/selenium-test-lens/latest/ai-assisted-integration/');
assert.equal(latestTarget.valid, true);
assert.equal(latestTarget.docsBase, 'https://test-lens.github.io/selenium-test-lens/0.5.0/');
const numericTarget = builder.resolveDocumentationTarget({ ...release, sourceRevision: 'abc123' }, 'https://test-lens.github.io/selenium-test-lens/0.5.0/ai-assisted-integration/');
assert.equal(numericTarget.valid, true);
const devTarget = builder.resolveDocumentationTarget({ ...release, sourceKind: 'development' }, 'http://127.0.0.1:8000/selenium-test-lens/dev/ai-assisted-integration/');
assert.equal(devTarget.valid, true);
assert.equal(devTarget.docsBase, 'https://test-lens.github.io/selenium-test-lens/0.5.0/');
assert.equal(builder.resolveDocumentationTarget({ ...release, targetRelease: '0.5.1' }, 'https://test-lens.github.io/selenium-test-lens/0.5.0/ai-assisted-integration/').valid, false);
assert.equal(builder.resolveDocumentationTarget({ ...release, targetRelease: '' }, 'https://test-lens.github.io/selenium-test-lens/latest/').valid, false);
assert.equal(builder.resolveDocumentationTarget({ ...release, publicDocsRoot: 'http://localhost:8000/' }, 'http://localhost:8000/').valid, false);

const pinnedPrompt = promptFor({ capabilities: ['hud'] });
assert.match(pinnedPrompt, /selenium-test-lens:0\.5\.0/);
assert.match(pinnedPrompt, /https:\/\/test-lens\.github\.io\/selenium-test-lens\/0\.5\.0\//);
assert.doesNotMatch(pinnedPrompt, /selenium-test-lens\/(?:latest|dev)\//);
assert.doesNotMatch(pinnedPrompt, /localhost|127\.0\.0\.1|file:\/\//);
const repeatedPrompt = builder.buildPrompt({ ...release, style: 'manual', capabilities: ['hud'] });
assert.equal(repeatedPrompt, pinnedPrompt, 'release prompt remains deterministically pinned');

assert.equal(builder.seleniumAssessment('', true).kind, 'unknown');
assert.equal(builder.seleniumAssessment('not-a-version', true).kind, 'invalid');
assert.equal(builder.seleniumAssessment('4.38.0', true).kind, 'blocked');
assert.equal(builder.seleniumAssessment('4.39.0', true).kind, 'compatible-version');
assert.equal(builder.seleniumAssessment('4.100.0', true).kind, 'compatible-version');
assert.equal(builder.seleniumAssessment('4.9.0', true).kind, 'blocked');
for (const version of ['4.39.0-rc1', '4.39.0-beta-1', '4.39.0-SNAPSHOT', '4.40.0-alpha.2']) {
  const assessment = builder.seleniumAssessment(version, true);
  assert.equal(assessment.kind, 'prerelease-review', version);
  assert.match(assessment.message, /prerelease|SNAPSHOT/i);
}
assert.equal(builder.seleniumAssessment('4.39.0', false).kind, 'hint-only');
assert.match(builder.seleniumAssessment('4.38.0', true).message, /version entered/i);
assert.doesNotMatch(builder.seleniumAssessment('4.38.0', true).message, /currently resolves/i);
const invalidHintPrompt = promptFor({ capabilities: ['bidiNetwork'], seleniumVersion: '<script>bad</script>' });
assert.doesNotMatch(invalidHintPrompt, /<script>bad<\/script>/);
assert.match(invalidHintPrompt, /has not been copied into this prompt as technical evidence/);

assert.deepEqual(Array.from(builder.resolveCapabilities([]).effective), []);
const uploadOnly = builder.resolveCapabilities(['reportUpload']);
assert.deepEqual(Array.from(uploadOnly.explicit), ['reportUpload']);
assert.deepEqual(Array.from(uploadOnly.required), ['reportsEvidence']);
const explicitReportsUpload = builder.resolveCapabilities(['reportsEvidence', 'reportUpload']);
assert.deepEqual(Array.from(explicitReportsUpload.required), []);
const allureOnly = builder.resolveCapabilities(['allure']);
assert.deepEqual(Array.from(allureOnly.required), ['reportsEvidence']);

const headings = {
  nativeObservation: '## Native Selenium observation',
  uiLocator: '## Lens-native interactions',
  hud: '## HUD and live diagnostics',
  reportsEvidence: '## Reports and evidence',
  reportUpload: '## Explicit report upload',
  allure: '## Allure coexistence',
  bidiNetwork: '## WebDriver BiDi network diagnostics',
  visualRedaction: '## Visual redaction',
  managedAuth: '## Managed Auth State',
  managedTestState: '## Managed Test State & Resources',
  reactSpa: '## React & SPA resilience',
  applicationWaits: '## Application-aware waits'
};

const coreOnly = promptFor({ capabilities: [] });
assert.match(coreOnly, /Explicitly selected capabilities: none; core lifecycle only/);
for (const heading of Object.values(headings)) assert.ok(!coreOnly.includes(heading));

for (const feature of builder.FEATURE_ORDER) {
  const prompt = promptFor({ capabilities: [feature] });
  assert.ok(prompt.includes(headings[feature]), feature);
  for (const [other, heading] of Object.entries(headings)) {
    const requiredReport = (feature === 'reportUpload' || feature === 'allure') && other === 'reportsEvidence';
    if (other !== feature && !requiredReport) assert.ok(!prompt.includes(heading), `${feature} must not implement ${other}`);
  }
}

const waitsOnly = promptFor({ capabilities: ['applicationWaits'] });
assert.match(waitsOnly, /BiDi diagnostics are a separate option/);
assert.doesNotMatch(waitsOnly, /## WebDriver BiDi network diagnostics/);
assert.doesNotMatch(waitsOnly, /enableBiDi\(\)|webSocketUrl/);
const uiOnly = promptFor({ capabilities: ['uiLocator'] });
assert.doesNotMatch(uiOnly, /## Native Selenium observation/);
const observeOnly = promptFor({ capabilities: ['nativeObservation'] });
assert.doesNotMatch(observeOnly, /## Lens-native interactions/);
const reactOnly = promptFor({ capabilities: ['reactSpa'] });
assert.doesNotMatch(reactOnly, /## Lens-native interactions/);

for (const style of ['manual', 'junit5', 'testng']) {
  const composed = promptFor({ style, capabilities: ['reportUpload', 'allure'] });
  assert.match(composed, /## Finalization, publication and cleanup composition/);
  assert.match(composed, /same finalized result|same result/i);
  assert.match(composed, /endpoint|configuration/);
  if (style !== 'manual') assert.match(composed, /adapter normally owns terminal mapping/);
}
const testNgObserved = promptFor({ style: 'testng', capabilities: ['nativeObservation'] });
assert.match(testNgObserved, /PER_CLASS/);
assert.match(testNgObserved, /never retain an observed facade from a finalized invocation/);

const mandatory = ['Mandatory project discovery before changes', 'test-runtime classpath', 'Non-negotiable lifecycle invariants', 'Preserve CI and project execution', 'Required validation', 'Final report'];
for (const [preset, capabilities] of Object.entries(builder.PRESETS)) {
  const prompt = promptFor({ style: 'auto', capabilities });
  for (const contract of mandatory) assert.ok(prompt.includes(contract), `${preset}: ${contract}`);
}

assert.equal((page.match(/data-copy-prompt/g) || []).length, 1);
assert.equal((page.match(/data-generated-prompt/g) || []).length, 1);
assert.equal((page.match(/data-feature=/g) || []).length, builder.FEATURE_ORDER.length);
assert.doesNotMatch(page, /data-test-lens-version|data-docs-base/);
assert.doesNotMatch(page, /name="(?:detail|prompt-level|verbosity)"/i);
assert.doesNotMatch(page, />\s*(?:Short prompt|Basic prompt|Advanced prompt)\s*</i);

console.log('AI Integration Builder behavior OK: trusted release binding, Selenium qualifiers, explicit scope, dependencies and lifecycle composition.');
