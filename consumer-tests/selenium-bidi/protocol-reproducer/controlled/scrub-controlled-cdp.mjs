import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';

// Parse the actual ChromeDriver DevTools trace, independently of Selenium's JSON model.
const methods = new Set(['requestWillBeSent', 'requestWillBeSentExtraInfo', 'responseReceived',
  'responseReceivedExtraInfo', 'loadingFinished', 'loadingFailed'].map(name => 'Network.' + name));
const rowLimit = 1000;
const blockLimit = 8 * 1024 * 1024;
const options = {};
let invalidOptions = false;
for (let i = 2; i < process.argv.length; i += 2) {
  const key = process.argv[i];
  if (!['--input', '--output'].includes(key) || !process.argv[i + 1]) invalidOptions = true;
  else options[key.slice(2)] = process.argv[i + 1];
}
const evidence = {outcome: 'NOT_RUN', missingInput: false, parseFailures: 0, truncated: false,
  rowLimit, counts: {blocks: 0, networkEvents: 0, matchedRequests: 0, matchedEvents: 0, rows: 0}, rows: []};

function safeUrl(value) {
  if (typeof value !== 'string') return undefined;
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol)
      && !url.username && !url.password && !value.includes('#')
      && ['localhost', '127.0.0.1', 'staging.example.org'].includes(url.hostname)
      && ['/ok', '/redirect', '/json'].includes(url.pathname)
      && /^\?caseId=(page|json|[1-9]|[1-4][0-9]|50)$/.test(url.search) ? url.href : undefined;
  } catch { return undefined; }
}

// Retain only Location, never a general header object or an arbitrary value.
function safeLocation(headers, base) {
  if (!headers || typeof headers !== 'object' || !base) return undefined;
  const entry = Object.entries(headers).find(([name]) => name.toLowerCase() === 'location');
  if (!entry || typeof entry[1] !== 'string') return undefined;
  try { return safeUrl(new URL(entry[1], base).href); }
  catch { return undefined; }
}

// Scan balanced JSON, respecting braces inside strings. JSON.parse decides validity.
function* blocks(source) {
  const marker = /DevTools WebSocket Event: (Network\.(?:requestWillBeSent(?:ExtraInfo)?|responseReceived(?:ExtraInfo)?|loadingFinished|loadingFailed)|Runtime\.bindingCalled)\b[^\r\n]*/g;
  let match;
  while ((match = marker.exec(source))) {
    const lineEnd = source.indexOf('\n', match.index);
    let start = source.indexOf('{', match.index + match[0].indexOf(match[1]) + match[1].length);
    // A payload may start on the next line, but never consume another log record.
    const nextRecord = source.indexOf('DevTools WebSocket Event:', match.index + match[0].length);
    const nextLogLine = source.slice(lineEnd < 0 ? source.length : lineEnd + 1).search(/^\[/m);
    const boundary = Math.min(nextRecord < 0 ? source.length : nextRecord,
      nextLogLine < 0 ? source.length : (lineEnd + 1 + nextLogLine));
    if (start < 0 || start >= boundary) { yield {failure: true}; continue; }
    let depth = 0, quoted = false, escaped = false, end = -1;
    for (let i = start; i < Math.min(boundary, start + blockLimit); i++) {
      const char = source[i];
      if (quoted) {
        if (escaped) escaped = false;
        else if (char === '\\') escaped = true;
        else if (char === '"') quoted = false;
      } else if (char === '"') quoted = true;
      else if (char === '{' || char === '[') depth++;
      else if (char === '}' || char === ']') { if (--depth === 0) { end = i + 1; break; } }
    }
    if (end < 0) { yield {failure: true}; continue; }
    marker.lastIndex = end;
    try { yield {method: match[1], params: JSON.parse(source.slice(start, end))}; }
    catch { yield {failure: true}; }
  }
}

function* events(source, count) {
  for (const block of blocks(source)) {
    if (count) evidence.counts.blocks++;
    if (block.failure) { if (count) evidence.parseFailures++; continue; }
    if (methods.has(block.method)) { yield block; continue; }
    // ChromeDriver can expand the binding's JSON string into a payload object.
    // Only exact known Network methods qualify, including payload.messages entries.
    let payload = block.params?.payload;
    if (typeof payload === 'string') {
      try { payload = JSON.parse(payload); }
      catch { if (count) evidence.parseFailures++; continue; }
    }
    const pending = [{value: payload, depth: 0}];
    let visited = 0;
    while (pending.length && visited++ < 1000) {
      const {value, depth} = pending.pop();
      if (!value || typeof value !== 'object') continue;
      if (methods.has(value.method) && value.params && typeof value.params === 'object') {
        yield {method: value.method, params: value.params};
        continue;
      }
      const children = Object.values(value).filter(child => child && typeof child === 'object');
      if (depth < 8) for (const child of children.reverse()) pending.push({value: child, depth: depth + 1});
      else if (children.length && count) evidence.truncated = true;
    }
    if (pending.length && count) evidence.truncated = true;
  }
}

try {
  if (invalidOptions || !options.input || !options.output) throw new Error('options');
  let raw;
  try { raw = fs.readFileSync(options.input); }
  catch { evidence.missingInput = true; throw new Error('input'); }
  evidence.sourceSha256 = createHash('sha256').update(raw).digest('hex');
  const source = raw.toString('utf8');
  const ids = new Map();
  const knownUrls = new Map();
  for (const {params} of events(source, true)) {
    evidence.counts.networkEvents++;
    if (typeof params?.requestId === 'string'
        && [params.request?.url, params.response?.url, params.redirectResponse?.url].some(safeUrl)) {
      if (!ids.has(params.requestId)) ids.set(params.requestId, 'request-' + (ids.size + 1));
      if (!knownUrls.has(params.requestId)) knownUrls.set(params.requestId,
        [params.request?.url, params.response?.url, params.redirectResponse?.url].map(safeUrl).find(Boolean));
    }
  }
  evidence.counts.matchedRequests = ids.size;
  let index = 0;
  const currentUrls = new Map();
  for (const {method, params} of events(source, false)) {
    const eventIndex = index++;
    if (!ids.has(params?.requestId)) continue;
    evidence.counts.matchedEvents++;
    if (evidence.rows.length >= rowLimit) { evidence.truncated = true; continue; }
    const row = {method, index: eventIndex, requestId: ids.get(params.requestId)};
    for (const [key, value] of [['timestamp', params.timestamp],
      ['requestTime', params.requestTime ?? params.connectTiming?.requestTime ?? params.response?.timing?.requestTime],
      ['encodedDataLength', params.encodedDataLength], ['encodedBodyLength', params.encodedBodyLength]]) {
      if (typeof value === 'number' && Number.isFinite(value)) row[key] = value;
    }
    for (const [key, value] of [['requestUrl', params.request?.url], ['responseUrl', params.response?.url],
      ['redirectUrl', params.redirectResponse?.url]]) {
      const safe = safeUrl(value);
      if (safe !== undefined) row[key] = safe;
    }
    const explicitUrl = row.requestUrl || row.responseUrl;
    if (explicitUrl) currentUrls.set(params.requestId, explicitUrl);
    const base = explicitUrl || currentUrls.get(params.requestId) || knownUrls.get(params.requestId);
    for (const [key, headers, url] of [['location', params.response?.headers ?? params.headers, base],
      ['redirectLocation', params.redirectResponse?.headers, row.redirectUrl || base]]) {
      const safe = safeLocation(headers, url);
      if (safe !== undefined) row[key] = safe;
    }
    for (const [key, value] of [['status', params.response?.status ?? params.statusCode],
      ['redirectStatus', params.redirectResponse?.status]]) {
      if (typeof value === 'number' && Number.isFinite(value)) row[key] = value;
    }
    for (const [key, value] of [['hasExtraInfo', params.hasExtraInfo],
      ['redirectHasExtraInfo', params.redirectHasExtraInfo], ['connectionReused', params.response?.connectionReused]]) {
      if (typeof value === 'boolean') row[key] = value;
    }
    evidence.rows.push(row);
  }
  evidence.counts.rows = evidence.rows.length;
  evidence.outcome = evidence.parseFailures || evidence.truncated ? 'PARTIAL'
    : evidence.counts.rows === 0 ? 'NO_MATCHED_EVENTS' : 'PARSED';
} catch {
  // Deliberately never serialize source errors, paths, payloads or exception messages.
  process.exitCode = 1;
}
if (options.output) {
  try {
    fs.mkdirSync(path.dirname(path.resolve(options.output)), {recursive: true});
    fs.writeFileSync(options.output, JSON.stringify(evidence, null, 2) + '\n');
  } catch { process.exitCode = 1; }
} else process.exitCode = 1;
