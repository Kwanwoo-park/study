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

test('loads commits through the admin API and renders untrusted messages as text', async () => {
    const page = await loadPage();
    assert.equal(page.calls[0].url, '/api/admin/github/commits?page=1');
    assert.equal(page.calls[0].options.credentials, 'same-origin');
    assert.equal(page.calls[0].options.cache, 'no-store');
    const nodes = descendants(page.element('list'));
    assert.ok(nodes.some(node => node.tag === 'h3' && node.textContent === commit.message));
    const link = nodes.find(node => node.tag === 'a');
    assert.equal(link.rel, 'noopener noreferrer');
    assert.equal(link.href, commit.url);
    assert.match(page.element('status').textContent, /09:00:00/);
    assert.equal(page.element('prev').disabled, true);
    assert.equal(page.element('next').disabled, true);
});

test('switches to actual push records, follows opaque cursors and returns to the previous page', async () => {
    const activity = {id: '42', type: 'force', actor: 'pusher', pushedAt: '2026-09-12T00:00:00Z', ref: 'refs/heads/main', before: 'a', after: 'b'};
    const page = await loadPage(async url => ({body: url.includes('/activity')
        ? response([activity], url.endsWith('cursor=') ? 'before:opaque+/=' : null)
        : response([commit], '2')}));
    await page.element('activity').emit('click');
    assert.equal(page.calls.at(-1).url, '/api/admin/github/activity?cursor=');
    assert.equal(page.element('activity')['aria-pressed'], 'true');
    const nodes = descendants(page.element('list'));
    assert.ok(nodes.some(node => node.textContent === '강제 Push'));
    assert.ok(nodes.some(node => node.textContent.includes('Push 시각:')));
    await page.element('next').emit('click');
    assert.equal(page.calls.at(-1).url, '/api/admin/github/activity?cursor=before%3Aopaque%2B%2F%3D');
    assert.equal(page.element('page').textContent, '2 페이지');
    await page.element('prev').emit('click');
    assert.equal(page.calls.at(-1).url, '/api/admin/github/activity?cursor=');
    assert.equal(page.element('page').textContent, '1 페이지');
});

test('refresh returns to the latest page instead of keeping an old cursor', async () => {
    const page = await loadPage(async () => ({body: response([commit], '2')}));
    await page.element('next').emit('click');
    assert.equal(page.calls.at(-1).url, '/api/admin/github/commits?page=2');
    await page.element('refresh').emit('click');
    assert.equal(page.calls.at(-1).url, '/api/admin/github/commits?page=1');
    assert.equal(page.element('prev').disabled, true);
});

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

test('expired login clears previously displayed data and allows retry', async () => {
    let expired = false;
    const page = await loadPage(async () => expired ? {status: 401} : {body: response([commit])});
    expired = true;
    await page.element('refresh').emit('click');
    assert.equal(page.element('list').children.length, 0);
    assert.match(page.element('status').textContent, /로그인이 만료/);
    assert.equal(page.element('refresh').disabled, false);
    assert.equal(page.element('list')['aria-busy'], 'false');
});

test('shows permission, upstream and malformed response errors without stale entries', async () => {
    for (const result of [
        {status: 403},
        {status: 503, body: {message: 'GitHub API 조회 한도에 도달했습니다.'}},
        {status: 200, body: '<html>Login page</html>'},
    ]) {
        const page = await loadPage(async () => result);
        assert.equal(page.element('list').children.length, 0);
        assert.equal(page.element('next').disabled, true);
        assert.equal(page.element('refresh').disabled, false);
        assert.ok(page.element('status').textContent.length > 0);
    }
});

test('shows an empty state and never renders malicious external links', async () => {
    const empty = await loadPage(async () => ({body: response()}));
    assert.equal(empty.element('list').children[0].textContent, '조회할 내역이 없습니다.');
    for (const url of ['javascript:alert(1)', 'https://github.com.attacker.test/path', 'https://token@github.com/path']) {
        const page = await loadPage(async () => ({body: response([{...commit, url}])}));
        assert.equal(descendants(page.element('list')).some(node => node.tag === 'a'), false);
    }
});

test('disables navigation during pending loads to prevent overlapping tab responses', async () => {
    let finish;
    const page = await loadPage(() => new Promise(resolve => { finish = resolve; }));
    assert.equal(page.element('commits').disabled, true);
    assert.equal(page.element('activity').disabled, true);
    page.element('activity').emit('click');
    assert.equal(page.calls.length, 1);
    finish({body: response([commit])});
    await flush();
    assert.equal(page.element('activity').disabled, false);
    assert.equal(page.element('commits')['aria-pressed'], 'true');
});
