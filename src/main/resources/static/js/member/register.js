(function () {
    const form = document.getElementById('registerForm');
    const submit = document.getElementById('submit');
    const status = document.getElementById('registrationStatus');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (submit.disabled) return;

        submit.disabled = true;
        status.textContent = '';
        try {
            const response = await fetch('/api/member/register', {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(Object.fromEntries(new FormData(form))),
            });
            const result = await response.json();
            if (!response.ok) {
                throw new Error(result.message || '회원가입을 처리하지 못했어요. 다시 시도해 주세요.');
            }
            alert('회원가입이 완료되었습니다.');
            window.location.href = '/member/login';
        } catch (error) {
            status.textContent = error.message;
        } finally {
            submit.disabled = false;
        }
    });
})();
