const followContainer = document.querySelector('.container');
const followList = document.getElementById('followList');

const FOLLOW_LIMIT = 10;

let nextFollowCursor = 1;
let followLoading = false;

window.onload = function() {
    loadMoreFollow(true);
};

followList.addEventListener('scroll', () => {
    if (followList.scrollTop + followList.clientHeight >= followList.scrollHeight - 10) loadMoreFollow();
});
followList.addEventListener('click', event => {
    const button = event.target.closest('[data-action="follow"]');
    if (button) toggleMemberFollow(button);
});

async function loadMoreFollow(reset = false) {
    if (followLoading) {
        return;
    }

    if (reset) {
        nextFollowCursor = 1;
        followList.innerHTML = '';
    }

    if (!nextFollowCursor) {
        return;
    }

    followLoading = true;

    try {
        const targetEmail = followContainer.dataset.targetEmail;
        const mode = followContainer.dataset.mode;
        const response = await fetch(`/api/follow/${mode}?email=${encodeURIComponent(targetEmail)}&cursor=${nextFollowCursor - 1}&limit=${FOLLOW_LIMIT}`, {
            credentials: 'include',
        });
        const json = await response.json();

        if (json.result < 0) {
            alert('다시 시도하여주십시오');
            return;
        }

        renderFollow(json, mode);
        nextFollowCursor = json.nextCursor;
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        followLoading = false;
    }
}

function renderFollow(data, mode) {
    const items = data[mode];
    const emptyMessage = mode === 'follower' ? '팔로워가 없습니다' : '팔로잉 중인 회원이 없습니다';
    const startIndex = followList.children.length;

    if (items.length === 0 && startIndex === 0) {
        const empty = document.createElement('li');
        empty.className = 'list-group-item';
        empty.innerText = emptyMessage;
        followList.append(empty);
        return;
    }

    items.forEach((item, index) => {
        const profile = item[mode];
        const li = document.createElement('li');
        li.className = 'list-group-item follow-list-item';
        li.innerHTML = `
            <div class="follow-list-row">
                <span class="follow-list-index">${startIndex + index + 1}</span>
                <img src="${escapeHtml(profile.profile)}" class="follow-list-profile-img" alt="profile">
                <div class="follow-list-profile-info">
                    <a class="follow-list-name" href="/member/search/detail?email=${encodeURIComponent(profile.email)}">
                        ${escapeHtml(profile.name)}
                    </a>
                    <span class="follow-list-email">${escapeHtml(profile.email)}</span>
                </div>
                ${profile.email !== data.email ? `
                    <div class="follow-list-actions">
                        <button type="button" class="btn btn-success" data-action="follow" data-email="${escapeHtml(profile.email)}">${data.follow[item.id] ? 'Unfollow' : 'Follow'}</button>
                    </div>
                ` : ''}
            </div>
        `;
        followList.append(li);
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
