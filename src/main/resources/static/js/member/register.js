(function () {
    const form = document.getElementById('registerForm');
    const email = document.getElementById('email');
    const checkEmail = document.getElementById('email_check');
    const sendCode = document.getElementById('email_certification');
    const changeEmail = document.getElementById('changeEmail');
    const codeGroup = document.getElementById('check');
    const code = document.getElementById('memailconfirm');
    const verify = document.getElementById('verifyEmail');
    const status = document.getElementById('emailVerificationStatus');
    const clock = document.getElementById('emailVerificationClock');
    const submit = document.getElementById('submit');
    let token = null;
    let expiresAt = 0;
    let resendAt = 0;
    let timer;
    let sending = false;
    let verifying = false;

    function refreshClock() {
        clearTimeout(timer);
        const now = Date.now();
        const remaining = Math.max(0, Math.ceil((expiresAt - now) / 1000));
        const cooldown = Math.max(0, Math.ceil((resendAt - now) / 1000));
        if (expiresAt && !remaining) {
            status.textContent = '인증 시간이 지났어요. 인증번호를 다시 받아 주세요.';
            expiresAt = 0;
            token = null;
        }
        const steps = [];
        if (remaining) steps.push(`${token ? '가입 완료까지' : '인증번호 유효 시간'} ${Math.floor(remaining / 60)}분 ${remaining % 60}초`);
        if (cooldown) steps.push(`재발송까지 ${cooldown}초`);
        clock.textContent = steps.join(' · ');
        sendCode.disabled = sending || verifying || cooldown > 0;
        changeEmail.disabled = sending || verifying;
        verify.disabled = verifying || sending || !remaining || Boolean(token);
        submit.classList.toggle('is-hidden', !token);
        if (remaining || cooldown) timer = setTimeout(refreshClock, 1000);
    }

    email.addEventListener('input', () => sendCode.classList.add('is-hidden'));
    checkEmail.addEventListener('click', async () => {
        if (!email.reportValidity()) return;
        const address = email.value.trim();
        checkEmail.disabled = true;
        status.textContent = '이메일을 확인하는 중이에요.';
        try {
            await request('/api/member/duplicateCheck?email=' + encodeURIComponent(address));
            if (email.value.trim() !== address) return;
            sendCode.classList.remove('is-hidden');
            status.textContent = '사용 가능한 이메일이에요. 인증번호를 받아 주세요.';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            checkEmail.disabled = email.readOnly;
        }
    });
    sendCode.addEventListener('click', async () => {
        const address = email.value.trim();
        sending = true;
        refreshClock();
        try {
            const data = await request('/api/mail/confirm', {email: address});
            if (email.value.trim() !== address) return;
            token = null;
            expiresAt = Date.now() + data.expiresInSeconds * 1000;
            resendAt = Date.now() + data.resendAfterSeconds * 1000;
            email.readOnly = true;
            checkEmail.disabled = true;
            code.value = '';
            sendCode.textContent = '인증번호 다시 받기';
            changeEmail.classList.remove('is-hidden');
            codeGroup.classList.remove('is-hidden');
            status.textContent = '인증번호를 보냈어요. 이메일을 확인해 주세요.';
            code.focus();
        } catch (error) {
            status.textContent = error.message;
        } finally {
            sending = false;
            refreshClock();
        }
    });
    changeEmail.addEventListener('click', () => {
        token = null;
        expiresAt = 0;
        resendAt = 0;
        email.readOnly = false;
        checkEmail.disabled = false;
        sendCode.textContent = '인증번호 받기';
        [sendCode, changeEmail, codeGroup].forEach(element => element.classList.add('is-hidden'));
        status.textContent = '새 이메일을 입력하고 확인해 주세요.';
        refreshClock();
        email.focus();
    });
    verify.addEventListener('click', async () => {
        if (!code.value.trim()) {
            status.textContent = '이메일로 받은 인증번호를 입력해 주세요.';
            return;
        }
        verifying = true;
        refreshClock();
        try {
            const data = await request('/api/mail/verify', {email: email.value.trim(), code: code.value.trim()});
            token = data.verificationToken;
            expiresAt = Date.now() + data.expiresInSeconds * 1000;
            status.textContent = '이메일 인증이 완료됐어요. 아래 정보를 입력하고 가입해 주세요.';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            verifying = false;
            refreshClock();
        }
    });
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!token || Date.now() >= expiresAt || submit.disabled) return;
        const fields = new FormData(form);
        submit.disabled = true;
        try {
            await request('/api/member/register', {...Object.fromEntries(fields), verificationToken: token});
            alert('회원가입이 완료되었습니다.');
            window.location.href = '/member/login';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            submit.disabled = false;
        }
    });

    async function request(url, body) {
        const options = {credentials: 'include'};
        if (body) {
            options.method = 'POST';
            options.headers = {'Content-Type': 'application/json'};
            options.body = JSON.stringify(body);
        }
        const response = await fetch(url, options);
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || '요청을 처리하지 못했어요. 다시 시도해 주세요.');
        return data;
    }
})();
