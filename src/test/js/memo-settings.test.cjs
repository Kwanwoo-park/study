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

async function setup({empty = false, patchStatus = 200, dropDraftResponse = false, csrfFailures = 0, csrfRefreshStatus = 200} = {}) {
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
    let csrfRequests = 0;
    const saveStatus = () => ({id: stored.id, version: stored.version, pending: stored.pending, conflicted: false});
    const response = (body, status = 200) => ({status, ok: status >= 200 && status < 300, json: async () => body});
    vm.runInNewContext(script, {
        document, window, URLSearchParams, Date: TestDate, crypto: {randomUUID},
        setTimeout: setTimer, clearTimeout: id => timers.delete(id),
        fetch: async (url, options) => {
            const payload = options.body ? JSON.parse(options.body) : null;
            requests.push({url, options, payload});
            if (url.endsWith('/csrf')) {
                csrfRequests++;
                if (csrfRequests > 1 && csrfRefreshStatus !== 200) return response({}, csrfRefreshStatus);
                return response({headerName: 'X-CSRF-TOKEN', token: csrfRequests === 1 ? 'test-token' : `test-token-${csrfRequests}`});
            }
            if (options.method) {
                if (csrfFailures > 0) { csrfFailures--; return response({code: 'CSRF_INVALID'}, 403); }
                if (options.method === 'PATCH' && patchStatus !== 200) return response({code: patchStatus === 403 ? 'FORBIDDEN' : undefined, message: '다른 화면에서 변경된 메모입니다'}, patchStatus);
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
            const visible = stored && (parameters.get('pinnedOnly') !== 'true' || stored.pinned)
                && (!parameters.get('color') || parameters.get('color') === stored.color);
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

test('color filtering combines with search and pin filters, resets paging, and preserves the editor without writes', async () => {
    const ui = await setup();
    ui.node('content').value = '편집 중인 본문';
    await ui.node('content').emit('input');
    ui.node('list-color').value = 'MINT';
    await ui.node('list-color').emit('change');
    let params = new URLSearchParams(ui.requests.at(-1).url.split('?')[1]);
    assert.equal(params.get('color'), 'MINT');
    assert.equal(params.get('page'), '0');
    assert.equal(ui.node('list').children.filter(card => card.dataset.id).length, 0);
    assert.equal(ui.node('content').value, '편집 중인 본문');
    assert.equal(ui.writes().length, 0);

    await ui.node('filter-pinned').emit('click');
    await flush();
    ui.node('search').value = '검색어';
    await ui.node('search').emit('input');
    await ui.advance(250);
    params = new URLSearchParams(ui.requests.at(-1).url.split('?')[1]);
    assert.equal(params.get('color'), 'MINT');
    assert.equal(params.get('query'), '검색어');
    assert.equal(params.get('pinnedOnly'), 'true');

    ui.node('list-color').value = '';
    await ui.node('list-color').emit('change');
    assert.equal(new URLSearchParams(ui.requests.at(-1).url.split('?')[1]).has('color'), false);
    assert.equal(ui.node('content').value, '편집 중인 본문');
    assert.equal(ui.writes().length, 0);
});

test('recoloring a memo removes its card from the active color filter but keeps the editor open', async () => {
    const ui = await setup();
    ui.node('list-color').value = 'LAVENDER';
    await ui.node('list-color').emit('change');
    assert.equal(ui.node('list').children.filter(card => card.dataset.id).length, 1);
    await ui.colors[1].emit('click');
    await flush();
    assert.equal(ui.stored().color, 'MINT');
    assert.equal(ui.node('list').children.filter(card => card.dataset.id).length, 0);
    assert.equal(ui.node('form').hidden, false);
    assert.equal(ui.node('content').value, '저장된 본문');
    ui.node('list-color').value = 'MINT';
    await ui.node('list-color').emit('change');
    assert.equal(ui.node('list').children[0].dataset.color, 'MINT');
    await ui.node('new').emit('click');
    assert.equal(ui.colors[1].attributes['aria-pressed'], 'true');
    assert.equal(ui.writes().length, 1);
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

test('autosave distinguishes temporary and completed saves using plain language without reloading the list', async () => {
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
    assert.equal(ui.node('save-state').textContent, '임시 저장됨');
    assert.equal(ui.node('status').textContent, '내용을 임시로 저장했어요. 저장을 마무리하고 있어요.');
    assert.equal(ui.node('save-label').textContent, '지금 저장');
    assert.equal(ui.requests.filter(request => request.url.includes('?query=')).length, 1);
    await ui.advance(11000);
    assert.equal(ui.node('save-state').textContent, '저장 완료');
    assert.equal(ui.node('status').textContent, '메모를 저장했어요.');
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

test('CSRF recovery retries the same create or draft operation once and continues autosaving', async () => {
    for (const empty of [false, true]) {
        const ui = await setup({empty, csrfFailures: 1});
        if (empty) await ui.node('new').emit('click');
        ui.node('content').value = '인증 갱신 중에도 보존할 본문';
        await ui.node('content').emit('input');
        await ui.advance(1000);
        assert.equal(ui.writes().length, 2);
        const [first, retried] = ui.writes();
        assert.equal(first.url, retried.url);
        assert.equal(first.options.method, retried.options.method);
        assert.equal(first.options.body, retried.options.body);
        assert.equal(first.options.headers['X-CSRF-TOKEN'], 'test-token');
        assert.equal(retried.options.headers['X-CSRF-TOKEN'], 'test-token-2');
        assert.equal(ui.stored().content, '인증 갱신 중에도 보존할 본문');
        assert.equal(ui.stored().version, 1);
        ui.node('content').value += ' · 다음 변경';
        await ui.node('content').emit('input');
        await ui.advance(1000);
        assert.equal(ui.writes().length, 3);
        assert.equal(ui.stored().version, 2);
        assert.equal(ui.stored().content, ui.node('content').value);
    }
});

test('CSRF recovery stops after one unsuccessful retry and preserves unsaved input', async () => {
    const ui = await setup({csrfFailures: 3});
    ui.node('content').value = '보존해야 할 본문';
    await ui.node('content').emit('input');
    await ui.advance(1000);
    assert.equal(ui.writes().length, 2);
    assert.equal(ui.requests.filter(request => request.url.endsWith('/csrf')).length, 2);
    assert.equal(ui.node('content').value, '보존해야 할 본문');
    assert.equal(ui.stored().content, '저장된 본문');
    assert.equal(ui.node('save').disabled, true);
    await ui.advance(60000);
    assert.equal(ui.writes().length, 2);
});

test('permission denial and authentication expiry never trigger a CSRF retry', async () => {
    for (const patchStatus of [403, 401]) {
        const ui = await setup({patchStatus});
        ui.node('content').value = '권한 오류 시에도 유지할 내용';
        await ui.node('content').emit('input');
        await ui.advance(1000);
        assert.equal(ui.writes().length, 1);
        assert.equal(ui.requests.filter(request => request.url.endsWith('/csrf')).length, 1);
        assert.equal(ui.node('content').value, '권한 오류 시에도 유지할 내용');
        assert.match(ui.node('status').textContent, patchStatus === 401 ? /로그인/ : /권한/);
        await ui.advance(60000);
        assert.equal(ui.writes().length, 1);
    }
});

test('expired login during CSRF renewal stops the write and retains the draft', async () => {
    const ui = await setup({csrfFailures: 1, csrfRefreshStatus: 401});
    ui.node('content').value = '다시 로그인하기 전에 복사할 내용';
    await ui.node('content').emit('input');
    await ui.advance(1000);
    assert.equal(ui.writes().length, 1);
    assert.equal(ui.requests.filter(request => request.url.endsWith('/csrf')).length, 2);
    assert.equal(ui.node('content').value, '다시 로그인하기 전에 복사할 내용');
    assert.match(ui.node('status').textContent, /로그인/);
    await ui.advance(60000);
    assert.equal(ui.writes().length, 1);
});
