const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;
const btn = document.querySelector("#follow");
const chatting = document.querySelector("#chatting");

const url = new URL(window.location.href);
const urlParams = url.searchParams;
const email = urlParams.get('email');

let method;
if (btn && btn.innerText === 'Follow') method = "POST";
else method = "DELETE";

if (follower === '0') document.querySelector("#follower").removeAttribute('href');
if (following === '0') document.querySelector("#following").removeAttribute('href');

if (typeof initMemberBoardModal === 'function') {
    initMemberBoardModal();
}

if (typeof initCommentModal === 'function') {
    initCommentModal();
}

if (typeof initFavoriteModal === 'function') {
    initFavoriteModal();
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
