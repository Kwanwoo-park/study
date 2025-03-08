const status = document.querySelector("#status").value;
const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;
const btn = document.querySelector("#follow");
const chatting = document.querySelector("#chatting");

const url = new URL(window.location.href)
const urlParams = url.searchParams
const email = urlParams.get('email')

var method;

if (btn.innerText == 'Follow')
    method = "POST"
else
    method = "DELETE"

if (follower == '0')
    document.querySelector("#follower").removeAttribute('href');
if (following == '0')
    document.querySelector("#following").removeAttribute('href');

if (btn) {
    btn.addEventListener('click', (event) => {
        const data = {
            email: email
        }

        fetch(`/api/follow`, {
            method: method,
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
        })
        .then((response) => response.json())
        .then((json) => {
            window.location.reload();
        })
        .catch((error) => {
            alert("다시 시도하여주십시오");
        })
    })
}

if (chatting) {
    chatting.addEventListener('click', (event) => {
        const data = {
            email: email
        }

        fetch (`/api/chat/create`, {
            method: 'POST',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            body: JSON.stringify(data),
        })
        .then((response) => response.json())
        .then((json) => {
            location.href = '/chat/chatRoom?roomId=' + json['roomId']
        })
        .catch((error) => {
            alert("다시 시도하여주십시오");
        })
    })
}