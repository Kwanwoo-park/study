(function (global) {
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
                modalSubmit.click()
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
                } else if (json.result == -3) {
                    alert('금칙어를 사용하여 계정이 정지되었습니다');
                    window.location.reload();
                    return;
                } else if (json.result === -10) {
                    alert('다시 시도하여주십시오');
                    return;
                }

                resetComposer();
                await loadComments(currentBoardId);
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
            const commentId = actionTarget.dataset.commentId;
            const name = actionTarget.dataset.name;

            if (action === 'reply') {
                isReplyMode = true;
                currentCommentId = Number(commentId);
                modalCancel.style.display = 'inline';
                modalInput.value = '@' + name + ' ';
                modalInput.focus();
                return;
            }

            if (action === 'toggle-replies') {
                await toggleReplies(Number(commentId), actionTarget);
                return;
            }

            if (action === 'edit') {
                await toggleEdit(Number(commentId));
                return;
            }

            if (action === 'delete') {
                await deleteComment(Number(commentId));
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
            modal.classList.remove('hidden');
            document.body.classList.add('comment-modal-open');
            modalOpen = true;

            await loadComments(boardId);

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

            if (!fromPopState && history.state && history.state.commentModal) {
                history.back();
            }
        }

        async function loadComments(boardId) {
            try {
                const response = await fetch(`/api/comment/list?id=${boardId}`, {
                    method: 'GET',
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    credentials: 'include',
                });
                const json = await response.json();

                if (json.result !== 10) {
                    alert('다시 시도하여주십시오');
                    return;
                }

                renderComments(json);
                updateCommentCount(boardId, json.list.length);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            }
        }

        function renderComments(data) {
            modalTitle.innerText = `댓글 ${data.list.length}개`;

            if (data.list.length === 0) {
                modalBody.innerHTML = '<div>등록된 댓글이 없습니다.</div>';
                return;
            }

            modalBody.innerHTML = `
                <ul class="comment-modal-list">
                    ${data.list.map((item) => `
                        <li class="comment-modal-item">
                            <div id="commentArea${item.id}">
                                <div class="comment-modal-profile-row">
                                    <img src="${escapeHtml(item.member.profile)}" class="comment-modal-profile-img" alt="profile">
                                    <a class="comment-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">${escapeHtml(item.member.name)}</a>
                                    ${item.member.email === data.member ? `
                                        <div class="comment-modal-actions">
                                            <button type="button" class="btn btn-primary btn-sm" data-action="edit" data-comment-id="${item.id}">Edit</button>
                                            <button type="button" class="btn btn-danger btn-sm" data-action="delete" data-comment-id="${item.id}">Delete</button>
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
                        </li>
                    `).join('')}
                </ul>
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

                    await loadComments(currentBoardId);
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
                    await loadComments(currentBoardId);
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

            try {
                const response = await fetch(`/api/reply/list?id=${commentId}`, {
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

                const area = document.createElement('div');
                area.id = 'replyArea' + commentId;
                area.className = 'comment-modal-reply-list';

                json.list.forEach((item) => {
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

                commentArea.append(area);
                toggleTarget.innerText = toggleTarget.innerText.replace('모두 보기', '숨기기');
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
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
