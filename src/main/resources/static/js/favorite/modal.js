(function (global) {
    function initFavoriteModal() {
        const modal = document.getElementById('favoriteModal');
        const modalOverlay = document.getElementById('favoriteModalOverlay');
        const modalClose = document.getElementById('favoriteModalClose');
        const modalBody = document.getElementById('favoriteModalBody');

        if (!modal || !modalBody) {
            return;
        }

        let currentBoardId = null;
        let modalOpen = false;

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeFavoriteModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeFavoriteModal());
        }

        modalBody.addEventListener('click', async (event) => {
            const actionTarget = event.target.closest('[data-action]');
            if (!actionTarget) return;

            const action = actionTarget.dataset.action;
            const followId = actionTarget.dataset.followId;
            const email = actionTarget.dataset.email;

            if (action === 'follow') {
                await toggleFollow(Number(followId), email);
                return;
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
            modal.classList.remove('hidden');
            document.body.classList.add('favorite-modal-open');
            modalOpen = true;

            await loadFavorites(boardId);

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

            if (!fromPopState && history.state && history.state.favoriteModal) {
                history.back();
            }
        }

        async function loadFavorites(boardId) {
            try {
                const response = await fetch(`/api/favorite/list?id=${boardId}`, {
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

                renderFavorite(json);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            }
        }

        function renderFavorite(data) {
            if (data.list.length === 0) {
                modalBody.innerHTML = '<div>아직 좋아요를 누른 회원이 없습니다</div>';
                return;
            }

            modalBody.innerHTML = `
                <ul class="favorite-modal-list">
                    ${data.list.map((item) => `
                        <li class="favorite-modal-item">
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
                        </li>
                    `).join('')}
                </ul>
            `;
        }

        async function toggleFollow(id, email) {
            const followText = document.getElementById('follow' + id);

            const data = { email: email }

            let method;

            if (!followText) return;

            if (followText.innerText === "Follow")
                method = 'POST';
            else
                method = 'DELETE';

            try {
                const response = await fetch(`/api/follow`, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json; charset=utf-8',
                    },
                    body: JSON.stringify(data),
                    credentials: 'include',
                });

                const json = await response.json();

                if (json.result > 0) {
                    if (method === 'POST')
                        followText.innerText = 'Unfollow';
                    else
                        followText.innerText = 'Follow';
                }
                else
                    alert("다시 시도하여주십시오");
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오.');
            }
        }

        function escapeHtml(value) {
            return String(value)
                .replaceAll('&', '&amp;')
                .replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;')
                .replaceAll('"', '&quot;')
                .replaceAll("'", '&#39;')
        }

        global.openFavoriteModal = openFavoriteModal;
        global.closeFavoriteModal = closeFavoriteModal;
    }

    global.initFavoriteModal = initFavoriteModal;
})(window);