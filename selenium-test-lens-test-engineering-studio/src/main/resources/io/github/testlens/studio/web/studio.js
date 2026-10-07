(function (root, factory) {
  "use strict";
  var exported = factory();
  if (typeof module === "object" && module.exports) module.exports = exported;
  root.TestEngineeringStudio = exported;
  if (root.document) {
    var boot = function () { exported.createStudio({ document: root.document, fetch: root.fetch.bind(root), location: root.location }).start(); };
    if (root.document.readyState === "loading") root.document.addEventListener("DOMContentLoaded", boot, { once: true });
    else boot();
  }
}(typeof globalThis !== "undefined" ? globalThis : this, function () {
  "use strict";

  var ACTIONS = new Set([
    "SCAN_PROJECT", "MAP_APPLICATION", "CORRELATE", "REFRESH_PROJECT", "CREATE_REQUIREMENT",
    "GENERATE_PLAN", "REGENERATE_PLAN", "GENERATE_IMPLEMENTATION", "RUN", "DIAGNOSE", "PREPARE_REPAIR",
    "REJECT_REPAIR", "APPROVE_REPAIR", "RERUN"
  ]);
  var TITLES = { overview: "Project overview", application: "Application map", "page-objects": "Page Object correlation", problems: "Problems", requirements: "Create test", plans: "Test plan", runs: "Verification", repairs: "Repair review", history: "Workflow history" };
  var PHASES = {
    SCAN_PROJECT: "Scanning source and building indexes…", MAP_APPLICATION: "Mapping the current application…",
    CORRELATE: "Correlating application elements with source…", REFRESH_PROJECT: "Refreshing stale project artifacts…",
    CREATE_REQUIREMENT: "Saving requirement…", GENERATE_PLAN: "Slicing context and running Architect…", REGENERATE_PLAN: "Refreshing project context and running Architect…",
    GENERATE_IMPLEMENTATION: "Generating implementation and validating policy…", RUN: "Validating policy and compiling…",
    DIAGNOSE: "Classifying failure and collecting evidence…", PREPARE_REPAIR: "Preparing a validated repair proposal…",
    REJECT_REPAIR: "Recording rejection without changing source…", APPROVE_REPAIR: "Rechecking source preconditions and applying trusted repair…",
    RERUN: "Running the same verification target…"
  };
  var ERROR_MESSAGES = {
    AGENT_NOT_AVAILABLE: "The configured agent is not available. Check the local runner configuration.",
    AGENT_TIMEOUT: "The agent did not complete within the allowed time.",
    AGENT_PROCESS_FAILED: "The agent process failed. Open diagnostics for the captured runner output.",
    AGENT_OUTPUT_INVALID: "The agent returned output that could not be validated.",
    AGENT_CONTEXT_REJECTED: "The selected context was rejected by the agent runner.",
    SOURCE_PRECONDITION_FAILED: "Source changed after this proposal was created. The repair is stale and was not applied.",
    OPERATION_ALREADY_RUNNING: "An equivalent operation is already running for this workspace.",
    UNAUTHORIZED: "The local Studio session is no longer valid. Reopen Studio from the tooling host."
  };

  function first(object, keys, fallback) {
    if (!object) return fallback;
    for (var i = 0; i < keys.length; i += 1) if (object[keys[i]] !== undefined && object[keys[i]] !== null) return object[keys[i]];
    return fallback;
  }
  function list(value) { return Array.isArray(value) ? value : []; }
  function text(value, fallback) { return value === undefined || value === null || value === "" ? (fallback || "—") : String(value); }
  function normalizedStatus(value) {
    var status = text(value, "NOT_STARTED").toUpperCase().replace(/[ -]+/g, "_");
    return ["NOT_STARTED", "RUNNING", "PASS", "PARTIAL", "FAILED", "BLOCKED", "NEEDS_REVIEW", "STALE"].indexOf(status) >= 0 ? status : status;
  }
  function stageOf(value) { return value ? (value.stage || value) : { status: "NOT_STARTED", summary: "Not started" }; }
  function safeJson(value) {
    var result;
    try { result = JSON.stringify(value, null, 2); } catch (ignored) { result = "Projection could not be serialized."; }
    return result.length > 50000 ? result.slice(0, 50000) + "\n… projection truncated in Studio" : result;
  }
  function describeError(error) {
    var code = first(error, ["code", "reasonCode", "error"], "REQUEST_FAILED");
    var detail = first(error, ["message", "detail"], "The local tooling request failed.");
    return { code: text(code), message: ERROR_MESSAGES[code] || text(detail) };
  }

  function createStudio(environment) {
    var document = environment.document;
    var fetchFn = environment.fetch;
    var state = { view: "overview", project: null, config: null, hostStatus: null, application: null, problems: null, correlations: null, workflow: null, workflows: [], busy: false, filter: "", category: "ALL", selected: null };
    function byId(id) { return document.getElementById(id); }
    function node(tag, className, content) {
      var result = document.createElement(tag);
      if (className) result.className = className;
      if (content !== undefined && content !== null) result.textContent = String(content);
      return result;
    }
    function add(parent) { for (var i = 1; i < arguments.length; i += 1) if (arguments[i]) parent.appendChild(arguments[i]); return parent; }
    function button(label, action, kind, payload) {
      var result = node("button", kind || "primary", label); result.type = "button";
      if (action) result.dataset.action = action;
      if (payload) Object.keys(payload).forEach(function (key) { if (payload[key] !== undefined && payload[key] !== null) result.dataset[key] = String(payload[key]); });
      result.disabled = state.busy;
      return result;
    }
    function statusBadge(value) { var status = normalizedStatus(value); var result = node("span", "status", status.replace(/_/g, " ")); result.dataset.status = status; return result; }
    function card(title, status, summary) {
      var result = node("section", "card"); var header = node("div", "card-header");
      add(header, node("h2", "", title), statusBadge(status)); add(result, header);
      if (summary) add(result, node("p", "stage-summary", summary)); return result;
    }
    function metric(label, value) { var result = node("div", "metric"); add(result, node("strong", "", text(value, "0")), node("span", "", label)); return result; }
    function actions(parent, values) { var row = node("div", "action-row"); var labels = { GENERATE_PLAN: "Generate plan", GENERATE_IMPLEMENTATION: "Generate test", RUN: "Run", DIAGNOSE: "Diagnose", PREPARE_REPAIR: "Propose repair", APPROVE_REPAIR: "Approve and apply", REJECT_REPAIR: "Reject", RERUN: "Run verification", REFRESH_PROJECT: "Refresh project" }; list(values).forEach(function (action) { var raw = typeof action === "string" ? action : first(action, ["action", "actionId", "id"], ""); var name = String(raw).replace(/-/g, "_").toUpperCase(); if (ACTIONS.has(name) && (typeof action === "string" || first(action, ["enabled"], true))) add(row, button(first(action, ["label"], labels[name] || name.replace(/_/g, " ")), name, "primary")); }); if (row.childNodes.length) add(parent, row); }
    function workflowActions(fallback) { return Array.isArray((state.workflow || {}).availableActions) ? state.workflow.availableActions.filter(function (action) { return ACTIONS.has(action); }) : list(fallback); }
    function workflowAllows(action) { return !Array.isArray((state.workflow || {}).availableActions) || state.workflow.availableActions.indexOf(action) >= 0; }
    function updateWorkflowBanner() {
      var banner = byId("workflow-banner"); if (!banner) return; banner.replaceChildren();
      var workflow = state.workflow || {}; var messages = []; var freshness = normalizedStatus(first(workflow, ["freshness"], "FRESH")); var status = normalizedStatus(first(workflow, ["status", "state"], "NOT_STARTED"));
      if (workflow.resumed) messages.push("This workflow was restored from the local project workspace after a Studio restart.");
      if (freshness !== "FRESH") messages.push("Workflow artifacts are " + freshness.replace(/_/g, " ").toLowerCase() + ". Review or regenerate them before continuing.");
      if (status.indexOf("INTERRUPTED") >= 0) messages.push("The previous " + (status.indexOf("AGENT") >= 0 ? "agent operation" : "execution") + " was interrupted. Studio did not assume it completed.");
      var available = list(workflow.availableActions); if (messages.length && available.length) messages.push("Available next actions: " + available.map(function (item) { return item.replace(/_/g, " ").toLowerCase(); }).join(", ") + ".");
      messages.forEach(function (message) { add(banner, node("p", "", message)); });
      var supported = available.filter(function (action) { return ACTIONS.has(action); }); if (supported.length) { var row = node("div", "action-row"); supported.forEach(function (action) { add(row, button(action.replace(/_/g, " "), action, "secondary", { runId: workflow.runId })); }); add(banner, row); }
      banner.hidden = messages.length === 0;
    }
    function empty(title, copy, action, label) { var result = node("section", "empty-state"); add(result, node("h2", "", title), node("p", "", copy)); if (action) add(result, button(label, action)); return result; }
    function renderList(items, className) { var ul = node("ul", className || "evidence-list"); list(items).forEach(function (item) { var value=typeof item === "string" ? item : [first(item,["code"],null),first(item,["detail","summary","label","message"],null)].filter(Boolean).join(" · ") || safeJson(item); add(ul, node("li", "", value)); }); return ul; }
    function rawDetails(label, projection) { var details = node("details"); add(details, node("summary", "", label), node("pre", "code-block raw-json", safeJson(projection))); return details; }
    function detailsFor(stage, projection) {
      var panel = byId("details-panel"); panel.replaceChildren(); stage = stageOf(stage);
      var evidence = node("section", "details-card"); add(evidence, node("h2", "", "Evidence"));
      if (list(stage.evidence).length) add(evidence, renderList(stage.evidence)); else add(evidence, node("p", "muted", "No evidence recorded for this stage."));
      var limitations = node("section", "details-card"); add(limitations, node("h2", "", "Limitations"));
      if (list(stage.limitations).length) add(limitations, renderList(stage.limitations, "evidence-list limitations")); else add(limitations, node("p", "muted", "No reported limitations."));
      var artifacts = node("section", "details-card"); add(artifacts, node("h2", "", "Artifacts"));
      if (list(stage.artifacts).length) add(artifacts, renderList(stage.artifacts)); else add(artifacts, node("p", "muted", "No artifacts yet."));
      if (projection) add(artifacts, rawDetails("View projection JSON", projection)); add(panel, evidence, limitations, artifacts);
    }
    function countsBlock(counts, keys) { var grid = node("div", "metrics-grid"); keys.forEach(function (entry) { add(grid, metric(entry[1], first(counts, [entry[0]], 0))); }); return grid; }
    function projectCounts(project) { return Object.assign({}, project && project.source || {}, project && project.application || {}, project && project.correlation || {}, project && project.counts || {}, { problems: Number(first(project, ["problemCount"], first(project && project.counts, ["problems"], 0))) }); }
    function projectStages(project) {
      var counts = projectCounts(project || {}), overall = stageOf(project && project.stage);
      return {
        scan: project && project.scan || { status: Number(counts.files) > 0 ? "PASS" : "NOT_STARTED", summary: Number(counts.files) > 0 ? counts.files + " files scanned" : "Project not scanned", actions: ["SCAN_PROJECT"] },
        mapping: project && project.mapping || { status: Number(counts.pages) > 0 ? (overall.status === "FAILED" ? "FAILED" : "PASS") : "NOT_STARTED", summary: Number(counts.pages) > 0 ? counts.pages + " pages mapped" : "Application not mapped", actions: ["MAP_APPLICATION"] },
        correlation: project && project.correlationStage || { status: Number(counts.exact) + Number(counts.strong) + Number(counts.probable) + Number(counts.ambiguous) + Number(counts.noMatch) + Number(counts.conflict) > 0 ? (Number(counts.ambiguous) + Number(counts.noMatch) + Number(counts.conflict) > 0 ? "NEEDS_REVIEW" : "PASS") : "NOT_STARTED", summary: Number(counts.exact) + Number(counts.strong) + " elements correlated", actions: ["CORRELATE"] }
      };
    }

    function renderOverview() {
      var project = state.project;
      if (!project) { showPrimary(empty("Project unavailable", "Studio could not load the project projection.")); detailsFor(null); return; }
      var wrap = document.createDocumentFragment(); var counts = projectCounts(project); var projectStage = projectStages(project);
      var statusCard = card("Project status", project.status || aggregateProjectStatus(project), "Source, application and correlations are retained in this project workspace.");
      var stages = node("div", "stage-grid");
      [["Source", projectStage.scan, "SCAN_PROJECT", "Scan project"], ["Application", projectStage.mapping, "MAP_APPLICATION", "Start mapping"], ["Page Objects", projectStage.correlation, "CORRELATE", "Correlate"]].forEach(function (entry) {
        var stage = stageOf(entry[1]); var item = card(entry[0], stage.status, stage.summary); actions(item, list(stage.actions).length ? stage.actions : [entry[2]]); item.querySelector("button") && (item.querySelector("button").textContent = entry[3]); add(stages, item);
      }); add(statusCard, stages); add(wrap, statusCard);
      var app = card("Application and source", stageOf(projectStage.mapping).status, "Measured facts from current artifacts.");
      add(app, countsBlock(counts, [["pages", "Pages"], ["states", "States"], ["elements", "Elements"], ["pageObjects", "Page Objects"], ["locatorDeclarations", "Locator declarations"], ["tests", "Tests"]])); add(wrap, app);
      var corr = card("Correlation", stageOf(projectStage.correlation).status, "Application elements linked to existing source.");
      add(corr, countsBlock(counts, [["exact", "Exact"], ["strong", "Strong"], ["probable", "Probable"], ["ambiguous", "Ambiguous"], ["noMatch", "No match"], ["conflict", "Conflict"]])); add(wrap, corr);
      var problems = card("Problems", Number(counts.problems || 0) ? "NEEDS_REVIEW" : "PASS", Number(counts.problems || 0) ? counts.problems + " items need attention." : "No current project problems.");
      if (Number(counts.problems || 0)) { var open = button("Review problems", null, "secondary"); open.dataset.view = "problems"; add(problems, node("div", "action-row")); problems.lastChild.appendChild(open); } add(wrap, problems);
      var config = state.config; if (config) { var configured = card("Project configuration", config.status || "PASS", "Resolved from " + text(config.configurationSource, "project discovery").replace(/_/g, " ").toLowerCase() + "."); var facts = node("table", "facts"); var factBody = node("tbody"); [["Project", config.name], ["Build", config.build], ["Root", config.root], ["Workspace", config.workspace], ["Main sources", list(config.mainSourceRoots).join(", ")], ["Test sources", list(config.testSourceRoots).join(", ")], ["Browser", text(config.browser) + (config.headless ? " (headless)" : " (headed)")], ["Browser capability", first(config.browserCapability,["status"],"NOT_AVAILABLE") + " — " + first(config.browserCapability,["reason"],"")], ["Agent capability", first(config.agentCapability,["status"],"NOT_AVAILABLE") + " — " + first(config.agentCapability,["reason"],"")], ["Compilation capability", first(config.compilationCapability,["status"],"NOT_AVAILABLE") + " — " + first(config.compilationCapability,["reason"],"")]].forEach(function (entry) { var row = node("tr"); add(row, node("th", "", entry[0]), node("td", "", text(entry[1]))); add(factBody, row); }); add(facts, factBody); add(configured, facts); add(wrap, configured); }
      if (state.hostStatus) { var host = card("Local host", state.hostStatus.operationRunning ? "RUNNING" : "PASS", state.hostStatus.operationRunning ? "A bounded Studio operation is running." : "Ready for an explicit action."); add(wrap, host); }
      showPrimary(wrap); detailsFor(project.stage || projectStage.scan, project);
    }
    function aggregateProjectStatus(project) { var stages = projectStages(project); var statuses = [stageOf(stages.scan).status, stageOf(stages.mapping).status, stageOf(stages.correlation).status]; if (statuses.indexOf("FAILED") >= 0) return "FAILED"; if (statuses.indexOf("PARTIAL") >= 0 || statuses.indexOf("NEEDS_REVIEW") >= 0) return "PARTIAL"; if (statuses.every(function (s) { return s === "PASS"; })) return "PASS"; return "NOT_STARTED"; }
    function renderApplication() {
      var project = state.project || {}; var application = state.application || {}; var mapping = application.stage || project.mapping || projectStages(project).mapping; var stage = stageOf(mapping);
      if (stage.status === "NOT_STARTED") { var missing = empty("Application not mapped", "Map only the current page, or keep a guided mapper across caller-owned navigation."); var choices = node("div", "action-row"); add(choices, button("Map current page", "MAP_APPLICATION", "primary", { mode: "CURRENT_PAGE" }), button("Start guided mapping", "MAP_APPLICATION", "secondary", { mode: "GUIDED" })); add(missing, choices); showPrimary(missing); detailsFor(stage); return; }
      var result = card("Application model", stage.status, stage.summary); var counts = Object.assign({}, projectCounts(project), application.counts || {});
      add(result, countsBlock(counts, [["pages", "Pages"], ["states", "States"], ["elements", "Elements"], ["transitions", "Transitions"], ["sharedComponents", "Shared components"], ["selectorIssues", "Selector issues"]])); var mappingActions = node("div", "action-row"); add(mappingActions, button("Observe current page", "MAP_APPLICATION", "primary", { mode: "CURRENT_PAGE" }), button("Continue guided mapping", "MAP_APPLICATION", "secondary", { mode: "GUIDED" })); add(result, mappingActions);
      var freshness = card("Artifact freshness", "PASS", "Fingerprint-derived state; Studio does not run a file watcher."); var table = node("table", "facts"); var body = node("tbody"); Object.keys(project.freshness || {}).forEach(function (key) { var row = node("tr"); add(row, node("th", "", key.replace(/([A-Z])/g, " $1")), node("td", "", text(project.freshness[key]))); add(body, row); }); add(table, body); add(freshness, table); var fragment = document.createDocumentFragment(); add(fragment, result, freshness); showPrimary(fragment); detailsFor(stage, mapping);
    }
    function filtered(items) { var needle = state.filter.toLowerCase(); return list(items).filter(function (item) { var category = text(first(item, ["category", "state", "severity"], ""), ""); return (state.category === "ALL" || category === state.category) && (!needle || safeJson(item).toLowerCase().indexOf(needle) >= 0); }); }
    function filterControls(categories) { var row = node("div", "filter-row"); var input = node("input"); input.type = "search"; input.placeholder = "Filter current results"; input.value = state.filter; input.dataset.filter = "text"; input.setAttribute("aria-label", "Filter current results"); var select = node("select"); select.dataset.filter = "category"; select.setAttribute("aria-label", "Filter by category"); ["ALL"].concat(categories).forEach(function (value) { var option = node("option", "", value.replace(/_/g, " ")); option.value = value; option.selected = state.category === value; add(select, option); }); add(row, input, select); return row; }
    function pager(projection, resource) { var result = node("div", "pager"); var size = Number(first(projection, ["limit", "size"], 50)); var offset = Number(first(projection, ["offset"], Number(first(projection, ["page"], 0)) * size)); var page = Math.floor(offset / size); var total = Number(first(projection, ["total"], list(projection.items || projection.problems).length)); add(result, node("span", "", total + " total · page " + (page + 1))); var controls = node("div"); var previous = button("Previous", null, "secondary"); previous.dataset.pageResource = resource; previous.dataset.page = String(Math.max(0, page - 1)); previous.disabled = state.busy || page <= 0; var next = button("Next", null, "secondary"); next.dataset.pageResource = resource; next.dataset.page = String(page + 1); next.disabled = state.busy || (offset + size >= total); add(controls, previous, next); add(result, controls); return result; }
    function renderCorrelations() {
      var projection = state.correlations;
      if (!projection || (!projection.total && !list(projection.items).length)) { showPrimary(empty("No correlations", "Scan source and map the application before correlating Page Objects.", "CORRELATE", "Correlate Page Objects")); detailsFor((state.project || {}).correlation); return; }
      var result = card("Correlation", stageOf(projection).status, projection.total + " application elements in this result set."); var counts = projection.counts || {};
      add(result, countsBlock(counts, [["exact", "Exact"], ["strong", "Strong"], ["probable", "Probable"], ["ambiguous", "Ambiguous"], ["noMatch", "No match"], ["conflict", "Conflict"]])); add(result, filterControls(["EXACT", "STRONG", "PROBABLE", "AMBIGUOUS", "NO_MATCH", "CONFLICT"]));
      var table = node("table", "facts"); var head = node("thead"); var hr = node("tr"); ["Application", "Source", "Correlation"].forEach(function (label) { add(hr, node("th", "", label)); }); add(head, hr); var body = node("tbody"); filtered(projection.items).forEach(function (item, index) { var row = node("tr"); row.tabIndex = 0; row.dataset.selectCorrelation = String(index); var source = first(item, ["sourceLabel", "sourceDeclarationRef", "sourceElementId"], "No source match"); add(row, node("td", "", first(item, ["semanticName", "applicationElementId"], "Unknown element")), node("td", "", source)); var cell = node("td"); add(cell, statusBadge(item.state)); add(row, cell); add(body, row); }); add(table, head, body); add(result, table, pager(projection, "correlations")); showPrimary(result); var selected = state.selected && state.selected.type === "correlation" ? state.selected.value : projection; detailsFor(selected, selected);
    }
    function renderProblems() {
      var projection = state.problems;
      var problemItems = list(projection && (projection.items || projection.problems));
      if (!projection || (!projection.total && !problemItems.length)) { showPrimary(empty("No current problems", "No selector, correlation, source-index or model limitations need attention.")); detailsFor(projection, projection); return; }
      var result = card("Problems", "NEEDS_REVIEW", projection.total + " items across the current project."); var categories = problemItems.map(function (item) { return item.category; }).filter(function (v, i, a) { return v && a.indexOf(v) === i; }); add(result, filterControls(categories)); var ul = node("ul", "list");
      filtered(problemItems).forEach(function (item, index) { var li = node("li"); li.tabIndex = 0; li.dataset.selectProblem = String(index); var title = node("div", "list-title"); add(title, node("span", "", first(item, ["title", "category"], "Problem")), statusBadge(first(item, ["status", "severity"], "NEEDS_REVIEW"))); add(li, title, node("div", "list-meta", [item.category, item.location, item.suggestedAction].filter(Boolean).join(" · "))); add(ul, li); }); add(result, ul, pager(projection, "problems")); showPrimary(result); var selected = state.selected && state.selected.type === "problem" ? state.selected.value : projection; detailsFor(selected, selected);
    }
    function workflowStage(name) { var workflow = state.workflow || {}; return first(workflow, [name], null); }
    function renderRequirements() {
      var requirement = first(state.workflow, ["requirement"], ""); var result = card("Create test", requirement ? "PASS" : "NOT_STARTED", "Describe observable behavior. Advanced context selection stays with the existing Context Slicer."); var label = node("label", "field-label", "Requirement"); label.htmlFor = "requirement-input"; var input = node("textarea"); input.id = "requirement-input"; input.maxLength = 8000; input.placeholder = "Invalid password should display an error message."; input.value = typeof requirement === "string" ? requirement : first(requirement, ["text", "summary"], ""); add(result, label, input, node("p", "field-help", "The requirement is sent only to the local tooling host and persisted with this workflow.")); var row = node("div", "action-row"); add(row, button("Generate plan", "CREATE_REQUIREMENT")); add(result, row); showPrimary(result); detailsFor(workflowStage("context") || state.workflow, state.workflow);
    }
    function renderPlans() {
      var plan = workflowStage("plan"); if (!plan) { showPrimary(empty("No test plan", "Create a requirement to generate a plan from a bounded project context.", null)); detailsFor(state.workflow); return; }
      if (list(plan.scenarios).length) plan = Object.assign({}, plan, plan.scenarios[0]);
      var stage = stageOf(plan); var result = card(first(plan, ["title", "name"], "Test plan"), stage.status, first(plan, ["summary"], stage.summary));
      var table = node("table", "facts"); var body = node("tbody"); [["Priority", first(plan, ["priority"], "—")], ["Preconditions", list(plan.preconditions).join("; ")], ["Expected", list(first(plan, ["expected", "expectedResults"], [])).join("; ")], ["Uses", list(first(plan, ["uses", "pageObjects"], [])).join(", ")], ["Existing coverage", first(plan, ["existingCoverage", "relatedTests"], "—")]].forEach(function (entry) { var row = node("tr"); add(row, node("th", "", entry[0]), node("td", "", text(entry[1]))); add(body, row); }); add(table, body); add(result, table);
      if (list(plan.steps).length) { add(result, node("h3", "", "Steps")); var ol = node("ol"); list(plan.steps).forEach(function (step) { add(ol, node("li", "", typeof step === "string" ? step : first(step, ["action", "summary"], safeJson(step)))); }); add(result, ol); } actions(result, workflowActions(list(stage.actions).length ? stage.actions : ["GENERATE_IMPLEMENTATION"]));
      var context = workflowStage("context"); var fragment = document.createDocumentFragment(); add(fragment, result); if (context) { var contextCard = card("Context", stageOf(context).status, text(first(context, ["bytesLabel", "sizeBytes"], "Bounded context"))); add(contextCard, renderList(list(first(context, ["included"], [])))); if (list(context.excluded).length) { add(contextCard, node("h3", "", "Excluded"), renderList(context.excluded, "evidence-list limitations")); } add(fragment, contextCard); } showPrimary(fragment); detailsFor(stage, plan);
    }
    function renderDiff(parent, diff) { var pre = node("pre", "diff"); var value = typeof diff === "string" ? diff : first(diff, ["patch", "source", "content"], ""); text(value, "No source diff available.").split("\n").forEach(function (line) { var className = line.charAt(0) === "+" ? "diff-line addition" : line.charAt(0) === "-" ? "diff-line removal" : "diff-line"; add(pre, node("span", className, line)); }); add(parent, pre); }
    function renderRuns() {
      var workflow = state.workflow || {}; var implementation = workflow.implementation; var execution = workflow.execution; var diagnosis = workflow.diagnosis; if (!implementation && !execution) { showPrimary(empty("No proposed test", "Generate an implementation from an approved test plan.")); detailsFor(workflow); return; }
      var fragment = document.createDocumentFragment();
      if (implementation) { var istage = stageOf(implementation); var proposed = card("Proposed test", istage.status, first(implementation, ["file", "scenarioId", "summary"], istage.summary)); renderDiff(proposed, first(implementation, ["diff", "patch", "source", "sourcePatch"], "")); var policy = list(first(implementation, ["policy", "policyResults"], [])); if (policy.length) { add(proposed, node("h3", "", "Policy")); var pList = node("ul", "evidence-list"); policy.forEach(function (entry) { var pass = typeof entry === "string" || first(entry, ["pass", "allowed"], false); add(pList, node("li", pass ? "policy-pass" : "policy-fail", (pass ? "PASS · " : "BLOCKED · ") + text(typeof entry === "string" ? entry : first(entry, ["rule", "message", "code"], "Policy rule")))); }); add(proposed, pList); } var blocked = normalizedStatus(istage.status) === "BLOCKED" || policy.some(function (entry) { return typeof entry !== "string" && first(entry, ["pass", "allowed"], true) === false; }); var runRow = node("div", "action-row"); var run = button(blocked ? "Execution blocked by policy" : "Run", "RUN"); run.disabled = state.busy || blocked || !workflowAllows("RUN"); add(runRow, run); add(proposed, runRow); add(fragment, proposed); }
      if (execution) { var estage = stageOf(execution); var executionCard = card("Execution", estage.status, estage.summary); var table = node("table", "facts"); var body = node("tbody"); [["Compilation", first(execution, ["compilationStatus", "compileStatus", "compilation"], "—")], ["Execution", first(execution, ["executionStatus", "testStatus", "execution"], estage.status)], ["Duration", first(execution, ["duration", "durationMs", "durationMillis"], "—")], ["Failed action", first(execution, ["failedAction"], "—")], ["Exception", first(execution, ["exception", "exceptionType", "failureSummary"], "—")]].forEach(function (entry) { var row = node("tr"); add(row, node("th", "", entry[0]), node("td", "", text(entry[1]))); add(body, row); }); add(table, body); add(executionCard, table); var trace = list(first(execution, ["trace", "steps"], [])); if (trace.length) { add(executionCard, node("h3", "", "Test Lens trace")); var traceList = node("ul", "list"); trace.forEach(function (entry) { var success = normalizedStatus(first(entry, ["status"], "PASS")) === "PASS"; add(traceList, node("li", success ? "trace-pass" : "trace-fail", (success ? "✓ " : "✗ ") + text(typeof entry === "string" ? entry : first(entry, ["summary", "action", "message"], "Trace event")))); }); add(executionCard, traceList); } if (normalizedStatus(estage.status) === "FAILED" && !diagnosis) actions(executionCard, workflowActions(["DIAGNOSE"])); add(executionCard, rawDetails("Raw diagnostics", first(execution, ["diagnostics"], execution))); add(fragment, executionCard); }
      if (diagnosis) { var dstage = stageOf(diagnosis); var diagnosisCard = card("Diagnosis · " + text(first(diagnosis, ["classification", "failureClassification", "category"], "UNCLASSIFIED")), dstage.status, dstage.summary); if (list(diagnosis.counterEvidence).length) { add(diagnosisCard, node("h3", "", "Counter-evidence"), renderList(diagnosis.counterEvidence, "evidence-list limitations")); } actions(diagnosisCard, workflowActions(list(dstage.actions).length ? dstage.actions : ["PREPARE_REPAIR"])); add(fragment, diagnosisCard); }
      showPrimary(fragment); detailsFor(diagnosis || execution || implementation, diagnosis || execution || implementation);
    }
    function repairId(repair) { return first(repair, ["proposalId", "id", "repairProposalId"], ""); }
    function renderRepairs() {
      var repair = workflowStage("repair"); if (!repair) { showPrimary(empty("No repair proposals", "A repair can be prepared only after a diagnosable failed execution.")); detailsFor(state.workflow); return; }
      var stage = stageOf(repair); var result = card("Proposed repair", stage.status, first(repair, ["file", "logicalPath", "summary"], stage.summary)); var repairDiff = first(repair, ["diff", "patch"], ""); if (!repairDiff && (repair.oldSelector || repair.newSelector)) repairDiff = "- " + text(repair.oldSelector) + "\n+ " + text(repair.newSelector); renderDiff(result, repairDiff);
      var selector = first(repair, ["selectorIntelligence", "validation"], null); var selectorEvidence = selector ? list(first(selector, ["evidence", "checks"], [])) : list(repair.selectorEvidence); if (selectorEvidence.length) { add(result, node("h3", "", "Selector Intelligence"), renderList(selectorEvidence)); }
      var affected = first(repair, ["affectedScope", "affected"], null) || { methods: repair.affectedMethods, tests: repair.affectedTests }; if (list(affected.methods).length || list(affected.tests).length) { add(result, node("h3", "", "Affected scope"), rawDetails("View affected methods and tests", affected)); }
      var row = node("div", "action-row"); var id = repairId(repair); var status = normalizedStatus(stage.status); var stale = status === "STALE" || !!repair.stale; var decided = !!first(repair, ["decision"], null); var approve = button(stale ? "Proposal is stale" : "Approve and apply", "APPROVE_REPAIR", "primary", { proposalId: id }); approve.disabled = state.busy || stale || decided; var reject = button("Reject", "REJECT_REPAIR", "danger", { proposalId: id }); reject.disabled = state.busy || decided; add(row, approve, reject); add(result, row, node("p", "field-help", "Approval is an explicit trusted-host action. Source fingerprints, old selector and path safety are checked again before any write.")); if (first(repair, ["applyStatus"], null) === "APPLIED" || (first(repair, ["applyResult"], null) && normalizedStatus(first(repair.applyResult, ["status"], "")) === "PASS")) { var verify = button("Run verification", "RERUN", "primary", { runId: first(state.workflow, ["runId"], "") }); add(result, node("div", "action-row")); result.lastChild.appendChild(verify); } showPrimary(result); detailsFor(stage, repair);
    }
    function renderHistory() {
      var fragment = document.createDocumentFragment(); var summaries = list(state.workflows); var saved = card("Saved workflows", summaries.length ? "PASS" : "NOT_STARTED", summaries.length + " workflow" + (summaries.length === 1 ? "" : "s") + " retained in this project workspace.");
      if (summaries.length) { var choices = node("ul", "list workflow-list"); summaries.forEach(function (summary) { var item = node("li"); var select = button(text(summary.requirement, summary.runId), null, "secondary"); select.dataset.workflowRunId = summary.runId; if (state.workflow && summary.runId === state.workflow.runId) select.setAttribute("aria-current", "true"); add(item, select, node("div", "list-meta", [summary.state, summary.freshness, summary.resumed ? "RESUMED" : null, summary.updatedAt].filter(Boolean).join(" · "))); add(choices, item); }); add(saved, choices); } add(fragment, saved);
      var workflow = state.workflow; if (workflow && list(workflow.timeline).length) { var result = card("Workflow timeline", workflow.status, first(workflow, ["requirement"], "")); var timeline = node("ol", "timeline"); list(workflow.timeline).forEach(function (entry) { var li = node("li"); add(li, node("strong", "", text(typeof entry === "string" ? entry : first(entry, ["label", "event", "summary", "name"], "Workflow event")))); var meta = [first(entry, ["status"], null), first(entry, ["timestamp", "at"], null)].filter(Boolean).join(" · "); if (meta) add(li, node("small", "", meta)); add(timeline, li); }); add(result, timeline); if (workflow.metrics) add(result, rawDetails("Workflow metrics", workflow.metrics)); add(fragment, result); } else add(fragment, empty("No workflow timeline", "Select a saved workflow or create a requirement to start one.", null)); showPrimary(fragment); detailsFor(workflow, workflow);
    }
    function showPrimary(content) { var primary = byId("primary-view"); primary.replaceChildren(content); primary.setAttribute("aria-busy", state.busy ? "true" : "false"); }
    function render() {
      byId("view-title").textContent = TITLES[state.view] || "Test Engineering";
      byId("project-label").textContent = [first(state.config, ["name"], first(state.project, ["projectName"], "Project")), first(state.config, ["root"], first(state.project, ["rootLabel"], ""))].filter(Boolean).join(" · ");
      list(document.querySelectorAll("[data-view]")).forEach(function (item) { if (item.closest && item.closest("nav")) item.setAttribute("aria-current", item.dataset.view === state.view ? "page" : "false"); });
      var totalProblems = Number(first(state.problems, ["total"], first(state.project && state.project.counts, ["problems"], 0))); var badge = byId("problem-count"); badge.textContent = String(totalProblems); badge.hidden = totalProblems < 1;
      updateWorkflowBanner(); ({ overview: renderOverview, application: renderApplication, "page-objects": renderCorrelations, problems: renderProblems, requirements: renderRequirements, plans: renderPlans, runs: renderRuns, repairs: renderRepairs, history: renderHistory }[state.view] || renderOverview)();
    }
    function sessionToken() { var meta = document.querySelector('meta[name="test-lens-session"]'); return (meta && meta.content) || (document.documentElement.dataset && document.documentElement.dataset.sessionToken) || (document.body.dataset && document.body.dataset.sessionToken) || ""; }
    async function request(path, options) {
      var response;
      try { response = await fetchFn(path, options || { method: "GET", credentials: "same-origin", cache: "no-store" }); }
      catch (error) { throw { code: "TOOLING_HOST_UNAVAILABLE", message: "Cannot reach the local Test Engineering host." }; }
      var body = null; try { body = await response.json(); } catch (ignored) { body = null; }
      if (!response.ok || (body && body.ok === false)) throw (body && (body.error || body)) || { code: "HTTP_" + response.status, message: "Tooling host request failed." };
      return body;
    }
    async function loadAll() {
      setBusy(true, "Loading persisted project state…");
      try {
        var values = await Promise.all([request("/api/project"), request("/api/config"), request("/api/status"), request("/api/application"), request("/api/problems"), request("/api/correlations"), request("/api/workflows"), request("/api/workflow")]);
        state.project = values[0]; state.config = values[1]; state.hostStatus = values[2]; state.application = values[3]; state.problems = values[4]; state.correlations = values[5]; state.workflows = values[6]; state.workflow = values[7]; setConnection(true); clearError();
      } catch (error) { setConnection(false); showError(error); } finally { setBusy(false); render(); }
    }
    async function loadPage(resource, page) {
      setBusy(true, "Loading page " + (page + 1) + "…"); try { state[resource] = await request("/api/" + resource + "?offset=" + encodeURIComponent(page * 50) + "&limit=50"); clearError(); } catch (error) { showError(error); } finally { setBusy(false); render(); }
    }
    async function loadWorkflow(runId) { if (!runId || state.busy) return; setBusy(true, "Loading workflow…"); try { state.workflow = await request("/api/workflow?runId=" + encodeURIComponent(runId)); clearError(); } catch (error) { showError(error); } finally { setBusy(false); render(); } }
    async function performAction(action, dataset) {
      if (!ACTIONS.has(action) || state.busy) return;
      var payload = { action: action }; var requirement = byId("requirement-input");
      if (action === "CREATE_REQUIREMENT") { payload.requirement = requirement ? requirement.value.trim() : ""; if (!payload.requirement) { showError({ code: "REQUIREMENT_REQUIRED", message: "Enter a requirement before generating a plan." }); return; } }
      if (action === "MAP_APPLICATION") payload.mode = dataset && dataset.mode || "CURRENT_PAGE";
      if (dataset && dataset.proposalId) payload.proposalId = dataset.proposalId;
      if (dataset && dataset.runId) payload.runId = dataset.runId;
      if (state.workflow && state.workflow.runId && !payload.runId) payload.runId = state.workflow.runId;
      setBusy(true, PHASES[action]); clearError();
      try {
        var response = await request("/api/actions", { method: "POST", credentials: "same-origin", cache: "no-store", headers: { "Content-Type": "application/json", "X-Test-Lens-Session": sessionToken() }, body: JSON.stringify(payload) });
        if (action === "CREATE_REQUIREMENT") {
          var createdRunId = first(response.result, ["runId"], "");
          if (!createdRunId) throw { code: "WORKFLOW_RUN_MISSING", message: "The tooling host did not return a workflow run identifier." };
          payload = { action: "GENERATE_PLAN", runId: createdRunId };
          response = await request("/api/actions", { method: "POST", credentials: "same-origin", cache: "no-store", headers: { "Content-Type": "application/json", "X-Test-Lens-Session": sessionToken() }, body: JSON.stringify(payload) });
        }
        if (response.project) state.project = response.project; if (response.workflow) state.workflow = response.workflow;
        var workflowQuery = payload.runId ? "?runId=" + encodeURIComponent(payload.runId) : "";
        var refreshed = await Promise.all([request("/api/project"), request("/api/config"), request("/api/status"), request("/api/application"), request("/api/problems"), request("/api/correlations"), request("/api/workflows"), request("/api/workflow" + workflowQuery)]);
        state.project = refreshed[0]; state.config = refreshed[1]; state.hostStatus = refreshed[2]; state.application = refreshed[3]; state.problems = refreshed[4]; state.correlations = refreshed[5]; state.workflows = refreshed[6]; state.workflow = refreshed[7];
        if (action === "CREATE_REQUIREMENT") state.view = "plans"; else if (action === "GENERATE_IMPLEMENTATION" || action === "RUN" || action === "DIAGNOSE") state.view = "runs"; else if (action === "PREPARE_REPAIR" || action === "APPROVE_REPAIR" || action === "REJECT_REPAIR") state.view = "repairs";
      } catch (error) { showError(error); } finally { setBusy(false); render(); }
    }
    function setBusy(value, message) { state.busy = value; var status = byId("operation-status"); if (status) { status.hidden = !value; status.textContent = value ? text(message, "Working…") : ""; } }
    function setConnection(connected) { var footer = byId("connection-status"); if (!footer) return; footer.parentElement.className = connected ? "connected" : "disconnected"; footer.textContent = connected ? "Local tooling connected" : "Local tooling unavailable"; }
    function clearError() { var banner = byId("error-banner"); banner.hidden = true; banner.textContent = ""; }
    function showError(error) { var detail = describeError(error); var banner = byId("error-banner"); banner.textContent = detail.code + " · " + detail.message; banner.hidden = false; }
    function selectItem(kind, index) { var source = kind === "problem" ? filtered((state.problems || {}).items || (state.problems || {}).problems) : filtered((state.correlations || {}).items); state.selected = { type: kind, value: source[index] }; render(); }
    function bind() {
      document.addEventListener("click", function (event) {
        var target = event.target.closest ? event.target.closest("button,[data-select-problem],[data-select-correlation]") : event.target;
        if (!target) return;
        if (target.dataset.view) { state.view = target.dataset.view; state.selected = null; render(); byId("studio-content").focus(); return; }
        if (target.dataset.action) { performAction(target.dataset.action, target.dataset); return; }
        if (target.dataset.workflowRunId) { loadWorkflow(target.dataset.workflowRunId); return; }
        if (target.dataset.pageResource) { loadPage(target.dataset.pageResource, Number(target.dataset.page)); return; }
        if (target.dataset.selectProblem !== undefined) selectItem("problem", Number(target.dataset.selectProblem));
        if (target.dataset.selectCorrelation !== undefined) selectItem("correlation", Number(target.dataset.selectCorrelation));
      });
      document.addEventListener("input", function (event) { if (event.target.dataset.filter === "text") { state.filter = event.target.value; render(); var replacement = document.querySelector('[data-filter="text"]'); replacement && replacement.focus(); } });
      document.addEventListener("change", function (event) { if (event.target.dataset.filter === "category") { state.category = event.target.value; render(); } });
      document.addEventListener("keydown", function (event) { if ((event.key === "Enter" || event.key === " ") && event.target.dataset && event.target.dataset.selectProblem !== undefined) { event.preventDefault(); selectItem("problem", Number(event.target.dataset.selectProblem)); } if ((event.key === "Enter" || event.key === " ") && event.target.dataset && event.target.dataset.selectCorrelation !== undefined) { event.preventDefault(); selectItem("correlation", Number(event.target.dataset.selectCorrelation)); } });
    }
    function start() { bind(); loadAll(); return api; }
    var api = { start: start, loadAll: loadAll, performAction: performAction, render: render, state: state };
    return api;
  }

  return { createStudio: createStudio, describeError: describeError, normalizedStatus: normalizedStatus, safeJson: safeJson, ACTIONS: ACTIONS };
}));
