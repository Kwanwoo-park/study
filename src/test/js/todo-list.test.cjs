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
