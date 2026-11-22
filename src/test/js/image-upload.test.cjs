const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const scripts = join(__dirname, '../../main/resources/static/js');
const helper = readFileSync(join(scripts, 'common/image-upload.js'), 'utf8');

function deferred() {
    let resolve;
    const promise = new Promise(yes => { resolve = yes; });
    return { promise, resolve };
}

function transientFile(name = 'photo.jpg', gate = null) {
    let available = true;
    return {
        name, type: 'image/jpeg', size: 3, lastModified: 1234,
        release() { available = false; },
        async arrayBuffer() {
            if (gate) await gate.promise;
            if (!available) throw new Error('picker handle released');
            return Uint8Array.from([255, 216, 217]).buffer;
        },
    };
}

function loadHelper() {
    const context = vm.createContext({ File, Blob, FormData });
    context.window = context;
    vm.runInContext(helper, context);
    return context.ImageUpload;
}

test('snapshot preserves the binary and metadata after the picker reference disappears', async () => {
    const upload = loadHelper();
    const source = transientFile();
    const snapshots = await upload.snapshotFiles([source]);
    source.release();
    const data = upload.buildFormData(snapshots);
    const file = data.get('file');
    assert.equal(file.type, 'image/jpeg');
    assert.equal(file.lastModified, 1234);

    // Exercise actual multipart serialization, not console output of FormData.
    const request = new Request('https://example.test/upload', { method: 'POST', body: data });
    assert.match(request.headers.get('Content-Type'), /multipart\/form-data; boundary=/);
    const received = await request.formData();
    assert.equal(received.get('file').name, 'photo.jpg');
    assert.deepEqual([...new Uint8Array(await received.get('file').arrayBuffer())], [255, 216, 217]);
    assert.notEqual(upload.buildFormData(snapshots), data);
});

// Small DOM/event double for running the actual page scripts without a server.
class Element {
    constructor() {
        this.children = [];
        this.listeners = {};
        this.dataset = {};
        this.style = {};
        this.value = '';
        this.className = '';
        this.innerText = '';
        this.files = [];
        this.disabled = false;
        this.classList = {
            add: (...names) => { this.className = [...new Set([...this.className.split(' '), ...names])].join(' '); },
            remove: (...names) => { this.className = this.className.split(' ').filter(n => !names.includes(n)).join(' '); },
            toggle: (name, force) => {
                const on = force ?? !this.className.split(' ').includes(name);
                this.classList[on ? 'add' : 'remove'](name);
                return on;
            },
        };
    }
    addEventListener(type, listener) { (this.listeners[type] ||= []).push(listener); }
    async emit(type) {
        for (const listener of this.listeners[type] || []) await listener({ preventDefault() {} });
    }
    click() { return this.emit('click'); }
    append(...items) { items.forEach(item => { item.parent = this; this.children.push(item); }); }
    insertBefore(item) { this.append(item); }
    remove() { if (this.parent) this.parent.children = this.parent.children.filter(item => item !== this); }
    setAttribute() {}
    matches(selector) {
        if (selector === 'button[type="submit"]') return this.type === 'submit';
        if (selector === '[data-existing-url]') return !!this.dataset.existingUrl;
        return selector.startsWith('.') && this.className.split(' ').includes(selector.slice(1));
    }
    querySelectorAll(selector) {
        return this.children.flatMap(child => [
            ...(child.matches(selector) ? [child] : []), ...child.querySelectorAll(selector),
        ]);
    }
    querySelector(selector) { return this.querySelectorAll(selector)[0] || null; }
}

function loadPage(path) {
    const nodes = new Map();
    const element = id => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    const docEvents = {};
    const calls = [], alerts = [], messages = [];
    const submit = element('submit');
    submit.type = 'submit';
    element('diaryForm').append(submit);
    element('flag').value = 'false';
    element('room').value = 'room-1';
    const document = {
        body: new Element(),
        getElementById: element,
        querySelector: selector => selector.startsWith('#') ? element(selector.slice(1))
            : ['.container', '.imgGrid', '.imgDiv'].includes(selector) ? element(selector.slice(1)) : null,
        createElement: () => new Element(),
        addEventListener: (type, callback) => { docEvents[type] = callback; },
    };
    const socket = { connected: true, heartbeat: {}, connect() {}, send: (url, headers, body) => messages.push(JSON.parse(body)) };
    const context = vm.createContext({
        File, Blob, FormData, document,
        console: { log() {}, debug() {}, info() {}, error() {} },
        URL: { createObjectURL: () => 'blob:preview', revokeObjectURL() {} },
        alert: message => alerts.push(message),
        location: { reload() {}, replace() {} },
        addEventListener() {},
        SockJS: function() {}, Stomp: { over: () => socket },
        setTimeout() {}, clearTimeout() {},
        fetch: async (url, options) => {
            calls.push({ url, ...options });
            return { ok: true, json: async () => ({ result: 1, imgSrc: '/test.jpg', messageId: 'test', list: ['/test.jpg'], imageUrls: ['/test.jpg'] }) };
        },
    });
    context.window = context;
    vm.runInContext(helper, context);
    vm.runInContext(readFileSync(join(scripts, path), 'utf8'), context, { filename: path });
    if (docEvents.DOMContentLoaded) docEvents.DOMContentLoaded();
    return { context, element, calls, alerts, messages, docEvents };
}

test('diary Cmd/Ctrl+S submits the same form for writing and editing without repeating the save', async () => {
    for (const [modifier, id, endpoint, method] of [
        ['metaKey', '', '/api/diary/write', 'POST'],
        ['ctrlKey', '42', '/api/diary/update', 'PATCH'],
    ]) {
        const page = loadPage('diary/write.js');
        const form = page.element('diaryForm');
        form.dataset.diaryId = id;
        page.element('title').value = '오늘의 일기';
        page.element('content').value = '저장할 본문';
        let submitted;
        let prevented = 0;
        page.element('submit').addEventListener('click', () => { submitted = form.emit('submit'); });
        const shortcut = { key: 's', [modifier]: true, preventDefault: () => prevented++ };

        page.docEvents.keydown({ ...shortcut, shiftKey: true });
        assert.equal(prevented, 0);
        page.docEvents.keydown({ ...shortcut, repeat: true });
        assert.equal(prevented, 1);
        assert.equal(submitted, undefined);

        page.docEvents.keydown(shortcut);
        await submitted;
        assert.equal(prevented, 2);
        assert.equal(page.calls.length, 1);
        assert.equal(page.calls[0].url, endpoint);
        assert.equal(page.calls[0].method, method);
        assert.equal(JSON.parse(page.calls[0].body).title, '오늘의 일기');
        assert.equal(page.context.location.href, '/diary/list');
    }
});

const pageCases = [
    { script: 'board/write.js', endpoint: '/api/boardImg/save', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnImgSave() },
    { script: 'member/detail.js', endpoint: '/api/member/detail/action', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnSave() },
    { script: 'collection/save.js', endpoint: '/api/collection/save/img', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnSave() },
    { script: 'diary/write.js', endpoint: '/api/diary/image/upload', input: 'imageInput', load: (p, input) => input.emit('change'), save: p => p.element('diaryForm').emit('submit') },
    { script: 'chat/chat.js', endpoint: '/api/chat/sendImage?roomId=room-1', load: (p, input) => p.context.fnLoad(input) },
];

for (const scenario of pageCases) {
    test(`${scenario.script}: no request during copying; retained bytes sent with explicit filename afterwards`, async () => {
        const page = loadPage(scenario.script);
        const input = page.element(scenario.input || 'upload');
        const gate = deferred();
        const source = transientFile('mobile.jpg', gate);
        input.files = [source];
        const selection = scenario.load(page, input);
        assert.equal(input.disabled, true);
        if (scenario.save) await scenario.save(page);
        input.files = [transientFile('other.jpg')];
        await scenario.load(page, input);
        assert.equal(page.calls.length, 0);
        gate.resolve();
        await selection;
        source.release();
        input.files = [];
        await scenario.load(page, input);
        if (scenario.save) await scenario.save(page);
        const call = page.calls.find(item => item.url.startsWith(scenario.endpoint));
        assert.ok(call, `expected upload request; alerts=${page.alerts.join(', ')}`);
        assert.equal(call.credentials, 'include');
        assert.equal(call.headers?.['Content-Type'], undefined);
        const files = call.body.getAll('file');
        assert.equal(files.length, 1);
        assert.equal(files[0].name, 'mobile.jpg');
        assert.deepEqual([...new Uint8Array(await files[0].arrayBuffer())], [255, 216, 217]);
        assert.equal(page.alerts.filter(message => /실패|없습니다/.test(message)).length, 0);
        assert.equal(input.disabled, false);
        if (scenario.script === 'chat/chat.js') {
            assert.deepEqual(page.messages[0], {id: 'test', type: 'IMAGE', roomId: 'room-1'});
        }
    });

}
