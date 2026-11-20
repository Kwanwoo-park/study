(function () {
    const draft = document.getElementById('chat-reply-draft');
    const draftText = document.getElementById('chat-reply-draft-text');
    const unavailable = new Set();
    let replyingTo = null;

    function label(id) {
        const row = document.getElementById('chat-message-' + id);
        if (unavailable.has(id) || row?.classList.contains('deleted')) return '볼 수 없는 메시지';
        if (!row) return '이전 메시지';
        const text = row.dataset.messageType === 'IMAGE' ? '사진'
                : row.querySelector('.chat-message-text')?.textContent.trim().slice(0, 80);
        return [row.dataset.messageAuthor, text].filter(Boolean).join(' · ') || '이전 메시지';
    }

    function refresh() {
        for (const element of document.querySelectorAll('[data-chat-quote]')) {
            const id = element.dataset.chatQuote;
            element.textContent = '답장 · ' + label(id);
            element.disabled = unavailable.has(id);
        }
        if (replyingTo) draftText.textContent = '답장 · ' + label(replyingTo);
    }

    function clear(id = replyingTo) {
        if (replyingTo !== id) return;
        replyingTo = null;
        draft.hidden = true;
    }

    function invalidate(id) {
        unavailable.add(id);
        clear(id);
        refresh();
    }

    function select(id) {
        if (unavailable.has(id)) return;
        replyingTo = id;
        draft.hidden = false;
        refresh();
        document.getElementById('chat').focus();
    }

    function quote(id, jump) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'chat-quote';
        button.dataset.chatQuote = id;
        button.addEventListener('click', () => jump(id));
        return button;
    }

    document.getElementById('chat-reply-cancel').addEventListener('click', () => clear());
    window.ChatQuotes = {select, clear, refresh, invalidate, quote, current: () => replyingTo};
})();
