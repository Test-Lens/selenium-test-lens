const assert = require('node:assert/strict');
const { chromium } = require('playwright');

const rootUrl = (process.env.AI_BUILDER_BASE_URL || 'http://127.0.0.1:8000/').replace(/\/?$/, '/');
const expectedRevision = process.env.AI_BUILDER_EXPECTED_REVISION || '';
const expectedSourceKind = process.env.AI_BUILDER_EXPECTED_SOURCE_KIND || '';

(async () => {
  const browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } });
  await context.grantPermissions(['clipboard-read', 'clipboard-write']);
  const page = await context.newPage();
  const pageErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));

  await page.goto(rootUrl, { waitUntil: 'domcontentloaded' });
  const aiLink = page.locator('.md-content a[href*="ai-assisted-integration"]').first();
  assert.ok(await aiLink.count(), 'navigation must link to AI-assisted integration');
  await aiLink.click();
  await page.waitForLoadState('domcontentloaded');
  assert.match(page.url(), /ai-assisted-integration/);
  await page.goBack({ waitUntil: 'domcontentloaded' });
  await page.goForward({ waitUntil: 'domcontentloaded' });

  const builder = page.locator('[data-ai-integration-builder]');
  assert.equal(await builder.getAttribute('data-ai-builder-initialized'), 'true');
  const targetStatus = await page.locator('[data-target-status]').innerText();
  assert.match(targetStatus, /integration target Test Lens 0\.4\.0/);
  if (expectedRevision) assert.match(targetStatus, new RegExp(`Source revision: ${expectedRevision.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`));
  if (expectedSourceKind === 'development') assert.match(targetStatus, /^Development preview/);
  if (expectedSourceKind === 'release') assert.match(targetStatus, /^Versioned documentation/);
  const prompt = page.locator('[data-generated-prompt]');
  assert.match(await prompt.inputValue(), /selenium-test-lens:0\.4\.0/);
  assert.match(await prompt.inputValue(), /selenium-test-lens\/0\.4\.0\//);
  assert.doesNotMatch(await prompt.inputValue(), /localhost|127\.0\.0\.1|file:\/\//);

  const preset = name => page.locator(`[data-preset="${name}"]`);
  assert.equal(await preset('minimal').getAttribute('aria-pressed'), 'true');
  await preset('recommended').click();
  assert.equal(await preset('recommended').getAttribute('aria-pressed'), 'true');
  const hud = page.locator('[data-feature="hud"]');
  await hud.uncheck();
  assert.equal(await page.locator('[data-selection-mode]').innerText(), 'Custom selection');
  assert.equal(await preset('recommended').getAttribute('aria-pressed'), 'false');
  await hud.check();
  assert.match(await page.locator('[data-selection-mode]').innerText(), /Recommended observability/);
  assert.equal(await preset('recommended').getAttribute('aria-pressed'), 'true');

  await preset('minimal').click();
  const reports = page.locator('[data-feature="reportsEvidence"]');
  const upload = page.locator('[data-feature="reportUpload"]');
  await upload.check();
  assert.equal(await reports.isChecked(), true);
  assert.equal(await reports.isDisabled(), true);
  assert.match(await page.locator('[data-selection-summary]').innerText(), /required dependency/);
  await upload.uncheck();
  assert.equal(await reports.isChecked(), false);
  assert.equal(await reports.isDisabled(), false);
  await reports.check();
  await upload.check();
  assert.equal(await reports.isDisabled(), true);
  await upload.uncheck();
  assert.equal(await reports.isChecked(), true, 'explicit reports selection must survive removal of its former parent');

  const bidi = page.locator('[data-feature="bidiNetwork"]');
  const version = page.locator('[data-selenium-version]');
  await bidi.check();
  for (const [value, expected] of [
    ['4.38.0', /below the supported/],
    ['4.39.0', /meets the numeric/],
    ['4.39.0-rc1', /prerelease or SNAPSHOT/],
    ['4.39.0-SNAPSHOT', /prerelease or SNAPSHOT/],
    ['bad value with spaces', /not a supported Selenium version format/]
  ]) {
    await version.fill(value);
    assert.match(await page.locator('[data-selenium-warning]').innerText(), expected);
  }
  assert.doesNotMatch(await prompt.inputValue(), /bad value with spaces/);

  const expectedClipboard = await prompt.inputValue();
  await page.locator('[data-copy-prompt]').click();
  assert.equal((await page.evaluate(() => navigator.clipboard.readText())).replace(/\r\n/g, '\n'), expectedClipboard.replace(/\r\n/g, '\n'));
  assert.equal(await page.evaluate(() => document.activeElement.matches('[data-copy-prompt]')), true);

  await page.evaluate(() => {
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: { writeText: () => Promise.reject(new Error('denied')) } });
    document.execCommand = () => false;
  });
  await page.locator('[data-copy-prompt]').click();
  assert.equal(await page.locator('[data-copy-status]').innerText(), 'Select the prompt and copy it manually.');
  assert.equal(await page.evaluate(() => document.activeElement.matches('[data-copy-prompt]')), true);

  await page.emulateMedia({ colorScheme: 'dark' });
  await page.setViewportSize({ width: 390, height: 844 });
  await version.fill('4.39.0-SNAPSHOT-with-a-deliberately-long-qualifier-for-responsive-validation');
  const layout = await page.evaluate(() => ({
    documentWidth: document.documentElement.scrollWidth,
    viewportWidth: document.documentElement.clientWidth,
    builderWidth: document.querySelector('[data-ai-integration-builder]').getBoundingClientRect().width,
    promptWidth: document.querySelector('[data-generated-prompt]').getBoundingClientRect().width
  }));
  assert.equal(layout.documentWidth, layout.viewportWidth, 'mobile page must not overflow horizontally');
  assert.ok(layout.promptWidth <= layout.builderWidth);
  await page.emulateMedia({ colorScheme: 'light' });
  assert.deepEqual(pageErrors, []);

  console.log(JSON.stringify({ status: 'PASS', rootUrl, expectedRevision, expectedSourceKind, layout }));
  await browser.close();
})().catch(error => { console.error(error); process.exit(1); });
