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

test('multiple files retain their order and missing filenames receive a matching extension', async () => {
    const upload = loadHelper();
    const snapshots = await upload.snapshotFiles([transientFile('first.jpg'), transientFile('')]);
    const files = upload.buildFormData(snapshots).getAll('file');
    assert.equal(files.length, 2);
    assert.equal(files[0].name, 'first.jpg');
    assert.match(files[1].name, /^mobile-image-.*-1\.jpg$/);
});

test('cancellation is harmless and empty, unreadable or missing data cannot produce an upload', async () => {
    const upload = loadHelper();
    assert.equal((await upload.snapshotFiles(null)).length, 0);
    const broken = transientFile();
    broken.release();
    await assert.rejects(upload.snapshotFiles([transientFile(), broken]), /사진을 다시 선택/);
    await assert.rejects(upload.snapshotFiles([new File([], 'empty.jpg')]), /사진을 다시 선택/);
    for (const value of [[], [undefined], [new File([], 'empty.jpg')]]) {
        assert.throws(() => upload.buildFormData(value), /이미지를 다시 선택/);
    }
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

    test(`${scenario.script}: unreadable selection makes no request and allows another selection`, async () => {
        const page = loadPage(scenario.script);
        const input = page.element(scenario.input || 'upload');
        const broken = transientFile();
        broken.release();
        input.files = [broken];
        input.value = 'selected-photo';
        await scenario.load(page, input);
        assert.equal(page.calls.length, 0);
        assert.equal(input.disabled, false);
        assert.equal(input.value, '');
        if (scenario.script === 'diary/write.js') {
            assert.match(page.element('diaryMessage').textContent, /사진을 다시 선택/);
        } else {
            assert.match(page.alerts[0], /사진을 다시 선택/);
        }
        input.files = [transientFile()];
        await scenario.load(page, input);
        if (scenario.save) await scenario.save(page);
        assert.ok(page.calls.some(item => item.url.startsWith(scenario.endpoint)));
    });
}

for (const scenario of pageCases.filter(item => ['board/write.js', 'chat/chat.js', 'diary/write.js'].includes(item.script))) {
    test(`${scenario.script}: at most ten files are copied and uploaded in selection order`, async () => {
        const page = loadPage(scenario.script);
        const input = page.element(scenario.input || 'upload');
        input.files = Array.from({ length: 11 }, (_, i) => transientFile(`photo-${i}.jpg`));
        input.files[10].arrayBuffer = () => { throw new Error('file beyond limit must not be read'); };
        await scenario.load(page, input);
        if (scenario.save) await scenario.save(page);
        const call = page.calls.find(item => item.url.startsWith(scenario.endpoint));
        assert.ok(call);
        assert.deepEqual(call.body.getAll('file').map(file => file.name),
            Array.from({ length: 10 }, (_, i) => `photo-${i}.jpg`));
    });
}

test('diary: added batches retain separate copies and removing a preview excludes its file', async () => {
    const page = loadPage('diary/write.js');
    const input = page.element('imageInput');
    const first = transientFile('first.jpg');
    input.files = [first];
    await input.emit('change');
    first.release();
    input.files = [transientFile('second.jpg')];
    await input.emit('change');
    const previews = page.element('imagePreviewList').querySelectorAll('.image-preview-item');
    assert.equal(previews.length, 2);
    await previews[0].querySelector('.image-remove-button').emit('click');
    await page.element('diaryForm').emit('submit');
    const files = page.calls[0].body.getAll('file');
    assert.deepEqual(files.map(file => file.name), ['second.jpg']);
});

test('collection: a failed image re-upload never saves a collection using a previous image URL', async () => {
    const page = loadPage('collection/save.js');
    const input = page.element('upload');
    input.files = [transientFile()];
    await page.context.fnLoad(input);
    await page.context.fnImgSave();
    let writes = 0;
    page.context.fetch = async url => {
        if (url === '/api/collection/save/collection') writes++;
        return { ok: false, json: async () => ({ result: -400, message: 'retry' }) };
    };
    await page.context.fnSave();
    assert.equal(writes, 0);
    assert.equal(page.element('submit').disabled, false);
});
