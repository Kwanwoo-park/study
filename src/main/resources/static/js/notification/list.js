const notificationList = document.querySelector('.noti-ul');
const notificationMoreButton = document.getElementById('notification-more');
const notificationEmpty = document.getElementById('notification-empty');
const notificationRange = document.getElementById('notification-range');
const notificationError = document.getElementById('notification-error');
const notificationPeriods = [
    {value: 'DAY', label: '최근 24시간'},
    {value: 'WEEK', label: '최근 일주일'},
    {value: 'MONTH', label: '최근 한 달'},
    {value: 'ALL', label: '전체'},
];
const notificationGroupButtons = {
    ADMIN: {id: 'admin', label: '시스템'},
    FOLLOW: {id: 'follow', label: '팔로우'},
    FAVORITE: {id: 'favorite', label: '좋아요'},
    TRAN: {id: 'tran', label: '거래'},
    CHAT: {id: 'chat', label: '채팅'},
    COMMENT: {id: 'comment', label: '댓글'},
    REPLY: {id: 'reply', label: '답글'},
    CALL: {id: 'call', label: '통화'},
};
const notificationGroupUnreadCounts = {};
const notificationItemsById = new Map();
const deletedNotificationIds = new Set();
let notificationGroup = '';
let notificationPeriodIndex = 0;
let notificationAsOf = '';
let notificationBeforeTime = '';
let notificationBeforeId = '';
let notificationHasMore = true;
let notificationLoading = false;

window.onload = () => loadNotification();

async function loadNotification() {
    if (notificationLoading) return;
    notificationLoading = true;
    notificationError.textContent = '';
    fnSetNotificationLoading(true);

    const params = new URLSearchParams({period: notificationPeriods[notificationPeriodIndex].value});
    if (notificationGroup) params.set('group', notificationGroup);
    if (notificationAsOf) params.set('asOf', notificationAsOf);
    if (notificationBeforeTime) {
        params.set('beforeTime', notificationBeforeTime);
        params.set('beforeId', notificationBeforeId);
    }

    try {
        const response = await fetch(`/api/notification/history?${params}`, {credentials: 'include'});
        const data = await response.json();
        if (!response.ok || data.result < 0) throw new Error('알림 조회 실패');

        notificationAsOf = data.asOf;
        notificationBeforeTime = data.nextBeforeTime;
        notificationBeforeId = data.nextBeforeId;
        notificationHasMore = data.hasMore;
        Object.keys(notificationGroupButtons).forEach(group => notificationGroupUnreadCounts[group] = 0);
        data.unreadCounts.forEach(item => notificationGroupUnreadCounts[item.group] = item.count);
        fnRenderGroupUnreadCounts();
        data.list.forEach(original => {
            if (deletedNotificationIds.has(String(original.id))) return;
            const item = fnApplyNotificationReadWatermarks(original);
            const previous = notificationItemsById.get(String(item.id));
            if (previous && fnNotificationRevisionKey(previous.updateTime) > fnNotificationRevisionKey(item.updateTime)) return;
            notificationItemsById.set(String(item.id), item);
            fnDrawNotification(item);
        });
        notificationRange.textContent = `${notificationPeriods[notificationPeriodIndex].label} 알림`;
    } catch (error) {
        notificationError.textContent = '알림을 불러오지 못했습니다. 다시 시도해주세요.';
        notificationHasMore = true;
    } finally {
        notificationLoading = false;
        fnSetNotificationLoading(false);
        fnUpdateNotificationHistoryControls();
    }
}

function fnSetNotificationLoading(loading) {
    notificationList.setAttribute('aria-busy', String(loading));
    document.querySelectorAll('.noti-group button').forEach(button => button.disabled = loading);
    notificationMoreButton.disabled = loading;
    if (loading) notificationMoreButton.textContent = '불러오는 중…';
}

function fnUpdateNotificationHistoryControls() {
    notificationEmpty.hidden = notificationList.children.length > 0;
    notificationEmpty.textContent = '이 기간에 받은 알림이 없습니다.';
    notificationMoreButton.hidden = !notificationHasMore && notificationPeriodIndex === notificationPeriods.length - 1;
    if (notificationHasMore) {
        notificationMoreButton.textContent = '더보기';
        return;
    }
    const nextPeriod = notificationPeriods[notificationPeriodIndex + 1];
    if (nextPeriod) notificationMoreButton.textContent = `더보기 · ${nextPeriod.label}`;
}

function fnLoadMoreNotifications() {
    if (notificationLoading) return;
    if (!notificationHasMore && notificationPeriodIndex < notificationPeriods.length - 1) notificationPeriodIndex++;
    return loadNotification();
}

function fnClick(group) {
    if (notificationLoading) return;
    notificationGroup = group;
    notificationPeriodIndex = 0;
    notificationAsOf = '';
    notificationBeforeTime = '';
    notificationBeforeId = '';
    notificationHasMore = true;
    notificationList.replaceChildren();
    document.querySelectorAll('.noti-group button').forEach(button => {
        button.setAttribute('aria-pressed', String(button.dataset.group === group));
    });
    return loadNotification();
}

function fnDrawNotification(item, prepend = false) {
    const card = document.createElement('li');
    card.id = `notification-${item.id}`;
    card.className = 'notification-card';
    card.classList.toggle('is-read', item.readStatus === 'READ');

    const content = document.createElement('button');
    content.type = 'button';
    content.className = 'notification-content';
    content.onclick = async () => {
        if (item.readStatus !== 'READ' && !await fnRead(item.id)) return;
        fnNotificationMove(item.notiGroup, item.url);
    };
    const message = document.createElement('span');
    message.className = 'notification-message';
    message.textContent = item.message;
    const time = document.createElement('time');
    time.className = 'notification-time';
    time.dateTime = item.updateTime;
    time.textContent = new Date(item.updateTime).toLocaleString('ko-KR');
    content.append(message, time);

    const actions = document.createElement('div');
    actions.className = 'notification-actions';
    const readButton = document.createElement('button');
    readButton.type = 'button';
    readButton.className = 'btn btn-outline-dark mark-as-read-button';
    readButton.disabled = item.readStatus === 'READ';
    readButton.textContent = '읽음으로 표시';
    if (readButton.disabled) readButton.textContent = '읽음';
    readButton.onclick = () => fnRead(item.id);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn btn-outline-danger notification-delete';
    deleteButton.textContent = '삭제';
    deleteButton.setAttribute('aria-label', `${item.message} 알림 삭제`);
    deleteButton.onclick = () => fnDeleteNotification(item.id, deleteButton);
    actions.append(readButton, deleteButton);
    card.append(content, actions);

    const previousCard = document.getElementById(card.id);
    if (previousCard) previousCard.replaceWith(card);
    else if (prepend) notificationList.prepend(card);
    else notificationList.append(card);
}

async function fnRead(id) {
    if (await fnMarkNotificationAsRead(id)) {
        fnApplyNotificationReadState(id);
        return true;
    }
    alert('알림 읽음 처리에 실패했습니다. 다시 시도해주세요.');
    return false;
}

async function fnDeleteNotification(id, button) {
    if (!confirm('이 알림을 삭제할까요?')) return;
    button.disabled = true;
    try {
        const response = await fetch(`/api/notification/${id}`, {method: 'DELETE', credentials: 'include'});
        const data = await response.json();
        if (!response.ok || data.result < 0) throw new Error('알림 삭제 실패');
        fnApplyNotificationReadState(id);
        deletedNotificationIds.add(String(id));
        notificationItemsById.delete(String(id));
        document.getElementById(`notification-${id}`).remove();
        fnUpdateUnreadNotificationDot();
        fnUpdateNotificationHistoryControls();
    } catch (error) {
        button.disabled = false;
        alert('알림을 삭제하지 못했습니다. 다시 시도해주세요.');
    }
}

function fnRenderGroupUnreadCounts() {
    Object.entries(notificationGroupButtons).forEach(([group, info]) => {
        document.getElementById(info.id).textContent = `${info.label}(${notificationGroupUnreadCounts[group]})`;
    });
}

function fnApplyNotificationReadState(id) {
    const item = notificationItemsById.get(String(id));
    if (item && item.readStatus !== 'READ') {
        item.readStatus = 'READ';
        if (notificationGroupUnreadCounts[item.notiGroup] > 0) notificationGroupUnreadCounts[item.notiGroup]--;
        fnRenderGroupUnreadCounts();
    }
    const card = document.getElementById(`notification-${id}`);
    if (!card) return;
    card.classList.add('is-read');
    const button = card.querySelector('.mark-as-read-button');
    button.textContent = '읽음';
    button.disabled = true;
}

function fnHandleIncomingNotificationCount(notification) {
    const key = String(notification.id);
    if (deletedNotificationIds.has(key)) return;
    const previous = notificationItemsById.get(key);
    if (previous && fnNotificationRevisionKey(notification.updateTime) < fnNotificationRevisionKey(previous.updateTime)) return;
    if (notification.readStatus === 'READ') {
        fnApplyNotificationReadState(notification.id);
        notificationItemsById.set(key, notification);
        return;
    }
    notificationItemsById.set(key, notification);
    if ((!previous || previous.readStatus === 'READ') && notificationGroupUnreadCounts[notification.notiGroup] != null) {
        notificationGroupUnreadCounts[notification.notiGroup]++;
        fnRenderGroupUnreadCounts();
    }
    if (notificationAsOf && (!notificationGroup || notificationGroup === notification.notiGroup)) {
        fnDrawNotification(notification, true);
        fnUpdateNotificationHistoryControls();
    }
}

function fnApplyNotificationReadEvent(read) {
    notificationItemsById.forEach(item => {
        if (fnNotificationMatchesRead(item, read)) fnApplyNotificationReadState(item.id);
    });
    if (read.notificationId == null && read.notiGroup == null) {
        Object.keys(notificationGroupButtons).forEach(group => notificationGroupUnreadCounts[group] = 0);
        notificationItemsById.forEach(item => {
            if (item.readStatus === 'UNREAD') notificationGroupUnreadCounts[item.notiGroup]++;
        });
        fnRenderGroupUnreadCounts();
    }
}

async function fnAllRead() {
    try {
        const response = await fetch('/api/notification/mark-all-as-read', {method: 'PATCH', credentials: 'include'});
        const data = await response.json();
        if (!response.ok || data.result < 0) throw new Error('읽음 처리 실패');
        fnHandleNotificationReadEvent({readBefore: data.readBefore});
    } catch (error) {
        alert('알림 상태를 변경하지 못했습니다. 다시 시도해주세요.');
    }
}
