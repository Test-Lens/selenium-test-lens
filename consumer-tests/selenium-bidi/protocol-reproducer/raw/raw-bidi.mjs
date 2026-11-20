import fs from 'node:fs';
import path from 'node:path';
const options = {};
for (let i = 2; i < process.argv.length; i += 2) options[process.argv[i].replace(/^--/, '')] = process.argv[i + 1];
const fixture = options.fixture || fs.readFileSync(options['fixture-file'] || 'target/raw-bidi-diagnostic/fixture-url.txt', 'utf8').trim();
const driver = (options.webdriver || 'http://127.0.0.1:9515').replace(/\/$/, '');
const output = path.resolve(options.output || 'target/raw-bidi-diagnostic/evidence.json');
const events = ['network.beforeRequestSent', 'network.responseStarted', 'network.responseCompleted', 'network.fetchError'];
const evidence = {client: 'Node native WebSocket + fetch + JSON.parse; no Selenium or Lens', nodeVersion: process.version,
  stimuli: 50, eventDeadlineMs: 3000, outcome: 'INCOMPLETE', phases: [], serverJournal: [], subscriptionAcknowledgements: [], jsonParseFailures: 0, evidenceOverflow: false};
const pseudonyms = new Map();
const pending = new Map();
const changed = new Set();
let socket, session, nextId = 1, firstError;
function safePath(value) {
  if (typeof value !== 'string') return '';
  try { const pathname = new URL(value).pathname; return /^\/(redirect|final)\/[0-9]+$/.test(pathname) ? pathname : ''; }
  catch { return ''; }
}
function number(value) { return typeof value === 'number' && Number.isFinite(value) ? value : -1; }
function record(type, params) {
  const request = params.request || {}, response = params.response || {};
  const requestPath = safePath(request.url), responsePath = safePath(response.url);
  if (!requestPath && !responsePath) return;
  if (evidence.phases.length >= 1000) { evidence.evidenceOverflow = true; for (const fn of changed) fn(); return; }
  const id = typeof request.request === 'string' ? request.request : '<missing>';
  if (!pseudonyms.has(id)) pseudonyms.set(id, 'request-' + (pseudonyms.size + 1));
  evidence.phases.push({type, timestamp: number(params.timestamp), requestId: pseudonyms.get(id), requestPath, responsePath,
    hop: number(params.redirectCount), status: number(response.status)});
  for (const fn of changed) fn();
}
async function http(method, url, body) {
  const response = await fetch(url, {method, headers: body === undefined ? {} : {'Content-Type': 'application/json'},
    body: body === undefined ? undefined : JSON.stringify(body), signal: AbortSignal.timeout(20000)});
  const result = await response.json();
  if (!response.ok || result.value?.error) throw new Error('WebDriver HTTP failure status=' + response.status);
  return result.value;
}
function bidi(method, params) {
  return new Promise((resolve, reject) => {
    const id = nextId++;
    const timer = setTimeout(() => { pending.delete(id); reject(new Error('BiDi command timeout: ' + method)); }, 10000);
    pending.set(id, {resolve, reject, timer});
    socket.send(JSON.stringify({id, method, params}));
  });
}
function awaitCompletions(redirect, target) {
  return new Promise(resolve => {
    let timer;
    const finish = value => { clearTimeout(timer); changed.delete(check); resolve(value); };
    const check = () => {
      const completed = evidence.phases.filter(p => p.type === 'network.responseCompleted' && (p.requestPath === redirect || p.requestPath === target));
      if (evidence.evidenceOverflow) return finish(false);
      if (completed.length === 2 && completed.filter(p => p.requestPath === redirect && p.status === 302).length === 1
          && completed.filter(p => p.requestPath === target && p.status === 200).length === 1) finish(true);
    };
    timer = setTimeout(() => finish(false), 3000);
    changed.add(check); check();
  });
}
try {
  const control = await fetch(fixture + '/redirect/0', {redirect: 'manual', signal: AbortSignal.timeout(10000)});
  const controlBody = await control.arrayBuffer();
  evidence.directHttpControl = {status: control.status, locationPath: safePath(new URL(control.headers.get('location') || '', fixture).href), bodyBytes: controlBody.byteLength};
  if (control.status !== 302 || evidence.directHttpControl.locationPath !== '/final/0' || controlBody.byteLength !== 0)
    throw new Error('Direct no-follow HTTP control disagrees with the fixture');
  const chromeOptions = {args: ['--headless=new', '--no-sandbox', '--disable-dev-shm-usage']};
  if (options['chrome-binary']) chromeOptions.binary = path.resolve(options['chrome-binary']);
  const created = await http('POST', driver + '/session', {capabilities: {alwaysMatch: {browserName: 'chrome', webSocketUrl: true, 'goog:chromeOptions': chromeOptions}}});
  session = created.sessionId;
  evidence.browserVersion = created.capabilities.browserVersion;
  evidence.chromeDriverVersion = (created.capabilities.chrome?.chromedriverVersion || '').split(' ')[0];
  if (typeof created.capabilities.webSocketUrl !== 'string') throw new Error('No BiDi WebSocket exposed');
  socket = new WebSocket(created.capabilities.webSocketUrl);
  socket.addEventListener('message', message => {
    let frame;
    try { frame = JSON.parse(String(message.data)); }
    catch { evidence.jsonParseFailures++; return; }
    if (typeof frame.id === 'number' && pending.has(frame.id)) {
      const command = pending.get(frame.id); pending.delete(frame.id); clearTimeout(command.timer);
      if (frame.type === 'error' || frame.error) command.reject(new Error('BiDi command rejected'));
      else command.resolve(frame.result);
    } else if (events.includes(frame.method) && frame.params && typeof frame.params === 'object') record(frame.method, frame.params);
  });
  await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, {once:true}); socket.addEventListener('error', () => reject(new Error('BiDi WebSocket opening failed')), {once:true}); });
  for (const event of events) { await bidi('session.subscribe', {events:[event]}); evidence.subscriptionAcknowledgements.push(event); }
  await http('POST', driver + '/session/' + session + '/timeouts', {script:10000});
  await http('POST', driver + '/session/' + session + '/url', {url:fixture + '/probe'});
  for (let stimulus = 1; stimulus <= 50; stimulus++) {
    const redirect = '/redirect/' + stimulus, target = '/final/' + stimulus;
    const status = await http('POST', driver + '/session/' + session + '/execute/async', {
      script: "const url = arguments[0], done = arguments[arguments.length - 1]; fetch(url, {cache:'no-store'}).then(async response => { await response.text(); done(response.status); }).catch(() => done(-1));",
      args: [fixture + redirect]});
    if (status !== 200) { evidence.failedStimulus = stimulus; throw new Error('Native fetch did not finish with HTTP 200'); }
    if (!await awaitCompletions(redirect, target)) {
      evidence.outcome = 'PROTOCOL_MISMATCH'; evidence.failedStimulus = stimulus;
      throw new Error('Raw JSON BiDi completion mismatch at stimulus ' + stimulus);
    }
    evidence.completedStimuli = stimulus;
  }
  evidence.outcome = 'PASS';
} catch (error) {
  firstError = error;
  if (evidence.outcome === 'INCOMPLETE') evidence.outcome = 'FAILURE';
  const safeMessage = /^(WebDriver HTTP failure status=|BiDi command timeout: |BiDi command rejected|No BiDi WebSocket exposed|BiDi WebSocket opening failed|Native fetch did not finish|Raw JSON BiDi completion mismatch|Direct no-follow HTTP control)/.test(error.message)
    ? error.message : 'Independent HTTP/WebSocket operation failed';
  evidence.failure = {type:error.constructor.name, message:safeMessage};
} finally {
  try {
    const journal = await (await fetch(fixture + '/__journal', {signal:AbortSignal.timeout(5000)})).json();
    evidence.serverJournal = journal.rows;
    evidence.evidenceOverflow ||= journal.overflow;
    for (let stimulus = 1; stimulus <= (evidence.failedStimulus || evidence.completedStimuli || 0); stimulus++) {
      const redirectCount = journal.rows.filter(p => p.path === '/redirect/' + stimulus && p.status === 302).length;
      const targetCount = journal.rows.filter(p => p.path === '/final/' + stimulus && p.status === 200).length;
      if (redirectCount !== 1 || targetCount !== 1) { evidence.serverJournalMismatch = stimulus; evidence.outcome = 'FIXTURE_MISMATCH'; }
    }
  } catch { evidence.journalReadFailed = true; if (evidence.outcome === 'PASS') evidence.outcome = 'CLEANUP_FAILED'; }
  if (session) try { await http('DELETE', driver + '/session/' + session); } catch { evidence.sessionCleanupFailed = true; if (evidence.outcome === 'PASS') evidence.outcome = 'CLEANUP_FAILED'; }
  for (const command of pending.values()) { clearTimeout(command.timer); command.reject(new Error('Diagnostic closing')); }
  pending.clear();
  if (socket && socket.readyState < WebSocket.CLOSING) socket.close();
  try { await fetch(fixture + '/__shutdown', {method:'POST', signal:AbortSignal.timeout(5000)}); } catch { evidence.fixtureCleanupFailed = true; if (evidence.outcome === 'PASS') evidence.outcome = 'CLEANUP_FAILED'; }
  if (evidence.evidenceOverflow) evidence.outcome = 'EVIDENCE_OVERFLOW';
  if (evidence.jsonParseFailures > 0) evidence.outcome = 'RAW_JSON_ERROR';
  evidence.responseCompletedCount = evidence.phases.filter(p => p.type === 'network.responseCompleted').length;
  fs.mkdirSync(path.dirname(output), {recursive:true});
  fs.writeFileSync(output, JSON.stringify(evidence, null, 2));
  console.log(JSON.stringify({outcome:evidence.outcome, failedStimulus:evidence.failedStimulus, completedStimuli:evidence.completedStimuli, phases:evidence.phases.length, output}));
  if (firstError || evidence.outcome !== 'PASS') process.exitCode = 1;
}
