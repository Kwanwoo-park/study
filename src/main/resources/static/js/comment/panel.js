(function (global) {
    const PAGE_SIZE = 10;

    function createCommentPanel({list, input, submit, cancel, scrollArea = list, standalone = false, onCount}) {
        let view;
        let replyTo;
        let saving = false;

        scrollArea.addEventListener('scroll', () => {
            if (scrollArea.scrollTop + scrollArea.clientHeight >= scrollArea.scrollHeight - 10) loadComments();
        });
        cancel.addEventListener('click', resetComposer);
        submit.addEventListener('click', submitComment);
        input.addEventListener('keydown', event => {
            if (event.key !== 'Enter' || event.shiftKey || event.isComposing) return;
            event.preventDefault();
            submit.click();
        });
        list.addEventListener('click', event => {
            const action = event.target.closest('[data-comment-action]');
            if (!action) return;
            const row = action.closest('[data-comment-id]');
            const id = Number(row.dataset.commentId);

            switch (action.dataset.commentAction) {
                case 'reply':
                    replyTo = id;
                    input.value = standalone ? '@' + row.querySelector('.comment-modal-profile-link').textContent + ' ' : '';
                    cancel.classList.remove('is-hidden');
                    input.focus();
                    break;
                case 'replies':
                    toggleReplies(row);
                    break;
                case 'edit':
                    editComment(row, id);
                    break;
                case 'delete':
                    save(`/api/comment/delete?id=${view.boardId}`, 'DELETE', {id}, refresh);
                    break;
            }
        });

        function open(boardId, returnUrl = '') {
            resetComposer();
            return refresh(boardId, returnUrl);
        }

        function close() {
            view = null;
            list.replaceChildren();
            resetComposer();
        }

        function refresh(boardId = view.boardId, returnUrl = view.returnUrl) {
            view = {boardId, returnUrl, nextCursor: 1, loading: false};
            list.replaceChildren();
            return loadComments();
        }

        async function loadComments() {
            const current = view;
            if (!current || current.loading || !current.nextCursor) return;

            current.loading = true;
            try {
                const data = await request(listUrl('comment', current.boardId, current.nextCursor));
                if (current !== view) return;

                current.nextCursor = data.nextCursor;
                renderComments(data, current.returnUrl);
                onCount?.(current.boardId, data.totalCount);
            } catch (error) {
                if (current === view) showError(error);
            } finally {
                current.loading = false;
            }
        }

        function renderComments(data, returnUrl) {
            list.querySelector('#commentLoadMore')?.remove();

            if (list.children.length === 0 && data.list.length === 0) {
                const empty = document.createElement('li');
                empty.className = 'comment-modal-item';
                empty.textContent = '등록된 댓글이 없습니다';
                list.append(empty);
            }

            for (const item of data.list) {
                const row = document.createElement('li');
                row.className = 'comment-modal-item';
                row.dataset.commentId = item.id;
                row.innerHTML = commentMarkup(item, data.member, returnUrl);
                list.append(row);
            }

            if (standalone && data.nextCursor) {
                const more = document.createElement('li');
                more.id = 'commentLoadMore';
                more.className = 'comment-modal-item';
                more.append(moreButton('댓글 더 보기', loadComments));
                list.append(more);
            }
        }

        function submitComment() {
            if (!view || !input.value.trim()) return;
            const commentId = replyTo;

            if (!commentId) {
                return save('/api/comment', 'POST', {id: view.boardId, comments: input.value}, () => {
                    resetComposer();
                    return refresh();
                });
            }

            return save('/api/reply', 'POST', {id: commentId, reply: input.value}, () => {
                resetComposer();
                const row = list.querySelector(`[data-comment-id="${commentId}"]`);
                if (standalone && row) return showReplies(row);
                return refresh();
            });
        }

        function resetComposer() {
            replyTo = null;
            input.value = '';
            cancel.classList.add('is-hidden');
        }

        function editComment(row, id) {
            const editor = row.querySelector('.comment-modal-edit');
            const text = row.querySelector('.comment-modal-text');
            if (editor.classList.contains('is-hidden')) {
                editor.classList.remove('is-hidden');
                text.classList.add('is-hidden');
                editor.focus();
                return;
            }
            return save('/api/comment/update', 'PATCH', {id, comments: editor.value}, refresh);
        }

        async function save(url, method, body, onSaved) {
            if (saving) return;
            const current = view;
            saving = true;
            submit.disabled = true;
            try {
                const data = await request(url, method, body);
                if (current === view && data) await onSaved();
            } catch (error) {
                if (current === view) showError(error);
            } finally {
                saving = false;
                submit.disabled = false;
            }
        }

        function toggleReplies(row) {
            const area = row.querySelector('.comment-modal-reply-list');
            if (area) {
                area.remove();
                setRepliesExpanded(row, false);
            } else {
                return showReplies(row);
            }
        }

        function showReplies(row) {
            row.querySelector('.comment-modal-reply-list')?.remove();
            const area = document.createElement('div');
            area.className = 'comment-modal-reply-list';
            row.append(area);
            setRepliesExpanded(row, true);
            return loadReplies(area, row.dataset.commentId, 1);
        }

        function setRepliesExpanded(row, expanded) {
            const toggle = row.querySelector('[data-comment-action="replies"]');
            if (!toggle) return;
            toggle.setAttribute('aria-expanded', String(expanded));
            toggle.textContent = `답글 ${toggle.dataset.count}개 ${expanded ? '숨기기' : '모두 보기'}`;
        }

        async function loadReplies(area, id, cursor, button) {
            if (button) button.disabled = true;
            try {
                const data = await request(listUrl('reply', id, cursor));
                if (!list.contains(area)) return;

                button?.remove();
                for (const item of data.list) {
                    const row = document.createElement('div');
                    row.className = 'comment-modal-reply-item';
                    row.innerHTML = replyMarkup(item);
                    area.append(row);
                }
                if (data.nextCursor) {
                    area.append(moreButton('답글 더 보기', next => loadReplies(area, id, data.nextCursor, next)));
                }
            } catch (error) {
                if (!list.contains(area)) return;
                if (button) button.disabled = false;
                showError(error);
            }
        }

        return {open, close};
    }

    function listUrl(type, id, cursor) {
        return `/api/${type}/list?id=${encodeURIComponent(id)}&cursor=${cursor - 1}&limit=${PAGE_SIZE}`;
    }

    async function request(url, method = 'GET', body) {
        const options = {method, credentials: 'include'};
        if (body) {
            options.headers = {'Content-Type': 'application/json; charset=utf-8'};
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        const data = await response.json();

        if (method === 'POST' || method === 'PATCH') {
            if (data.result === -3) {
                alert('금칙어를 사용하여 계정이 정지되었습니다');
                window.location.reload();
                return null;
            }
            if (data.result === -1 || data.result === -2) {
                alert('부적절한 내용이 감지되었습니다');
                return null;
            }
        }
        if (!response.ok || !(data.result > 0)) throw new Error('댓글 또는 답글 요청 실패');
        return data;
    }

    function showError(error) {
        console.error(error);
        alert('다시 시도해 주세요');
    }

    function moreButton(text, load) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-light btn-sm';
        button.textContent = text;
        button.addEventListener('click', () => load(button));
        return button;
    }

    function profileLink(member, prefix = '') {
        return `<a class="comment-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(member.email)}">${prefix}${escapeHtml(member.name)}</a>`;
    }

    function profileImage(member) {
        return `<img src="${escapeHtml(member.profile)}" class="comment-modal-profile-img" alt="프로필">`;
    }

    function commentMarkup(item, memberEmail, returnUrl) {
        let reportUrl = `/report?targetType=COMMENT&targetId=${encodeURIComponent(item.id)}`;
        if (returnUrl) reportUrl += `&amp;returnUrl=${encodeURIComponent(returnUrl)}`;
        const actions = item.member.email === memberEmail
            ? `<button type="button" class="comment-text-button" data-comment-action="edit">Edit</button>
               <button type="button" class="comment-text-button" data-comment-action="delete">Delete</button>`
            : `<a class="comment-modal-report-link" href="${reportUrl}">신고</a>`;

        return `
            <div class="comment-modal-profile-row">
                ${profileImage(item.member)}
                ${profileLink(item.member)}
                <div class="comment-modal-actions">${actions}</div>
            </div>
            <pre class="comment-modal-text">${escapeHtml(item.comments)}</pre>
            <textarea class="form-control comment-modal-edit is-hidden" rows="4" aria-label="댓글 수정">${escapeHtml(item.comments)}</textarea>
            <div class="comment-modal-reply-row">
                <button type="button" class="comment-text-button" data-comment-action="reply">답글 달기</button>
                ${item.replyCount > 0 ? `<button type="button" class="comment-text-button" data-comment-action="replies" data-count="${item.replyCount}" aria-expanded="false">답글 ${item.replyCount}개 모두 보기</button>` : ''}
            </div>`;
    }

    function replyMarkup(item) {
        return `
            ${profileImage(item.member)}
            <div class="comment-modal-reply-content">
                ${profileLink(item.member)}
                <pre class="comment-modal-reply-text">${profileLink(item.commentMember, '@')} ${escapeHtml(item.reply)}</pre>
            </div>`;
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    global.createCommentPanel = createCommentPanel;
})(window);
