(function (global) {
    function initFollowerModal() {
        const modal = document.getElementById('followerModal');
        const modalOverlay = document.getElementById('followerModalOverlay');
        const modalClose = document.getElementById('followerModalClose');
        const modalBody = document.getElementById('followerModalBody');

        if (!modal || !modalBody) {
            return;
        }

        let currentMemberEmail = null;
        let modalOpen = false;

        if (modalOverlay) {
            modalOverlay.addEventListener('click', () => closeFollowerModal());
        }

        if (modalClose) {
            modalClose.addEventListener('click', () => closeFollowerModal());
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

            if (state && state.followerModal && state.memberEmail) {
                await openFollowerModal(state.memberEmail, false);
                return;
            }

            if (modalOpen) {
                closeFollowerModal(true);
            }
        });

        async function openFollowerModal(memberEmail, push = true) {
            currentMemberEmail = memberEmail;
            modal.classList.remove('hidden');
            document.body.classList.add('follower-modal-open');
            modalOpen = true;

            await loadFollower(memberEmail);

            if (push) {
                history.pushState({ followerModal: true, memberEmail: memberEmail }, '', `/follow/follower?email=${memberEmail}`);
            } else {
                history.replaceState({ followerModal: true, memberEmail: memberEmail }, '', `/follow/follower?email=${memberEmail}`);
            }
        }

        function closeFollowerModal(fromPopState = false) {
            modal.classList.add('hidden');
            modalBody.innerHTML = '';
            document.body.classList.remove('follower-modal-open');
            modalOpen = false;
            currentMemberEmail = null;

            if (!fromPopState && history.state && history.state.followerModal) {
                history.back();
            }
        }

        async function loadFollower(memberEmail) {
            try {
                const response = await fetch(`/api/follow/follower?email=${memberEmail}`, {
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

                renderFollower(json);
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            }
        }

        function renderFollower(data) {
            if (data.follower.length === 0) {
                modalBody.innerHTML = '<div>팔로잉한 회원이 없습니다</div>';
                return;
            }

            modalBody.innerHTML = `
                <ul class="follower-modal-list">
                    ${data.follower.map((item) => `
                        <li class="follower-modal-item">
                            <div class="follower-modal-profile-row">
                                <img src="${escapeHtml(item.follower.profile)}" class="follower-modal-profile-img" alt="profile">
                                <a class="follower-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(item.follower.email)}">${escapeHtml(item.follower.name)}</a>
                                <span class="follower-modal-text">${escapeHtml(item.follower.email)}</span>
                                ${item.follower.email !== data.email ? `
                                    <div class="follower-modal-actions">
                                        <button type="button" id="follow${item.id}" class="btn btn-success" data-action="follow" data-follow-id="${item.id}" data-email="${escapeHtml(item.follower.email)}">${data.follow[item.id] ? 'Unfollow' : 'Follow'}</button>
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

        global.openFollowerModal = openFollowerModal;
        global.closeFollowerModal = closeFollowerModal;
    }

    global.initFollowerModal = initFollowerModal;
})(window);