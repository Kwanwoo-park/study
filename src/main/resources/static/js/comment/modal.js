(function (global) {
    const COMMENT_LIMIT = 10;
    const REPLY_LIMIT = 10;

    function initCommentModal() {
        const modal = document.getElementById('commentModal');
        const modalOverlay = document.getElementById('commentModalOverlay');
        const modalClose = document.getElementById('commentModalClose');
        const modalBody = document.getElementById('commentModalBody');
        const modalTitle = document.getElementById('commentModalTitle');
        const modalInput = document.getElementById('commentModalInput');
        const modalSubmit = document.getElementById('commentModalSubmit');
        const modalCancel = document.getElementById('commentModalCancel');

        if (!modal || !modalBody || !modalInput || !modalSubmit || modal.dataset.initialized === 'true') {
            return;
        }

        modal.dataset.initialized = 'true';

        let currentBoardId = null;
        let currentCommentId = null;
        let isReplyMode = false;
        let modalOpen = false;
        let nextCommentCursor = 1;
        let isCommentLoading = false;
        const replyStates = new Map();

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeCommentModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeCommentModal());
        }

        if (modalCancel) {
            modalCancel.addEventListener('click', () => resetComposer());
        }

        modalInput.addEventListener('keydown', async (event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                modalSubmit.click();
            }
        });

        modalBody.addEventListener('scroll', () => {
            if (modalBody.scrollTop + modalBody.clientHeight >= modalBody.scrollHeight - 10) {
                loadComments();
            }
        });

        modalSubmit.addEventListener('click', async () => {
            const value = modalInput.value.trim();

            if (!currentBoardId || !value) {
                return;
            }

            const apiUrl = isReplyMode ? '/api/reply' : '/api/comment';
            const data = isReplyMode
                ? { id: currentCommentId, reply: modalInput.value }
                : { id: currentBoardId, comments: modalInput.value };

            try {
                modalSubmit.disabled = true;
                const response = await fetch(apiUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    body: JSON.stringify(data),
                    credentials: 'include',
                });
                const json = await response.json();

                if (json.result == -1 || json.result == -2) {
                    alert('부적절한 내용 감지되었습니다');
                    return;
                }
                if (json.result == -3) {
                    alert('금칙어를 사용하여 계정이 정지되었습니다');
                    window.location.reload();
                    return;
                }
                if (json.result === -10) {
                    alert('다시 시도하여주십시오');
                    return;
                }

                const savedReplyMode = isReplyMode;
                const savedCommentId = currentCommentId;
                resetComposer();
                if (savedReplyMode && savedCommentId) {
                    resetReplyState(savedCommentId);
                }
                await loadComments(true);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오.');
            } finally {
                modalSubmit.disabled = false;
            }
        });

        modalBody.addEventListener('click', async (event) => {
            const actionTarget = event.target.closest('[data-action]');
            if (!actionTarget) return;

            const action = actionTarget.dataset.action;
            const commentId = Number(actionTarget.dataset.commentId);
            const name = actionTarget.dataset.name;

            if (action === 'reply') {
                isReplyMode = true;
                currentCommentId = commentId;
                modalCancel.style.display = 'inline';
                modalInput.value = '';
                modalInput.focus();
                return;
            }

            if (action === 'toggle-replies') {
                await toggleReplies(commentId, actionTarget);
                return;
            }

            if (action === 'load-more-replies') {
                await loadReplies(commentId);
                return;
            }

            if (action === 'edit') {
                await toggleEdit(commentId);
                return;
            }

            if (action === 'delete') {
                await deleteComment(commentId);
            }
        });

        window.addEventListener('popstate', async (event) => {
            const state = event.state;

            if (state && state.commentModal && state.boardId) {
                await openCommentModal(state.boardId, false);
                return;
            }

            if (modalOpen) {
                closeCommentModal(true);
            }
        });

        async function openCommentModal(boardId, push = true) {
            currentBoardId = boardId;
            resetComposer();
            resetCommentState();
            modal.classList.remove('hidden');
            document.body.classList.add('comment-modal-open');
            modalOpen = true;

            await loadComments(true);

            if (push) {
                history.pushState({ commentModal: true, boardId: boardId }, '', `/comment?id=${boardId}`);
            } else {
                history.replaceState({ commentModal: true, boardId: boardId }, '', `/comment?id=${boardId}`);
            }
        }

        function closeCommentModal(fromPopState = false) {
            modal.classList.add('hidden');
            modalBody.innerHTML = '';
            document.body.classList.remove('comment-modal-open');
            modalOpen = false;
            currentBoardId = null;
            resetComposer();
            resetCommentState();

            if (!fromPopState && history.state && history.state.commentModal) {
                history.back();
            }
        }

        async function loadComments(reset = false) {
            if (!currentBoardId || isCommentLoading) {
                return;
            }

            if (reset) {
                resetCommentState();
            }

            if (!nextCommentCursor) {
                return;
            }

            isCommentLoading = true;

            try {
                const response = await fetch(`/api/comment/list?id=${currentBoardId}&cursor=${nextCommentCursor - 1}&limit=${COMMENT_LIMIT}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    credentials: 'include',
                });
                const json = await response.json();

                if (json.result < 0) {
                    alert('다시 시도하여주십시오');
                    return;
                }

                nextCommentCursor = json.nextCursor;
                renderComments(json, reset);
                updateCommentCount(currentBoardId, json.totalCount);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            } finally {
                isCommentLoading = false;
            }
        }

        function renderComments(data, reset) {
            modalTitle.innerText = `댓글 ${data.totalCount}개`;

            if (reset) {
                modalBody.innerHTML = '';
            }

            if (data.list.length === 0 && modalBody.children.length === 0) {
                modalBody.innerHTML = '<div>등록된 댓글이 없습니다</div>';
                return;
            }

            let list = modalBody.querySelector('.comment-modal-list');
            if (!list) {
                list = document.createElement('ul');
                list.className = 'comment-modal-list';
                modalBody.append(list);
            }

            data.list.forEach((item) => {
                const li = document.createElement('li');
                li.className = 'comment-modal-item';
                li.innerHTML = buildCommentItem(item, data.member);
                list.append(li);
            });
        }

        function buildCommentItem(item, memberEmail) {
            return `
                <div id="commentArea${item.id}">
                    <div class="comment-modal-profile-row">
                        <img src="${escapeHtml(item.member.profile)}" class="comment-modal-profile-img" alt="profile">
                        <a class="comment-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">${escapeHtml(item.member.name)}</a>
                        ${item.member.email === memberEmail ? `
                            <div class="comment-modal-actions">
                                <button type="button" class="btn btn-primary" data-action="edit" data-comment-id="${item.id}">Edit</button>
                                <button type="button" class="btn btn-danger" data-action="delete" data-comment-id="${item.id}">Delete</button>
                            </div>
                        ` : ''}
                    </div>
                    <pre class="comment-modal-text" id="comment${item.id}">${escapeHtml(item.comments)}</pre>
                    <textarea class="form-control comment-modal-edit" id="edit_comment${item.id}" rows="4">${escapeHtml(item.comments)}</textarea>
                    <div class="comment-modal-reply-row">
                        <span class="comment-modal-reply-button" data-action="reply" data-comment-id="${item.id}" data-name="${escapeHtmlAttr(item.member.name)}">답글 달기</span>
                        ${item.replyCount > 0 ? `
                            <span class="comment-modal-reply-button" data-action="toggle-replies" data-comment-id="${item.id}">답글 ${item.replyCount}개 모두 보기</span>
                        ` : ''}
                    </div>
                </div>
            `;
        }

        async function toggleEdit(commentId) {
            const commentText = document.getElementById('comment' + commentId);
            const commentEdit = document.getElementById('edit_comment' + commentId);

            if (!commentText || !commentEdit) return;

            if (commentEdit.style.display === 'block') {
                try {
                    const response = await fetch('/api/comment/update', {
                        method: 'PATCH',
                        headers: {
                            'Content-Type': 'application/json; charset=utf-8',
                        },
                        body: JSON.stringify({
                            id: commentId,
                            comments: commentEdit.value,
                        }),
                        credentials: 'include',
                    });
                    const json = await response.json();

                    if (json.result === -1) {
                        alert('부적절한 내용 감지되었습니다');
                        return;
                    }
                    if (json.result === -3) {
                        alert('금칙어를 사용하여 계정이 정지되었습니다');
                        window.location.reload();
                        return;
                    }
                    if (json.result === -10) {
                        alert('다시 시도하여주십시오');
                        return;
                    }

                    await loadComments(true);
                } catch (error) {
                    console.error(error);
                    alert('다시 시도하여주십시오.');
                }

                return;
            }

            commentEdit.style.display = 'block';
            commentText.style.display = 'none';
        }

        async function deleteComment(commentId) {
            try {
                const response = await fetch(`/api/comment/delete?id=${currentBoardId}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    body: JSON.stringify({ id: commentId }),
                    credentials: 'include',
                });
                const json = await response.json();

                if (json.result > 0) {
                    await loadComments(true);
                    return;
                }

                alert('다시 시도하여주십시오');
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            }
        }

        async function toggleReplies(commentId, toggleTarget) {
            const commentArea = document.getElementById('commentArea' + commentId);
            const replyArea = document.getElementById('replyArea' + commentId);

            if (replyArea) {
                replyArea.remove();
                toggleTarget.innerText = toggleTarget.innerText.replace('숨기기', '모두 보기');
                return;
            }

            const area = document.createElement('div');
            area.id = 'replyArea' + commentId;
            area.className = 'comment-modal-reply-list';
            commentArea.append(area);

            await loadReplies(commentId, true);
            toggleTarget.innerText = toggleTarget.innerText.replace('모두 보기', '숨기기');
        }

        function getReplyState(commentId) {
            if (!replyStates.has(commentId)) {
                replyStates.set(commentId, {
                    nextCursor: 1,
                    items: [],
                    isLoading: false
                });
            }

            return replyStates.get(commentId);
        }

        function resetReplyState(commentId) {
            replyStates.set(commentId, {
                nextCursor: 1,
                items: [],
                isLoading: false
            });
        }

        async function loadReplies(commentId, reset = false) {
            const state = getReplyState(commentId);

            if (state.isLoading) {
                return;
            }

            if (reset) {
                resetReplyState(commentId);
            }

            const currentState = getReplyState(commentId);
            if (!currentState.nextCursor) {
                renderReplies(commentId);
                return;
            }

            currentState.isLoading = true;

            try {
                const response = await fetch(`/api/reply/list?id=${commentId}&cursor=${currentState.nextCursor - 1}&limit=${REPLY_LIMIT}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    credentials: 'include',
                });
                const json = await response.json();

                if (json.result <= 0) {
                    alert('다시 시도하여주십시오');
                    return;
                }

                currentState.items = reset ? json.list.slice() : currentState.items.concat(json.list);
                currentState.nextCursor = json.nextCursor;
                renderReplies(commentId);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            } finally {
                currentState.isLoading = false;
            }
        }

        function renderReplies(commentId) {
            const area = document.getElementById('replyArea' + commentId);
            const state = getReplyState(commentId);

            if (!area) {
                return;
            }

            area.innerHTML = '';

            state.items.forEach((item) => {
                const replyItem = document.createElement('div');
                replyItem.className = 'comment-modal-reply-item';
                replyItem.innerHTML = `
                    <img src="${escapeHtml(item.member.profile)}" class="comment-modal-profile-img" alt="profile">
                    <div class="comment-modal-reply-content">
                        <a class="comment-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">${escapeHtml(item.member.name)}</a>
                        <pre class="comment-modal-reply-text"><a href="/member/search/detail?email=${encodeURIComponent(item.commentMember.email)}">@${escapeHtml(item.commentMember.name)}</a> ${escapeHtml(item.reply)}</pre>
                    </div>
                `;
                area.append(replyItem);
            });

            if (state.nextCursor) {
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'btn btn-light btn-sm';
                button.dataset.action = 'load-more-replies';
                button.dataset.commentId = String(commentId);
                button.innerText = '답글 더 보기';
                area.append(button);
            }
        }

        function resetComposer() {
            isReplyMode = false;
            currentCommentId = null;
            modalInput.value = '';
            modalSubmit.disabled = false;
            if (modalCancel) {
                modalCancel.style.display = 'none';
            }
        }

        function resetCommentState() {
            nextCommentCursor = 1;
            isCommentLoading = false;
            replyStates.clear();
            modalBody.innerHTML = '';
        }

        function updateCommentCount(boardId, count) {
            const commentCount = document.getElementById('comment_cnt' + boardId);
            if (commentCount) {
                commentCount.innerText = count;
            }
        }

        function escapeHtml(value) {
            return String(value)
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#39;');
        }

        function escapeHtmlAttr(value) {
            return escapeHtml(value);
        }

        global.openCommentModal = openCommentModal;
        global.closeCommentModal = closeCommentModal;
    }

    global.initCommentModal = initCommentModal;
})(window);
