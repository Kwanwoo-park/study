(function (global) {
    function initCommentModal() {
        const byId = id => document.getElementById('commentModal' + id);
        const modal = byId('');
        const panel = createCommentPanel({
            list: byId('List'),
            scrollArea: byId('Body'),
            input: byId('Input'),
            submit: byId('Submit'),
            cancel: byId('Cancel'),
            onCount(boardId, count) {
                byId('Title').textContent = `댓글 ${count}개`;
                const counter = document.getElementById('comment_cnt' + boardId);
                if (counter) counter.textContent = count;
            },
        });

        function openCommentModal(boardId, push = true) {
            const previous = history.state;
            const returnTo = previous?.commentReturnUrl || previous?.boardModalBaseUrl;
            let returnUrl = window.location.pathname + window.location.search + window.location.hash;
            if (typeof returnTo === 'string' && returnTo.startsWith('/') && !returnTo.startsWith('//')) returnUrl = returnTo;

            modal.classList.remove('hidden');
            document.body.classList.add('comment-modal-open');
            byId('Title').textContent = '댓글';
            const state = {commentModal: true, boardId, commentReturnUrl: returnUrl};
            const url = `/comment?id=${encodeURIComponent(boardId)}`;
            if (push) history.pushState(state, '', url);
            else history.replaceState(state, '', url);
            return panel.open(boardId, returnUrl);
        }

        function closeCommentModal(fromPopState = false) {
            modal.classList.add('hidden');
            document.body.classList.remove('comment-modal-open');
            panel.close();
            if (!fromPopState && history.state?.commentModal) history.back();
        }

        byId('Overlay').addEventListener('click', () => closeCommentModal());
        byId('Close').addEventListener('click', () => closeCommentModal());
        window.addEventListener('popstate', event => {
            if (event.state?.commentModal && event.state.boardId) openCommentModal(event.state.boardId, false);
            else if (!modal.classList.contains('hidden')) closeCommentModal(true);
        });

        global.openCommentModal = openCommentModal;
        global.closeCommentModal = closeCommentModal;
    }

    global.initCommentModal = initCommentModal;
})(window);
