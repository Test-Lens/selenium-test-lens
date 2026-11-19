import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const web = path.join(root, "selenium-test-lens-test-engineering-studio", "src", "main", "resources", "io", "github", "testlens", "studio", "web");
const scriptPath = path.join(web, "studio.js");
const html = fs.readFileSync(path.join(web, "index.html"), "utf8");
const css = fs.readFileSync(path.join(web, "studio.css"), "utf8");
const source = fs.readFileSync(scriptPath, "utf8");
const require = createRequire(import.meta.url);
const studioModule = require(scriptPath);

class MiniNode {
  constructor(tag = "div", owner = null) {
    this.tagName = tag.toUpperCase(); this.ownerDocument = owner; this.children = []; this.dataset = {};
    this.attributes = {}; this.className = ""; this.hidden = false; this.disabled = false; this.parentElement = null;
    this._text = ""; this.value = ""; this.id = ""; this.content = "";
  }
  set textContent(value) { this._text = String(value ?? ""); this.children = []; }
  get textContent() { return this._text + this.children.map(child => child.textContent || "").join(""); }
  get childNodes() { return this.children; }
  get lastChild() { return this.children[this.children.length - 1] || null; }
  appendChild(child) { if (!child) return child; if (child.tagName === "#FRAGMENT") { [...child.children].forEach(item => this.appendChild(item)); return child; } this.children.push(child); child.parentElement = this; if (child.id) this.ownerDocument?.ids.set(child.id, child); return child; }
  replaceChildren(...children) { this.children = []; this._text = ""; children.forEach(child => this.appendChild(child)); }
  setAttribute(name, value) { this.attributes[name] = String(value); if (name === "id") { this.id = String(value); this.ownerDocument?.ids.set(this.id, this); } }
  querySelector(selector) { return walk(this).find(item => selector === "button" ? item.tagName === "BUTTON" : false) || null; }
  closest(selector) { if (selector === "nav") { let item = this; while (item) { if (item.tagName === "NAV") return item; item = item.parentElement; } } return this; }
  focus() { this.ownerDocument.activeElement = this; }
}
function walk(node) { return [node, ...node.children.flatMap(walk)]; }
class MiniDocument {
  constructor() {
    this.ids = new Map(); this.listeners = new Map(); this.activeElement = null; this.readyState = "loading";
    this.documentElement = new MiniNode("html", this); this.documentElement.dataset.sessionToken = "session-from-bootstrap";
    this.body = new MiniNode("body", this); this.body.dataset = {}; this.meta = new MiniNode("meta", this); this.meta.content = "session-token";
    this.navigation = new MiniNode("nav", this);
    ["overview", "application", "page-objects", "problems", "requirements", "plans", "runs", "repairs", "history"].forEach(view => {
      const button = new MiniNode("button", this); button.dataset.view = view; button.textContent = view; this.navigation.appendChild(button);
    });
    this.body.appendChild(this.navigation);
    ["view-title", "project-label", "problem-count", "primary-view", "details-panel", "operation-status", "error-banner", "workflow-banner", "studio-content", "connection-status"].forEach(id => {
      const item = new MiniNode(id === "studio-content" ? "main" : "div", this); item.id = id; this.ids.set(id, item);
      if (id === "connection-status") { const parent = new MiniNode("footer", this); parent.appendChild(item); }
    });
  }
  createElement(tag) { return new MiniNode(tag, this); }
  createDocumentFragment() { return new MiniNode("#fragment", this); }
  getElementById(id) { return this.ids.get(id) || null; }
  querySelector(selector) { if (selector === 'meta[name="test-lens-session"]') return this.meta; if (selector === '[data-filter="text"]') return null; return null; }
  querySelectorAll(selector) {
    const values = selector === "[data-view]" ? this.navigation.children : [];
    const collection = { length: values.length, item(index) { return values[index] || null; } };
    values.forEach((value, index) => { collection[index] = value; });
    return collection;
  }
  addEventListener(type, listener) { if (!this.listeners.has(type)) this.listeners.set(type, []); this.listeners.get(type).push(listener); }
  dispatch(type, target, extra = {}) { for (const listener of this.listeners.get(type) || []) listener({ target, key: extra.key, preventDefault() {} }); }
}

const project = {
  stage: { status: "NEEDS_REVIEW", evidence: [{ code: "SOURCE_FINGERPRINT", detail: "verified" }], limitations: [], artifacts: [{ id: "application-model", label: "ApplicationModel", freshness: "FRESH" }], actions: [{ id: "scan-project", label: "Scan project", enabled: true }] },
  source: { files: 8, pageObjects: 4, components: 1, tests: 2, locatorDeclarations: 7, usageEdges: 12 },
  application: { pages: 3, states: 5, elements: 19, transitions: 2, sharedComponents: 1 },
  correlation: { exact: 12, strong: 4, probable: 1, ambiguous: 2, noMatch: 0, conflict: 0 },
  problemCount: 2,
  freshness: { ApplicationModel: "FRESH", PageObjectIndex: "FRESH", UsageGraph: "FRESH", Correlations: "FRESH" }
};
const application = { stage: { status: "PARTIAL", evidence: [{ code: "OBSERVATIONS", detail: "1" }], limitations: [{ code: "CLOSED_SHADOW_ROOT", detail: "Closed shadow root could not be inspected" }], artifacts: [{ id: "application-model", label: "ApplicationModel", freshness: "FRESH" }], actions: [] }, counts: project.application, pages: [] };
const config = { projectId: "project-1", name: "Example checkout", root: "C:/work/checkout", build: "MAVEN", status: "READY", configurationSource: "PROJECT_CONFIG", mainSourceRoots: ["src/main/java"], testSourceRoots: ["src/test/java"], workspace: ".test-lens", browser: "CHROME", headless: true, evidence: ["loaded .test-lens/project.json schema 1"], limitations: [] };
const hostStatus = { operationRunning: false };
const problems = { stage: { status: "NEEDS_REVIEW" }, offset: 0, limit: 50, total: 2, problems: [{ id: "p1", category: "AMBIGUOUS_CORRELATION", severity: "WARNING", status: "OPEN", location: "LoginPage.java:42", evidence: ["two candidates"], affectedArtifacts: ["LoginPage"], suggestedAction: "Review correlation" }] };
const correlations = { stage: { status: "NEEDS_REVIEW" }, offset: 0, limit: 50, total: 19, counts: project.correlation, items: [{ applicationElementId: "LOGIN_SUBMIT", sourceElementId: "LoginPage.loginButton", state: "EXACT", evidence: ["CANONICAL_SELECTOR_MATCH", "SAME_PAGE_IDENTITY"] }] };
const workflow = {
  runId: "run-1", status: "NEEDS_REVIEW", requirement: "Invalid password should display an error message",
  plan: { stage: { status: "PASS", evidence: ["Architect result validated"] }, scenarios: [{ id: "invalid-password", title: "Invalid password", priority: "HIGH", preconditions: ["Login page available"], steps: ["Enter username", "Enter invalid password", "Submit login"], expected: ["Error message visible"], existingCoverage: "PARTIAL", existingTests: ["LoginTest"] }] },
  implementation: { stage: { status: "PASS" }, scenarioId: "invalid-password", sourcePatch: "+ assertThat(login.error()).isVisible();", selectorAccessPolicy: "PAGE_OBJECTS_ONLY", policyResults: ["No raw selectors"] },
  execution: { stage: { status: "FAILED" }, compilation: "PASS", execution: "FAIL", durationMillis: 1840, failureSummary: "NoSuchElementException", trace: ["entered username", "click loginButton"] },
  review: { verdict: "REQUEST_CHANGES", reviewedArtifactRefs: ["implementation-1", "quality-1", "compile-success-1", "execution-1"], findings: [
    { category: "ASSERTION_INSUFFICIENT", severity: "ERROR", ruleId: "ASSERTION_REQUIRED", artifactRef: "implementation-1", problemCategory: "ASSERTION", requiredAction: "ADD_ASSERTION", location: "ASSERTION", evidenceAvailability: "VERIFIED" }
  ] },
  reviewHistory: [{ implementationVersion: 1, implementationArtifactRef: "implementation-1", executionArtifactRef: "execution-1", reviewArtifactRef: "review-1", compileOutcome: "PASS", executionOutcome: "PASS", durationMillis: 2361, review: { verdict: "REQUEST_CHANGES" } }],
  diagnosis: { stage: { status: "PASS", evidence: ["old selector resolves 0 elements"] }, category: "SELECTOR_INSTABILITY" },
  repair: { stage: { status: "NEEDS_REVIEW", evidence: ["SAME_TARGET", "UNIQUE"] }, proposalId: "repair-1", logicalPath: "LoginPage.java", oldSelector: "By.id(\"old\")", newSelector: "By.cssSelector(\"[data-testid='login-submit']\")", selectorEvidence: ["VERIFIED_IN_SCOPE", "SAME_TARGET", "UNIQUE", "STABLE"], affectedMethods: ["login"], affectedTests: ["LoginTest"] },
  timeline: [{ event: "Requirement created", status: "PASS" }, { event: "Repair proposed", status: "NEEDS_REVIEW" }]
};
const workflowHistory = [
  { runId: "run-1", requirement: workflow.requirement, state: "REPAIR_READY_FOR_REVIEW", freshness: "FRESH", updatedAt: "2026-10-07T10:00:00Z", resumed: false, availableActions: ["APPROVE_REPAIR", "REJECT_REPAIR"] },
  { runId: "run-2", requirement: "Checkout succeeds", state: "SUCCESS", freshness: "FRESH", updatedAt: "2026-10-06T09:00:00Z", resumed: true, availableActions: [] }
];

function response(body, status = 200) { return { ok: status >= 200 && status < 300, status, async json() { return body; } }; }
function fixtureFetch(calls, selectedWorkflow = workflow) {
  return async (url, options = {}) => {
    calls.push({ url, options });
    if (url === "/api/actions") return response({ ok: true, project, workflow });
    if (url === "/api/project") return response(project);
    if (url === "/api/config") return response(config);
    if (url === "/api/status") return response(hostStatus);
    if (url.startsWith("/api/application")) return response(application);
    if (url.startsWith("/api/problems")) return response(problems);
    if (url.startsWith("/api/correlations")) return response(correlations);
    if (url === "/api/workflows") return response(workflowHistory);
    if (url.startsWith("/api/workflow")) return response(url.includes("run-2") ? { ...selectedWorkflow, runId: "run-2", requirement: "Checkout succeeds", status: "SUCCESS", timeline: [] } : selectedWorkflow);
    return response({ error: { code: "NOT_FOUND" } }, 404);
  };
}
async function settle() { await new Promise(resolve => setTimeout(resolve, 0)); await new Promise(resolve => setTimeout(resolve, 0)); }

assert.match(html, /Test Engineering Studio/);
assert.match(html, /data-view="overview"/);
assert.match(html, /data-view="repairs"/);
assert.match(css, /prefers-reduced-motion/);
assert.match(css, /focus-visible/);
assert.doesNotMatch(source, /\.innerHTML\s*=/, "backend text must never be assigned through innerHTML");
for (const endpoint of ["/api/project", "/api/config", "/api/status", "/api/application", "/api/problems", "/api/correlations", "/api/workflows", "/api/workflow", "/api/actions"]) assert.ok(source.includes(endpoint), `missing ${endpoint}`);
for (const action of studioModule.ACTIONS) assert.ok(source.includes(action), `missing action ${action}`);

{
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle();
  assert.equal(calls.length, 8, "initial load must issue only bounded projection GETs");
  assert.ok(calls.every(call => !call.options.method || call.options.method === "GET"), "refresh must not run or apply anything");
  assert.match(document.getElementById("project-label").textContent, /Example checkout.*checkout/);
  assert.match(document.getElementById("primary-view").textContent, /19/, "real projection counts must drive the view");
  assert.match(document.getElementById("primary-view").textContent, /Example checkout.*MAVEN.*\.test-lens/, "resolved project configuration must be visible");
  assert.match(document.getElementById("details-panel").textContent, /SOURCE_FINGERPRINT.*verified/, "projection evidence must drive the evidence panel");
}

{
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle(); studio.state.view = "runs"; studio.render();
  const rendered = document.getElementById("primary-view").textContent;
  assert.match(rendered, /Reviewer.*REQUEST_CHANGES/i, "the reviewer decision must remain visible as a review outcome");
  assert.match(rendered, /ASSERTION_INSUFFICIENT.*ASSERTION_REQUIRED.*implementation-1.*ADD_ASSERTION.*VERIFIED/i,
    "only bounded typed feedback should be rendered for a requested correction");
  assert.doesNotMatch(rendered, /detail|recommendation|sourcePatch/i,
    "the review projection must not expose arbitrary model prose or generated source");
}

{
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle();
  const views = document.querySelectorAll("[data-view]");
  assert.equal(Array.isArray(views), false, "the DOM contract uses an array-like NodeList, not an Array");
  let previous = null;
  for (let index = 0; index < views.length; index += 1) {
    const target = views.item(index);
    document.dispatch("click", target);
    assert.equal(studio.state.view, target.dataset.view, `navigation must select ${target.dataset.view}`);
    assert.equal(target.attributes["aria-current"], "page", `${target.dataset.view} must be the current view`);
    assert.equal(Array.from({ length: views.length }, (_, i) => views.item(i)).filter(item => item.attributes["aria-current"] === "page").length, 1,
      "exactly one navigation item must be current");
    if (previous) assert.equal(previous.attributes["aria-current"], "false", "the previously selected view must be cleared");
    studio.render();
    assert.equal(target.attributes["aria-current"], "page", "rerender must preserve current navigation state");
    previous = target;
  }
  const back = views.item(views.length - 2);
  document.dispatch("click", back);
  assert.equal(back.attributes["aria-current"], "page", "back navigation must restore the previous view");
  assert.equal(previous.attributes["aria-current"], "false");
}

{
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle(); studio.state.view = "history"; studio.render();
  const workflowButton = walk(document.getElementById("primary-view")).find(item => item.dataset.workflowRunId === "run-2");
  assert.ok(workflowButton, "persisted workflow summaries must be selectable");
  document.dispatch("click", workflowButton); await settle();
  assert.ok(calls.some(call => call.url === "/api/workflow?runId=run-2"), "selection must fetch the chosen bounded workflow detail");
  assert.equal(studio.state.workflow.runId, "run-2");
}

{
  const interrupted = { ...workflow, status: "AGENT_EXECUTION_INTERRUPTED", freshness: "STALE", resumed: true, availableActions: ["REGENERATE_PLAN"] };
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls, interrupted), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle();
  const banner = document.getElementById("workflow-banner");
  assert.equal(banner.hidden, false);
  assert.match(banner.textContent, /restored.*stale.*agent operation.*regenerate plan/i, "resumed, stale, interrupted and backend-provided actions must be explicit");
  const regenerate = walk(banner).find(item => item.tagName === "BUTTON" && /regenerate plan/i.test(item.textContent));
  assert.ok(regenerate, "backend-provided plan regeneration must be actionable");
  document.dispatch("click", regenerate); await settle();
  assert.ok(calls.some(call => call.options && call.options.body && JSON.parse(call.options.body).action === "REGENERATE_PLAN"), "regeneration must use the explicit allowlisted action");
}

for (const [label, expected] of [["Approve and apply", "APPROVE_REPAIR"], ["Reject", "REJECT_REPAIR"]]) {
  const calls = []; const document = new MiniDocument();
  const studio = studioModule.createStudio({ document, fetch: fixtureFetch(calls), location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle(); studio.state.view = "repairs"; studio.render();
  assert.equal(calls.filter(call => call.url === "/api/actions").length, 0, "rendering a proposal must not mutate source");
  const actionButton = walk(document.getElementById("primary-view")).find(item => item.tagName === "BUTTON" && item.textContent === label);
  assert.ok(actionButton, `${label} button should be rendered`);
  document.dispatch("click", actionButton); await settle();
  const posts = calls.filter(call => call.url === "/api/actions"); assert.equal(posts.length, 1, `${label} must post once after explicit click`);
  assert.equal(JSON.parse(posts[0].options.body).action, expected);
  assert.equal(JSON.parse(posts[0].options.body).proposalId, "repair-1");
  assert.equal(posts[0].options.headers["X-Test-Lens-Session"], "session-token");
}

assert.deepEqual(studioModule.describeError({ code: "AGENT_TIMEOUT" }), { code: "AGENT_TIMEOUT", message: "The agent did not complete within the allowed time." });
assert.deepEqual(studioModule.describeError({ code: "SOURCE_PRECONDITION_FAILED" }), { code: "SOURCE_PRECONDITION_FAILED", message: "Source changed after this proposal was created. The repair is stale and was not applied." });
assert.deepEqual(studioModule.describeError({ error: "RESUME_UNAVAILABLE" }), {
  code: "RESUME_UNAVAILABLE",
  message: "This saved workflow is available for history only. Start a new workflow to use current verified project context."
});
{
  const described = studioModule.describeError({
    error: "AGENT_OUTPUT_INVALID",
    stage: "Architect",
    reasonCode: "JSON_INVALID",
    location: "stdout",
    message: "Planner returned invalid structured output."
  });
  assert.equal(described.code, "AGENT_OUTPUT_INVALID", "the compatible top-level error code must not be replaced by a lower-level reason code");
  assert.match(described.message, /Architect.*Planner returned invalid structured output/i,
    "structured failures must keep their friendly stage and safe detail");
}
assert.deepEqual(
  studioModule.describeError({ error: "AGENT_OUTPUT_INVALID" }),
  { code: "AGENT_OUTPUT_INVALID", message: "The agent returned output that could not be validated." },
  "legacy agent failures must still display a specific safe explanation"
);

for (const [payload, expectedMessage] of [
  [{ error: "PAGE_OBJECT_CAPABILITY_MISSING", stage: "TEST_IMPLEMENTER", reasonCode: "PAGE_OBJECT_CAPABILITY_MISSING" }, /Implementer.*verified Page Object does not provide a capability/i],
  [{ error: "AGENT_OUTPUT_INVALID", stage: "Architect", reasonCode: "SCHEMA_INVALID", message: "Planner output omitted scenarios." }, /AGENT_OUTPUT_INVALID.*Architect.*Planner output omitted scenarios/i],
  [{ error: "AGENT_OUTPUT_INVALID", stage: "Implementer", reasonCode: "PAGE_OBJECT_CAPABILITY_MISSING", message: "The verified Page Object does not expose the required operation." }, /AGENT_OUTPUT_INVALID.*Implementer.*does not expose the required operation/i],
  [{ error: "AGENT_OUTPUT_INVALID", stage: "Implementer", reasonCode: "PAGE_OBJECT_EXTENSION_PROPOSAL", message: "A reviewed Page Object extension is required before implementation." }, /AGENT_OUTPUT_INVALID.*Implementer.*reviewed Page Object extension is required/i],
  [{ error: "AGENT_OUTPUT_INVALID" }, /AGENT_OUTPUT_INVALID.*could not be validated/i]
]) {
  const calls = []; const document = new MiniDocument();
  const normalFetch = fixtureFetch(calls, { ...workflow, status: "AGENT_FAILED", availableActions: ["GENERATE_PLAN"] });
  const failedActionBodies = [];
  const fetch = async (url, options = {}) => {
    if (url === "/api/actions") { failedActionBodies.push(JSON.parse(options.body)); return response(payload, 502); }
    return normalFetch(url, options);
  };
  const studio = studioModule.createStudio({ document, fetch, location: { origin: "http://127.0.0.1" } });
  studio.start(); await settle();
  const workflowBanner = document.getElementById("workflow-banner");
  assert.equal(workflowBanner.hidden, false, "a fresh AGENT_FAILED workflow must expose its backend retry action");
  const retry = walk(workflowBanner).find(item => item.tagName === "BUTTON" && /generate plan/i.test(item.textContent));
  assert.ok(retry, "failed planning must retain the exact explicit retry action");
  document.dispatch("click", retry); await settle();
  assert.ok(failedActionBodies.some(body => body.action === "GENERATE_PLAN"),
    "Architect retry must submit the backend-provided GENERATE_PLAN action");
  const banner = document.getElementById("error-banner");
  assert.equal(banner.hidden, false);
  assert.match(banner.textContent, expectedMessage, "the banner must retain the original code and a safe specific message");
  assert.notEqual(banner.textContent, "undefined", "request failures must remain structured instead of throwing a bare string");
}
assert.equal(studioModule.normalizedStatus("needs review"), "NEEDS_REVIEW");

console.log("Test Engineering Studio contract: PASS");
