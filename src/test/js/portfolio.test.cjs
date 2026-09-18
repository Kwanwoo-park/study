const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

const directory = join(__dirname, '../../main/resources/static/portfolio');
const html = readFileSync(join(directory, 'index.html'), 'utf8');
const script = readFileSync(join(directory, 'app.js'), 'utf8');
const printCss = readFileSync(join(directory, 'print.css'), 'utf8');
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
        dispatch(name, event = {}) { (listeners.get(name) || []).forEach(callback => callback(event)); },
        listeners,
    };
}

function setup(userAgent = 'Desktop Browser', printMode = 'supported') {
    let printCalls = 0;
    const slides = titles.map(title => Object.assign(element(), {dataset: {title}}));
    const controls = Object.fromEntries(['toc', 'progressBar', 'slideCounter', 'prevBtn', 'nextBtn', 'savePdfBtn', 'pdfSaveStatus'].map(id => [id, element()]));
    controls.pdfSaveStatus.hidden = true;
    const document = Object.assign(element(), {
        body: element(),
        querySelectorAll(selector) { assert.equal(selector, '.slide'); return slides; },
        getElementById(id) { assert.ok(html.includes(`id="${id}"`)); return controls[id]; },
        createElement(tag) { assert.equal(tag, 'button'); return element(); },
    });
    const window = printMode === 'unsupported' ? {} : {
        print() {
            printCalls++;
            if (printMode === 'throws') throw new Error('Printing unavailable');
        },
    };
    vm.runInNewContext(script, {document, navigator: {userAgent}, window});
    function key(key) {
        const event = {key, defaultPrevented: false, preventDefault() { this.defaultPrevented = true; }};
        document.dispatch('keydown', event);
        return event;
    }
    return {slides, document, key, printCalls: () => printCalls, ...controls};
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

test('portfolio includes the new sections and a matching fallback slide count', () => {
    assert.equal(titles.length, 13);
    assert.equal(new Set(titles).size, titles.length);
    for (const title of ['Private Storage', 'Operations', 'Refactoring']) assert.ok(titles.includes(title));
    assert.match(html, new RegExp(`id="slideCounter">1 / ${titles.length}<`));
    assert.match(html, /href="\/member\/login" data-page-back/);
    assert.equal(Array.from(html.matchAll(/<article class="slide active"/g)).length, 1);
});

test('table of contents is built from all slides and initially selects the overview', () => {
    const ui = setup();
    assert.equal(ui.toc.children.length, titles.length);
    ui.toc.children.forEach((button, index) => {
        assert.equal(button.type, 'button');
        assert.equal(button.textContent, `${String(index + 1).padStart(2, '0')} ${titles[index]}`);
    });
    assertPosition(ui, 0);
});

test('left and right keys traverse every slide without wrapping past either boundary', () => {
    const ui = setup();
    assert.equal(ui.key('ArrowLeft').defaultPrevented, true);
    assertPosition(ui, 0);
    for (let index = 1; index < titles.length; index++) {
        assert.equal(ui.key('ArrowRight').defaultPrevented, true);
        assertPosition(ui, index);
    }
    ui.key('ArrowRight');
    assertPosition(ui, titles.length - 1);
    for (let index = titles.length - 2; index >= 0; index--) {
        ui.key('ArrowLeft');
        assertPosition(ui, index);
    }
});

test('previous, next, and table of contents buttons update the same slide state', () => {
    const ui = setup();
    ui.nextBtn.dispatch('click');
    assertPosition(ui, 1);
    ui.prevBtn.dispatch('click');
    assertPosition(ui, 0);
    ui.toc.children.forEach((button, index) => {
        button.dispatch('click');
        assertPosition(ui, index);
    });
});

test('scroll, touch, and unrelated keys do not switch slides or prevent page scrolling', () => {
    const ui = setup();
    ui.nextBtn.dispatch('click');
    for (const key of ['ArrowUp', 'ArrowDown', 'PageUp', 'PageDown', ' ', 'Tab']) {
        assert.equal(ui.key(key).defaultPrevented, false);
        assertPosition(ui, 1);
    }
    for (const name of ['wheel', 'scroll', 'touchstart', 'touchmove', 'touchend']) {
        assert.equal(ui.document.listeners.has(name), false);
        ui.document.dispatch(name);
        assertPosition(ui, 1);
    }
});

test('iOS and Android retain mobile mode while desktop keeps its existing layout', () => {
    for (const userAgent of ['Mozilla/5.0 (iPhone)', 'Mozilla/5.0 (iPad)', 'Mozilla/5.0 (Linux; Android 15)']) {
        const ui = setup(userAgent);
        assert.equal(ui.document.body.classList.contains('mobile-view'), true);
        ui.nextBtn.dispatch('click');
        assertPosition(ui, 1);
    }
    assert.equal(setup().document.body.classList.contains('mobile-view'), false);
});

test('save button prints the current document only when clicked and preserves the selected slide', () => {
    const ui = setup();
    assert.equal(ui.printCalls(), 0);
    assert.equal(ui.pdfSaveStatus.hidden, true);
    ui.toc.children[6].dispatch('click');
    ui.savePdfBtn.dispatch('click');
    assert.equal(ui.printCalls(), 1);
    assert.equal(ui.pdfSaveStatus.hidden, false);
    assert.match(ui.pdfSaveStatus.textContent, /PDF로 저장/);
    assertPosition(ui, 6);
    // Printing or cancelling does not leave a disabled button or change navigation.
    assert.equal(ui.savePdfBtn.disabled, false);
    ui.savePdfBtn.dispatch('click');
    assert.equal(ui.printCalls(), 2);
    ui.nextBtn.dispatch('click');
    assertPosition(ui, 7);
});

test('missing or failing print support gives instructions without breaking slide navigation', () => {
    for (const mode of ['unsupported', 'throws']) {
        const ui = setup('Mozilla/5.0 (Linux; Android 15)', mode);
        ui.savePdfBtn.dispatch('click');
        assert.equal(ui.pdfSaveStatus.hidden, false);
        assert.match(ui.pdfSaveStatus.textContent, /Chrome 또는 Safari/);
        assert.equal(ui.printCalls(), mode === 'throws' ? 1 : 0);
        ui.key('ArrowRight');
        assertPosition(ui, 1);
    }
});

test('print-only layout exposes all slides and removes screen clipping and navigation', () => {
    assert.match(html, /<link[^>]+href="\.\/print\.css\?[^\"]+"[^>]+media="print"/);
    assert.match(html, /id="savePdfBtn"[^>]+aria-describedby="pdfSaveStatus"[^>]*>저장<\/button>/);
    assert.match(html, /id="pdfSaveStatus"[^>]+role="status" hidden/);
    assert.match(printCss, /@page\s*\{[^}]*size:\s*A4 landscape/);
    const slideRule = printCss.match(/(?:^|\n)\.slide\s*\{([^}]+)\}/)[1];
    for (const declaration of ['display: block !important', 'max-height: none !important', 'overflow: visible !important', 'break-after: page']) {
        assert.ok(slideRule.includes(declaration), declaration);
    }
    assert.match(printCss, /\.sidebar,\s*\.progress,\s*\.controls\s*\{\s*display: none !important/);
    assert.match(printCss, /\.slide:last-of-type\s*\{[^}]*break-after: auto/);
});
