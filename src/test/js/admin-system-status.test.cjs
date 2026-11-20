const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

test('resource trends show measured values, leaving gaps when a reading is unavailable', () => {
    const resources = join(__dirname, '../../main/resources');
    const html = readFileSync(join(resources, 'templates/admin/administrator.html'), 'utf8');
    const elements = new Map(Array.from(html.matchAll(/id="(system-[^"]+)"/g), ([, id]) => [id, {
        attributes: new Map(),
        setAttribute(name, value) { this.attributes.set(name, value); }
    }]));
    const context = vm.createContext({window: {addEventListener() {}}, document: {getElementById: id => elements.get(id)}});
    vm.runInContext(readFileSync(join(resources, 'static/js/admin/system-status.js'), 'utf8'), context);

    const end = Date.UTC(2026, 10, 20, 4);
    const points = [0, 1, 2].map(index => ({sampledAt: end - (2 - index) * 30_000,
        cpuPercent: index === 1 ? null : 25, memoryPercent: 50 + index}));

    context.drawResourceTrend(points.slice(2), end);
    assert.equal(elements.get('system-usage-note').hidden, false);
    assert.equal(elements.get('system-cpu-point').attributes.get('visibility'), 'visible');

    context.drawResourceTrend(points, end);
    assert.equal(elements.get('system-usage-note').hidden, true);
    assert.match(elements.get('system-cpu-trend').attributes.get('d'), /^M[\d.]+ 120\.0 M1000\.0 120\.0$/);
    assert.match(elements.get('system-memory-trend').attributes.get('d'), / L1000\.0 76\.8$/);
});
