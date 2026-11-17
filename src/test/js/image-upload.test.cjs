const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const vm = require('node:vm');

const scripts = join(__dirname, '../../main/resources/static/js');
const helper = readFileSync(join(scripts, 'common/image-upload.js'), 'utf8');

function deferred() {
    let resolve, reject;
    const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
    return { promise, resolve, reject };
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
    assert.notEqual(file, source);
    assert.equal(file.name, 'photo.jpg');
    assert.equal(file.type, 'image/jpeg');
    assert.equal(file.lastModified, 1234);
    assert.deepEqual([...new Uint8Array(await file.arrayBuffer())], [255, 216, 217]);

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
    const document = {
        body: new Element(),
        getElementById: element,
        querySelector: selector => selector.startsWith('#') ? element(selector.slice(1))
            : ['.container', '.imgGrid', '.imgDiv'].includes(selector) ? element(selector.slice(1)) : null,
        createElement: () => new Element(),
        addEventListener: (type, callback) => { docEvents[type] = callback; },
    };
    const socket = { heartbeat: {}, connect() {}, send: (url, headers, body) => messages.push(JSON.parse(body)) };
    const context = vm.createContext({
        File, Blob, FormData, document,
        console: { log() {}, debug() {}, info() {}, error() {} },
        URL: { createObjectURL: () => 'blob:preview', revokeObjectURL() {} },
        alert: message => alerts.push(message),
        location: { reload() {}, replace() {} },
        addEventListener() {},
        SockJS: function() {}, Stomp: { over: () => socket },
        fetch: async (url, options) => {
            calls.push({ url, ...options });
            return { ok: true, json: async () => ({ result: 1, imgSrc: '/test.jpg', messageId: 'test', list: ['/test.jpg'], imageUrls: ['/test.jpg'] }) };
        },
    });
    context.window = context;
    vm.runInContext(helper, context);
    vm.runInContext(readFileSync(join(scripts, path), 'utf8'), context, { filename: path });
    if (docEvents.DOMContentLoaded) docEvents.DOMContentLoaded();
    return { context, element, calls, alerts, messages };
}

const pageCases = [
    { script: 'board/write.js', endpoint: '/api/boardImg/save', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnImgSave() },
    { script: 'member/detail.js', endpoint: '/api/member/detail/action', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnSave() },
    { script: 'collection/save.js', endpoint: '/api/collection/save/img', load: (p, input) => p.context.fnLoad(input), save: p => p.context.fnSave() },
    { script: 'diary/write.js', endpoint: '/api/diary/image/upload', input: 'imageInput', load: (p, input) => input.emit('change'), save: p => p.element('diaryForm').emit('submit') },
    { script: 'chat/chat.js', endpoint: '/api/chat/sendImage', load: (p, input) => p.context.fnLoad(input) },
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
        if (scenario.script === 'chat/chat.js') assert.equal(page.messages[0].type, 'IMAGE');
    });

}
