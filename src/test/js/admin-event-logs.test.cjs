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
    async trigger(name) { return this.listeners[name]?.(); }
}
const tick = () => new Promise(resolve => setImmediate(resolve));
function setup(respond) {
    const ids = ['list','status','broker','operation','outcome','hours','prev','next','refresh','page','diagnostics','auto'];
    const elements = Object.fromEntries(ids.map(id => [id, new Element()])); elements.hours.value = '24';
    const calls = []; let interval;
    const document = {hidden:false, getElementById:id => elements[id.replace('event-','')], createElement:() => new Element()};
    const context = {document, window:{addEventListener(){}}, URLSearchParams,
        setInterval: fn => {interval = fn; return 1;}, clearInterval(){},
        fetch:async url => {calls.push(url); return respond(url, calls.length);} };
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin/event-logs.js'), 'utf8'), context);
    return {elements,calls,document,refresh:() => interval()};
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
