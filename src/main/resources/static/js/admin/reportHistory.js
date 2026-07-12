const historyState = {
    status: '',
    page: 0,
    size: 10
};

window.addEventListener('load', function() {
    document.querySelectorAll('[data-status]').forEach((button) => {
        button.addEventListener('click', function() {
            historyState.status = button.dataset.status;
            historyState.page = 0;
            updateFilterButtons();
            loadReportHistory();
        });
    });
    loadReportHistory();
});

async function loadReportHistory() {
    const list = document.getElementById('report-history-list');
    const pagination = document.getElementById('report-history-pagination');
    list.innerHTML = '<div class="admin-report-empty">처리 내역을 불러오는 중입니다.</div>';
    pagination.innerHTML = '';

    const params = new URLSearchParams({
        page: String(historyState.page),
        size: String(historyState.size)
    });
    if (historyState.status) params.set('status', historyState.status);

    try {
        const response = await fetch(`/api/admin/report/history?${params.toString()}`, {
            credentials: 'include'
        });
        const data = await response.json();
        if (!response.ok || data.result < 0) {
            throw new Error(data.message || '처리 내역 조회 실패');
        }

        renderHistory(data.list || []);
        renderPagination(data.page || 0, data.totalPages || 0);
    } catch (error) {
        console.error(error);
        list.innerHTML = '<div class="admin-report-empty">처리 내역을 불러오지 못했습니다.</div>';
    }
}

function renderHistory(reports) {
    const list = document.getElementById('report-history-list');
    list.innerHTML = '';
    if (reports.length === 0) {
        list.innerHTML = '<div class="admin-report-empty">조건에 맞는 처리 내역이 없습니다.</div>';
        return;
    }

    reports.forEach((report) => {
        const item = document.createElement('article');
        item.className = 'admin-report-item';
        item.innerHTML = `
            <div class="admin-report-item-header">
                <div>
                    <strong>#${escapeHtml(report.id)} ${escapeHtml(formatTargetType(report.targetType))}</strong>
                    <span>${escapeHtml(formatReason(report))}</span>
                </div>
                <span class="admin-report-state admin-report-state-${String(report.status).toLowerCase()}">
                    ${escapeHtml(formatStatus(report.status))}
                </span>
            </div>
            <dl class="admin-report-meta admin-report-history-meta">
                <div><dt>대상 ID</dt><dd>${escapeHtml(report.targetId)}</dd></div>
                <div><dt>신고자</dt><dd>${escapeHtml(report.reporterName)} (${escapeHtml(report.reporterEmail)})</dd></div>
                <div><dt>신고일</dt><dd>${formatDateTime(report.registerTime)}</dd></div>
                <div><dt>처리 조치</dt><dd>${escapeHtml(formatAction(report.action))}</dd></div>
                <div><dt>처리일</dt><dd>${formatDateTime(report.updateTime)}</dd></div>
            </dl>
            <p class="admin-report-description">${escapeHtml(report.description)}</p>
            ${report.snapshot ? `<pre class="admin-report-snapshot">${escapeHtml(report.snapshot)}</pre>` : ''}
            <div class="admin-report-history-memo">
                <strong>처리 메모</strong>
                <p>${escapeHtml(report.reportMemo || '기록 없음')}</p>
            </div>
        `;
        list.append(item);
    });
}

function renderPagination(currentPage, totalPages) {
    const pagination = document.getElementById('report-history-pagination');
    pagination.innerHTML = '';
    if (totalPages <= 1) return;

    for (let page = 0; page < totalPages; page++) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = page === currentPage ? 'btn btn-primary btn-sm' : 'btn btn-outline-secondary btn-sm';
        button.textContent = String(page + 1);
        button.addEventListener('click', function() {
            historyState.page = page;
            loadReportHistory();
        });
        pagination.append(button);
    }
}

function updateFilterButtons() {
    document.querySelectorAll('[data-status]').forEach((button) => {
        const active = button.dataset.status === historyState.status;
        button.className = active ? 'btn btn-primary btn-sm' : 'btn btn-outline-secondary btn-sm';
    });
}

function formatStatus(value) {
    return { RESOLVED: '처리 완료', REJECTED: '반려' }[value] || value;
}

function formatAction(value) {
    return {
        NONE: '없음', CONTENT_DELETE: '콘텐츠 삭제', WARNING: '경고',
        TEMPORARY_SUSPEND: '임시 정지', PERMANENT_BAN: '영구 정지'
    }[value] || value;
}

function formatTargetType(value) {
    return { MEMBER: '회원', BOARD: '게시글', COMMENT: '댓글', CHAT_MESSAGE: '채팅 메시지' }[value] || value;
}

function formatReason(report) {
    const label = {
        SPAM: '스팸/도배', ABUSE: '욕설/괴롭힘', HATE: '혐오 표현', SEXUAL: '성적인 콘텐츠',
        FRAUD: '사기/허위 정보', PERSONAL_INFO: '개인정보 노출', COPYRIGHT: '저작권 침해', ETC: '기타'
    }[report.reason] || report.reason;
    return report.reasonDetail ? `${label} - ${report.reasonDetail}` : label;
}

function formatDateTime(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return escapeHtml(value);
    return date.toLocaleString('ko-KR');
}

function escapeHtml(value) {
    return String(value == null ? '' : value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
