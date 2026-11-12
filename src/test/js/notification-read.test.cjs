const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

function setup() {
    const hidden = new Set();
    const banner = {dataset: {}, classList: {add: value => hidden.add(value)}};
    const context = vm.createContext({
        document: {body: {}, addEventListener() {}, querySelector() {return null;}, getElementById: id => id === 'notification-banner' ? banner : null},
        window: {}, localStorage: {getItem() {return null;}}, console,
    });
    vm.runInContext(readFileSync(join(__dirname, '../../main/resources/static/js/common/common.js'), 'utf8'), context);
    vm.runInContext('fnUpdateUnreadNotificationDot = () => {};', context);
    return {context, banner, hidden, run: code => vm.runInContext(code, context)};
}

test('read events hide the matching popup and suppress delayed unread events, not newer ones', () => {
    const app = setup();
    app.context.notification = {id: 1, notiGroup: 'CHAT', url: 'room', readStatus: 'UNREAD', updateTime: '2026-10-21T12:00:00.123456'};
    app.banner.dataset.notification = JSON.stringify(app.context.notification);
    app.run("fnHandleNotificationReadEvent({notificationId: 1, readBefore: '2026-10-21T12:00:01'})");
    assert.equal(app.hidden.has('d-none'), true);
    assert.equal(app.run('fnApplyNotificationReadWatermarks(notification).readStatus'), 'READ');
    app.context.notification.updateTime = '2026-10-21T12:00:01.000001';
    assert.equal(app.run('fnApplyNotificationReadWatermarks(notification).readStatus'), 'UNREAD');
});

test('failed read requests do not navigate away', async () => {
    const app = setup();
    app.context.alert = () => {};
    app.run("fnMarkNotificationAsRead = () => Promise.resolve(false); fnNotificationMove = () => {throw new Error('must not navigate');}; fnMoveNotificationAfterRead(1, 'CHAT', 'room')");
    await new Promise(resolve => setImmediate(resolve));
});
