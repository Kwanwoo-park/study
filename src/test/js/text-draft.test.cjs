const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const {join} = require('node:path');
const vm = require('node:vm');

test('local drafts restore only the same member and diary saving is opt-in', () => {
    const storage = new Map();
    const script = readFileSync(join(__dirname, '../../main/resources/static/js/common/text-draft.js'), 'utf8');
    function open(key, optIn = false) {
        const events = {};
        const input = {id: 'content', value: '', addEventListener: (name, callback) => { input[name] = callback; }};
        const option = optIn ? {checked: false, addEventListener: (name, callback) => { option[name] = callback; }} : null;
        const clear = {hidden: true, addEventListener() {}};
        const status = {};
        const selectors = {'[data-draft-opt-in]': option, '[data-draft-clear]': clear, '[data-draft-status]': status};
        const context = {
            localStorage: {getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value),
                removeItem: key => storage.delete(key)},
            setTimeout: () => 1, clearTimeout() {},
            addEventListener: (name, callback) => { events[name] = callback; }
        };
        context.window = context;
        vm.runInNewContext(script, context);
        const draft = context.TextDraft.bind({dataset: {draftKey: key}, querySelectorAll: () => [input],
            querySelector: selector => selectors[selector]});
        return {draft, input, option, leave: events.pagehide};
    }

    const board = open('board:1');
    board.input.value = '쓰다 만 게시글';
    board.input.input();
    board.leave();
    assert.equal(open('board:1').input.value, '쓰다 만 게시글');
    assert.equal(open('board:2').input.value, '');

    const diary = open('diary:1:new', true);
    diary.input.value = '개인 일기';
    diary.input.input();
    diary.leave();
    assert.equal(storage.size, 1);
    diary.option.checked = true;
    diary.option.change();
    assert.equal(open('diary:1:new', true).input.value, '개인 일기');
    diary.draft.clear();
    assert.equal(open('diary:1:new', true).input.value, '');
});
