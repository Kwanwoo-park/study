const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;
const btn = document.querySelector("#follow");
const chatting = document.querySelector("#chatting");
const container = document.querySelector('.container');
const imgGrid = document.querySelector('.imgGrid');

const url = new URL(window.location.href);
const urlParams = url.searchParams;
const email = urlParams.get('email');
const memberEmail = container?.dataset.memberEmail ?? email;

const BOARD_LIMIT = 30;

let method;
let nextCursor = 1;
if (btn && btn.innerText === 'Follow') method = "POST";
else method = "DELETE";

window.addEventListener('load', () => {
    loadMoreBoards();
});

if (container) {
    container.addEventListener('scroll', () => {
        if (container.scrollTop + container.clientHeight >= container.scrollHeight - 1) {
            loadMoreBoards();
        }
    });
}

if (typeof initMemberBoardModal === 'function') {
    initMemberBoardModal();
}

if (typeof initCommentModal === 'function') {
    initCommentModal();
}

if (typeof initFavoriteModal === 'function') {
    initFavoriteModal();
}

if (typeof initFollowerModal === 'function') {
    initFollowerModal();
}

if (typeof initFollowingModal === 'function') {
    initFollowingModal();
}

if (btn) {
    btn.addEventListener('click', () => {
        const data = { email: email };

        fetch(`/api/follow`, {
            method: method,
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0) {
                window.location.reload();
            } else {
                alert("다시 시도하여주십시오");
            }
        })
        .catch(() => {
            alert("다시 시도하여주십시오");
        });
    });
}

if (chatting) {
    chatting.addEventListener('click', () => {
        const data = { email: email };

        fetch(`/api/chat/create`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            location.href = '/chat/chatRoom?roomId=' + json['room'].roomId;
        })
        .catch(() => {
            alert("다시 시도하여주십시오");
        });
    });
}

async function loadMoreBoards() {
    if (!imgGrid || !memberEmail || !nextCursor) {
        return;
    }

    try {
        const response = await fetch(`/api/board/member/detail?email=${encodeURIComponent(memberEmail)}&cursor=${nextCursor - 1}&limit=${BOARD_LIMIT}`, {
            method: 'GET',
            credentials: 'include',
        });

        const json = await response.json();

        if (json.result > 0) {
            drawBoards(json.boards);
            nextCursor = json.nextCursor;
        } else {
            alert("다시 시도하여주십시오");
        }
    } catch (error) {
        console.error(error);
        alert("다시 시도하여주십시오");
    }
}

function drawBoards(boards) {
    boards.forEach((board) => {
        const wrapper = document.createElement('div');
        wrapper.className = 'imgDiv';

        const image = document.createElement('img');
        image.className = 'main-image';
        image.id = `main_img${board.id}`;
        image.src = board.img.length === 0 ? '/img/IMG_0111.jpeg' : board.img[0].imgSrc;
        image.loading = 'lazy';
        image.onclick = function () {
            openBoardModal(board.id);
        };

        wrapper.append(image);
        imgGrid.append(wrapper);
    });
}
