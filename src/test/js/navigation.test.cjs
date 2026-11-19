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
    const navigated = [];
    const location = {
        origin: 'https://www.kwanwoo.site',
        get href() { return 'https://www.kwanwoo.site/board/main'; },
        set href(value) { navigated.push(value); },
        replace: value => replaced.push(value),
    };
    const context = vm.createContext({
        URL, URLSearchParams, location,
        localStorage: {getItem: () => 'light'},
        document: {addEventListener() {}, body: {classList: {add() {}}}},
        window: {location, addEventListener() {}},
        alert() {}, console,
    });
    return {context, replaced, navigated};
}

test('all application scripts parse and only bottom navigation uses location.replace', () => {
    for (const path of walk(resources).filter(p => /\.(?:js|html)$/.test(p) && !p.includes('/lib/'))) {
        let text = readFileSync(path, 'utf8');
        if (path.endsWith('.js')) assert.doesNotThrow(() => new vm.Script(text, {filename: path}));
        if (path.endsWith('/js/common/common.js')) {
            text = text.replace(/^function (?:fnMain|fnSearch|fnWrite|fnChatting|fnNotification|fnDetail|fnNavigationLogout)\([^]*?^\}/gm, '');
        }
        if (path.endsWith('/templates/fragments/common.html')) {
            const start = text.indexOf('<nav class="navi">');
            const end = text.indexOf('</nav>', start) + '</nav>'.length;
            const navigation = text.slice(start, end);
            assert.equal(/location\s*\.\s*href\s*=/.test(navigation), false);
            text = text.slice(0, start) + text.slice(end);
        }
        assert.equal(/\blocation\s*\.\s*replace\s*\(/.test(text), false, path);
    }
});

test('bottom navigation replaces history while profile menus and notification targets preserve it', async () => {
    const {context, replaced, navigated} = environment();
    vm.runInContext(source('common/common.js'), context);
    context.fnMain();
    context.fnSearch();
    context.fnWrite();
    context.fnChatting();
    context.fnNotification();
    context.fnDetail('member@example.test');

    context.fnMyReports();
    context.fnReportApply();
    context.fnForbidden();
    context.fnNotificationMove('CHAT', 'room-1');
    context.fnNotificationMove('FAVORITE', 12);
    context.fnNotificationMove('TRAN', '9191000');

    context.fetch = async () => ({json: async () => ({result: 1})});
    context.fnNavigationLogout();
    await flush();

    assert.deepEqual(replaced, ['/board/main', '/member/search', '/board/write', '/chat/chatList',
        '/notification/list', '/member/detail?email=member@example.test', '/member/login']);
    assert.deepEqual(navigated, ['/report/my', '/admin/report', '/forbidden/list',
        '/chat/chatRoom?roomId=room-1', '/board/view?id=12', '/account/transactions?account=9191000']);
});

test('password recovery navigates only after server email verification and stores the proof', async () => {
    const {context, replaced, navigated} = environment();
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
    assert.deepEqual(navigated, ['/member/updatePassword/member%40example.test']);
    assert.deepEqual(replaced, []);
    assert.equal(JSON.parse(stored.get('passwordRecoveryVerification')).token, 'verified-token');
});
