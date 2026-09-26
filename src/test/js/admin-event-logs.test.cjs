const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');

class Element {
    constructor() { this.value = ''; this.children = []; this.listeners = {}; this.attributes = {}; this.disabled = false; this.checked = false; this.textContent = ''; this.className = ''; this.classList = {add(){}, remove(){}, toggle(){}}; }
    addEventListener(name, fn) { this.listeners[name] = fn; }
    setAttribute(name, value) { this.attributes[name] = value; }
    append(...children) { this.children.push(...children); }
    replaceChildren(...children) { this.children = children; }
    async trigger(name, event = {}) { return this.listeners[name]?.(event); }
    showModal() { this.open = true; }
    close() { this.open = false; this.listeners.close?.(); }
    focus() { this.focused = true; }
}
const tick = () => new Promise(resolve => setImmediate(resolve));
function setup(respond) {
    const ids = ['list','status','broker','operation','outcome','hours','prev','next','refresh','page','diagnostics','auto',
        'detail','detail-title','detail-close','detail-retry','detail-content','detail-status'];
    const elements = Object.fromEntries(ids.map(id => [id, new Element()])); elements.hours.value = '24';
    const calls = [], options = []; let interval;
    const document = {hidden:false, getElementById:id => elements[id.replace('event-','')], createElement:() => new Element()};
    const context = {document, window:{addEventListener(){}}, URLSearchParams, AbortController,
        setInterval: fn => {interval = fn; return 1;}, clearInterval(){},
        fetch:async (url, requestOptions) => {calls.push(url); options.push(requestOptions); return respond(url, calls.length);} };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin/event-logs.js'), 'utf8'), context);
    return {elements,calls,options,document,refresh:() => interval()};
}
const response = (entries=[], nextBeforeId=null) => ({ok:true,status:200,json:async()=>({entries,nextBeforeId,retentionDays:7,diagnostics:{enabled:true}})});

test('pagination uses server cursors, previous returns to first page, filters reset cursors', async () => {
    const app = setup(url => response([], url.includes('beforeId') ? null : 50)); await tick();
    await app.elements.next.trigger('click'); await tick();
    assert.match(app.calls[1], /beforeId=50/); assert.equal(app.elements.page.textContent, '2 페이지');
    await app.elements.prev.trigger('click'); await tick();
    assert.doesNotMatch(app.calls[2], /beforeId/); assert.equal(app.elements.page.textContent, '1 페이지');
    app.elements.broker.value = 'REDIS'; await app.elements.broker.trigger('change'); await tick();
    assert.match(app.calls[3], /broker=REDIS/); assert.doesNotMatch(app.calls[3], /beforeId/);
});
test('auto refresh does not move historical pages or poll hidden tabs', async () => {
    const app = setup(() => response([], 50)); await tick(); app.elements.auto.checked = true;
    app.document.hidden = true; app.refresh(); await tick(); assert.equal(app.calls.length,1);
    app.document.hidden = false; await app.elements.next.trigger('click'); await tick();
    app.refresh(); await tick(); assert.equal(app.calls.length,2);
});
test('errors are readable and metadata is written as text rather than HTML', async () => {
    const entry = {id:1,broker:'REDIS',operation:'CONSUME',outcome:'FAILED',destination:'notification-events',itemCount:1,errorType:'<img src=x onerror=alert(1)>',instanceId:'node'};
    const app = setup(() => response([entry])); await tick();
    const card = app.elements.list.children[0];
    assert.equal(card.children.at(-1).textContent, '오류 유형: <img src=x onerror=alert(1)>');
    const denied = setup(() => ({ok:false,status:403,json:async()=>({})})); await tick();
    assert.match(denied.elements.status.textContent,/관리자만/); assert.equal(denied.elements.next.disabled,true);
});

const detailResponse = (body, status = 200) => ({ok:status < 400, status, json:async()=>body});
const detailEntry = {id:12, broker:'REDIS', route:'REALTIME_NOTIFICATION', destination:'notification-events', operation:'PUBLISH',
    outcome:'NO_SUBSCRIBERS', referenceId:42, itemCount:1, attempt:null, subscriberCount:0, instanceId:'node-1', occurredAt:'2026-09-26T10:01:02.123456'};
const text = element => [element.textContent, ...element.children.map(text)].join(' ');

test('detail loads only on selection, shows full metadata, and pauses refresh without moving the list', async () => {
    const app = setup(url => url.includes('/event-logs/12') ? detailResponse(detailEntry) : response([detailEntry]));
    await tick(); assert.equal(app.calls.length, 1);
    const row = app.elements.list.children[0]; row.isConnected = true;
    assert.equal(row.attributes.role, 'button'); assert.equal(row.attributes['aria-haspopup'], 'dialog');
    let prevented = false;
    await row.trigger('keydown', {key:'Enter', preventDefault(){prevented = true;}}); await tick();
    assert.equal(prevented,true); assert.equal(app.elements.detail.open,true);
    assert.equal(app.calls[1],'/api/admin/event-logs/12');
    assert.equal(app.options[1].credentials,'same-origin'); assert.equal(app.options[1].cache,'no-store');
    const content = text(app.elements['detail-content']);
    assert.match(content,/알림 ID 42/); assert.match(content,/Redis 구독 연결 수 0/);
    assert.match(content,/2026-09-26 10:01:02.123456/); assert.match(content,/서버 인스턴스 ID node-1/);
    app.elements.auto.checked = true; app.refresh(); await tick(); assert.equal(app.calls.length,2);
    await app.elements['detail-close'].trigger('click');
    assert.equal(app.elements.detail.open,false); assert.equal(row.focused,true);
    assert.equal(app.elements.list.children[0],row);
    app.refresh(); await tick(); assert.equal(app.calls.length,3);
});

test('missing or forbidden details do not show stale data and a failed load can be retried', async () => {
    let status = 404;
    const app = setup(url => url.includes('/event-logs/12') ? detailResponse(status === 200 ? detailEntry : {},status) : response([detailEntry]));
    await tick(); await app.elements.list.children[0].trigger('click'); await tick();
    assert.match(app.elements['detail-status'].textContent,/기록이 없거나/);
    assert.equal(app.elements['detail-content'].children.length,0); assert.equal(app.elements['detail-retry'].hidden,false);
    status = 200; await app.elements['detail-retry'].trigger('click'); await tick();
    assert.match(text(app.elements['detail-content']),/알림 ID 42/);
    await app.elements['detail-close'].trigger('click'); status = 403;
    await app.elements.list.children[0].trigger('click'); await tick();
    assert.match(app.elements['detail-status'].textContent,/관리자만/);
    assert.equal(app.elements['detail-content'].children.length,0);
});

test('closing aborts detail lookup and ignores a late response after another record is selected', async () => {
    let finish;
    const newer = {...detailEntry,id:13,instanceId:'new-node'};
    const app = setup(url => {
        if (url.endsWith('/12')) return new Promise(resolve => {finish = resolve;});
        if (url.endsWith('/13')) return detailResponse(newer);
        return response([detailEntry,newer]);
    });
    await tick(); await app.elements.list.children[0].trigger('click'); await tick();
    await app.elements['detail-close'].trigger('click'); assert.equal(app.options[1].signal.aborted,true);
    await app.elements.list.children[1].trigger('click'); await tick();
    finish(detailResponse(detailEntry)); await tick();
    assert.match(text(app.elements['detail-content']),/new-node/);
    assert.doesNotMatch(text(app.elements['detail-content']),/node-1/);
    assert.match(app.elements['detail-title'].textContent,/#13/);
});

test('detail differentiates outbox failure, DLT storage and replay acknowledgement without claiming delivery', async () => {
    const cases = [
        {operation:'PUBLISH',outcome:'DEAD_LETTER',expected:/Kafka 발행 전 Outbox/,reference:/Outbox ID 42/},
        {operation:'CONSUMER_RETRY',outcome:'DLT_PUBLISHED',expected:/DLT에 발행되었습니다/,reference:/관련 ID 42/},
        {operation:'REPLAY',outcome:'SUCCESS',expected:/소비자의 처리 완료나 사용자 수신 완료를 의미하지 않습니다/,reference:/소비 실패 기록 ID 42/}
    ];
    for (const scenario of cases) {
        const entry = {...detailEntry,broker:'KAFKA',route:'CHAT',...scenario,errorType:'<img src=x onerror=alert(1)>'};
        const app = setup(url => url.endsWith('/12') ? detailResponse(entry) : response([entry]));
        await tick(); await app.elements.list.children[0].trigger('click'); await tick();
        const content = text(app.elements['detail-content']);
        assert.match(content,scenario.expected); assert.match(content,scenario.reference);
        const fields = app.elements['detail-content'].children[1];
        assert.equal(fields.children.at(-1).textContent,entry.errorType);
    }
});
