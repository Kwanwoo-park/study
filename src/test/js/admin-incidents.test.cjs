const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

class Element {
    constructor() {
        this.children = []; this.dataset = {}; this.listeners = {}; this.innerText = '';
        this.classList = {toggle() {}};
    }
    replaceChildren(...children) {this.children = children;}
    append(...children) {this.children.push(...children);}
    addEventListener(name, listener) {this.listeners[name] = listener;}
}

test('acknowledging incidents refreshes the pending list and all-read leaves an empty dashboard', async () => {
    const incidents = [1, 2, 3].map(id => ({id, acknowledged: id === 1, occurredAt: '2026-11-17T09:00:00',
        requestMethod: 'GET', requestPath: `/incident/${id}`, httpStatus: 500, message: 'test incident'}));
    const list = new Element(), count = new Element(), all = new Element();
    all.innerText = '전체 확인';
    const elements = {'system-incident-list': list, 'incident-unacknowledged-count': count, 'incident-acknowledge-all': all};
    const context = vm.createContext({
        window: {}, console, confirm: () => true,
        alert(message) {throw new Error(message);},
        document: {getElementById: id => elements[id], createElement: () => new Element()},
        fetch: async (url, options) => {
            if (options.method === 'PATCH') {
                if (url.endsWith('/acknowledge-all')) incidents.forEach(incident => incident.acknowledged = true);
                else {
                    const id = Number(url.split('/').at(-2));
                    incidents.find(incident => incident.id === id).acknowledged = true;
                }
                return {ok: true, json: async () => ({result: 1})};
            }
            assert.equal(url, '/api/admin/system/incidents');
            const pending = incidents.filter(incident => !incident.acknowledged).reverse();
            return {ok: true, json: async () => ({result: 1, list: pending, unacknowledgedCount: pending.length})};
        },
    });
    vm.runInContext(readFileSync(join(__dirname, '../../main/resources/static/js/admin/main.js'), 'utf8'), context);

    await context.loadSystemIncidents();
    assert.equal(list.children.length, 2);
    assert.equal(count.innerText, '2건 미확인');
    assert.equal(all.disabled, false);

    await list.children[0].children.at(-1).listeners.click();
    assert.equal(list.children.length, 1);
    assert.equal(list.children[0].children[1].innerText, 'GET /incident/2');
    assert.equal(count.innerText, '1건 미확인');

    await context.acknowledgeAllSystemIncidents();
    assert.equal(list.children.length, 1);
    assert.equal(list.children[0].innerText, '미확인된 서버 장애가 없습니다.');
    assert.equal(count.innerText, '0건 미확인');
    assert.equal(all.disabled, true);
    assert.equal(all.innerText, '전체 확인');
    assert.equal(incidents.length, 3);
    assert.ok(incidents.every(incident => incident.acknowledged));
});
