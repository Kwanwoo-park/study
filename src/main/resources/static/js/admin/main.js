window.onload = async function() {
    await loadSystemIncidents();
    await loadTokenSessions();
    await loadActivityCards();
    await loadRecentReports();
    setInterval(loadSystemIncidents, 30000);

    const incidentRefresh = document.getElementById('incident-refresh');
    incidentRefresh.addEventListener('click', loadSystemIncidents);

    const incidentAcknowledgeAll = document.getElementById('incident-acknowledge-all');
    incidentAcknowledgeAll.addEventListener('click', acknowledgeAllSystemIncidents);
    const tokenSessionRefresh = document.getElementById('token-session-refresh');
    tokenSessionRefresh.addEventListener('click', loadTokenSessions);
}

async function loadSystemIncidents() {
    const incidentList = document.getElementById('system-incident-list');
    const incidentCount = document.getElementById('incident-unacknowledged-count');
    const acknowledgeAllButton = document.getElementById('incident-acknowledge-all');
    try {
        const response = await fetch('/api/admin/system/incidents', {
            credentials: 'include',
        });
        const data = await response.json();
        if (!response.ok || data.result < 0) {
            throw new Error('장애 기록 조회 실패');
        }

        const unacknowledgedCount = data.unacknowledgedCount;
        incidentCount.innerText = `${unacknowledgedCount}건 미확인`;
        incidentCount.classList.toggle('has-incidents', unacknowledgedCount > 0);
        acknowledgeAllButton.disabled = unacknowledgedCount === 0;
        acknowledgeAllButton.dataset.unacknowledgedCount = String(unacknowledgedCount);
        renderDeployment(data.deployment);
        renderSystemIncidents(data.list, data.deployment);
    } catch (error) {
        console.error(error);
        incidentList.innerHTML = '<div class="admin-incident-empty error">장애 기록을 불러오지 못했습니다.</div>';
    }
}

function renderDeployment(info) {
    const element = document.getElementById('admin-deployment-info');
    if (!element || !info) return;
    let version = info.commit ? info.commit.slice(0, 12) : '빌드 정보 없음';
    if (info.dirty) version += ' · 빌드 당시 미커밋 변경 포함';
    element.title = info.commit || '';
    element.textContent = `실행 버전 ${version} · 서버 시작 ${formatDateTime(info.startedAt)}`;
}

function renderSystemIncidents(incidents, deployment) {
    const incidentList = document.getElementById('system-incident-list');
    incidentList.replaceChildren();

    if (incidents.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'admin-incident-empty';
        empty.innerText = '미확인된 서버 장애가 없습니다.';
        incidentList.append(empty);
        return;
    }

    incidents.forEach(incident => incidentList.append(createIncidentCard(incident, deployment)));
}

function createIncidentCard(incident, deployment) {
    const card = document.createElement('article');
    const header = document.createElement('div');
    const status = document.createElement('span');
    const time = document.createElement('time');
    const route = document.createElement('strong');
    const ip = document.createElement('div');
    const type = document.createElement('div');
    const message = document.createElement('p');
    const acknowledge = document.createElement('button');

    card.className = 'admin-incident-card';
    header.className = 'admin-incident-card-header';
    status.className = 'admin-incident-http-status';
    status.innerText = String(incident.httpStatus || 500);
    time.innerText = formatDateTime(incident.occurredAt);
    if (incident.occurredAt && deployment?.startedAt) {
        time.innerText += incident.occurredAt >= deployment.startedAt ? ' · 이번 기동 이후' : ' · 이번 기동 이전';
    }
    route.className = 'admin-incident-route';
    route.innerText = `${incident.requestMethod || 'UNKNOWN'} ${incident.requestPath || 'UNKNOWN'}`;
    ip.className = 'admin-incident-ip';
    const location = incident.requestLocation?.displayName || '지역 확인 불가';
    ip.innerText = `IP ${incident.requestIp || 'UNKNOWN'} · ${location}`;
    type.className = 'admin-incident-type';
    const occurrenceCount = Number(incident.occurrenceCount || 1);
    type.innerText = `${incident.exceptionType || 'UnknownException'}${occurrenceCount > 1 ? ` · ${occurrenceCount}회 발생` : ''}`;
    message.className = 'admin-incident-message';
    message.innerText = incident.message || '메시지 없는 서버 오류';
    acknowledge.type = 'button';
    acknowledge.className = 'btn btn-sm btn-outline-danger';
    acknowledge.innerText = '확인 처리';
    acknowledge.addEventListener('click', () => acknowledgeSystemIncident(incident.id));

    header.append(status, time);
    card.append(header, route, ip, type, message, acknowledge);
    return card;
}

async function acknowledgeIncidents(path, failureMessage) {
    try {
        const response = await fetch('/api/admin/system/incidents' + path, {
            method: 'PATCH',
            credentials: 'include',
        });
        const data = await response.json();
        if (!response.ok || data.result < 0) {
            alert(data.message || failureMessage);
            return;
        }
        await loadSystemIncidents();
    } catch (error) {
        console.error(error);
        alert(failureMessage);
    }
}

function acknowledgeSystemIncident(incidentId) {
    return acknowledgeIncidents(`/${encodeURIComponent(incidentId)}/acknowledge`, '장애 기록을 확인 처리하지 못했습니다.');
}

async function acknowledgeAllSystemIncidents() {
    const button = document.getElementById('incident-acknowledge-all');
    const unacknowledgedCount = Number(button.dataset.unacknowledgedCount || 0);
    if (unacknowledgedCount === 0) return;

    if (!confirm(`미확인 서버 장애 ${unacknowledgedCount}건을 모두 확인 처리할까요?`)) return;

    const originalText = button.innerText;
    button.disabled = true;
    button.innerText = '처리 중...';

    try {
        await acknowledgeIncidents('/acknowledge-all', '장애 기록을 전체 확인 처리하지 못했습니다.');
    } finally {
        button.innerText = originalText;
        button.disabled = Number(button.dataset.unacknowledgedCount || 0) === 0;
    }
}

async function loadTokenSessions() {
    const tokenList = document.getElementById('token-session-list');
    try {
        const response = await fetch('/api/admin/token/sessions', {
            credentials: 'include',
        });
        const data = await response.json();
        if (!response.ok || data.result < 0) throw new Error('토큰 접속 정보 조회 실패');
        renderTokenSessions(data.list);
    } catch (error) {
        console.error(error);
        tokenList.innerHTML = '<div class="admin-token-empty error">로그인 접속 정보를 불러오지 못했습니다.</div>';
    }
}

function renderTokenSessions(sessions) {
    const tokenList = document.getElementById('token-session-list');
    tokenList.replaceChildren();

    if (sessions.length === 0) {
        const empty = document.createElement('div');
        empty.className = 'admin-token-empty';
        empty.innerText = '현재 유효한 로그인 토큰이 없습니다.';
        tokenList.append(empty);
        return;
    }

    sessions.forEach(session => {
        const item = document.createElement('article');
        const member = document.createElement('strong');
        const ip = document.createElement('span');
        const expiry = document.createElement('time');
        const location = session.ipLocation?.displayName || '지역 확인 불가';

        item.className = 'admin-token-item';
        member.innerText = session.memberEmail || `회원 ${session.memberId || '-'}`;
        ip.innerText = `IP ${session.ipAddress || 'UNKNOWN'} · ${location}`;
        expiry.innerText = `만료 ${formatDateTime(session.expiresAt)}`;
        item.append(member, ip, expiry);
        tokenList.append(item);
    });
}

async function loadActivityCards() {
    await loadActivityCard('/member/online', 'user-status', {
        label: 'LIVE',
        title: '현재 접속자',
        unit: '명',
        emptyMessage: '현재 접속 중인 사용자가 없습니다.',
    }, memberActivity);
    await loadActivityCard('/member/new', 'new-user', {
        label: 'LAST 24 HOURS',
        title: '신규 가입자',
        unit: '명',
        emptyMessage: '최근 가입한 사용자가 없습니다.',
    }, memberActivity);
    await loadActivityCard('/board/new', 'new-board', {
        label: 'LAST 7 DAYS',
        title: '신규 게시글',
        unit: '건',
        emptyMessage: '최근 등록된 게시글이 없습니다.',
    }, boardActivity);
    await loadActivityCard('/chat/active', 'active-chat-room', {
        label: 'LAST 1 HOUR',
        title: '활성 채팅방',
        unit: '개',
        emptyMessage: '최근 활성화된 채팅방이 없습니다.',
    }, chatActivity);
}

async function loadActivityCard(path, elementId, config, toItem) {
    const response = await fetch('/api/admin' + path, {credentials: 'include'});
    const data = await response.json();
    if (data.count >= 0) {
        renderActivityCard(document.getElementById(elementId), {
            ...config, count: data.count, items: data.list.map(toItem),
        });
    }
}

async function loadRecentReports() {
    const reportDiv = document.getElementById('recent-report-list');
    reportDiv.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오는 중입니다.</div>';

    try {
        const res = await fetch('/api/admin/report', {
            credentials: "include",
        });
        const data = await res.json();

        if (!res.ok || data.result < 0) {
            reportDiv.innerHTML = '<div class="admin-report-empty">신고 목록을 불러오지 못했습니다.</div>';
            return;
        }

        fnRecentReportDraw(data.list);
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
            window.location.href = '/admin/report';
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

function memberActivity(member) {
    return {
        badge: getInitial(member.name),
        imageUrl: member.profile,
        imageAlt: `${member.name} 프로필`,
        title: member.name,
        description: member.email,
    };
}

function boardActivity(board) {
    return {
        badge: '#',
        imageUrl: board.imageUrl,
        imageAlt: `게시글 #${board.id} 대표 이미지`,
        title: `게시글 #${board.id}`,
        description: `작성자 · ${board.memberName}`,
        onClick: () => fnBoardMove(board.id),
    };
}

function chatActivity(room) {
    return {
        badge: 'C',
        title: `채팅방 #${room.id}`,
        description: room.lastMessage ? `최근 메시지 · ${room.lastMessage}` : '아직 메시지가 없습니다.',
        onClick: () => fnChatRoomMove(room.roomId),
    };
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

    if (item.imageUrl) {
        const image = document.createElement('img');
        image.className = 'admin-activity-item-image';
        image.alt = item.imageAlt || '';
        image.addEventListener('load', () => badge.classList.add('has-image'));
        image.addEventListener('error', () => {
            badge.classList.remove('has-image');
            image.remove();
        });
        image.src = item.imageUrl;
        badge.append(image);
    }

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
    const value = report.reason;
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

    const detail = report.reasonDetail;
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

function fnBoardMove(id) {
    window.location.href = `/board/view?id=` + id;
}

function fnChatRoomMove(id) {
    window.location.href = `/chat/chatRoom?roomId=` + id;
}
