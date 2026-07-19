window.onload = async function() {
    await loadSystemStatus();
    await loadUserStatus();
    await loadNewUser();
    await loadNewBoard();
    await loadActiveChatting();
    await loadRecentReports();
    setInterval(loadSystemStatus, 30000);
}

async function loadSystemStatus() {
    const statusDiv = document.getElementById('system-status');
    const updatedAt = document.getElementById('system-status-updated');

    if (!statusDiv) return;

    try {
        const res = await fetch(`/api/admin/system/status`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data.result > 0) {
            fnSystemStatusDraw(data);

            if (updatedAt) {
                updatedAt.innerText = "최근 갱신: " + new Date().toLocaleTimeString('ko-KR');
            }
        }
    } catch (error) {
        console.error(error);
        statusDiv.innerHTML = '<div class="admin-system-error">서버 상태를 불러오지 못했습니다</div>';
    }
}

async function loadUserStatus() {
    const res = await fetch(`/api/admin/member/online`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnDraw(data);
}

async function loadNewUser() {
    const res = await fetch(`/api/admin/member/new`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnNewUserDraw(data);
}

async function loadNewBoard() {
    const res = await fetch(`/api/admin/board/new`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });
    const data = await res.json();

    if (data.count >= 0)
        fnNewBoardDraw(data);
}

async function loadActiveChatting() {
    const res = await fetch(`/api/admin/chat/active`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    });

    const data = await res.json();

    if (data.count >= 0)
        fnChattingActive(data);
}

async function loadRecentReports() {
    const reportDiv = document.getElementById('recent-report-list');
    if (!reportDiv) return;

    reportDiv.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오는 중입니다.</div>';

    try {
        const res = await fetch('/api/admin/report', {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (!res.ok || data.result < 0) {
            reportDiv.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오지 못했습니다.</div>';
            return;
        }

        fnRecentReportDraw(data.list || []);
    } catch (error) {
        console.error(error);
        reportDiv.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오지 못했습니다.</div>';
    }
}

function fnRecentReportDraw(reports) {
    const reportDiv = document.getElementById('recent-report-list');
    reportDiv.innerHTML = '';

    if (reports.length === 0) {
        reportDiv.innerHTML = '<div class="admin-report-empty">접수 대기 중인 신고가 없습니다.</div>';
        return;
    }

    reports.forEach(report => {
        const card = document.createElement('article');
        card.className = 'admin-report-card';
        card.onclick = function() {
            location.href = '/admin/report';
        };
        card.innerHTML = `
            <div class="admin-report-card-title">
                <strong>#${report.id} ${formatReportTargetType(report.targetType)}</strong>
                <span>${escapeHtml(formatReportReason(report))}</span>
            </div>
            <div class="admin-report-card-meta">대상 ID: ${escapeHtml(report.targetId)}</div>
            <div class="admin-report-card-meta">신고자: ${escapeHtml(report.reporterName)}</div>
            <p>${escapeHtml(report.description)}</p>
        `;
        reportDiv.append(card);
    });
}

function fnDraw(data) {
    const userDiv = document.getElementById('user-status');

    renderActivityCard(userDiv, {
        label: 'LIVE',
        title: '현재 접속자',
        count: data.count,
        unit: '명',
        emptyMessage: '현재 접속 중인 사용자가 없습니다.',
        items: data.list.map(member => ({
            badge: getInitial(member.name),
            title: member.name,
            description: member.email
        }))
    });
}

function fnNewUserDraw(data) {
    const newUserDiv = document.getElementById('new-user');

    renderActivityCard(newUserDiv, {
        label: 'LAST 24 HOURS',
        title: '신규 가입자',
        count: data.count,
        unit: '명',
        emptyMessage: '최근 가입한 사용자가 없습니다.',
        items: data.list.map(member => ({
            badge: getInitial(member.name),
            title: member.name,
            description: member.email
        }))
    });
}

function fnNewBoardDraw(data) {
    const newBoardDiv = document.getElementById('new-board');

    renderActivityCard(newBoardDiv, {
        label: 'LAST 7 DAYS',
        title: '신규 게시글',
        count: data.count,
        unit: '건',
        emptyMessage: '최근 등록된 게시글이 없습니다.',
        items: data.list.map(board => ({
            badge: '#',
            title: `게시글 #${board.id}`,
            description: `작성자 · ${board.memberName}`,
            onClick: () => fnBoardMove(board.id)
        }))
    });
}

function fnChattingActive(data) {
    const activeChattingDiv = document.getElementById('active-chat-room');

    renderActivityCard(activeChattingDiv, {
        label: 'LAST 1 HOUR',
        title: '활성 채팅방',
        count: data.count,
        unit: '개',
        emptyMessage: '최근 활성화된 채팅방이 없습니다.',
        items: data.list.map(room => ({
            badge: 'C',
            title: `채팅방 #${room.id}`,
            description: room.lastMessage ? `최근 메시지 · ${room.lastMessage}` : '아직 메시지가 없습니다.',
            onClick: () => fnChatRoomMove(room.roomId)
        }))
    });
}

function renderActivityCard(container, config) {
    const header = document.createElement('header');
    const heading = document.createElement('div');
    const label = document.createElement('span');
    const title = document.createElement('h3');
    const count = document.createElement('strong');
    const unit = document.createElement('span');
    const list = document.createElement('ul');

    header.className = 'admin-activity-card-header';
    heading.className = 'admin-activity-card-heading';
    label.className = 'admin-activity-label';
    count.className = 'admin-activity-count';
    unit.className = 'admin-activity-unit';
    list.className = 'admin-activity-list';

    label.innerText = config.label;
    title.innerText = config.title;
    count.innerText = config.count;
    unit.innerText = config.unit;

    heading.append(label, title);
    count.append(unit);
    header.append(heading, count);

    if (config.items.length === 0) {
        const empty = document.createElement('li');
        empty.className = 'admin-activity-empty';
        empty.innerText = config.emptyMessage;
        list.append(empty);
    } else {
        config.items.forEach(item => list.append(createActivityItem(item)));
    }

    container.replaceChildren(header, list);
}

function createActivityItem(item) {
    const row = document.createElement('li');
    const badge = document.createElement('span');
    const text = document.createElement('span');
    const title = document.createElement('strong');
    const description = document.createElement('span');

    row.className = 'admin-activity-item';
    badge.className = 'admin-activity-item-badge';
    text.className = 'admin-activity-item-text';
    description.className = 'admin-activity-item-description';

    badge.innerText = item.badge;
    title.innerText = item.title;
    description.innerText = item.description;
    text.append(title, description);
    row.append(badge, text);

    if (item.onClick) {
        const arrow = document.createElement('span');
        arrow.className = 'admin-activity-item-arrow';
        arrow.innerText = '›';
        row.classList.add('is-clickable');
        row.tabIndex = 0;
        row.setAttribute('role', 'link');
        row.append(arrow);
        row.addEventListener('click', item.onClick);
        row.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                item.onClick();
            }
        });
    }

    return row;
}

function getInitial(name) {
    const normalizedName = String(name || '').trim();
    return normalizedName ? normalizedName.charAt(0).toUpperCase() : '?';
}

function fnSystemStatusDraw(data) {
    const statusDiv = document.getElementById('system-status');

    statusDiv.replaceChildren(
        createMetricCard('CPU', data.cpu.systemPercent + '%', [
            '프로세스 ' + data.cpu.processPercent + '%',
            data.cpu.cores + ' cores'
        ], data.cpu.systemPercent),
        createMetricCard('RAM', data.memory.usedPercent + '%', [
            formatBytes(data.memory.usedBytes) + ' / ' + formatBytes(data.memory.totalBytes)
        ], data.memory.usedPercent),
        createMetricCard('Disk', data.disk.usedPercent + '%', [
            formatBytes(data.disk.usedBytes) + ' / ' + formatBytes(data.disk.totalBytes)
        ], data.disk.usedPercent),
        createMetricCard('JVM Heap', data.jvm.heapUsedPercent + '%', [
            formatBytes(data.jvm.heapUsedBytes) + ' / ' + formatBytes(data.jvm.heapMaxBytes)
        ], data.jvm.heapUsedPercent),
        createMetricCard('Uptime', data.jvm.uptime, [
            '시작: ' + formatDateTime(data.jvm.startedAt)
        ]),
        createMetricCard('접속', data.traffic.activeSessions + '명', [
            'WebSocket ' + data.traffic.activeWebSockets + '개'
        ])
    );
}

function createMetricCard(title, value, details, percent) {
    const card = document.createElement('article');
    const titleEl = document.createElement('span');
    const valueEl = document.createElement('strong');
    const detailList = document.createElement('div');

    card.className = 'admin-system-card';
    titleEl.className = 'admin-system-title';
    valueEl.className = 'admin-system-value';
    detailList.className = 'admin-system-details';

    titleEl.innerText = title;
    valueEl.innerText = value;

    details.forEach(detail => {
        const item = document.createElement('span');
        item.innerText = detail;
        detailList.append(item);
    });

    card.append(titleEl);
    card.append(valueEl);

    if (typeof percent === 'number') {
        const meter = document.createElement('div');
        const meterBar = document.createElement('span');

        meter.className = 'admin-system-meter';
        meterBar.style.width = Math.min(Math.max(percent, 0), 100) + '%';

        if (percent >= 90) {
            meterBar.className = 'danger';
        } else if (percent >= 75) {
            meterBar.className = 'warning';
        }

        meter.append(meterBar);
        card.append(meter);
    }

    card.append(detailList);

    return card;
}

function formatBytes(bytes) {
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let value = Number(bytes || 0);
    let unitIndex = 0;

    while (value >= 1024 && unitIndex < units.length - 1) {
        value /= 1024;
        unitIndex += 1;
    }

    return value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1) + units[unitIndex];
}

function formatDateTime(value) {
    if (!value) return '-';

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString('ko-KR');
}

function formatReportTargetType(value) {
    return {
        MEMBER: '회원',
        BOARD: '게시글',
        COMMENT: '댓글',
        CHAT_MESSAGE: '채팅 메시지'
    }[value] || value;
}

function formatReportReason(report) {
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

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function fnLogout() {
    fetch(`/api/member/logout`, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            location.replace(`/member/login`);
        } else
            alert("다시 시도하여주십시오");
    })
}

function fnBoardMove(id) {
    location.replace(`/board/view?id=` + id);
}

function fnChatRoomMove(id) {
    location.replace(`/chat/chatRoom?roomId=` + id);
}
