const button = document.getElementById('login');
const email = document.getElementById('email');
const password = document.getElementById('password');
const appealLink = document.getElementById('appealLink');

const url = new URL(window.location.href);
const urlParams = url.searchParams;
const requestedUrl = urlParams.get('url');

function loginDestination() {
    if (!requestedUrl) return '/board/main';
    try {
        const target = new URL(requestedUrl, url.origin);
        if (target.origin === url.origin) return target.href;
    } catch (error) { /* An invalid destination falls back to the feed. */ }
    return '/board/main';
}

const updateAppealLink = () => {
    const enteredEmail = email.value.trim();
    appealLink.href = enteredEmail
        ? `/appeal?email=${encodeURIComponent(enteredEmail)}`
        : '/appeal';
};
email.addEventListener('input', updateAppealLink);
updateAppealLink();

button.addEventListener('click', (event) => {
    event.preventDefault();

    if (email.value != '' && password.value != '') {
        fetch(`/api/member/login`, {
            method: 'PATCH',
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                email: email.value,
                password: password.value,
            }),
            credentials: "include",
        })
        .then((response) => response.json())
        .then((json) => {
            if (json['result'] > 0) {
                if (!requestedUrl) alert(json.member.name + "님 환영합니다!");
                window.location.href = loginDestination();
            } else if (json['result'] == -10) {
                alert('Login error occurred. Please try again later');
            }
            else if (json['result'] == -2)
                alert("Access Deny");
            else if (json['result'] == -3)
                alert("이메일을 확인해주세요")
            else
                alert("비밀번호를 확인해주세요")
        })
        .catch((error) => {
            console.error('Error during login:', error);
            alert('Login error occurred. Please try again later');
        });
    }
});

password.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.isComposing)
        button.click();
});
