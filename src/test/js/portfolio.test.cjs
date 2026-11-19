const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

const directory = join(__dirname, '../../main/resources/static/portfolio');
const html = readFileSync(join(directory, 'index.html'), 'utf8');
const script = readFileSync(join(directory, 'app.js'), 'utf8');
const titles = Array.from(html.matchAll(/<article class="slide(?: active)?" data-title="([^"]+)"/g), match => match[1]);

function element() {
    const classes = new Set();
    const listeners = new Map();
    return {
        children: [], attributes: {}, style: {}, dataset: {}, textContent: '', disabled: false,
        classList: {
            toggle(name, enabled) { if (enabled) classes.add(name); else classes.delete(name); },
            contains(name) { return classes.has(name); },
        },
        appendChild(child) { this.children.push(child); },
        setAttribute(name, value) { this.attributes[name] = value; },
        addEventListener(name, callback) {
            if (!listeners.has(name)) listeners.set(name, []);
            listeners.get(name).push(callback);
        },
        dispatch(name, event = {}) { return Promise.all((listeners.get(name) || []).map(callback => callback(event))); },
        click() { this.clicked = true; },
        remove() { this.removed = true; },
        listeners,
    };
}

function setup(userAgent = 'Desktop Browser', fetchResponse = async () => ({ok: true, headers: {get: () => 'application/pdf'}, blob: async () => 'pdf-bytes'})) {
    const requests = [], downloads = [], revoked = [], timers = [];
    const slides = titles.map(title => Object.assign(element(), {dataset: {title}}));
    const controls = Object.fromEntries(['toc', 'progressBar', 'slideCounter', 'prevBtn', 'nextBtn', 'savePdfBtn', 'pdfSaveStatus'].map(id => [id, element()]));
    controls.pdfSaveStatus.hidden = true;
    const document = Object.assign(element(), {
        body: element(),
        querySelectorAll(selector) { assert.equal(selector, '.slide'); return slides; },
        getElementById(id) { assert.ok(html.includes(`id="${id}"`)); return controls[id]; },
        createElement(tag) {
            assert.ok(['button', 'a'].includes(tag));
            const created = element();
            if (tag === 'a') downloads.push(created);
            return created;
        },
    });
    const window = {
        print() { assert.fail('Saving must not open a print dialog'); },
        setTimeout(callback) { timers.push(callback); },
    };
    const URL = {createObjectURL(blob) { assert.equal(blob, 'pdf-bytes'); return 'blob:portfolio'; }, revokeObjectURL(url) { revoked.push(url); }};
    const fetch = async (url, options) => { requests.push({url, options}); return fetchResponse(); };
    vm.runInNewContext(script, {document, navigator: {userAgent}, window, fetch, URL});
    function key(key) {
        const event = {key, defaultPrevented: false, preventDefault() { this.defaultPrevented = true; }};
        document.dispatch('keydown', event);
        return event;
    }
    return {slides, document, key, requests, downloads, revoked, timers, ...controls};
}

function assertPosition(ui, index) {
    assert.equal(ui.slideCounter.textContent, `${index + 1} / ${titles.length}`);
    assert.equal(ui.progressBar.style.width, `${((index + 1) / titles.length) * 100}%`);
    assert.equal(ui.prevBtn.disabled, index === 0);
    assert.equal(ui.nextBtn.disabled, index === titles.length - 1);
    ui.slides.forEach((slide, position) => {
        assert.equal(slide.classList.contains('active'), position === index);
        assert.equal(ui.toc.children[position].classList.contains('active'), position === index);
        assert.equal(ui.toc.children[position].attributes['aria-current'], position === index ? 'step' : 'false');
    });
}

test('save downloads the server PDF only on click, prevents duplicate requests, and preserves navigation', async () => {
    const ui = setup();
    assert.equal(ui.requests.length, 0);
    assert.equal(ui.pdfSaveStatus.hidden, true);
    ui.toc.children[6].dispatch('click');
    const saving = ui.savePdfBtn.dispatch('click');
    assert.equal(ui.savePdfBtn.disabled, true);
    await ui.savePdfBtn.dispatch('click');
    await saving;
    assert.equal(ui.requests.length, 1);
    assert.equal(ui.requests[0].url, '/api/portfolio/pdf');
    assert.equal(ui.requests[0].options.cache, 'no-store');
    assert.equal(ui.downloads[0].href, 'blob:portfolio');
    assert.equal(ui.downloads[0].download, 'study-portfolio.pdf');
    assert.equal(ui.downloads[0].clicked, true);
    assert.equal(ui.downloads[0].removed, true);
    ui.timers.forEach(callback => callback());
    assert.deepEqual(ui.revoked, ['blob:portfolio']);
    assert.equal(ui.pdfSaveStatus.hidden, false);
    assert.match(ui.pdfSaveStatus.textContent, /PDF 다운로드를 요청/);
    assertPosition(ui, 6);
    assert.equal(ui.savePdfBtn.disabled, false);
    await ui.savePdfBtn.dispatch('click');
    assert.equal(ui.requests.length, 2);
    ui.nextBtn.dispatch('click');
    assertPosition(ui, 7);
});

test('non-PDF responses and network failures allow retry without downloading an error page', async () => {
    for (const fetchResponse of [async () => ({ok: true, headers: {get: () => 'text/html'}}), async () => { throw new Error('Offline'); }]) {
        const ui = setup('Mozilla/5.0 (Linux; Android 15)', fetchResponse);
        await ui.savePdfBtn.dispatch('click');
        assert.equal(ui.pdfSaveStatus.hidden, false);
        assert.match(ui.pdfSaveStatus.textContent, /다운로드하지 못했습니다/);
        assert.equal(ui.downloads.length, 0);
        assert.equal(ui.savePdfBtn.disabled, false);
        await ui.savePdfBtn.dispatch('click');
        assert.equal(ui.requests.length, 2);
        ui.key('ArrowRight');
        assertPosition(ui, 1);
    }
});
