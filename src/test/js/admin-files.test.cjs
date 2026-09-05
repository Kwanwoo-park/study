const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/admin/files.js'), 'utf8');

class Element {
    constructor() {
        this.children = [];
        this.listeners = {};
        this.files = [];
        this.textContent = '';
        this.classList = {toggle() {}};
    }
    addEventListener(type, listener) { this.listeners[type] = listener; }
    emit(type) { return this.listeners[type]({preventDefault() {}}); }
    append(...children) { this.children.push(...children); }
    replaceChildren() { this.children = []; }
    setAttribute(name, value) { this[name] = value; }
    set innerHTML(value) { throw new Error('Untrusted filenames must never be inserted as HTML'); }
}

const flush = () => new Promise(resolve => setImmediate(resolve));

async function loadPage({status = 200, content = [], uploadStatus = 201} = {}) {
    const nodes = new Map();
    const requests = [];
    const element = id => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    class Request {
        constructor() { this.headers = {}; this.events = {}; this.upload = {addEventListener() {}}; requests.push(this); }
        open(method, url) { this.method = method; this.url = url; }
        setRequestHeader(name, value) { this.headers[name] = value; }
        addEventListener(type, listener) { this.events[type] = listener; }
        send(data) { this.data = data; this.status = uploadStatus; this.response = {}; this.events.load(); }
    }
    const context = vm.createContext({
        document: {getElementById: element, createElement: () => new Element()},
        FormData,
        XMLHttpRequest: Request,
        fetch: async url => ({
            ok: status === 200, status,
            json: async () => url.endsWith('/csrf')
                ? {headerName: 'X-XSRF-TOKEN', token: 'masked-token', maxFileSize: 100}
                : {content, number: 0, totalElements: content.length, totalPages: 1, first: true, last: true},
        }),
    });
    vm.runInContext(script, context);
    await flush();
    return {element, requests};
}

test('renders filenames as text and uses only the authorized download route', async () => {
    const filename = '<img src=x onerror=alert(1)>.exe';
    const {element} = await loadPage({content: [{id: 'file-id', originalFilename: filename, size: 4, createdAt: '2026-09-05T12:00:00', uploadedBy: 7}]});
    const row = element('admin-file-list').children[0];
    assert.equal(row.children[0].children[0].textContent, filename);
    assert.equal(row.children[1].href, '/api/admin/files/file-id/download');
    assert.equal(element('admin-file-prev').disabled, true);
    assert.equal(element('admin-file-next').disabled, true);
});

test('uploads executable binary as multipart with the CSRF header', async () => {
    const {element, requests} = await loadPage();
    const file = new File([Uint8Array.from([77, 90, 0, 255])], 'setup.exe', {type: 'application/octet-stream'});
    element('admin-file-input').files = [file];
    await element('admin-file-input').emit('change');
    assert.equal(element('admin-file-upload').disabled, false);
    await element('admin-file-form').emit('submit');
    assert.equal(requests.length, 1);
    assert.equal(requests[0].url, '/api/admin/files');
    assert.equal(requests[0].method, 'POST');
    assert.equal(requests[0].headers['X-XSRF-TOKEN'], 'masked-token');
    assert.equal(requests[0].headers['Content-Type'], undefined);
    assert.equal(requests[0].data.get('file').name, 'setup.exe');
    assert.deepEqual([...new Uint8Array(await requests[0].data.get('file').arrayBuffer())], [77, 90, 0, 255]);
    assert.match(element('admin-file-status').textContent, /보관했습니다/);
});

test('blocks oversized files before any upload', async () => {
    const {element, requests} = await loadPage();
    element('admin-file-input').files = [new File([new Uint8Array(101)], 'large.zip')];
    await element('admin-file-input').emit('change');
    assert.equal(element('admin-file-upload').disabled, true);
    await element('admin-file-form').emit('submit');
    assert.equal(requests.length, 0);
});

test('server rejection restores controls and does not report success', async () => {
    const {element} = await loadPage({uploadStatus: 413});
    element('admin-file-input').files = [new File(['test'], 'archive.zip')];
    await element('admin-file-form').emit('submit');
    assert.equal(element('admin-file-input').disabled, false);
    assert.equal(element('admin-file-progress').hidden, true);
    assert.match(element('admin-file-status').textContent, /용량 제한/);
});

for (const status of [401, 403]) {
    test(`displays authorization error for HTTP ${status}`, async () => {
        const {element, requests} = await loadPage({status});
        assert.match(element('admin-file-status').textContent, /로그인|관리자 권한/);
        assert.equal(requests.length, 0);
    });
}
