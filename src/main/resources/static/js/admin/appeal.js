(function () {
    const list = document.getElementById('adminAppealList');
    const pagination = document.getElementById('adminAppealPagination');
    if (!list || !pagination) return;

    const selectedId = new URLSearchParams(window.location.search).get('appealId');
    let currentPage = 0;
    loadAppeals(0);

    async function loadAppeals(page) {
        list.textContent = '상소문을 불러오는 중입니다.';
        try {
            const response = await fetch(`/api/admin/appeal?status=PENDING&page=${page}&size=20`, {
                credentials: 'include'
            });
            const data = await response.json();
            if (!response.ok || data.result < 0) {
                list.textContent = data.message || '상소문을 불러오지 못했습니다.';
                return;
            }
            currentPage = Number(data.page || 0);
            renderAppeals(data.list || []);
            renderPagination(data);
        } catch (error) {
            console.error(error);
            list.textContent = '상소문을 불러오지 못했습니다.';
        }
    }

    function renderAppeals(appeals) {
        if (appeals.length === 0) {
            list.textContent = '검토 대기 중인 상소문이 없습니다.';
            return;
        }
        list.innerHTML = appeals.map(item => `
            <article id="appeal-${escapeHtml(item.id)}" class="admin-appeal-card ${String(item.id) === selectedId ? 'selected' : ''}">
                <div class="admin-appeal-card-header">
                    <div>
                        <strong>${escapeHtml(item.title)}</strong>
                        <span>${escapeHtml(item.memberName)} (${escapeHtml(item.memberEmail)})</span>
                    </div>
                    <span class="admin-appeal-status">검토 대기</span>
                </div>
                <pre>${escapeHtml(item.content)}</pre>
                <div class="admin-appeal-meta">
                    <span>상소 번호 ${escapeHtml(item.id)}</span>
                    <span>관련 신고 ${escapeHtml(item.reportId || '-')}</span>
                    <span>${escapeHtml(formatDate(item.registerTime))}</span>
                </div>
            </article>`).join('');

        if (selectedId) document.getElementById(`appeal-${selectedId}`)?.scrollIntoView({ block: 'center' });
    }

    function renderPagination(data) {
        const totalPages = Math.max(Number(data.totalPages || 0), 1);
        pagination.replaceChildren();
        const previous = button('이전', currentPage > 0, () => loadAppeals(currentPage - 1));
        const pageText = document.createElement('span');
        pageText.textContent = `${currentPage + 1} / ${totalPages}`;
        const next = button('다음', currentPage + 1 < Number(data.totalPages || 0), () => loadAppeals(currentPage + 1));
        pagination.append(previous, pageText, next);
    }

    function button(label, enabled, handler) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = 'btn btn-outline-secondary';
        element.textContent = label;
        element.disabled = !enabled;
        element.addEventListener('click', handler);
        return element;
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
