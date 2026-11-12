const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync, readdirSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');
const resources = join(__dirname, '../../main/resources');
const source = name => readFileSync(join(resources, 'static/js', name), 'utf8');
const flush = () => new Promise(resolve => setImmediate(resolve));

function walk(directory) {
    return readdirSync(directory, {withFileTypes: true})
        .flatMap(entry => entry.isDirectory() ? walk(join(directory, entry.name)) : [join(directory, entry.name)]);
}

function environment() {
    const replaced = [];
    const location = {
        origin: 'https://www.kwanwoo.site',
        get href() { return 'https://www.kwanwoo.site/board/main'; },
        set href(value) { throw new Error('Navigation must use replace, not href'); },
        assign() { throw new Error('Navigation must use replace, not assign'); },
        replace: value => replaced.push(value),
    };
    const context = vm.createContext({
        URL, URLSearchParams, location,
        localStorage: {getItem: () => 'light'},
        document: {addEventListener() {}, body: {classList: {add() {}}}},
        window: {location, addEventListener() {}},
        alert() {}, console,
    });
    return {context, replaced};
}

test('all application scripts parse and no script or template writes location.href or calls assign', () => {
    const forbidden = /\blocation\s*\.\s*(?:(?:href|pathname|search|hash)\s*=(?!=)|assign\s*\()|\b(?:window|document|self|top|parent)\s*\.\s*location\s*=(?!=)|(?:^|[;{}])\s*location\s*=(?!=)/m;
    for (const path of walk(resources).filter(p => /\.(?:js|html)$/.test(p) && !p.includes('/lib/'))) {
        const text = readFileSync(path, 'utf8');
        assert.equal(forbidden.test(text), false, path);
        if (path.endsWith('.js')) assert.doesNotThrow(() => new vm.Script(text, {filename: path}));
    }
});

test('password recovery navigates only after server email verification and stores the proof', async () => {
    const {context, replaced} = environment();
    const elements = new Map();
    const stored = new Map();
    context.sessionStorage = {setItem: (key, value) => stored.set(key, value)};
    context.document.getElementById = id => {
        if (!elements.has(id)) elements.set(id, {value: 'member@example.test', classList: {remove() {}}, listeners: {},
            addEventListener(type, handler) { this.listeners[type] = handler; }});
        return elements.get(id);
    };
    context.fetch = async (url, request) => {
        assert.equal(url, '/api/mail/verify');
        assert.equal(JSON.parse(request.body).email, 'member@example.test');
        return {ok: true, json: async () => ({status: 'ok', verificationToken: 'verified-token'})};
    };
    vm.runInContext(source('member/email_find.js'), context);
    await elements.get('find').listeners.click();
    assert.deepEqual(replaced, ['/member/updatePassword/member%40example.test']);
    assert.equal(JSON.parse(stored.get('passwordRecoveryVerification')).token, 'verified-token');
});
