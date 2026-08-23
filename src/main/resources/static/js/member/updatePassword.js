(function () {
    const page = document.getElementById('passwordUpdatePage');
    const password = document.getElementById('password');
    const email = document.getElementById('email');
    const updateButton = document.getElementById('update');
    if (!page || !password || !email || !updateButton) return;

    const verificationRequired = page.dataset.verificationRequired === 'true';
    const sendButton = document.getElementById('sendVerification');
    const verifyButton = document.getElementById('verifyCode');
    const codeGroup = document.getElementById('verificationCodeGroup');
    const codeInput = document.getElementById('verificationCode');
    const verificationMessage = document.getElementById('verificationMessage');
    let emailVerified = !verificationRequired;

    sendButton?.addEventListener('click', sendVerificationCode);
    verifyButton?.addEventListener('click', verifyCode);
    codeInput?.addEventListener('input', () => {
        codeInput.value = codeInput.value.replace(/\D/g, '').slice(0, 6);
    });
    updateButton.addEventListener('click', updatePassword);
    password.addEventListener('keydown', event => {
        if (event.key === 'Enter') updateButton.click();
    });

    async function sendVerificationCode() {
        sendButton.disabled = true;
        showVerificationMessage('인증번호를 발송하는 중입니다.', '');
        try {
            const response = await fetch('/api/member/password-verification/send', {
                method: 'POST',
                credentials: 'include'
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                showVerificationMessage(data.message || '인증번호를 발송하지 못했습니다.', 'error');
                return;
            }

            emailVerified = false;
            updateButton.disabled = true;
            codeGroup.classList.remove('is-hidden');
            codeInput.disabled = false;
            verifyButton.disabled = false;
            showVerificationMessage(data.message || '인증번호를 이메일로 발송했습니다.', 'success');
            codeInput.focus();
        } catch (error) {
            console.error(error);
            showVerificationMessage('인증번호 발송 중 오류가 발생했습니다.', 'error');
        } finally {
            sendButton.disabled = false;
        }
    }

    async function verifyCode() {
        const code = codeInput.value.trim();
        if (!/^\d{6}$/.test(code)) {
            showVerificationMessage('인증번호 6자리를 입력해주세요.', 'error');
            return;
        }

        verifyButton.disabled = true;
        try {
            const response = await fetch('/api/member/password-verification/verify', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=utf-8' },
                credentials: 'include',
                body: JSON.stringify({ code })
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                showVerificationMessage(data.message || '인증번호를 확인해주세요.', 'error');
                return;
            }

            emailVerified = true;
            updateButton.disabled = false;
            codeInput.disabled = true;
            showVerificationMessage(data.message || '이메일 인증이 완료되었습니다.', 'success');
        } catch (error) {
            console.error(error);
            showVerificationMessage('이메일 인증 중 오류가 발생했습니다.', 'error');
        } finally {
            if (!emailVerified) verifyButton.disabled = false;
        }
    }

    async function updatePassword(event) {
        event?.preventDefault();
        if (verificationRequired && !emailVerified) {
            showVerificationMessage('이메일 인증 후 비밀번호를 변경할 수 있습니다.', 'error');
            return;
        }
        if (!password.value) {
            alert('새 비밀번호를 입력해주세요.');
            return;
        }

        updateButton.disabled = true;
        try {
            const endpoint = verificationRequired
                ? '/api/member/updatePassword/authenticated'
                : '/api/member/updatePassword';
            const response = await fetch(endpoint, {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json; charset=utf-8' },
                credentials: 'include',
                body: JSON.stringify({
                    email: email.value,
                    password: password.value
                })
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                if (verificationRequired) {
                    emailVerified = false;
                    codeInput.value = '';
                    codeInput.disabled = false;
                    verifyButton.disabled = false;
                    showVerificationMessage('인증번호를 다시 발급받아 인증해주세요.', 'error');
                }
                alert(data.message || '비밀번호를 변경하지 못했습니다.');
                return;
            }

            alert('비밀번호가 정상적으로 변경되었습니다. 다시 로그인해주세요.');
            window.location.replace('/member/login');
        } catch (error) {
            console.error(error);
            alert('비밀번호 변경 중 오류가 발생했습니다.');
        } finally {
            updateButton.disabled = verificationRequired && !emailVerified;
        }
    }

    function showVerificationMessage(text, type) {
        if (!verificationMessage) return;
        verificationMessage.textContent = text;
        verificationMessage.classList.remove('success', 'error');
        if (type) verificationMessage.classList.add(type);
    }
})();
