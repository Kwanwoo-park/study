const notiDiv = document.querySelector('.noti-div')
const notificationGroupButtons = {
    ADMIN: { id: 'admin', label: '시스템' },
    FOLLOW: { id: 'follow', label: '팔로우' },
    FAVORITE: { id: 'favorite', label: '좋아요' },
    CHAT: { id: 'chat', label: '채팅' },
    COMMENT: { id: 'comment', label: '댓글' },
    REPLY: { id: 'reply', label: '답글' },
};
const notificationGroupUnreadCounts = {};
const notificationItemsById = new Map();

window.onload = function() {
    loadNotification();
}

async function loadNotification() {
    try {
        const res = await fetch(`/api/notification/load`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data.result > 0) {
            fnUpdateGroupUnreadCounts(data.list);
            fnDraw(data);
        } else
            alert('잠시 후에 다시 시도하여 주십시오');
    } catch (e) {
        console.error('로드 오류', e);
    }
}

async function fnClick(group) {
    const notiUl = document.querySelector('.noti-ul')
    if (notiUl) notiUl.remove();

    try {
        const res = await fetch(`/api/notification/sort/` + group, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data.result > 0)
            fnDraw(data);
        else
            alert('잠시 후에 다시 시도하여 주십시오');
    } catch (e) {
        console.error('로드 오류', e);
    }
}

function fnDraw(data) {
    const notiUl = document.createElement('ul');
    notiUl.className = 'noti-ul';

    data.list.forEach(item => {
        notificationItemsById.set(String(item.id), item);

        const mainLi = document.createElement('li');
        mainLi.className = 'notification-card';

        const clickDiv = document.createElement('div');
        clickDiv.onclick = async function() {
            const updated = await fnReadSilent(item.id);
            if (updated) fnApplyNotificationReadState(item.id);
            fnNotificationMove(item.notiGroup, item.url);
        }

        const message = document.createElement('p');
        message.innerText = item.message;
        message.className = 'notification-message';

        clickDiv.append(message);
        mainLi.append(clickDiv);

        const div = document.createElement('div');
        div.className = 'notification-actions';

        const btn = document.createElement('button')
        btn.className = 'btn btn-outline-dark mark-as-read-button'
        btn.id = item.id;

        if (item.readStatus == 'READ') {
            btn.disabled = true;
            btn.innerText = '읽음';
        } else {
            btn.innerText = '읽음으로 표시';
            btn.onclick = function() {
                fnRead(item.id);
            }
        }

        div.append(btn);

        mainLi.append(div);

        notiUl.append(mainLi);
    })

    notiDiv.append(notiUl);
}

function fnRead(id) {
    const button = document.getElementById(id);

    fetch(`/api/notification/mark-as-read?id=` + id, {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            alert('알림이 읽음으로 표시되었습니다');
            fnApplyNotificationReadState(id);
            if (typeof fnUpdateUnreadNotificationDot === 'function') fnUpdateUnreadNotificationDot();
        }
        else if (json['result'] == -1)
            alert('존재하지 않는 알림입니다');
        else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    });
}

function fnReadSilent(id) {
    if (typeof fnMarkNotificationAsRead === 'function') {
        return fnMarkNotificationAsRead(id);
    }

    return fetch(`/api/notification/mark-as-read?id=` + encodeURIComponent(id), {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => json['result'] > 0)
    .catch((error) => {
        console.error('알림 읽음 처리 오류:', error);
        return false;
    });
}

function fnUpdateGroupUnreadCounts(list) {
    Object.keys(notificationGroupButtons).forEach(group => {
        notificationGroupUnreadCounts[group] = 0;
    });

    list.forEach(item => {
        notificationItemsById.set(String(item.id), item);
        if (item.readStatus == 'UNREAD' && notificationGroupUnreadCounts[item.notiGroup] != null) {
            notificationGroupUnreadCounts[item.notiGroup] += 1;
        }
    });

    fnRenderGroupUnreadCounts();
}

function fnRenderGroupUnreadCounts() {
    Object.entries(notificationGroupButtons).forEach(([group, buttonInfo]) => {
        const button = document.getElementById(buttonInfo.id);
        if (!button) return;

        button.innerText = `${buttonInfo.label}(${notificationGroupUnreadCounts[group] || 0})`;
    });
}

function fnApplyNotificationReadState(id) {
    const key = String(id);
    const item = notificationItemsById.get(key);

    if (item && item.readStatus != 'READ') {
        item.readStatus = 'READ';
        if (notificationGroupUnreadCounts[item.notiGroup] > 0) {
            notificationGroupUnreadCounts[item.notiGroup] -= 1;
        }
        fnRenderGroupUnreadCounts();
    }

    const button = document.getElementById(id);
    if (!button) return;

    button.innerText = '읽음';
    button.disabled = true;
}

function fnHandleIncomingNotificationCount(notification) {
    const key = String(notification.id);
    if (notificationItemsById.has(key)) return;

    notificationItemsById.set(key, notification);

    const group = notification.notiGroup;
    const readStatus = notification.readStatus || 'UNREAD';
    if (readStatus == 'UNREAD' && notificationGroupUnreadCounts[group] != null) {
        notificationGroupUnreadCounts[group] += 1;
        fnRenderGroupUnreadCounts();
    }
}

function fnAllRead() {
    fetch(`/api/notification/mark-all-as-read`, {
        method: 'PATCH',
        headers: {
            "Content-Type": "appllication/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            alert('알림이 모두 읽음으로 표시되었습니다');
            window.location.reload();
        } else {
            alert('알림 상태를 변경하는 중 오류가 발생했습니다');
        }
    })
    .catch((error) => {
        console.error(error);
        alert("다시 시도하여주십시오");
    })
}
