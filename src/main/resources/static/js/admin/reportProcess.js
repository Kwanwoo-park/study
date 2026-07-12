window.addEventListener('load', loadReviewingReports);

async function loadReviewingReports() {
    const list = document.getElementById('report-process-list');
    if (!list) return;

    list.innerHTML = '<div class="admin-report-empty">처리할 신고 목록을 불러오는 중입니다.</div>';

    try {
        const response = await fetch('/api/admin/report/reviewing', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
        const data = await response.json();

        if (!response.ok || data.result < 0) {
            list.innerHTML = '<div class="admin-report-empty">처리할 신고 목록을 불러오지 못했습니다.</div>';
            return;
        }

        renderReports(data.list || []);
    } catch (error) {
        console.error(error);
        list.innerHTML = '<div class="admin-report-empty">처리할 신고 목록을 불러오지 못했습니다.</div>';
    }
}

function renderReports(reports) {
    const list = document.getElementById('report-process-list');
    list.innerHTML = '';

    if (reports.length === 0) {
        list.innerHTML = '<div class="admin-report-empty">처리 대기 중인 접수 신고가 없습니다.</div>';
        return;
    }

    reports.forEach((report) => {
        const contentDeleteOption = report.targetType === 'MEMBER'
            ? ''
            : '<option value="CONTENT_DELETE">콘텐츠 삭제</option>';
        const item = document.createElement('article');
        item.className = 'admin-report-item';
        item.innerHTML = `
            <div class="admin-report-item-header">
                <div>
                    <strong>#${escapeHtml(report.id)} ${formatTargetType(report.targetType)}</strong>
                    <span>${escapeHtml(formatReason(report))}</span>
                </div>
                <span class="admin-report-state">검토 중</span>
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
            <form class="admin-report-process-form"
                  data-report-id="${escapeHtml(report.id)}"
                  data-target-type="${escapeHtml(report.targetType)}"
                  data-target-id="${escapeHtml(report.targetId)}">
                <div class="admin-report-process-grid">
                    <label>
                        <span>처리 상태</span>
                        <select class="form-control form-control-sm" name="status">
                            <option value="RESOLVED">처리 완료</option>
                            <option value="REJECTED">반려</option>
                        </select>
                    </label>
                    <label>
                        <span>조치</span>
                        <select class="form-control form-control-sm" name="action">
                            <option value="NONE">없음</option>
                            ${contentDeleteOption}
                            <option value="WARNING">경고</option>
                            <option value="TEMPORARY_SUSPEND">임시 정지</option>
                            <option value="PERMANENT_BAN">영구 정지</option>
                        </select>
                    </label>
                    <label class="temporary-suspend-field" hidden>
                        <span>정지 종료</span>
                        <input class="form-control form-control-sm" type="datetime-local" name="suspendedUntil">
                    </label>
                </div>
                <label class="admin-report-process-memo">
                    <span>처리 메모</span>
                    <textarea class="form-control" name="reportMemo" rows="3" maxlength="2000" placeholder="처리 결과나 반려 사유를 입력하세요."></textarea>
                </label>
                <div class="admin-report-process-actions">
                    <button type="submit" class="btn btn-primary btn-sm">처리 저장</button>
                </div>
            </form>
        `;

        const form = item.querySelector('.admin-report-process-form');
        const actionSelect = form.elements.action;
        const suspendField = form.querySelector('.temporary-suspend-field');
        const suspendedUntilInput = form.elements.suspendedUntil;
        actionSelect.addEventListener('change', function() {
            const temporary = actionSelect.value === 'TEMPORARY_SUSPEND';
            suspendField.hidden = !temporary;
            suspendedUntilInput.required = temporary;
            if (!temporary) suspendedUntilInput.value = '';
        });
        form.addEventListener('submit', function(event) {
            event.preventDefault();
            processReport(form);
        });

        list.append(item);
    });
}

async function processReport(form) {
    const reportId = form.dataset.reportId;
    const targetType = form.dataset.targetType;
    const targetId = form.dataset.targetId;
    const submitButton = form.querySelector('button[type="submit"]');
    const payload = {
        status: form.elements.status.value,
        action: form.elements.action.value,
        reportMemo: form.elements.reportMemo.value.trim(),
        suspendedUntil: form.elements.suspendedUntil.value || null
    };

    if (!reportId) return;

    submitButton.disabled = true;

    try {
        if (payload.status === 'RESOLVED' && payload.action === 'CONTENT_DELETE') {
            const deleted = await deleteReportedContent(targetType, targetId);
            if (!deleted) {
                submitButton.disabled = false;
                return;
            }
        }

        const response = await fetch(`/api/admin/report/${encodeURIComponent(reportId)}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const data = await response.json();

        if (!response.ok || data.result < 0) {
            alert(data.message || '신고 처리에 실패했습니다.');
            submitButton.disabled = false;
            return;
        }

        await loadReviewingReports();
    } catch (error) {
        console.error(error);
        alert('신고 처리에 실패했습니다.');
        submitButton.disabled = false;
    }
}

async function deleteReportedContent(targetType, targetId) {
    if (!targetId) {
        alert('삭제할 콘텐츠 정보가 없습니다.');
        return false;
    }

    let response;

    if (targetType === 'BOARD') {
        response = await fetch(`/api/board/view/delete?id=${encodeURIComponent(targetId)}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
    } else if (targetType === 'COMMENT') {
        response = await fetch(`/api/comment/delete?id=${encodeURIComponent(targetId)}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
            body: JSON.stringify({
                id: Number(targetId)
            })
        });
    } else if (targetType === 'CHAT_MESSAGE') {
        response = await fetch(`/api/chat/message/delete?id=${encodeURIComponent(targetId)}`, {
            method: 'DELETE',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            credentials: 'include',
        });
    } else {
        alert('이 신고 대상은 콘텐츠 삭제를 할 수 없습니다.');
        return false;
    }

    const data = await response.json();
    if (!response.ok || data.result < 0) {
        alert(data.message || '콘텐츠 삭제에 실패했습니다.');
        return false;
    }

    return true;
}

function formatTargetType(value) {
    return {
        MEMBER: '회원',
        BOARD: '게시글',
        COMMENT: '댓글',
        CHAT_MESSAGE: '채팅 메시지'
    }[value] || value;
}

function formatReason(report) {
    const value = report && typeof report === 'object' ? report.reason : report;
    const label = {
        SPAM: '스팸/도배',
        ABUSE: '욕설/괴롭힘',
        HATE: '혐오 표현',
        SEXUAL: '성적인 콘텐츠',
        FRAUD: '사기/허위 정보',
        PERSONAL_INFO: '개인정보 노출',
        COPYRIGHT: '저작권 침해',
        ETC: '기타'
    }[value] || value;
    const detail = report && typeof report === 'object' ? report.reasonDetail : '';

    if (value === 'ETC' && detail) {
        return `${label}: ${detail}`;
    }

    return label;
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
