const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

class Element {
    constructor() { this.value = ''; this.children = []; this.listeners = {}; this.disabled = false; this.checked = true; this.attributes = {}; this.textContent = ''; }
    set textContent(value) { this.text = String(value); }
    get textContent() { return this.text; }
    setAttribute(name, value) { this.attributes[name] = String(value); }
    getAttribute(name) { return this.attributes[name]; }
    focus() { this.focused = true; }
    addEventListener(name, fn) { this.listeners[name] = fn; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    trigger(name, event = {}) { return this.listeners[name]?.(event); }
}
const tick = () => new Promise(resolve => setImmediate(resolve));
const response = (body, status = 200) => ({ok: status < 400, status, json: async () => body});
const entry = {id: 12, topic: 'topic', partition: 0, offset: 30, status: 'NEW', attempts: 0, replayable: true};
function setup(respond) {
    const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/admin/kafka_operations.html'), 'utf8');
    const ids = [...template.matchAll(/id="kafka-([^"]+)"/g)].map(match => match[1]);
    const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
    const calls = []; let interval;
    const document = {hidden: false, getElementById: id => elements[id.replace('kafka-', '')], createElement: () => new Element()};
    const context = {document, window: {addEventListener(){}, confirm: () => true}, URLSearchParams,
        setInterval: fn => { interval = fn; return 1; }, clearInterval(){},
        fetch: async (url, options) => {
            calls.push({url, options});
            if (url.endsWith('/overview')) return response({observedAt: null, totalLag: null, warnings: [], topics: [], lags: []});
            return respond(url, options);
        }};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin/kafka-operations.js'), 'utf8'), context);
    return {elements, calls, refresh: () => interval()};
}

test('Kafka list follows cursors, returns to previous pages, and resets the cursor on filter changes', async () => {
    const app = setup(url => response({entries: [], nextBeforeId: url.includes('beforeId') ? null : 12}));
    await tick();
    app.elements.next.trigger('click'); await tick();
    assert.match(app.calls.at(-1).url, /beforeId=12/);
    assert.equal(app.elements.page.textContent, '2 페이지');
    app.elements.prev.trigger('click'); await tick();
    assert.doesNotMatch(app.calls.at(-1).url, /beforeId/);
    assert.equal(app.elements.page.textContent, '1 페이지');
    app.elements.filter.value = 'REPLAY_FAILED'; app.elements.filter.trigger('change'); await tick();
    assert.match(app.calls.at(-1).url, /status=REPLAY_FAILED/);
    assert.doesNotMatch(app.calls.at(-1).url, /beforeId/);
});

test('Kafka replay fetches CSRF, prevents duplicate submissions, and displays queued rather than delivered', async () => {
    let finishReplay, pending = false;
    const app = setup((url, options) => {
        if (url.endsWith('/csrf')) return response({headerName: 'X-CSRF-TOKEN', token: 'test-csrf'});
        if (url.endsWith('/replay')) {
            assert.equal(options.method, 'POST');
            assert.equal(options.headers['X-CSRF-TOKEN'], 'test-csrf');
            return new Promise(resolve => { finishReplay = () => { pending = true; resolve(response({message: '재발행 요청을 저장했습니다.'}, 202)); }; });
        }
        return response({entries: [{...entry, status: pending ? 'REPLAY_PENDING' : 'NEW', replayable: !pending}], nextBeforeId: null});
    });
    await tick();
    const button = app.elements['dead-letters'].children[0].children.at(-1);
    button.trigger('click'); await tick();
    assert.equal(button.disabled, true); assert.equal(app.elements.refresh.disabled, true);
    button.trigger('click'); app.refresh(); await tick();
    assert.equal(app.calls.filter(call => call.url.endsWith('/replay')).length, 1);
    finishReplay(); await tick();
    assert.equal(app.elements['list-status'].textContent, '재발행 요청을 저장했습니다.');
    assert.equal(app.elements['dead-letters'].children[0].children[0].children[1].textContent, '재발행 대기');
    assert.equal(app.elements.refresh.disabled, false);
});
