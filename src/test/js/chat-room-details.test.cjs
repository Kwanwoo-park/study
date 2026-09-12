const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const script = readFileSync(join(__dirname, '../../main/resources/static/js/chat/room-details.js'), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

class Events {
    constructor() { this.listeners = {}; }
    addEventListener(type, handler) { (this.listeners[type] ||= []).push(handler); }
    dispatchEvent(event) { for (const handler of this.listeners[event.type] || []) handler(event); }
}

class Element extends Events {
    constructor(tag = 'div') {
        super();
        this.tagName = tag;
        this.children = [];
        this.hidden = false;
        this.text = '';
        const classes = new Set();
        this.classList = {
            add: value => classes.add(value), remove: value => classes.delete(value), contains: value => classes.has(value),
            toggle(value, enabled) { if (enabled ?? !classes.has(value)) classes.add(value); else classes.delete(value); },
        };
    }
    set textContent(value) { this.text = value; this.replaceChildren(); }
    get textContent() { return this.text; }
    set innerHTML(value) { throw new Error('Member names and photo metadata must be rendered as text'); }
    append(...nodes) { for (const node of nodes) { node.parent = this; this.children.push(node); } }
    replaceChildren() { for (const child of this.children) child.parent = null; this.children = []; }
    remove() { if (this.parent) this.parent.children = this.parent.children.filter(child => child !== this); this.parent = null; }
    setAttribute(name, value) { this[name] = value; }
    getBoundingClientRect() { return {}; }
    contains(target) { return this === target || this.children.some(child => child.contains(target)); }
    querySelectorAll() { return descendants(this).filter(node => node.tagName === 'a' && node.href || node.tagName === 'button' && !node.disabled); }
    emit(type) { this.dispatchEvent({type, target: this}); }
}

function descendants(element) { return element.children.flatMap(child => [child, ...descendants(child)]); }

const details = {roomId: 'room-a', name: '우리 채팅방', participants: [
    {name: '나', email: 'me@example.test', profile: '/me.png', me: true},
    {name: '<script>not HTML</script>', email: 'a+b@example.test', profile: '/other.png', me: false},
]};
const photo = {id: 24, messageId: 'message-a', imgSrc: 'https://example.test/photo.png', senderName: '회원', sentAt: '2026-09-12T12:00:00'};

async function setup(reply = async path => ({body: path.endsWith('/details') ? details : {images: [photo], nextCursor: null}})) {
    const nodes = new Map();
    const document = new Events();
    const window = new Events();
    const requests = [];
    const previews = [];
    const timers = new Map();
    let timerId = 0;
    const element = id => {
        if (!nodes.has(id)) {
            const tag = /-(open|close|refresh|more|backdrop)$/.test(id) ? 'button' : 'div';
            const node = new Element(tag);
            node.focus = () => { document.activeElement = node; };
            nodes.set(id, node);
        }
        return nodes.get(id);
    };
    document.body = new Element('body');
    document.getElementById = element;
    document.createElement = tag => {
        const node = new Element(tag);
        node.focus = () => { document.activeElement = node; };
        return node;
    };
    element('room').value = 'room-a';
    element('chat-details-overlay').hidden = true;
    element('chatImageModal').classList.add('is-hidden');
    const panel = element('chat-details-panel');
    panel.append(...['close', 'refresh', 'members', 'images', 'more'].map(name => element(`chat-details-${name}`)));
    element('chat-details-overlay').append(panel);
    document.body.append(element('chat-details-open'), element('chat-details-overlay'));
    window.location = {origin: 'https://www.kwanwoo.site'};
    window.openChatImageModal = (sources, index) => {
        previews.push({sources: Array.from(sources), index});
        element('chatImageModal').classList.remove('is-hidden');
    };
    let previewCloses = 0;
    window.closeChatImageModal = () => {
        previewCloses++;
        element('chatImageModal').classList.add('is-hidden');
        window.dispatchEvent({type: 'chat:image-preview-closed'});
    };
    vm.runInNewContext(script, {
        document, window, URL, AbortController,
        setTimeout: callback => { timers.set(++timerId, callback); return timerId; },
        clearTimeout: id => timers.delete(id),
        fetch: async (path, options) => {
            requests.push({path, options});
            const result = await reply(path);
            const status = result.status || 200;
            return {status, ok: status === 200, json: async () => result.body};
        },
    });
    return {
        element: name => element(`chat-details-${name}`), document, window, requests, previews,
        previewCloses: () => previewCloses,
        open: async () => { element('chat-details-open').emit('click'); await flush(); },
        change: detail => window.dispatchEvent({type: 'chat:room-details-change', detail}),
        flushTimers: () => { for (const callback of timers.values()) callback(); timers.clear(); },
    };
}

test('opens a side panel on demand and preserves chat-origin profile navigation', async () => {
    const page = await setup();
    assert.equal(page.requests.length, 0);
    await page.open();
    assert.equal(page.element('overlay').hidden, false);
    assert.equal(page.element('open')['aria-expanded'], 'true');
    assert.equal(page.document.activeElement, page.element('close'));
    assert.equal(page.element('member-count').textContent, '(2명)');
    const nodes = descendants(page.element('members'));
    assert.ok(nodes.some(node => node.textContent === '<script>not HTML</script>'));
    const links = nodes.filter(node => node.tagName === 'a');
    assert.equal(links[0].href, '/member/detail?email=me%40example.test');
    assert.equal(links[1].href, '/member/search/detail?email=a%2Bb%40example.test&source=chat');
    assert.equal(page.requests[0].options.cache, 'no-store');
    assert.equal(page.requests[0].options.credentials, 'include');
});

test('paginates images without duplicates and opens the shared swipe preview', async () => {
    const older = {...photo, id: 20, messageId: 'older', imgSrc: 'https://example.test/older.png'};
    const page = await setup(async path => ({body: path.endsWith('/details') ? details
        : path.includes('cursor=24') ? {images: [photo, older], nextCursor: null}
        : {images: [photo], nextCursor: 24}}));
    await page.open();
    assert.equal(page.element('more').hidden, false);
    page.element('more').emit('click');
    await flush();
    assert.equal(page.element('images').children.length, 2);
    assert.equal(page.element('more').hidden, true);
    page.element('images').children[1].emit('click');
    assert.deepEqual(page.previews[0], {sources: [photo.imgSrc, older.imgSrc], index: 1});
});

test('removes deleted messages from the gallery and closes their preview', async () => {
    for (const action of ['DELETED_FOR_ALL', 'DELETED_FOR_ME']) {
        const page = await setup();
        await page.open();
        page.element('images').children[0].emit('click');
        page.change({roomId: 'room-a', id: photo.messageId, action});
        assert.equal(page.element('images').children.length, 0);
        assert.equal(page.previewCloses(), 1);
    }
});

test('a late photo response cannot bring back an already deleted message', async () => {
    let resolvePhotos;
    const page = await setup(path => path.endsWith('/details') ? Promise.resolve({body: details})
        : new Promise(resolve => { resolvePhotos = resolve; }));
    await page.open();
    page.change({roomId: 'room-a', id: photo.messageId, action: 'DELETED_FOR_ALL'});
    resolvePhotos({body: {images: [photo], nextCursor: null}});
    await flush();
    assert.equal(page.element('images').children.length, 0);
});

test('closing cancels requests and ignores late responses when reopened', async () => {
    const pending = [];
    const page = await setup(path => new Promise(resolve => pending.push({path, resolve})));
    await page.open();
    page.element('close').emit('click');
    assert.ok(page.requests.every(request => request.options.signal.aborted));
    assert.equal(page.element('open')['aria-expanded'], 'false');
    assert.equal(page.document.activeElement, page.element('open'));
    page.flushTimers();
    assert.equal(page.element('overlay').hidden, true);
    await page.open();
    pending.slice(0, 2).forEach(item => item.resolve({body: item.path.endsWith('/details') ? details : {images: [photo]}}));
    await flush();
    assert.equal(page.element('members').children.length, 0);
    assert.equal(page.element('images').children.length, 0);
    pending.slice(2).forEach(item => item.resolve({body: item.path.endsWith('/details') ? details : {images: []}}));
    await flush();
    assert.equal(page.element('members').children.length, 2);
});

test('access denial clears previously loaded participants and photos', async () => {
    let denied = false;
    const page = await setup(async path => denied ? {status: 403}
        : {body: path.endsWith('/details') ? details : {images: [photo]}});
    await page.open();
    denied = true;
    page.element('refresh').emit('click');
    await flush();
    assert.equal(page.element('members').children.length, 0);
    assert.equal(page.element('images').children.length, 0);
    assert.match(page.element('members-status').textContent, /참여 중인 채팅방/);
});

test('shows empty and retry states, and rejects unsafe image URLs', async () => {
    const page = await setup(async path => ({body: path.endsWith('/details') ? details
        : {images: [{...photo, imgSrc: 'javascript:alert(1)'}], nextCursor: null}}));
    await page.open();
    assert.equal(page.element('images').children.length, 0);
    assert.match(page.element('images-status').textContent, /사진이 없습니다/);
    const failure = await setup(async path => path.endsWith('/details') ? {body: details} : {status: 503, body: {message: '다시 시도'}});
    await failure.open();
    assert.equal(failure.element('members').children.length, 2);
    assert.equal(failure.element('images-status').textContent, '다시 시도');
});

test('notices new images, refreshes participant changes and ignores other rooms', async () => {
    const page = await setup();
    await page.open();
    page.change({roomId: 'different', type: 'IMAGE'});
    assert.equal(page.requests.length, 2);
    page.change({roomId: 'room-a', type: 'IMAGE'});
    assert.equal(page.element('update').hidden, false);
    page.change({roomId: 'room-a', type: 'QUIT'});
    await flush();
    assert.equal(page.requests.length, 3);
    assert.ok(page.requests.at(-1).path.endsWith('/details'));
});

test('Escape closes only the drawer when no image preview is open and restores focus', async () => {
    const page = await setup();
    await page.open();
    page.element('images').children[0].emit('click');
    page.document.dispatchEvent({type: 'keydown', key: 'Escape', preventDefault() {}});
    assert.equal(page.element('open')['aria-expanded'], 'true');
    page.window.closeChatImageModal();
    let prevented = false;
    page.document.dispatchEvent({type: 'keydown', key: 'Escape', preventDefault() { prevented = true; }});
    assert.equal(prevented, true);
    assert.equal(page.element('open')['aria-expanded'], 'false');
    assert.equal(page.document.activeElement, page.element('open'));
});
