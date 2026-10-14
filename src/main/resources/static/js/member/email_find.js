(function () {
    const button = document.getElementById('find');
    const email = document.getElementById('email');
    const sendButton = document.getElementById('email_certification');
    const check = document.getElementById('check');
    const code = document.getElementById('memailconfirm');
    const message = document.getElementById('memailconfirmTxt');
    if (!button || !email || !sendButton || !code) return;
    email.value = new URLSearchParams(location.search).get('email') || email.value;

    sendButton.addEventListener('click', async () => {
        if (!email.value.trim()) return alert('이메일을 입력해 주세요.');
        sendButton.disabled = true;
        try {
            await post('/api/mail/confirm', { email: email.value.trim() });
            sessionStorage.removeItem('passwordRecoveryVerification');
            email.disabled = true;
            check.classList.remove('is-hidden');
            button.classList.remove('is-hidden');
            message.textContent = '이메일로 받은 인증번호를 5분 이내에 입력해 주세요.';
            alert('인증번호를 발송했습니다.');
        } catch (error) {
            alert(error.message);
        } finally {
            sendButton.disabled = false;
        }
    });

    button.addEventListener('click', async () => {
        if (!code.value.trim()) return alert('인증번호를 입력해 주세요.');
        button.disabled = true;
        try {
            const data = await post('/api/mail/verify', { email: email.value.trim(), code: code.value.trim() });
            sessionStorage.setItem('passwordRecoveryVerification', JSON.stringify({
                email: email.value.trim(), token: data.verificationToken, expiresAt: Date.now() + 5 * 60 * 1000
            }));
            location.replace('/member/updatePassword/' + encodeURIComponent(email.value.trim()));
        } catch (error) {
            message.textContent = error.message;
        } finally {
            button.disabled = false;
        }
    });

    async function post(url, body) {
        const response = await fetch(url, {
            method: 'POST', headers: { 'Content-Type': 'application/json; charset=utf-8' },
            credentials: 'include', body: JSON.stringify(body)
        });
        const data = await response.json();
        if (!response.ok || data.status !== 'ok' || (url.endsWith('/verify') && !data.verificationToken)) {
            throw new Error(data.message || '이메일 인증에 실패했습니다. 다시 시도해 주세요.');
        }
        return data;
    }
})();
