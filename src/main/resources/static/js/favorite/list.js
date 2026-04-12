const favoriteContainer = document.querySelector('.container');
const favoriteList = document.getElementById('favoriteList');

const FAVORITE_LIMIT = 10;

let nextFavoriteCursor = 1;
let favoriteLoading = false;

window.onload = function() {
    loadMoreFavorites(true);
};

if (favoriteContainer) {
    favoriteContainer.addEventListener('scroll', () => {
        if (favoriteContainer.scrollTop + favoriteContainer.clientHeight >= favoriteContainer.scrollHeight - 10) {
            loadMoreFavorites();
        }
    });
}

async function loadMoreFavorites(reset = false) {
    if (!favoriteContainer || !favoriteList || favoriteLoading) {
        return;
    }

    if (reset) {
        nextFavoriteCursor = 1;
        favoriteList.innerHTML = '';
    }

    if (!nextFavoriteCursor) {
        return;
    }

    favoriteLoading = true;

    try {
        const boardId = favoriteContainer.dataset.boardId;
        const response = await fetch(`/api/favorite/list?id=${boardId}&cursor=${nextFavoriteCursor - 1}&limit=${FAVORITE_LIMIT}`, {
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

        renderFavorites(json, reset);
        nextFavoriteCursor = json.nextCursor;
    } catch (error) {
        console.error(error);
        alert('다시 시도하여주십시오');
    } finally {
        favoriteLoading = false;
    }
}

function renderFavorites(data, reset) {
    if (reset) {
        favoriteList.innerHTML = '';
    }

    const startIndex = favoriteList.children.length;

    if (data.list.length === 0 && startIndex === 0) {
        const empty = document.createElement('li');
        empty.className = 'list-group-item';
        empty.innerText = '아직 좋아요를 누른 회원이 없습니다';
        favoriteList.append(empty);
        return;
    }

    data.list.forEach((item, index) => {
        const li = document.createElement('li');
        li.className = 'list-group-item';
        li.innerHTML = `
            <span>
                <span>${startIndex + index + 1}</span>
                <img src="${escapeHtml(item.member.profile)}" width="30vw" height="30vw" style="border-radius: 90px" alt="profile">
                <a href="/member/search/detail?email=${encodeURIComponent(item.member.email)}">
                    <span>${escapeHtml(item.member.name)}</span>
                </a>
                <span>${escapeHtml(item.member.email)}</span>
                ${item.member.email !== data.email ? `
                    <div class="float-right">
                        <button type="button" id="follow${item.id}" class="btn btn-success" onclick="fnFollow(${item.id}, '${escapeJsString(item.member.email)}')">${data.following[item.id] ? 'Unfollow' : 'Follow'}</button>
                    </div>
                ` : ''}
            </span>
        `;
        favoriteList.append(li);
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
