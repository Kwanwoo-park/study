const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/admin/github.js'), 'utf8');

class Element {
    constructor(tag = 'div') {
        this.tag = tag;
        this.children = [];
        this.listeners = {};
        this.textContent = '';
        this.classList = {add() {}, remove() {}};
    }
    addEventListener(type, handler) { this.listeners[type] = handler; }
    emit(type) { return this.listeners[type](); }
    append(...children) { this.children.push(...children); }
    replaceChildren() { this.children = []; }
    setAttribute(name, value) { this[name] = value; }
    set innerHTML(value) { throw new Error('GitHub content must never be inserted as HTML'); }
}

const flush = () => new Promise(resolve => setImmediate(resolve));
const response = (entries = [], nextCursor = null) => ({
    repository: 'Kwanwoo-park/study', branch: 'main', entries, nextCursor, fetchedAt: '2026-09-12T00:00:00Z',
});
const commit = {
    sha: 'a'.repeat(40), message: '<img src=x onerror=alert(1)>', author: 'Author',
    authoredAt: '2026-09-10T00:00:00Z', committedAt: '2026-09-11T00:00:00Z',
    url: 'https://github.com/Kwanwoo-park/study/commit/' + 'a'.repeat(40),
};

async function loadPage(reply = async () => ({body: response([commit])})) {
    const nodes = new Map();
    const calls = [];
    const element = id => {
        if (!nodes.has(id)) nodes.set(id, new Element());
        return nodes.get(id);
    };
    vm.runInNewContext(script, {
        document: {getElementById: element, createElement: tag => new Element(tag)},
        Intl, Date, URL,
        fetch: async (url, options) => {
            calls.push({url, options});
            const result = await reply(url);
            const status = result.status || 200;
            return {status, ok: status === 200, json: async () => result.body};
        },
    });
    await flush();
    return {element: id => element(`github-${id}`), calls};
}

function descendants(element) { return [element, ...element.children.flatMap(descendants)]; }

test('commit pagination enables the right buttons and renders each page when moving back and forth', async () => {
    const page = await loadPage(async url => {
        const number = Number(new URL(url, 'https://www.kwanwoo.site').searchParams.get('page'));
        return {body: response([{...commit, message: `Commit page ${number}`}], number < 3 ? String(number + 1) : null)};
    });
    const checkPage = (number, previousDisabled, nextDisabled) => {
        assert.equal(page.element('page').textContent, `${number} 페이지`);
        assert.equal(page.element('prev').disabled, previousDisabled);
        assert.equal(page.element('next').disabled, nextDisabled);
        assert.ok(descendants(page.element('list')).some(node => node.tag === 'h3' && node.textContent === `Commit page ${number}`));
    };

    checkPage(1, true, false);
    await page.element('next').emit('click');
    checkPage(2, false, false);
    await page.element('next').emit('click');
    checkPage(3, false, true);
    await page.element('prev').emit('click');
    checkPage(2, false, false);
    await page.element('prev').emit('click');
    checkPage(1, true, false);
    await page.element('next').emit('click');
    checkPage(2, false, false);
    assert.deepEqual(page.calls.map(call => call.url), [1, 2, 3, 2, 1, 2].map(number => `/api/admin/github/commits?page=${number}`));
});
