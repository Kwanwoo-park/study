const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const {randomUUID} = require('node:crypto');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/memo/memos.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

class Element {
    constructor() {
        this.children = [];
        this.listeners = {};
        this.attributes = {};
        this.dataset = {};
        this.value = '';
        this.disabled = false;
        this.hidden = false;
        const classes = new Set();
        this.classList = {
            add: name => classes.add(name),
            remove: name => classes.delete(name),
            contains: name => classes.has(name),
            toggle: (name, enabled = !classes.has(name)) => enabled ? classes.add(name) : classes.delete(name),
        };
    }
    setAttribute(name, value) { this.attributes[name] = value; }
    removeAttribute(name) { delete this.attributes[name]; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    focus() {}
    addEventListener(type, listener) { (this.listeners[type] ||= []).push(listener); }
    emit(type) {
        if (type === 'click' && this.disabled) return Promise.resolve();
        return Promise.all((this.listeners[type] || []).map(listener => listener({target: this, preventDefault() {}})));
    }
}

async function setup({empty = false, patchStatus = 200, dropDraftResponse = false} = {}) {
    let now = 100000;
    let timerId = 0;
    const timers = new Map();
    const setTimer = (callback, delay) => { const id = ++timerId; timers.set(id, {callback, at: now + delay}); return id; };
    class TestDate extends Date {
        constructor(...args) { super(...(args.length ? args : [now])); }
        static now() { return now; }
    }
    const nodes = new Map();
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    const colors = ['LAVENDER', 'MINT', 'AMBER', 'ROSE'].map(color => {
        const button = new Element();
        button.dataset.color = color;
        return button;
    });
    const document = new Element();
    document.body = new Element();
    document.getElementById = id => node(id.replace(/^memo-/, ''));
    document.querySelectorAll = selector => selector === '.memo-color' ? colors : node('list').children.filter(card => card.dataset.id);
    document.createElement = document.createElementNS = () => new Element();
    const window = new Element();
    window.confirm = () => true;
    const requests = [];
    let stored = empty ? null : {id: 1, title: '저장된 제목', content: '저장된 본문', color: 'LAVENDER', pinned: false, version: 0, pending: false};
    let lastOperation;
    let holdWrite = false;
    let release;
    const saveStatus = () => ({id: stored.id, version: stored.version, pending: stored.pending, conflicted: false});
    const response = (body, status = 200) => ({status, ok: status >= 200 && status < 300, json: async () => body});
    vm.runInNewContext(script, {
        document, window, URLSearchParams, Date: TestDate, crypto: {randomUUID},
        setTimeout: setTimer, clearTimeout: id => timers.delete(id),
        fetch: async (url, options) => {
            const payload = options.body ? JSON.parse(options.body) : null;
            requests.push({url, options, payload});
            if (url.endsWith('/csrf')) return response({headerName: 'X-CSRF-TOKEN', token: 'test-token'});
            if (options.method) {
                if (options.method === 'PATCH' && patchStatus !== 200) return response({message: '다른 화면에서 변경된 메모입니다'}, patchStatus);
                if (payload.operationKey === lastOperation) return response(url.endsWith('/draft') ? saveStatus() : stored);
                if (options.method !== 'POST' && payload.version !== stored.version) return response({message: '버전 충돌'}, 409);
                const pending = url.endsWith('/draft') || url.includes('draft=true');
                stored = options.method === 'POST' ? {id: 1, ...payload, version: pending ? 1 : 0, pending}
                    : {...stored, ...payload, version: stored.version + 1, pending};
                lastOperation = payload.operationKey;
                if (url.endsWith('/draft') && dropDraftResponse) { dropDraftResponse = false; throw new Error('응답 유실'); }
                const reply = response(url.endsWith('/draft') ? saveStatus() : stored, options.method === 'POST' ? 201 : 200);
                if (holdWrite) { holdWrite = false; return new Promise(resolve => { release = () => resolve(reply); }); }
                return reply;
            }
            if (url.endsWith('/status')) { stored = {...stored, pending: false}; return response(saveStatus()); }
            if (!url.includes('?')) return response(stored);
            const parameters = new URLSearchParams(url.split('?')[1]);
            const visible = stored && (parameters.get('pinnedOnly') !== 'true' || stored.pinned);
            return response({content: visible ? [{...stored, preview: stored.content}] : [], page: 0,
                totalPages: visible ? 1 : 0, totalCount: stored ? 1 : 0, pinnedCount: stored?.pinned ? 1 : 0, matchedCount: visible ? 1 : 0});
        },
    });
    await flush();
    if (!empty) await node('list').children[0].emit('click');
    async function advance(milliseconds) {
        const end = now + milliseconds;
        while (true) {
            const next = [...timers].filter(([, timer]) => timer.at <= end).sort((a, b) => a[1].at - b[1].at)[0];
            if (!next) break;
            timers.delete(next[0]);
            now = next[1].at;
            await next[1].callback();
            await flush();
        }
        now = end;
    }
    return {node, colors, requests, advance, holdNext: () => { holdWrite = true; }, release: () => release(),
        stored: () => stored, writes: () => requests.filter(request => request.options.method)};
}

test('pin and color save immediately without saving a text draft; manual save uses the latest version', async () => {
    const ui = await setup();
    await ui.node('pin').emit('click');
    assert.deepEqual({...ui.writes()[0].payload, operationKey: undefined}, {color: 'LAVENDER', pinned: true, version: 0, operationKey: undefined});
    assert.equal(ui.writes()[0].url, '/api/memos/1/settings');
    assert.equal(ui.writes()[0].options.method, 'PATCH');
    assert.equal(ui.writes()[0].options.headers['X-CSRF-TOKEN'], 'test-token');
    assert.equal(ui.node('save').disabled, true);

    ui.node('title').value = '작성 중인 제목';
    ui.node('content').value = '아직 저장하지 않은 본문';
    await ui.node('content').emit('input');
    await ui.colors[1].emit('click');
    assert.deepEqual({...ui.writes()[1].payload, operationKey: undefined}, {color: 'MINT', pinned: true, version: 1, operationKey: undefined});
    assert.equal(ui.stored().content, '저장된 본문');
    assert.equal(ui.node('title').value, '작성 중인 제목');
    assert.equal(ui.node('content').value, '아직 저장하지 않은 본문');
    assert.equal(ui.node('save').disabled, false);
    assert.equal(ui.node('edit-dot').classList.contains('is-dirty'), true);
    assert.equal(ui.node('list').children[0].dataset.color, 'MINT');
    await ui.colors[1].emit('click');
    assert.equal(ui.writes().length, 2);

    await ui.node('form').emit('submit');
    assert.equal(ui.writes()[2].options.method, 'PUT');
    assert.equal(ui.writes()[2].payload.version, 2);
    assert.equal(ui.stored().content, '아직 저장하지 않은 본문');
    assert.equal(ui.node('save').disabled, true);
});

test('failed settings save restores controls and keeps the text draft', async () => {
    const ui = await setup({patchStatus: 409});
    ui.node('content').value = '보존할 본문';
    await ui.node('content').emit('input');

    await ui.node('pin').emit('click');

    assert.equal(ui.writes().length, 1);
    assert.equal(ui.stored().pinned, false);
    assert.equal(ui.node('pin').attributes['aria-pressed'], 'false');
    assert.equal(ui.node('content').value, '보존할 본문');
    assert.equal(ui.node('save').disabled, true);
    assert.equal(ui.node('reload').hidden, false);
    assert.match(ui.node('status').textContent, /다른 화면/);
    await ui.advance(60000);
    assert.equal(ui.writes().length, 1);
});

test('changing a new empty memo color creates it once and later pin changes use PATCH', async () => {
    const ui = await setup({empty: true});
    await ui.node('new').emit('click');
    const saving = ui.colors[1].emit('click');
    assert.equal(ui.node('pin').disabled, true);
    await ui.node('pin').emit('click');
    await saving;

    assert.equal(ui.writes().length, 1);
    assert.equal(ui.writes()[0].options.method, 'POST');
    assert.equal(ui.stored().title, '제목 없는 메모');
    assert.equal(ui.stored().content, '');
    assert.equal(ui.stored().color, 'MINT');
    assert.equal(ui.node('save').disabled, true);

    await ui.node('pin').emit('click');
    assert.equal(ui.writes()[1].options.method, 'PATCH');
    assert.equal(ui.stored().pinned, true);
});

test('body debounces for one second, sends only content, and checks DB completion without reloading the list', async () => {
    const ui = await setup();
    ui.node('content').value = '자동 저장할 본문';
    await ui.node('content').emit('input');
    await ui.advance(999);
    assert.equal(ui.writes().length, 0);
    await ui.advance(1);
    assert.equal(ui.writes()[0].url, '/api/memos/1/draft');
    assert.equal(ui.writes()[0].payload.content, '자동 저장할 본문');
    assert.equal('color' in ui.writes()[0].payload, false);
    assert.equal(ui.stored().pending, true);
    assert.match(ui.node('save-state').textContent, /임시 저장됨/);
    assert.equal(ui.requests.filter(request => request.url.includes('?query=')).length, 1);
    await ui.advance(11000);
    assert.match(ui.node('save-state').textContent, /DB 저장 완료/);
    assert.equal(ui.requests.filter(request => request.url.includes('?query=')).length, 1);
});

test('continuous typing sends the latest draft by five seconds and respects IME composition', async () => {
    const ui = await setup();
    for (let index = 0; index < 6; index++) {
        ui.node('content').value += String(index);
        await ui.node('content').emit('input');
        await ui.advance(900);
    }
    assert.equal(ui.writes().length, 1);
    await ui.node('content').emit('compositionstart');
    ui.node('content').value += '한';
    await ui.node('content').emit('input');
    await ui.advance(5000);
    assert.equal(ui.writes().length, 1);
    await ui.node('content').emit('compositionend');
    await ui.advance(1000);
    assert.equal(ui.writes().length, 2);
    assert.equal(ui.stored().content.endsWith('한'), true);
});

test('an in-flight save never disables typing or overwrites newer text', async () => {
    const ui = await setup();
    ui.node('content').value = '전송할 본문';
    await ui.node('content').emit('input');
    ui.holdNext();
    const saving = ui.node('form').emit('submit');
    await flush();
    assert.equal(ui.node('content').disabled, false);
    ui.node('content').value = '저장 중 추가한 본문';
    await ui.node('content').emit('input');
    ui.release();
    await saving;
    assert.equal(ui.node('content').value, '저장 중 추가한 본문');
    await ui.advance(1000);
    assert.equal(ui.writes()[1].payload.version, 1);
    assert.equal(ui.stored().content, '저장 중 추가한 본문');
});

test('a lost acknowledgement retries the same operation before sending newer input', async () => {
    const ui = await setup({dropDraftResponse: true});
    ui.node('content').value = '첫 전송';
    await ui.node('content').emit('input');
    await ui.advance(1000);
    ui.node('content').value = '더 최신 입력';
    await ui.node('content').emit('input');
    await ui.advance(1000);
    assert.equal(ui.writes()[0].payload.operationKey, ui.writes()[1].payload.operationKey);
    assert.equal(ui.writes()[1].payload.content, '첫 전송');
    await ui.advance(1000);
    assert.equal(ui.stored().content, '더 최신 입력');
    assert.equal(ui.stored().version, 2);
});
