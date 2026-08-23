(function () {
    const page = document.getElementById('appealPage');
    const form = document.getElementById('appealForm');
    if (!page || !form) return;

    const authenticated = page.dataset.authenticated === 'true';
    const email = document.getElementById('appealEmail');
    const password = document.getElementById('appealPassword');
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

    back?.addEventListener('click', () => {
        if (window.history.length > 1) window.history.back();
        else window.location.replace(authenticated ? '/board/main' : '/member/login');
    });
    content.addEventListener('input', () => {
        contentCount.textContent = String(content.value.length);
    });
    form.addEventListener('submit', submitAppeal);

    if (authenticated) loadContext();

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
            password: password?.value || null,
            sanctionId: sanctionSelect?.value ? Number(sanctionSelect.value) : null,
            title: title.value.trim(),
            content: content.value.trim()
        };
        if (!payload.email || (!authenticated && !payload.password) || !payload.title || !payload.content) {
            showMessage('필수 항목을 모두 입력해주세요.', 'error');
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
                return;
            }
            showMessage(data.message || '상소문이 접수되었습니다.', 'success');
            title.value = '';
            content.value = '';
            contentCount.textContent = '0';
            if (password) password.value = '';
            if (authenticated) await loadContext();
        } catch (error) {
            console.error(error);
            showMessage('상소문 접수 중 오류가 발생했습니다.', 'error');
        } finally {
            submit.disabled = false;
        }
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
