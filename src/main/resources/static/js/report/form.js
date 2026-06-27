(function() {
    const form = document.getElementById('reportForm');
    if (!form) return;

    const targetTypeDisplay = document.getElementById('targetTypeDisplay');
    const targetIdDisplay = document.getElementById('targetIdDisplay');
    const reason = document.getElementById('reason');
    const description = document.getElementById('description');
    const snapshot = document.getElementById('snapshot');
    const descriptionCount = document.getElementById('descriptionCount');
    const message = document.getElementById('reportMessage');
    const submitButton = document.getElementById('submitButton');
    const cancelButton = document.getElementById('cancelButton');

    renderTargetType();
    updateDescriptionCount();

    description.addEventListener('input', updateDescriptionCount);
    cancelButton.addEventListener('click', moveToOriginalPage);

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        submitReport();
    });

    function renderTargetType() {
        const labels = {
            MEMBER: '회원',
            BOARD: '게시글',
            COMMENT: '댓글',
            CHAT_MESSAGE: '채팅 메시지'
        };
        const value = targetTypeDisplay.dataset.value;
        targetTypeDisplay.textContent = labels[value] || value || '-';
    }

    function updateDescriptionCount() {
        descriptionCount.textContent = String(description.value.length);
    }

    async function submitReport() {
        const payload = {
            targetType: targetTypeDisplay.dataset.value || '',
            targetId: (targetIdDisplay.dataset.value || '').trim(),
            reason: reason.value,
            description: description.value.trim(),
            snapshot: snapshot.value.trim()
        };

        const validationMessage = validate(payload);
        if (validationMessage) {
            showMessage(validationMessage, 'error');
            return;
        }

        setLoading(true);
        showMessage('신고를 제출하는 중입니다.', '');

        try {
            const response = await fetch('/api/report', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8'
                },
                credentials: 'include',
                body: JSON.stringify(payload)
            });

            let json = {};
            try {
                json = await response.json();
            } catch (error) {
                json = {};
            }

            if (response.status === 401) {
                showMessage('로그인이 필요합니다. 로그인 후 다시 신고해주세요.', 'error');
                return;
            }

            if (response.status === 409) {
                showMessage('이미 신고한 대상입니다.', 'error');
                moveToOriginalPage();
                return;
            }

            if (!response.ok || json.result < 0) {
                showMessage(json.message || '신고 제출에 실패했습니다. 다시 시도해주세요.', 'error');
                return;
            }

            showMessage('신고가 접수되었습니다.', 'success');
            moveToOriginalPage();
        } catch (error) {
            console.error(error);
            showMessage('신고 제출에 실패했습니다. 다시 시도해주세요.', 'error');
        } finally {
            setLoading(false);
        }
    }

    function validate(payload) {
        if (!payload.targetType) return '신고 대상을 선택해주세요.';
        if (!payload.targetId) return '대상 ID를 입력해주세요.';
        if (!payload.reason) return '신고 사유를 선택해주세요.';
        if (!payload.description) return '상세 내용을 입력해주세요.';
        if (payload.description.length > 1000) return '상세 내용은 1000자 이하로 입력해주세요.';
        if (payload.targetId.length > 100) return '대상 ID는 100자 이하로 입력해주세요.';
        return '';
    }

    function setLoading(isLoading) {
        submitButton.disabled = isLoading;
        submitButton.textContent = isLoading ? '제출 중' : '신고 제출';
    }

    function showMessage(text, type) {
        message.textContent = text;
        message.classList.remove('error', 'success');
        if (type) message.classList.add(type);
    }

    function moveToOriginalPage() {
        if (document.referrer) {
            try {
                const referrer = new URL(document.referrer);
                if (referrer.origin === window.location.origin && referrer.pathname !== window.location.pathname) {
                    window.location.replace(referrer.href);
                    return;
                }
            } catch (error) {
                // Ignore invalid referrer values and fall back to browser history.
            }
        }

        if (window.history.length > 1) {
            window.history.back();
            return;
        }

        window.location.replace('/');
    }
})();
