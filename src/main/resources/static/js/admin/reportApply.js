window.addEventListener('load', loadPendingReports);

async function loadPendingReports() {
    const list = document.getElementById('report-list');
    if (!list) return;

    list.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오는 중입니다.</div>';

    try {
        const response = await fetch('/api/admin/report/pending', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
        const data = await response.json();

        if (!response.ok || data.result < 0) {
            list.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오지 못했습니다.</div>';
            return;
        }

        renderReports(data.list || []);
    } catch (error) {
        console.error(error);
        list.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오지 못했습니다.</div>';
    }
}

function renderReports(reports) {
    const list = document.getElementById('report-list');
    list.innerHTML = '';

    if (reports.length === 0) {
        list.innerHTML = '<div class="admin-report-empty">접수 대기 중인 신고가 없습니다.</div>';
        return;
    }

    reports.forEach((report) => {
        const item = document.createElement('article');
        item.className = 'admin-report-item';
        item.innerHTML = `
            <div class="admin-report-item-header">
                <div>
                    <strong>#${report.id} ${formatTargetType(report.targetType)}</strong>
                    <span>${formatReason(report.reason)}</span>
                </div>
                <button type="button" class="btn btn-success btn-sm" onclick="acceptReport(${report.id})">접수</button>
            </div>
            <dl class="admin-report-meta">
                <div>
                    <dt>대상 ID</dt>
                    <dd>${escapeHtml(report.targetId)}</dd>
                </div>
                <div>
                    <dt>신고자</dt>
                    <dd>${escapeHtml(report.reporterName)} (${escapeHtml(report.reporterEmail)})</dd>
                </div>
                <div>
                    <dt>신고일</dt>
                    <dd>${formatDateTime(report.registerTime)}</dd>
                </div>
            </dl>
            <p class="admin-report-description">${escapeHtml(report.description)}</p>
            ${report.snapshot ? `<pre class="admin-report-snapshot">${escapeHtml(report.snapshot)}</pre>` : ''}
        `;
        list.append(item);
    });
}

async function acceptReport(reportId) {
    try {
        const response = await fetch(`/api/admin/report/${encodeURIComponent(reportId)}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
            body: JSON.stringify({
                status: 'REVIEWING',
                action: 'NONE',
                reportMemo: '신고 접수'
            })
        });
        const data = await response.json();

        if (!response.ok || data.result < 0) {
            alert(data.message || '신고 접수에 실패했습니다.');
            return;
        }

        await loadPendingReports();
    } catch (error) {
        console.error(error);
        alert('신고 접수에 실패했습니다.');
    }
}

function formatTargetType(value) {
    return {
        MEMBER: '회원',
        BOARD: '게시글',
        COMMENT: '댓글',
        CHAT_MESSAGE: '채팅 메시지'
    }[value] || value;
}

function formatReason(value) {
    return {
        SPAM: '스팸/도배',
        ABUSE: '욕설/괴롭힘',
        HATE: '혐오 표현',
        SEXUAL: '성적인 콘텐츠',
        FRAUD: '사기/허위 정보',
        PERSONAL_INFO: '개인정보 노출',
        COPYRIGHT: '저작권 침해',
        ETC: '기타'
    }[value] || value;
}

function formatDateTime(value) {
    if (!value) return '-';

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString('ko-KR');
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
