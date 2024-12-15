const status = document.querySelector("#status").value;
const follower = document.querySelector("#follower_label").innerText;
const following = document.querySelector("#following_label").innerText;
const btn = document.querySelector("#follow");
var method;

if (status == 'true') {
    btn.innerText = "Unfollow";
    method = 'DELETE';
}
else {
    btn.innerText = "Follow";
    method = 'POST';
}

if (follower == '0')
    document.querySelector("#follower").removeAttribute('href');
if (following == '0')
    document.querySelector("#following").removeAttribute('href');

if (btn) {
    btn.addEventListener('click', (event) => {
        const data = {
            email: document.querySelector("#email").innerText
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
            alert("다시 시도하여주십시오.");
        })
    })
}