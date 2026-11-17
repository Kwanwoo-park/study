const btn = document.querySelector("#follow");
const chatting = document.querySelector("#chatting");
const chatMemberDetailBack = document.getElementById('chatMemberDetailBack');
const email = document.querySelector('.container').dataset.memberEmail;

if (chatMemberDetailBack) {
    chatMemberDetailBack.addEventListener('click', () => {
        if (window.history.length > 1) {
            window.history.back();
            return;
        }
        window.location.href = '/chat/chatList';
    });
}

btn.dataset.email = email;
btn.addEventListener('click', async () => {
    if (await toggleMemberFollow(btn)) window.location.reload();
});

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
            window.location.href = '/chat/chatRoom?roomId=' + json['room'].roomId;
        })
        .catch(() => {
            alert("다시 시도하여주십시오");
        });
    });
}
