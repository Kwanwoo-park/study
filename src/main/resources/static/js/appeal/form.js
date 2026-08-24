(function () {
    const page = document.getElementById('appealPage');
    const form = document.getElementById('appealForm');
    if (!page || !form) return;

    const authenticated = page.dataset.authenticated === 'true';
    const email = document.getElementById('appealEmail');
    const verificationSend = document.getElementById('appealVerificationSend');
    const verificationCode = document.getElementById('appealVerificationCode');
    const verificationConfirm = document.getElementById('appealVerificationConfirm');
    const verificationStatus = document.getElementById('appealVerificationStatus');
    const sanctionGroup = document.getElementById('appealSanctionGroup');
    const sanctionSelect = document.getElementById('appealSanction');
    const title = document.getElementById('appealTitle');
    const content = document.getElementById('appealContent');
    const contentCount = document.getElementById('appealContentCount');
    const message = document.getElementById('appealMessage');
    const submit = document.getElementById('appealSubmit');
    const back = document.getElementById('appealBack');
    const sanctionList = document.getElementById('sanctionList');
    const appealHistory = document.getElementById('appealHistory');
    let verificationToken = null;
    let verificationTimer = null;
    let resendTimer = null;
    let resendSeconds = 0;

    back?.addEventListener('click', () => {
        if (window.history.length > 1) window.history.back();
        else window.location.replace(authenticated ? '/board/main' : '/member/login');
    });
    content.addEventListener('input', () => {
        contentCount.textContent = String(content.value.length);
    });
    form.addEventListener('submit', submitAppeal);
    verificationSend?.addEventListener('click', sendVerificationCode);
    verificationConfirm?.addEventListener('click', confirmVerificationCode);
    if (!authenticated) {
        email.addEventListener('input', resetVerificationForEmailChange);
        verificationCode?.addEventListener('input', () => {
            verificationCode.value = verificationCode.value.replace(/\D/g, '').slice(0, 6);
        });
    }

    if (authenticated) loadContext();

    async function sendVerificationCode() {
        const requestedEmail = email.value.trim();
        if (!requestedEmail || !email.checkValidity()) {
            showMessage('이메일 형식을 확인해주세요.', 'error');
            email.focus();
            return;
        }

        verificationSend.disabled = true;
        showMessage('인증번호를 요청하는 중입니다.', '');
        try {
            const response = await fetch('/api/appeal/verification/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=utf-8' },
                credentials: 'include',
                body: JSON.stringify({email: requestedEmail})
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                showMessage(data.message || '인증번호를 발송할 수 없습니다.', 'error');
                return;
            }

            verificationToken = null;
            verificationCode.value = '';
            verificationCode.readOnly = false;
            verificationConfirm.disabled = false;
            startVerificationTimer(Number(data.expiresInSeconds || 300), false);
            startResendCooldown(60);
            showMessage(data.message, 'success');
            verificationCode.focus();
        } catch (error) {
            console.error(error);
            showMessage('인증번호 발송 중 오류가 발생했습니다.', 'error');
        } finally {
            if (resendSeconds <= 0 && !verificationToken) verificationSend.disabled = false;
        }
    }

    async function confirmVerificationCode() {
        const requestedEmail = email.value.trim();
        const code = verificationCode?.value.trim() || '';
        if (!requestedEmail || !/^\d{6}$/.test(code)) {
            showMessage('이메일과 인증번호 6자리를 확인해주세요.', 'error');
            return;
        }

        verificationConfirm.disabled = true;
        showMessage('이메일을 인증하는 중입니다.', '');
        try {
            const response = await fetch('/api/appeal/verification/verify', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=utf-8' },
                credentials: 'include',
                body: JSON.stringify({email: requestedEmail, code: code})
            });
            const data = await response.json();
            if (!response.ok || data.result < 0 || !data.verificationToken) {
                showMessage(data.message || '이메일 인증에 실패했습니다.', 'error');
                return;
            }

            verificationToken = data.verificationToken;
            email.readOnly = true;
            verificationCode.readOnly = true;
            verificationSend.disabled = true;
            startVerificationTimer(Number(data.expiresInSeconds || 300), true);
            showMessage(data.message, 'success');
        } catch (error) {
            console.error(error);
            showMessage('이메일 인증 중 오류가 발생했습니다.', 'error');
        } finally {
            verificationConfirm.disabled = Boolean(verificationToken);
        }
    }

    function startVerificationTimer(seconds, verified) {
        if (verificationTimer) window.clearInterval(verificationTimer);
        let remaining = Math.max(0, Number(seconds));

        const update = () => {
            if (!verificationStatus) return;
            const minutes = String(Math.floor(remaining / 60)).padStart(2, '0');
            const secs = String(remaining % 60).padStart(2, '0');
            verificationStatus.textContent = verified
                ? `인증 완료 · 상소문 제출까지 ${minutes}:${secs}`
                : `인증번호 유효시간 ${minutes}:${secs}`;
            verificationStatus.classList.toggle('verified', verified);
            verificationStatus.classList.remove('expired');

            if (remaining <= 0) {
                window.clearInterval(verificationTimer);
                verificationTimer = null;
                verificationToken = null;
                email.readOnly = false;
                verificationCode.readOnly = false;
                verificationConfirm.disabled = false;
                verificationStatus.textContent = verified
                    ? '이메일 인증이 만료되었습니다. 다시 인증해주세요.'
                    : '인증번호가 만료되었습니다. 다시 발급해주세요.';
                verificationStatus.classList.remove('verified');
                verificationStatus.classList.add('expired');
                if (resendSeconds <= 0) verificationSend.disabled = false;
                return;
            }
            remaining--;
        };

        update();
        verificationTimer = window.setInterval(update, 1000);
    }

    function startResendCooldown(seconds) {
        if (resendTimer) window.clearInterval(resendTimer);
        resendSeconds = Math.max(0, Number(seconds));
        verificationSend.disabled = true;

        const update = () => {
            if (resendSeconds <= 0) {
                window.clearInterval(resendTimer);
                resendTimer = null;
                verificationSend.textContent = '인증번호 재발송';
                verificationSend.disabled = Boolean(verificationToken);
                return;
            }
            verificationSend.textContent = `재발송 (${resendSeconds}초)`;
            resendSeconds--;
        };

        update();
        resendTimer = window.setInterval(update, 1000);
    }

    function resetVerificationForEmailChange() {
        if (verificationToken || verificationCode?.value) {
            verificationToken = null;
            verificationCode.value = '';
            verificationCode.readOnly = false;
            verificationConfirm.disabled = false;
        }
        if (verificationTimer) window.clearInterval(verificationTimer);
        verificationTimer = null;
        if (verificationStatus) {
            verificationStatus.textContent = '인증번호를 발송해주세요.';
            verificationStatus.classList.remove('verified', 'expired');
        }
    }

    async function loadContext() {
        try {
            const response = await fetch('/api/appeal/context', { credentials: 'include' });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                showMessage(data.message || '신고·제재 내역을 불러오지 못했습니다.', 'error');
                return;
            }
            renderSanctions(data.sanctions || []);
            renderAppeals(data.appeals || []);
        } catch (error) {
            console.error(error);
            showMessage('신고·제재 내역을 불러오지 못했습니다.', 'error');
        }
    }

    function renderSanctions(sanctions) {
        if (!sanctionList) return;
        if (sanctions.length === 0) {
            sanctionList.textContent = '본인에게 적용된 신고·제재 내역이 없습니다.';
            return;
        }

        sanctionList.innerHTML = sanctions.map(item => `
            <article class="appeal-history-item">
                <strong>${escapeHtml(targetLabel(item.targetType))} 신고 · ${escapeHtml(actionLabel(item.sanctionType))}</strong>
                <span>신고 사유: ${escapeHtml(reasonLabel(item.reportReason))}</span>
                <span>제재 사유: ${escapeHtml(item.sanctionReason || '-')}</span>
                <span>적용일: ${escapeHtml(formatDate(item.startedAt))}</span>
            </article>`).join('');
        sanctionSelect.innerHTML = '<option value="">특정 내역을 선택하지 않음</option>'
            + sanctions.map(item => `<option value="${escapeHtml(item.sanctionId)}">
                ${escapeHtml(targetLabel(item.targetType))} 신고 / ${escapeHtml(actionLabel(item.sanctionType))} / ${escapeHtml(formatDate(item.startedAt))}
              </option>`).join('');
        sanctionGroup.classList.remove('is-hidden');
    }

    function renderAppeals(appeals) {
        if (!appealHistory) return;
        if (appeals.length === 0) {
            appealHistory.textContent = '작성한 상소문이 없습니다.';
            return;
        }
        appealHistory.innerHTML = appeals.map(item => `
            <article class="appeal-history-item">
                <strong>${escapeHtml(item.title)}</strong>
                <span>상태: ${escapeHtml(statusLabel(item.status))}</span>
                <span>접수일: ${escapeHtml(formatDate(item.registerTime))}</span>
            </article>`).join('');
    }

    async function submitAppeal(event) {
        event.preventDefault();
        const payload = {
            email: email.value.trim(),
            verificationToken: authenticated ? null : verificationToken,
            sanctionId: sanctionSelect?.value ? Number(sanctionSelect.value) : null,
            title: title.value.trim(),
            content: content.value.trim()
        };
        if (!payload.email || (!authenticated && !payload.verificationToken) || !payload.title || !payload.content) {
            showMessage(!authenticated && !payload.verificationToken
                ? '5분 이내에 이메일 인증을 완료해주세요.'
                : '필수 항목을 모두 입력해주세요.', 'error');
            return;
        }

        submit.disabled = true;
        showMessage('상소문을 접수하는 중입니다.', '');
        try {
            const response = await fetch('/api/appeal', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=utf-8' },
                credentials: 'include',
                body: JSON.stringify(payload)
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                showMessage(data.message || '상소문을 접수할 수 없습니다.', 'error');
                if (!authenticated) clearSubmittedVerification();
                return;
            }
            showMessage(data.message || '상소문이 접수되었습니다.', 'success');
            title.value = '';
            content.value = '';
            contentCount.textContent = '0';
            verificationToken = null;
            if (authenticated) await loadContext();
        } catch (error) {
            console.error(error);
            if (!authenticated) clearSubmittedVerification();
            showMessage('상소문 접수 중 오류가 발생했습니다.', 'error');
        } finally {
            submit.disabled = false;
        }
    }

    function clearSubmittedVerification() {
        verificationToken = null;
        email.readOnly = false;
        verificationCode.readOnly = false;
        verificationConfirm.disabled = false;
        if (verificationTimer) window.clearInterval(verificationTimer);
        verificationTimer = null;
        if (verificationStatus) {
            verificationStatus.textContent = '제출에 실패하여 이메일 인증이 해제되었습니다. 다시 인증해주세요.';
            verificationStatus.classList.remove('verified');
            verificationStatus.classList.add('expired');
        }
        if (resendSeconds <= 0) verificationSend.disabled = false;
    }

    function showMessage(text, type) {
        message.textContent = text;
        message.classList.remove('error', 'success');
        if (type) message.classList.add(type);
    }

    function targetLabel(value) {
        return { MEMBER: '회원', BOARD: '게시글', COMMENT: '댓글', CHAT_MESSAGE: '채팅 메시지' }[value] || value || '-';
    }

    function actionLabel(value) {
        return { WARNING: '경고', TEMPORARY_SUSPEND: '기간 정지', PERMANENT_BAN: '영구 차단' }[value] || value || '-';
    }

    function reasonLabel(value) {
        return { SPAM: '스팸', ABUSE: '욕설·비방', HATE: '혐오', SEXUAL: '성적 내용', FRAUD: '사기', PERSONAL_INFO: '개인정보', COPYRIGHT: '저작권', ETC: '기타' }[value] || value || '-';
    }

    function statusLabel(value) {
        return { PENDING: '검토 대기', REVIEWING: '검토 중', ACCEPTED: '인용', REJECTED: '기각' }[value] || value || '-';
    }

    function formatDate(value) {
        return value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '-';
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }
})();
