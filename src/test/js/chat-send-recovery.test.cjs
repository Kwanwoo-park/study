const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

const script = readFileSync(join(__dirname, '../../main/resources/static/js/chat/chat.js'), 'utf8');

function setup(fetch) {
    const nodes = new Map();
    function getElementById(id) {
        if (!nodes.has(id)) nodes.set(id, {value: '', listeners: {}, append() {}, setAttribute() {},
            addEventListener(name, handler) { this.listeners[name] = handler; }});
        return nodes.get(id);
    }
    getElementById('room').value = 'room-1';
    getElementById('email').value = 'sender@example.test';
    const messages = [];
    const timers = new Map();
    const socket = {connected: true, heartbeat: {}, connect() {}, send: (_, headers, body) => messages.push(JSON.parse(body))};
    const context = vm.createContext({
        document: {getElementById, createElement: () => getElementById('stub'), body: getElementById('body'),
            addEventListener() {}, querySelector: selector => getElementById(selector.replace(/^#/, ''))},
        fetch, crypto: {randomUUID: () => 'd52238ca-8cbd-4c06-b8cf-7c61f502e204'}, console,
        addEventListener() {}, SockJS: function () {}, Stomp: {over: () => socket},
        setTimeout: callback => { const id = Symbol(); timers.set(id, callback); return id; },
        clearTimeout: id => timers.delete(id)
    });
    context.window = context;
    vm.runInContext(script, context);
    return {context, getElementById, messages, timers};
}

test('a missing storage receipt enables retry with the original ID, without checking the text again', async () => {
    const checks = [];
    const app = setup(async (path, options) => {
        if (path.includes('/receipt?')) return {ok: false, status: 404};
        assert.equal(path, '/api/chat/message/check');
        checks.push(JSON.parse(options.body).message);
        return {ok: true, json: async () => ({result: 1})};
    });
    app.getElementById('chat').value = '연결 확인 & 재시도';
    await app.context.sendMsg();
    assert.equal(app.getElementById('chat-send-status').textContent, '전송 중…');
    assert.deepEqual(checks, ['연결 확인 & 재시도']);
    await [...app.timers.values()][0]();
    assert.equal(app.getElementById('chat-send-retry').hidden, false);
    app.getElementById('chat-send-retry').listeners.click();
    assert.equal(checks.length, 1);
    assert.equal(app.messages.length, 2);
    assert.deepEqual(app.messages[0], app.messages[1]);
    app.context.registerMessageForRender({id: app.messages[0].id, email: 'sender@example.test',
        registerTime: '2026-11-20T10:00:00'});
    assert.equal(app.getElementById('chat-send-status').textContent, '');
    assert.equal(app.getElementById('chat-send-retry').hidden, true);
});

test('reconnect follows the server cursor and renders each stored message once', async () => {
    const requests = [];
    const first = {id: 'a', registerTime: '2026-11-20T10:00:01'};
    const second = {id: 'b', registerTime: '2026-11-20T10:00:02'};
    const app = setup(async path => {
        requests.push(path);
        const data = requests.length === 1 ? {message: [first], img: {}, hasMore: true}
            : {message: [first, second], img: {}, hasMore: false};
        return {ok: true, json: async () => data};
    });
    const rendered = [];
    app.context.insertChatMessage = (_, item) => rendered.push(item.id);
    app.context.refreshDateSeparators = () => {};
    await app.context.recoverMessages('last-seen');
    assert.deepEqual(requests, [
        '/api/chat/missed?roomId=room-1&after=last-seen',
        '/api/chat/missed?roomId=room-1&after=a',
    ]);
    assert.deepEqual(rendered, ['a', 'b']);
});
