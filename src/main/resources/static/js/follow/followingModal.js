(function (global) {
    function initFollowingModal() {
        const modal = document.getElementById('followingModal');
        const modalOverlay = document.getElementById('followingModalOverlay');
        const modalClose = document.getElementById('followingModalClose');
        const modalBody = document.getElementById('followingModalBody');

        if (!modal || !modalBody) {
            return;
        }

        let currentMemberEmail = null;
        let modalOpen = false;

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeFollowingModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeFollowingModal());
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

            if (state && state.followingModal && state.memberEmail) {
                await openFollowingModal(state.memberEmail, false);
                return;
            }

            if (modalOpen) {
                closeFollowingModal(true);
            }
        });

        async function openFollowingModal(memberEmail, push = true) {
            currentMemberEmail = memberEmail;
            modal.classList.remove('hidden');
            document.body.classList.add('following-modal-open');
            modalOpen = true;

            await loadFollowing(memberEmail);

            if (push) {
                history.pushState({ followingModal: true, memberEmail: memberEmail }, '', `/follow/following?email=${memberEmail}`);
            } else {
                history.replaceState({ followingModal: true, memberEmail: memberEmail }, '', `/follow/following?email=${memberEmail}`);
            }
        }

        function closeFollowingModal(fromPopState = false) {
            modal.classList.add('hidden');
            modalBody.innerHTML = '';
            document.body.classList.remove('following-modal-open');
            modalOpen = false;
            currentMemberEmail = null;

            if (!fromPopState && history.state && history.state.followingModal) {
                history.back();
            }
        }

        async function loadFollowing(memberEmail) {
            try {
                const response = await fetch(`/api/follow/following?email=${memberEmail}`, {
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

                renderFollowing(json);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            }
        }

        function renderFollowing(data) {
            if (data.following.length === 0) {
                modalBody.innerHTML = '<div>팔로잉 중인 회원이 없습니다</div>';
                return;
            }

            modalBody.innerHTML = `
                <ul class="following-modal-list">
                    ${data.following.map((item) => `
                        <li class="following-modal-item">
                            <div class="following-modal-profile-row">
                                <img src="${escapeHtml(item.following.profile)}" class="following-modal-profile-img" alt="profile">
                                <a class="following-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.following.email)}">${escapeHtml(item.following.name)}</a>
                                <span class="following-modal-text">${escapeHtml(item.following.email)}</span>
                                ${item.following.email !== data.email ? `
                                    <div class="following-modal-actions">
                                        <button type="button" id="follow${item.id}" class="btn btn-success" data-action="follow" data-follow-id="${item.id}" data-email="${escapeHtml(item.following.email)}">${data.follow[item.id] ? 'Unfollow' : 'Follow'}</button>
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

            const data = { email : email }

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
                } else
                    alert("다시 시도하여주십시오");
            } catch (error) {
                console.error(error);
                alert("다시 시도하여주십시오");
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

        global.openFollowingModal = openFollowingModal;
        global.closeFollowingModal = closeFollowingModal;
    }

    global.initFollowingModal = initFollowingModal;
})(window);