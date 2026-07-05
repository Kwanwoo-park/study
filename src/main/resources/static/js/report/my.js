(function() {
    const list = document.getElementById('myReportList');
    if (!list) return;

    const message = document.getElementById('myReportMessage');
    const pagination = document.getElementById('myReportPagination');
    const refreshButton = document.getElementById('refreshReportsButton');
    const pageSize = 10;
    let currentPage = 0;
    let loading = false;

    refreshButton.addEventListener('click', function() {
        loadReports(currentPage);
    });

    loadReports(0);

    async function loadReports(page) {
        if (loading) return;

        loading = true;
        currentPage = Math.max(Number(page) || 0, 0);
        setMessage('신고 내역을 불러오는 중입니다.', '');
        list.innerHTML = '';
        pagination.innerHTML = '';

        try {
            const response = await fetch(`/api/report/my?page=${currentPage}&size=${pageSize}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8'
                },
                credentials: 'include'
            });

            let json = {};
            try {
                json = await response.json();
            } catch (error) {
                json = {};
            }

            if (response.status === 401) {
                location.replace('/member/login?error=true&exception=Not Found&url=/report/my');
                return;
            }

            if (!response.ok || json.result < 0) {
                setMessage(json.message || '신고 내역을 불러오지 못했습니다.', 'error');
                return;
            }

            renderReports(json.list || []);
            renderPagination(json.page || 0, json.totalPages || 0);
            setMessage('', '');
        } catch (error) {
            console.error(error);
            setMessage('신고 내역을 불러오지 못했습니다.', 'error');
        } finally {
            loading = false;
        }
    }

    function renderReports(reports) {
        list.innerHTML = '';

        if (reports.length === 0) {
            list.innerHTML = '<div class="report-empty">제출한 신고 내역이 없습니다.</div>';
            return;
        }

        reports.forEach(function(report) {
            const item = document.createElement('article');
            item.className = 'report-item';

            const canCancel = report.status === 'PENDING';
            item.innerHTML = `
                <div class="report-item-header">
                    <div>
                        <strong>#${escapeHtml(report.id)} ${formatTargetType(report.targetType)}</strong>
                        <span class="report-status ${getStatusClass(report.status)}">${formatStatus(report.status)}</span>
                    </div>
                    ${canCancel ? `<button type="button" class="btn btn-outline-danger btn-sm report-cancel-button" data-report-id="${escapeHtml(report.id)}">취소</button>` : ''}
                </div>
                <dl class="report-meta">
                    <div>
                        <dt>대상 ID</dt>
                        <dd>${escapeHtml(report.targetId)}</dd>
                    </div>
                    <div>
                        <dt>신고 사유</dt>
                        <dd>${escapeHtml(formatReason(report))}</dd>
                    </div>
                    <div>
                        <dt>신고일</dt>
                        <dd>${formatDateTime(report.registerTime)}</dd>
                    </div>
                    <div>
                        <dt>처리 결과</dt>
                        <dd>${formatAction(report.action)}</dd>
                    </div>
                </dl>
                <p class="report-item-description">${escapeHtml(report.description)}</p>
                ${report.reportMemo ? `<p class="report-item-memo">${escapeHtml(report.reportMemo)}</p>` : ''}
            `;

            const cancelButton = item.querySelector('.report-cancel-button');
            if (cancelButton) {
                cancelButton.addEventListener('click', function() {
                    cancelReport(cancelButton.dataset.reportId, cancelButton);
                });
            }

            list.append(item);
        });
    }

    function renderPagination(page, totalPages) {
        pagination.innerHTML = '';
        if (totalPages <= 1) return;

        const prevButton = createPageButton('이전', page - 1, page <= 0);
        const nextButton = createPageButton('다음', page + 1, page >= totalPages - 1);
        const pageInfo = document.createElement('span');
        pageInfo.className = 'report-page-info';
        pageInfo.textContent = `${page + 1} / ${totalPages}`;

        pagination.append(prevButton, pageInfo, nextButton);
    }

    function createPageButton(label, page, disabled) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-outline-secondary btn-sm';
        button.textContent = label;
        button.disabled = disabled;
        button.addEventListener('click', function() {
            loadReports(page);
        });
        return button;
    }

    async function cancelReport(reportId, button) {
        if (!reportId || !confirm('아직 처리되지 않은 신고를 취소할까요?')) return;

        button.disabled = true;
        setMessage('신고를 취소하는 중입니다.', '');

        try {
            const response = await fetch(`/api/report/${encodeURIComponent(reportId)}/cancel`, {
                method: 'PATCH',
                headers: {
                    'Content-Type': 'application/json; charset=utf-8'
                },
                credentials: 'include'
            });

            let json = {};
            try {
                json = await response.json();
            } catch (error) {
                json = {};
            }

            if (response.status === 401) {
                location.replace('/member/login?error=true&exception=Not Found&url=/report/my');
                return;
            }

            if (!response.ok || json.result < 0) {
                setMessage(json.message || '신고 취소에 실패했습니다.', 'error');
                button.disabled = false;
                return;
            }

            await loadReports(currentPage);
            setMessage('신고가 취소되었습니다.', 'success');
        } catch (error) {
            console.error(error);
            setMessage('신고 취소에 실패했습니다.', 'error');
            button.disabled = false;
        }
    }

    function setMessage(text, type) {
        message.textContent = text;
        message.classList.remove('error', 'success');
        if (type) message.classList.add(type);
    }

    function formatTargetType(value) {
        const labels = {
            MEMBER: '회원',
            BOARD: '게시글',
            COMMENT: '댓글',
            CHAT_MESSAGE: '채팅 메시지'
        };
        return labels[value] || value || '-';
    }

    function formatStatus(value) {
        const labels = {
            PENDING: '접수 대기',
            REVIEWING: '검토 중',
            RESOLVED: '처리 완료',
            REJECTED: '반려',
            CANCELED: '취소됨'
        };
        return labels[value] || value || '-';
    }

    function getStatusClass(value) {
        const classes = {
            PENDING: 'pending',
            REVIEWING: 'reviewing',
            RESOLVED: 'resolved',
            REJECTED: 'rejected',
            CANCELED: 'canceled'
        };
        return classes[value] || '';
    }

    function formatReason(report) {
        const value = report && typeof report === 'object' ? report.reason : report;
        const labels = {
            SPAM: '스팸/도배',
            ABUSE: '욕설/괴롭힘',
            HATE: '혐오 표현',
            SEXUAL: '성적인 콘텐츠',
            FRAUD: '사기/허위 정보',
            PERSONAL_INFO: '개인정보 노출',
            COPYRIGHT: '저작권 침해',
            ETC: '기타'
        };
        const label = labels[value] || value || '-';
        const detail = report && typeof report === 'object' ? report.reasonDetail : '';

        if (value === 'ETC' && detail) {
            return `${label}: ${detail}`;
        }

        return label;
    }

    function formatAction(value) {
        const labels = {
            NONE: '없음',
            CONTENT_DELETE: '콘텐츠 삭제',
            WARNING: '경고',
            TEMPORARY_SUSPEND: '임시 정지',
            PERMANENT_BAN: '영구 정지'
        };
        return labels[value] || value || '-';
    }

    function formatDateTime(value) {
        if (!value) return '-';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return value;

        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
})();
