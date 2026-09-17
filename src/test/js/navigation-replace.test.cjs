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

test('inline navigation handlers replace the page and the landing redirect also uses replace', () => {
    let count = 0;
    for (const path of walk(join(resources, 'templates')).filter(p => p.endsWith('.html'))) {
        const text = readFileSync(path, 'utf8');
        for (const match of text.matchAll(/\bonclick="((?:window\.)?location\.replace\([^"\n]+\);?)"/g)) {
            const {context, replaced} = environment();
            vm.runInContext(match[1], context);
            assert.equal(replaced.length, 1, path);
            assert.match(replaced[0], /^\//, path);
            count++;
        }
    }
    assert.ok(count >= 30, 'Cover every inline navigation handler, not only the common menu');
    const html = readFileSync(join(resources, 'templates/index.html'), 'utf8');
    const {context, replaced} = environment();
    vm.runInContext(html.match(/<script[^>]*>([\s\S]*?)<\/script>/)[1], context);
    assert.deepEqual(replaced, ['/member/login']);
});

test('board profile, report and non-modal comment/favorite routes use replace', () => {
    const {context, replaced} = environment();
    vm.runInContext(source('board/common-actions.js'), context);
    context.fnComment(42);
    context.fnHref(42);
    context.fnReportBoard('message/id');
    context.fnProfile('member@example.test');
    assert.deepEqual(replaced, [
        '/comment?id=42', '/favorites?id=42', '/report?targetType=BOARD&targetId=message%2Fid',
        '/member/search/detail?email=member@example.test',
    ]);
});

test('existing comment and favorite modals stay in-page instead of forcing navigation', () => {
    const {context, replaced} = environment();
    const opened = [];
    context.openCommentModal = id => opened.push(['comment', id]);
    context.openFavoriteModal = id => opened.push(['favorite', id]);
    vm.runInContext(source('board/common-actions.js'), context);
    context.fnComment(42);
    context.fnHref(42);
    assert.deepEqual(opened, [['comment', 42], ['favorite', 42]]);
    assert.deepEqual(replaced, []);
});

test('chat room entry replaces the current list page', () => {
    const {context, replaced} = environment();
    vm.runInContext(source('chat/list.js'), context);
    context.fnClick('room-42');
    assert.deepEqual(replaced, ['/chat/chatRoom?roomId=room-42']);
});

test('all notification destinations replace the current page without weakening call URL validation', () => {
    const {context, replaced} = environment();
    vm.runInContext(source('common/common.js'), context);
    context.fnNotificationMove('CHAT', 'room-42');
    context.fnNotificationMove('CALL', '/chat/chatRoom?roomId=room-42&callId=call-1');
    context.fnNotificationMove('CALL', 'https://example.test/chat/chatRoom?roomId=room-42&callId=call-1');
    context.fnNotificationMove('CALL', '/chat/chatRoom?roomId=room-42');
    context.fnNotificationMove('COMMENT', 42);
    context.fnNotificationMove('FAVORITE', 43);
    context.fnNotificationMove('TRAN', 'account/1');
    context.fnNotificationMove('ADMIN', '/admin/appeal?appealId=1');
    assert.deepEqual(replaced, [
        '/chat/chatRoom?roomId=room-42', '/chat/chatRoom?roomId=room-42&callId=call-1',
        '/comment?id=42', '/board/view?id=43', '/account/transactions?account=account%2F1', '/admin/appeal?appealId=1',
    ]);
});

test('accepting a call replaces the page while retaining room, call and auto-accept parameters', () => {
    const {context, replaced} = environment();
    vm.runInContext(source('common/common.js'), context);
    vm.runInContext(`
        activeIncomingAudioCall = {callId: 'call-1', notificationId: 1, url: '/chat/chatRoom?roomId=room-42&callId=call-1'};
        fnCloseIncomingAudioCall = () => {};
        fnMarkNotificationAsRead = () => {};
        fnAcceptIncomingAudioCall();
    `, context);
    assert.deepEqual(replaced, ['/chat/chatRoom?roomId=room-42&callId=call-1&acceptAudioCall=call-1']);
});

for (const [file, buttonId, expected] of [
    ['member/email_find.js', 'find', '/member/updatePassword/member@example.test'],
    ['member/info_find.js', 'find', '/member/updatePassword/member@example.test'],
    ['member/updatePhone.js', 'update', '/board/main'],
]) {
    test(`${file}: successful response navigates with replace`, async () => {
        const {context, replaced} = environment();
        const elements = new Map();
        context.document.getElementById = id => {
            if (!elements.has(id)) elements.set(id, {value: 'test-value', style: {}, listeners: {},
                addEventListener(type, handler) { this.listeners[type] = handler; }});
            return elements.get(id);
        };
        context.fetch = async () => ({json: async () => ({result: 1, member: {email: 'member@example.test'}})});
        vm.runInContext(source(file), context);
        elements.get(buttonId).listeners.click({preventDefault() {}});
        await flush();
        assert.deepEqual(replaced, [expected]);
    });
}
