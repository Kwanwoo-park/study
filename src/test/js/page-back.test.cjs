const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/common/page-back.js'), 'utf8');

function setup({referrer = 'https://www.kwanwoo.site/board/main', current = 'https://www.kwanwoo.site/board/view?id=1',
    length = 2, fallback = '/board/main', backThrows = false} = {}) {
    const listeners = [];
    const replaced = [];
    let backCalls = 0;
    const link = {href: new URL(fallback, current).href};
    const context = {
        URL,
        document: {referrer, addEventListener: (name, callback) => { if (name === 'click') listeners.push(callback); }},
        window: {
            location: {href: current, replace: href => replaced.push(href)},
            history: {length, back() { backCalls++; if (backThrows) throw new Error('Unavailable'); }},
        },
    };
    vm.createContext(context);
    vm.runInContext(script, context);
    function click(options = {}) {
        const event = {button: 0, defaultPrevented: false, target: {closest: selector => selector === 'a[data-page-back]' ? link : null},
            preventDefault() { this.defaultPrevented = true; }, ...options};
        listeners.forEach(listener => listener(event));
        return event;
    }
    return {click, replaced, link, listeners, backCalls: () => backCalls, reload: () => vm.runInContext(script, context)};
}

test('uses browser back for a previous page on the same site, including a clicked arrow child', () => {
    const ui = setup();
    assert.equal(ui.click().defaultPrevented, true);
    assert.equal(ui.backCalls(), 1);
    assert.deepEqual(ui.replaced, []);
});
