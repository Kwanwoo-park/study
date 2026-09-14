const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/todo/list.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

class Element {
    constructor(tag = 'div') {
        this.tagName = tag;
        this.children = [];
        this.listeners = {};
        this.dataset = {};
        this.attributes = {};
        this.value = '';
        this.disabled = false;
        this.className = '';
        this.classList = {
            contains: name => this.className.split(' ').includes(name),
            add: name => { if (!this.classList.contains(name)) this.className += ` ${name}`; },
            remove: name => { this.className = this.className.split(' ').filter(part => part !== name).join(' '); },
            toggle: (name, enabled) => {
                if (enabled ?? !this.classList.contains(name)) this.classList.add(name);
                else this.classList.remove(name);
            },
        };
    }
    set innerHTML(value) { throw new Error('Never render todo content as HTML'); }
    set textContent(value) { this.text = value; this.replaceChildren(); }
    get textContent() { return this.text ?? this.children.map(child => child.textContent).join(''); }
    append(...children) { children.forEach(child => { child.parent = this; this.children.push(child); }); }
    replaceChildren(...children) {
        this.children.forEach(child => { child.parent = null; });
        this.children = [];
        this.append(...children);
    }
    replaceWith(child) {
        this.parent.children[this.parent.children.indexOf(this)] = child;
        child.parent = this.parent;
        this.parent = null;
    }
    querySelectorAll(selector) {
        const tags = selector.split(',').map(tag => tag.trim());
        return this.children.flatMap(child => [child, ...child.querySelectorAll('*')])
            .filter(child => selector === '*' || tags.includes(child.tagName));
    }
    setAttribute(name, value) { this.attributes[name] = value; }
    focus() { this.focused = true; }
    addEventListener(type, listener) { (this.listeners[type] ||= []).push(listener); }
    emit(type, extra = {}) {
        if (type === 'click' && this.disabled) return;
        for (const listener of this.listeners[type] || []) listener({target: this, preventDefault() {}, ...extra});
    }
}

const todo = (id = 1, content = '할 일', completed = false) => ({id, content, completed});
const page = (list = [], totalPages = list.length ? 1 : 0, totalElements = list.length) => ({list, totalPages, totalElements});

async function setup(reply = async () => ({body: page()})) {
    const document = new Element();
    const window = new Element();
    window.confirm = () => true;
    const nodes = new Map();
    const tags = {todoCreateForm: 'form', todoContent: 'input', todoList: 'ul', todoPrevious: 'button', todoNext: 'button', todoRetry: 'button'};
    const ids = ['todoPage', 'todoCreateForm', 'todoContent', 'todoList', 'todoMessage', 'todoEmpty',
        'todoCount', 'todoPrevious', 'todoNext', 'todoPageNumber', 'todoRetry'];
    ids.forEach(id => nodes.set(id, new Element(tags[id])));
    const filters = ['', 'false', 'true'].map(completed => {
        const button = new Element('button');
        button.dataset.completed = completed;
        return button;
    });
    const root = nodes.get('todoPage');
    root.append(...ids.filter(id => id !== 'todoPage').map(id => nodes.get(id)), ...filters);
    document.getElementById = id => nodes.get(id);
    document.querySelectorAll = () => filters;
    document.createElement = tag => new Element(tag);
    const requests = [];
    vm.runInNewContext(script, {
        document, window, URLSearchParams, AbortController,
        fetch: async (url, options) => {
            requests.push({url, options});
            const response = await reply(url, options);
            const status = response.status || 200;
            return {status, ok: status >= 200 && status < 300, json: async () => response.body};
        },
    });
    document.emit('DOMContentLoaded');
    await flush();
    return {nodes, root, window, filters, requests, rows: () => nodes.get('todoList').children};
}

test('loads private paged list and renders content as text with correct completion controls', async () => {
    const content = '<img src=x onerror=alert(1)>';
    const ui = await setup(async () => ({body: page([todo(1, content), todo(2, '완료', true)])}));
    assert.equal(ui.requests[0].url, '/api/todo?page=0&size=20');
    assert.equal(ui.requests[0].options.credentials, 'include');
    assert.equal(ui.requests[0].options.cache, 'no-store');
    assert.equal(ui.rows()[0].children[1].textContent, content);
    assert.equal(ui.rows()[1].children[0].checked, true);
    assert.equal(ui.rows()[1].classList.contains('is-completed'), true);
    assert.equal(ui.nodes.get('todoPrevious').disabled, true);
    assert.equal(ui.nodes.get('todoNext').disabled, true);
    assert.equal(ui.nodes.get('todoCount').textContent, '2개');
});

test('creates content without a diary or member ID and clears input only after success', async () => {
    const ui = await setup(async (url, options) => ({body: options.method ? {result: 1} : page()}));
    ui.nodes.get('todoContent').value = '  새 할 일  ';
    ui.nodes.get('todoCreateForm').emit('submit');
    await flush();
    assert.deepEqual(JSON.parse(ui.requests[1].options.body), {content: '새 할 일'});
    assert.equal(ui.requests[1].options.method, 'POST');
    assert.equal(ui.nodes.get('todoContent').value, '');
    assert.match(ui.nodes.get('todoMessage').textContent, /추가했습니다/);
    assert.equal(ui.root.attributes['aria-busy'], 'false');
});

test('rejects blank content locally and preserves content when creation fails', async () => {
    const ui = await setup(async (url, options) => options.method
        ? {status: 400, body: {message: '저장 실패'}} : {body: page()});
    ui.nodes.get('todoContent').value = '  ';
    ui.nodes.get('todoCreateForm').emit('submit');
    assert.equal(ui.requests.length, 1);
    ui.nodes.get('todoContent').value = '보존할 내용';
    ui.nodes.get('todoCreateForm').emit('submit');
    await flush();
    assert.equal(ui.nodes.get('todoContent').value, '보존할 내용');
    assert.equal(ui.nodes.get('todoMessage').textContent, '저장 실패');
    assert.equal(ui.nodes.get('todoContent').disabled, false);
});

test('failed completion changes leave the original checkbox state intact', async () => {
    const ui = await setup(async (url, options) => options.method
        ? {status: 500, body: {message: '잠시 후 다시 시도'}} : {body: page([todo()])});
    const checkbox = ui.rows()[0].children[0];
    checkbox.checked = true;
    checkbox.emit('change');
    await flush();
    assert.equal(checkbox.checked, false);
    assert.equal(ui.requests[1].url, '/api/todo/1/completion');
    assert.deepEqual(JSON.parse(ui.requests[1].options.body), {completed: true});
    assert.equal(checkbox.disabled, false);
});

test('completed items can be explicitly unchecked', async () => {
    const ui = await setup(async (url, options) => ({body: options.method ? {result: 1} : page([todo(1, '할 일', true)])}));
    ui.rows()[0].children[0].emit('change');
    await flush();
    assert.deepEqual(JSON.parse(ui.requests[1].options.body), {completed: false});
});

test('inline edit saves only content and cancel restores the row', async () => {
    const ui = await setup(async (url, options) => ({body: options.method ? {result: 1} : page([todo()])}));
    ui.rows()[0].children[2].children[0].emit('click');
    let edit = ui.rows()[0].children[0];
    edit.children[0].value = '수정 내용';
    edit.emit('submit');
    await flush();
    assert.equal(ui.requests[1].url, '/api/todo/1');
    assert.equal(ui.requests[1].options.method, 'PATCH');
    assert.deepEqual(JSON.parse(ui.requests[1].options.body), {content: '수정 내용'});
    ui.rows()[0].children[2].children[0].emit('click');
    edit = ui.rows()[0].children[0];
    edit.children[0].emit('keydown', {key: 'Escape'});
    assert.equal(ui.rows()[0].children[1].textContent, '할 일');
    assert.equal(ui.requests.length, 3);
});

test('failed inline edits retain the entered content for retry', async () => {
    const ui = await setup(async (url, options) => options.method
        ? {status: 400, body: {message: '수정 실패'}} : {body: page([todo()])});
    ui.rows()[0].children[2].children[0].emit('click');
    const form = ui.rows()[0].children[0];
    form.children[0].value = '보존할 수정 내용';
    form.emit('submit');
    await flush();
    assert.equal(form.children[0].value, '보존할 수정 내용');
    assert.equal(form.children[0].disabled, false);
    assert.equal(ui.nodes.get('todoMessage').textContent, '수정 실패');
});

test('deleting the last item on a page moves back to the last valid page', async () => {
    let deleted = false;
    const ui = await setup(async (url, options) => {
        if (options.method === 'DELETE') { deleted = true; return {body: {result: 1}}; }
        return {body: page(deleted && url.includes('page=1') ? [] : [todo()], deleted ? 1 : 2, deleted ? 20 : 21)};
    });
    ui.nodes.get('todoNext').emit('click');
    await flush();
    ui.window.confirm = () => false;
    ui.rows()[0].children[2].children[1].emit('click');
    assert.equal(ui.requests.length, 2);
    ui.window.confirm = () => true;
    ui.rows()[0].children[2].children[1].emit('click');
    await flush();
    assert.equal(ui.requests[2].options.method, 'DELETE');
    assert.equal(ui.requests.at(-1).url, '/api/todo?page=0&size=20');
    assert.equal(ui.nodes.get('todoPageNumber').textContent, '1 / 1');
});

test('filters start at the first page and send explicit true or false', async () => {
    const ui = await setup(async () => ({body: page([todo()], 2, 21)}));
    ui.nodes.get('todoNext').emit('click');
    await flush();
    ui.filters[1].emit('click');
    await flush();
    assert.equal(ui.requests.at(-1).url, '/api/todo?page=0&size=20&completed=false');
    assert.equal(ui.filters[1].attributes['aria-pressed'], 'true');
    ui.filters[2].emit('click');
    await flush();
    assert.equal(ui.requests.at(-1).url, '/api/todo?page=0&size=20&completed=true');
});

test('reports expired authentication and allows retrying malformed list responses', async () => {
    let phase = 0;
    const ui = await setup(async () => phase === 0 ? {status: 401} : phase === 1 ? {body: {}} : {body: page()});
    assert.match(ui.nodes.get('todoMessage').textContent, /로그인이 만료/);
    assert.equal(ui.nodes.get('todoRetry').classList.contains('is-hidden'), false);
    phase = 1;
    ui.nodes.get('todoRetry').emit('click');
    await flush();
    assert.match(ui.nodes.get('todoMessage').textContent, /목록 형식/);
    phase = 2;
    ui.nodes.get('todoRetry').emit('click');
    await flush();
    assert.equal(ui.nodes.get('todoEmpty').classList.contains('is-hidden'), false);
    assert.equal(ui.nodes.get('todoRetry').classList.contains('is-hidden'), true);
});

test('serializes duplicate submissions and distinguishes saved data from a failed refresh', async () => {
    let finish;
    let saved = false;
    const ui = await setup(async (url, options) => {
        if (options.method === 'POST') {
            await new Promise(resolve => { finish = resolve; });
            saved = true;
            return {body: {result: 1}};
        }
        return saved ? {status: 500, body: {}} : {body: page()};
    });
    ui.nodes.get('todoContent').value = '새 할 일';
    ui.nodes.get('todoCreateForm').emit('submit');
    ui.nodes.get('todoCreateForm').emit('submit');
    assert.equal(ui.requests.filter(request => request.options.method === 'POST').length, 1);
    assert.equal(ui.nodes.get('todoContent').disabled, true);
    finish();
    await flush();
    assert.equal(ui.nodes.get('todoContent').value, '');
    assert.match(ui.nodes.get('todoMessage').textContent, /추가했습니다.*다시 불러와/);
});

test('returning from browser history refreshes an aborted list and restores controls', async () => {
    let finish;
    let pending = true;
    const ui = await setup(async () => {
        if (pending) await new Promise(resolve => { finish = resolve; });
        return {body: page([todo()])};
    });
    ui.window.emit('pagehide');
    assert.equal(ui.requests[0].options.signal.aborted, true);
    pending = false;
    ui.window.emit('pageshow', {persisted: true});
    await flush();
    finish();
    await flush();
    assert.equal(ui.requests.length, 2);
    assert.equal(ui.rows().length, 1);
    assert.equal(ui.nodes.get('todoContent').disabled, false);
});
