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
function setup(respond, overview = () => response({observedAt: null, totalLag: null, warnings: [], topics: [], lags: []})) {
    const template = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/admin/kafka_operations.html'), 'utf8');
    const ids = [...template.matchAll(/id="kafka-([^"]+)"/g)].map(match => match[1]);
    const elements = Object.fromEntries(ids.map(id => [id, new Element()]));
    const calls = []; let interval;
    const document = {hidden: false, getElementById: id => elements[id.replace('kafka-', '')], createElement: () => new Element()};
    const context = {document, window: {addEventListener(){}, confirm: () => true}, URLSearchParams,
        setInterval: fn => { interval = fn; return 1; }, clearInterval(){},
        fetch: async (url, options) => {
            calls.push({url, options});
            if (url.endsWith('/overview')) return overview();
            return respond(url, options);
        }};
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin/kafka-operations.js'), 'utf8'), context);
    return {elements, calls, document, refresh: () => interval()};
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

test('Kafka replay rejection restores the button and leaves the record available for retry', async () => {
    const app = setup(url => {
        if (url.endsWith('/csrf')) return response({headerName: 'X-CSRF-TOKEN', token: 'test-csrf'});
        if (url.endsWith('/replay')) return response({message: '요청을 처리하지 못했습니다.'}, 503);
        return response({entries: [entry], nextBeforeId: null});
    });
    await tick();
    const button = app.elements['dead-letters'].children[0].children.at(-1);
    button.trigger('click'); await tick();
    assert.equal(button.disabled, false);
    assert.equal(app.elements['list-status'].textContent, '요청을 처리하지 못했습니다.');
});

test('Kafka metadata uses text and a rejected list refresh clears stale records', async () => {
    let denied = false;
    const app = setup(() => denied ? response({}, 403) : response({entries: [{...entry, errorType: '<img src=x onerror=alert(1)>'}], nextBeforeId: 12}));
    await tick();
    assert.equal(app.elements['dead-letters'].children[0].children[2].textContent, '오류 유형: <img src=x onerror=alert(1)>');
    denied = true; app.elements.refresh.trigger('click'); await tick();
    assert.equal(app.elements['dead-letters'].children.length, 0);
    assert.match(app.elements['list-status'].textContent, /관리자 인증/);
    assert.equal(app.elements.next.disabled, true);
});

test('Kafka auto refresh preserves historical pages and skips hidden tabs', async () => {
    const app = setup(() => response({entries: [], nextBeforeId: 12})); await tick();
    app.document.hidden = true; app.refresh(); await tick(); assert.equal(app.calls.length, 2);
    app.document.hidden = false; app.elements.next.trigger('click'); await tick();
    const listCalls = app.calls.filter(call => call.url.includes('/dead-letters')).length;
    app.refresh(); await tick();
    assert.equal(app.calls.filter(call => call.url.includes('/dead-letters')).length, listCalls);
    assert.equal(app.elements.page.textContent, '2 페이지');
    app.elements.refresh.trigger('click'); await tick();
    assert.equal(app.elements.page.textContent, '2 페이지');
    assert.match(app.calls.at(-1).url, /beforeId=12/);
});

const metadata = {
    clusterId: 'cluster-test', controllerId: 1, observedAt: null, totalLag: null, warnings: [], lags: [],
    brokers: [{id: 1, host: 'broker.test', port: 9092, controller: true, leaderPartitions: 4, replicaPartitions: 4}],
    topics: [{topic: 'topic', partitions: 1, configuredPartitions: 1, replicas: 1, underReplicatedPartitions: 0,
        retentionMs: '604800000', configs: {'cleanup.policy': 'delete'},
        partitionDetails: [{partition: 0, leader: 1, replicas: [1], inSyncReplicas: [1], startOffset: 0, endOffset: 10}]}],
    consumerGroups: [{groupId: 'study', state: 'STABLE', memberCount: 1, coordinatorId: 1, assignor: 'range', totalLag: null,
        members: [{consumerId: 'consumer-1', clientId: 'client-1', host: '/127.0.0.1', assignments: ['topic / 0']}], lags: []}]
};
const walk = element => [element, ...element.children.flatMap(walk)];
const text = element => walk(element).map(child => child.textContent).join(' ');
const metadataApp = () => setup(() => response({entries: [], nextBeforeId: null}), () => response(metadata));

test('Kafka UI tabs work with clicks and keyboard navigation', async () => {
    const app = metadataApp(); await tick();
    app.elements['tab-brokers'].trigger('click');
    assert.equal(app.elements['panel-overview'].hidden, true);
    assert.equal(app.elements['panel-brokers'].hidden, false);
    assert.equal(app.elements['tab-brokers'].getAttribute('aria-selected'), 'true');
    app.elements['tab-brokers'].trigger('keydown', {key: 'ArrowRight', preventDefault(){}});
    assert.equal(app.elements['panel-topics'].hidden, false);
    assert.equal(app.elements['tab-topics'].focused, true);
    assert.match(text(app.elements.brokers), /broker.test/);
    assert.match(text(app.elements.brokers), /컨트롤러/);
});

test('Kafka topic search and drill-down show partition and read-only configuration details', async () => {
    const app = metadataApp(); await tick();
    app.elements['topic-search'].value = 'missing'; app.elements['topic-search'].trigger('input');
    assert.match(text(app.elements.topics), /검색 결과가 없습니다/);
    app.elements['topic-search'].value = ''; app.elements['topic-search'].trigger('input');
    walk(app.elements.topics).find(element => element.type === 'button').trigger('click');
    assert.equal(app.elements['topic-list'].hidden, true);
    assert.equal(app.elements['topic-detail'].hidden, false);
    assert.match(text(app.elements.configs), /cleanup.policy.*delete/);
    assert.match(text(app.elements.partitions), /시작 오프셋.*최신 오프셋/);
    app.elements['topic-back'].trigger('click');
    assert.equal(app.elements['topic-list'].hidden, false);
});

test('Kafka consumer details show members while unknown lag remains unknown', async () => {
    const app = metadataApp(); await tick();
    assert.match(text(app.elements.consumers), /미확인/);
    walk(app.elements.consumers).find(element => element.type === 'button').trigger('click');
    assert.equal(app.elements['consumer-detail'].hidden, false);
    assert.match(text(app.elements.members), /consumer-1.*client-1.*127.0.0.1.*topic \/ 0/);
    app.elements['consumer-back'].trigger('click');
    assert.equal(app.elements['consumer-list'].hidden, false);
});

test('Kafka overview failures clear stale metadata and manual refresh still works when auto refresh is paused', async () => {
    let denied = false;
    const app = setup(() => response({entries: [], nextBeforeId: null}), () => denied ? response({}, 403) : response(metadata));
    await tick();
    app.elements['auto-refresh'].checked = false;
    const previous = app.calls.length; app.refresh(); await tick();
    assert.equal(app.calls.length, previous);
    denied = true; app.elements.refresh.trigger('click'); await tick();
    assert.doesNotMatch(text(app.elements.brokers), /broker.test/);
    assert.match(app.elements.status.textContent, /관리자 인증/);
    assert.doesNotMatch(text(app.elements.summary), /0건/);
});
