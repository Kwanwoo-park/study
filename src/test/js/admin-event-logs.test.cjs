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
