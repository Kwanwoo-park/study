const followContainer = document.querySelector('.container');
const followList = document.getElementById('followList');

const FOLLOW_LIMIT = 10;

let nextFollowCursor = 1;
let followLoading = false;

window.onload = function() {
    loadMoreFollow(true);
};

if (followContainer) {
    followContainer.addEventListener('scroll', () => {
        if (followContainer.scrollTop + followContainer.clientHeight >= followContainer.scrollHeight - 10) {
            loadMoreFollow();
        }
    });
}

window.addEventListener('scroll', () => {
    if (window.innerHeight + window.scrollY >= document.body.offsetHeight - 10) {
        loadMoreFollow();
    }
});

async function loadMoreFollow(reset = false) {
    if (!followContainer || !followList || followLoading) {
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
        const apiPath = mode === 'follower' ? 'follower' : 'following';
        const response = await fetch(`/api/follow/${apiPath}?email=${encodeURIComponent(targetEmail)}&cursor=${nextFollowCursor - 1}&limit=${FOLLOW_LIMIT}`, {
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

        renderFollow(json, mode, reset);
        nextFollowCursor = json.nextCursor;
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        followLoading = false;
    }
}

function renderFollow(data, mode, reset) {
    if (reset) {
        followList.innerHTML = '';
    }

    const items = mode === 'follower' ? data.follower : data.following;
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
        const profile = mode === 'follower' ? item.follower : item.following;
        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = `
            <span>
                <span>${startIndex + index + 1}</span>
                <img src="${escapeHtml(profile.profile)}" width="30vw" height="30vw" style="border-radius: 90px" alt="profile">
                <a href="/member/search/detail?email=${encodeURIComponent(profile.email)}">
                    <span>${escapeHtml(profile.name)}</span>
                </a>
                <span>${escapeHtml(profile.email)}</span>
                ${profile.email !== data.email ? `
                    <div class="float-right">
                        <button type="button" id="follow${item.id}" class="btn btn-success" onclick="fnFollow(${item.id}, '${escapeJsString(profile.email)}')">${data.follow[item.id] ? 'Unfollow' : 'Follow'}</button>
                    </div>
                ` : ''}
            </span>
        `;
        followList.append(li);
    });
}

function fnFollow(listId, memberEmail) {
    const btn = document.getElementById('follow' + listId);
    if (!btn) return;

    const method = btn.innerText === 'Follow' ? 'POST' : 'DELETE';
    const data = { email: memberEmail };

    fetch('/api/follow', {
        method: method,
        headers: {
            'Content-Type': 'application/json; charset=utf-8',
        },
        body: JSON.stringify(data),
        credentials: 'include',
    })
        .then((response) => response.json())
        .then((json) => {
            if (json.result > 0) {
                btn.innerText = method === 'POST' ? 'Unfollow' : 'Follow';
            } else {
                alert('다시 시도하여주십시오');
            }
        })
        .catch(() => {
            alert('다시 시도하여주십시오');
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

function escapeJsString(value) {
    return String(value)
        .replaceAll('\\', '\\\\')
        .replaceAll("'", "\\'");
}
