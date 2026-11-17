(function (global) {
    function initFollowModal(type) {
        const modal = document.getElementById(`${type}Modal`);
        const body = document.getElementById(`${type}ModalBody`);
        let memberEmail;
        let nextCursor = 1;
        let loading = false;
        let opened = false;

        document.getElementById(`${type}ModalOverlay`).addEventListener('click', () => close());
        document.getElementById(`${type}ModalClose`).addEventListener('click', () => close());
        body.addEventListener('scroll', () => {
            if (body.scrollTop + body.clientHeight >= body.scrollHeight - 10) load();
        });
        body.addEventListener('click', event => {
            const button = event.target.closest('[data-action="follow"]');
            if (button) toggleMemberFollow(button);
        });
        window.addEventListener('popstate', event => {
            if (event.state?.[`${type}Modal`]) open(event.state.memberEmail, false);
            else if (opened) close(true);
        });

        async function open(email, push = true) {
            memberEmail = email;
            nextCursor = 1;
            body.replaceChildren();
            modal.classList.remove('hidden');
            document.body.classList.add(`${type}-modal-open`);
            opened = true;
            await load();
            const state = {[`${type}Modal`]: true, memberEmail};
            const url = `/follow/${type}?email=${encodeURIComponent(memberEmail)}`;
            if (push) history.pushState(state, '', url);
            else history.replaceState(state, '', url);
        }

        function close(fromPopState = false) {
            modal.classList.add('hidden');
            body.replaceChildren();
            document.body.classList.remove(`${type}-modal-open`);
            opened = false;
            memberEmail = null;
            if (!fromPopState && history.state?.[`${type}Modal`]) history.back();
        }

        async function load() {
            if (!memberEmail || loading || !nextCursor) return;
            loading = true;
            try {
                const response = await fetch(`/api/follow/${type}?email=${encodeURIComponent(memberEmail)}&cursor=${nextCursor - 1}&limit=10`, {
                    credentials: 'include',
                });
                const data = await response.json();
                if (data.result < 0) throw new Error('다시 시도하여주십시오');
                render(data);
                nextCursor = data.nextCursor;
            } catch (error) {
                console.error(error);
                alert('다시 시도하여주십시오');
            } finally {
                loading = false;
            }
        }

        function render(data) {
            if (!data[type].length && !body.children.length) {
                body.textContent = type === 'follower' ? '팔로워가 없습니다' : '팔로잉 중인 회원이 없습니다';
                return;
            }
            let list = body.querySelector(`.${type}-modal-list`);
            if (!list) {
                list = document.createElement('ul');
                list.className = `${type}-modal-list`;
                body.append(list);
            }
            for (const item of data[type]) {
                const member = item[type];
                const row = document.createElement('li');
                row.className = `${type}-modal-item`;
                row.innerHTML = `
                    <div class="${type}-modal-profile-row">
                        <img src="${escapeHtml(member.profile)}" class="${type}-modal-profile-img" alt="profile">
                        <div class="${type}-modal-profile-info">
                            <a class="${type}-modal-profile-link" href="/member/search/detail?email=${encodeURIComponent(member.email)}">${escapeHtml(member.name)}</a>
                            <span class="${type}-modal-email">${escapeHtml(member.email)}</span>
                        </div>
                    </div>`;
                if (member.email !== data.email) {
                    const actions = document.createElement('div');
                    actions.className = `${type}-modal-actions`;
                    const button = document.createElement('button');
                    button.type = 'button';
                    button.className = 'btn btn-success';
                    button.dataset.action = 'follow';
                    button.dataset.email = member.email;
                    button.textContent = data.follow[item.id] ? 'Unfollow' : 'Follow';
                    actions.append(button);
                    row.firstElementChild.append(actions);
                }
                list.append(row);
            }
        }

        function escapeHtml(value) {
            return String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;')
                .replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
        }
        return {open, close};
    }

    global.initFollowerModal = () => {
        const modal = initFollowModal('follower');
        global.openFollowerModal = modal.open;
        global.closeFollowerModal = modal.close;
    };
    global.initFollowingModal = () => {
        const modal = initFollowModal('following');
        global.openFollowingModal = modal.open;
        global.closeFollowingModal = modal.close;
    };
})(window);
