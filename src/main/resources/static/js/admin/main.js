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
        const res = await fetch('/api/report/admin', {
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
                <span>${formatReportReason(report.reason)}</span>
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

    const userCount = document.createElement('label');
    userCount.innerText = "현재 접속자 수: " + data.count;

    userDiv.append(userCount);

    const ul = document.createElement('ul');

    data.list.forEach(member => {
        const li = document.createElement('li');

        const userName = document.createElement('label');
        userName.innerText = member.name;

        const email = document.createElement('label');
        email.innerText = "(" + member.email + ")";
        email.style.color = 'gray';

        li.append(userName);
        li.append(email);

        ul.append(li);
    })

    userDiv.append(ul);
}

function fnNewUserDraw(data) {
    const newUserDiv = document.getElementById('new-user');

    const userCount = document.createElement('label');
    userCount.innerText = "신규 가입자 수: " + data.count;

    newUserDiv.append(userCount);

    const ul = document.createElement('ul');

    data.list.forEach(member => {
        const li = document.createElement('li');

        const userName = document.createElement('label');
        userName.innerText = member.name;

        const email = document.createElement('label');
        email.innerText = "(" + member.email + ")";
        email.style.color = 'gray';

        li.append(userName);
        li.append(email);

        ul.append(li);
    });

    newUserDiv.append(ul);
}

function fnNewBoardDraw(data) {
    const newBoardDiv = document.getElementById('new-board');

    const boardCount = document.createElement('label');
    boardCount.innerText = "신규 게시글 수: " + data.count;

    newBoardDiv.append(boardCount);

    const ul = document.createElement('ul');

    data.list.forEach(board => {
        const li = document.createElement('li');
        li.onclick = function() {
            fnBoardMove(board.id);
        }

        const boardId = document.createElement('label');
        boardId.innerText = board.id;

        const boardUser = document.createElement('label');
        boardUser.innerText = "(작성자: " + board.member.name + ")";
        boardUser.style.color = 'gray';

        li.append(boardId);
        li.append(boardUser);

        ul.append(li);
    })

    newBoardDiv.append(ul);
}

function fnChattingActive(data) {
    const activeChattingDiv = document.getElementById('active-chat-room');

    const roomCount = document.createElement('label');
    roomCount.innerText = "활성화된 채팅방 수: " + data.count;

    activeChattingDiv.append(roomCount);

    const ul = document.createElement('ul');

    data.list.forEach(room => {
        const li = document.createElement('li');
        li.onclick = function() {
            fnChatRoomMove(room.roomId);
        }

        const roomId = document.createElement('label');
        roomId.innerText = room.id;

        const lastMessage = document.createElement('label');
        lastMessage.innerText = "(마지막 메시지: " + room.lastMessage + ")";
        lastMessage.style.color = 'gray';

        li.append(roomId);
        li.append(lastMessage);

        ul.append(li);
    })

    activeChattingDiv.append(ul);
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

function formatReportReason(value) {
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
