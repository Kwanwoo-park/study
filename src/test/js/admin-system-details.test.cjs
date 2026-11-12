const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

class Element {
    constructor() {
        this.children = []; this.listeners = {}; this.attributes = {};
        this.classList = {add() {}}; this.hidden = false; this.isConnected = true;
        this.textContent = ''; this.value = ''; this.disabled = false;
    }
    addEventListener(name, listener) { this.listeners[name] = listener; }
    replaceChildren(...children) { this.children = children; }
    append(...children) { this.children.push(...children); }
    setAttribute(name, value) { this.attributes[name] = value; }
    focus() { this.focused = true; }
    trigger(name, event = {}) { return this.listeners[name]?.(event); }
}

function setup(respond) {
    const ids = ['system-details', 'system-details-title', 'system-details-description',
        'system-details-close', 'system-details-controls', 'system-details-content'];
    const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
    elements['system-details'].hidden = true;
    const calls = [];
    const document = {
        activeElement: new Element(),
        getElementById: id => elements[id],
        createElement: () => new Element(),
        createTextNode: text => ({textContent: text}),
        addEventListener() {}
    };
    const context = {document, URLSearchParams, AbortController,
        formatBytes: value => `${value}B`,
        fetch: async url => { calls.push(url); return {ok: true, json: async () => respond(url)}; }};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname,
        '../../main/resources/static/js/admin/system-details.js'), 'utf8'), context);
    return {context, elements, calls};
}

const tick = () => new Promise(resolve => setImmediate(resolve));

test('disk card navigates folders and pages with bounded requests', async () => {
    const app = setup(url => {
        const query = new URL(url, 'https://example.test').searchParams;
        return {disk: {roots: [{id: 'project', label: '실행 프로젝트'}], root: 'project',
            path: query.get('path'), page: Number(query.get('page')), pageSize: 100,
            totalCount: query.get('path') ? 1 : 101,
            entries: [{name: 'folder', directory: true, sizeBytes: 0, modifiedAt: null}]}};
    });
    app.context.openSystemDetails('disk');
    await tick();
    const rows = app.elements['system-details-content'].children[0].children;
    rows[1].trigger('click');
    await tick();
    assert.match(app.calls[1], /path=folder/);
    const back = app.elements['system-details-controls'].children[1];
    back.trigger('click');
    await tick();
    const paging = app.elements['system-details-content'].children[1];
    paging.children[2].trigger('click');
    await tick();
    assert.match(app.calls[3], /page=1/);
});
