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

for (const [name, config] of [
    ['direct entry', {length: 1}],
    ['missing referrer', {referrer: ''}],
    ['external referrer', {referrer: 'https://example.test/search'}],
    ['similar host', {referrer: 'https://www.kwanwoo.site.example.test/board/main'}],
    ['different protocol', {referrer: 'http://www.kwanwoo.site/board/main'}],
    ['invalid URL', {referrer: 'not a URL'}],
    ['same page', {referrer: 'https://www.kwanwoo.site/board/view?id=1'}],
    ['same page hash', {referrer: 'https://www.kwanwoo.site/board/view?id=1#comments'}],
    ['landing redirect', {referrer: 'https://www.kwanwoo.site/'}],
    ['index redirect', {referrer: 'https://www.kwanwoo.site/index.html'}],
    ['OAuth callback', {referrer: 'https://www.kwanwoo.site/login/oauth2/code/google?code=example'}],
    ['OAuth start', {referrer: 'https://www.kwanwoo.site/oauth2/authorization/naver'}],
]) {
    test(`${name}: replaces the current page with the fallback without adding history`, () => {
        const ui = setup({...config, fallback: '/chat/chatList'});
        assert.equal(ui.click().defaultPrevented, true);
        assert.equal(ui.backCalls(), 0);
        assert.deepEqual(ui.replaced, ['https://www.kwanwoo.site/chat/chatList']);
    });
}

test('login does not return to an authentication-protected page or repeat a login error', () => {
    for (const referrer of ['/board/main', '/admin/administrator', '/member/login?error=true']) {
        const ui = setup({current: 'https://www.kwanwoo.site/member/login', referrer: 'https://www.kwanwoo.site' + referrer,
            fallback: '/portfolio/'});
        assert.equal(ui.click().defaultPrevented, true);
        assert.equal(ui.backCalls(), 0);
        assert.deepEqual(ui.replaced, ['https://www.kwanwoo.site/portfolio/']);
    }
});

test('login can return to a public page such as registration, recovery, or portfolio', () => {
    for (const referrer of ['/member/register', '/member/find', '/member/findByEmail', '/member/findByInfo', '/appeal', '/portfolio/']) {
        const ui = setup({current: 'https://www.kwanwoo.site/member/login', referrer: 'https://www.kwanwoo.site' + referrer});
        assert.equal(ui.click().defaultPrevented, true);
        assert.equal(ui.backCalls(), 1);
    }
});

test('modified and middle clicks preserve the link behavior for opening a new tab', () => {
    for (const options of [{ctrlKey: true}, {metaKey: true}, {shiftKey: true}, {altKey: true}, {button: 1}]) {
        const ui = setup();
        assert.equal(ui.click(options).defaultPrevented, false);
        assert.equal(ui.backCalls(), 0);
    }
});

test('unrelated clicks and already handled events are not intercepted', () => {
    const ui = setup();
    ui.click({target: {closest: () => null}});
    ui.click({target: {}});
    ui.click({defaultPrevented: true});
    assert.equal(ui.backCalls(), 0);
});

test('duplicate script inclusion does not register a second navigation handler', () => {
    const ui = setup();
    ui.reload();
    assert.equal(ui.listeners.length, 1);
    ui.click();
    assert.equal(ui.backCalls(), 1);
});

test('a browser history error uses the page-specific fallback', () => {
    const ui = setup({backThrows: true, fallback: '/board/view?id=42'});
    ui.click();
    assert.deepEqual(ui.replaced, ['https://www.kwanwoo.site/board/view?id=42']);
});
