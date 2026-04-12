(function (global) {
    const FAVORITE_LIMIT = 10;

    function initFavoriteModal() {
        const modal = document.getElementById('favoriteModal');
        const modalOverlay = document.getElementById('favoriteModalOverlay');
        const modalClose = document.getElementById('favoriteModalClose');
        const modalBody = document.getElementById('favoriteModalBody');

        if (!modal || !modalBody || modal.dataset.initialized === 'true') {
            return;
        }

        modal.dataset.initialized = 'true';

        let currentBoardId = null;
        let modalOpen = false;
        let nextCursor = 1;
        let isLoading = false;

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeFavoriteModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeFavoriteModal());
        }

        modalBody.addEventListener('scroll', () => {
            if (modalBody.scrollTop + modalBody.clientHeight >= modalBody.scrollHeight - 10) {
                loadFavorites();
            }
        });

        modalBody.addEventListener('click', async (event) => {
            const actionTarget = event.target.closest('[data-action]');
            if (!actionTarget) return;

            const action = actionTarget.dataset.action;
            const followId = actionTarget.dataset.followId;
            const email = actionTarget.dataset.email;

            if (action === 'follow') {
                await toggleFollow(Number(followId), email);
            }
        });

        window.addEventListener('popstate', async (event) => {
            const state = event.state;

            if (state && state.favoriteModal && state.boardId) {
                await openFavoriteModal(state.boardId, false);
                return;
            }

            if (modalOpen) {
                closeFavoriteModal(true);
            }
        });

        async function openFavoriteModal(boardId, push = true) {
            currentBoardId = boardId;
            nextCursor = 1;
            modal.classList.remove('hidden');
            document.body.classList.add('favorite-modal-open');
            modalOpen = true;
            modalBody.innerHTML = '';

            await loadFavorites(true);

            if (push) {
                history.pushState({ favoriteModal: true, boardId: boardId }, '', `/favorites?id=${boardId}`);
            } else {
                history.replaceState({ favoriteModal: true, boardId: boardId }, '', `/favorites?id=${boardId}`);
            }
        }

        function closeFavoriteModal(fromPopState = false) {
            modal.classList.add('hidden');
            modalBody.innerHTML = '';
            document.body.classList.remove('favorite-modal-open');
            modalOpen = false;
            currentBoardId = null;
            nextCursor = 1;

            if (!fromPopState && history.state && history.state.favoriteModal) {
                history.back();
            }
        }

        async function loadFavorites(reset = false) {
            if (!currentBoardId || isLoading) {
                return;
            }

            if (reset) {
                nextCursor = 1;
                modalBody.innerHTML = '';
            }

            if (!nextCursor) {
                return;
            }

            isLoading = true;

            try {
                const response = await fetch(`/api/favorite/list?id=${currentBoardId}&cursor=${nextCursor - 1}&limit=${FAVORITE_LIMIT}`, {
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

                renderFavorite(json, reset);
                nextCursor = json.nextCursor;
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            } finally {
                isLoading = false;
            }
        }

        function renderFavorite(data, reset) {
            if (reset) {
                modalBody.innerHTML = '';
            }

            if (data.list.length === 0 && modalBody.children.length === 0) {
                modalBody.innerHTML = '<div>아직 좋아요를 누른 회원이 없습니다</div>';
                return;
            }

            let list = modalBody.querySelector('.favorite-modal-list');
            if (!list) {
                list = document.createElement('ul');
                list.className = 'favorite-modal-list';
                modalBody.append(list);
            }

            data.list.forEach((item) => {
                const li = document.createElement('li');
                li.className = 'favorite-modal-item';
                li.innerHTML = `
                    <div class="favorite-modal-profile-row">
                        <img src="${escapeHtml(item.member.profile)}" class="favorite-modal-profile-img" alt="profile">
                        <a class="favorite-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">${escapeHtml(item.member.name)}</a>
                        <span class="favorite-modal-text">${escapeHtml(item.member.email)}</span>
                        ${item.member.email !== data.email ? `
                            <div class="favorite-modal-actions">
                                <button type="button" id="follow${item.id}" class="btn btn-success" data-action="follow" data-follow-id="${item.id}" data-email="${escapeHtml(item.member.email)}">${data.following[item.id] ? 'Unfollow' : 'Follow'}</button>
                            </div>
                        ` : ''}
                    </div>
                `;
                list.append(li);
            });
        }

        async function toggleFollow(id, email) {
            const followText = document.getElementById('follow' + id);
            const data = { email: email };

            if (!followText) return;

            const method = followText.innerText === 'Follow' ? 'POST' : 'DELETE';

            try {
                const response = await fetch('/api/follow', {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    body: JSON.stringify(data),
                    credentials: 'include',
                });

                const json = await response.json();

                if (json.result > 0) {
                    followText.innerText = method === 'POST' ? 'Unfollow' : 'Follow';
                } else {
                    alert('다시 시도하여주십시오');
                }
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
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

        global.openFavoriteModal = openFavoriteModal;
        global.closeFavoriteModal = closeFavoriteModal;
    }

    global.initFavoriteModal = initFavoriteModal;
})(window);
