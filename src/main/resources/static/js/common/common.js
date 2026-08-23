(function() {
    const savedTheme = fnGetStoredTheme();
    if (savedTheme === 'dark' && document.body) {
        document.body.classList.add('dark-mode');
    }
})();

let activeIncomingAudioCall = null;
let incomingAudioCallTimeout = null;
let incomingAudioRingtoneContext = null;
let incomingAudioRingtoneInterval = null;
const incomingAudioSystemNotifications = new Map();

document.addEventListener('DOMContentLoaded', function() {
    fnInitThemeSelector();
    fnInitNavigationSettings();
    fnUpdateUnreadNotificationDot();

    const eventSource = new EventSource('/api/notification/stream');

    eventSource.addEventListener('notification', function(event) {
        try {
            let json = JSON.parse(event.data);
            const notificationId = json['id'];
            const notificationMessage = json['message'];
            const notificationGroup = json['notiGroup'];
            const notificationUrl = json['url'];
            const notificationReadStatus = json['readStatus'] || 'UNREAD';
            const isAudioCall = notificationGroup === 'CALL';

            if (notificationMessage) {
                fnUpdateUnreadNotificationDot();
                if (typeof fnHandleIncomingNotificationCount === 'function') fnHandleIncomingNotificationCount(json);
                if (isAudioCall) {
                    if (notificationReadStatus === 'READ' && typeof fnApplyNotificationReadState === 'function') {
                        fnApplyNotificationReadState(notificationId);
                    }
                    fnHandleAudioCallRealtime(json).catch(error => {
                        console.error('통화 알림 처리 오류:', error);
                    });
                    return;
                }
                if (notificationReadStatus !== 'UNREAD') return;

                const notificationBanner = fnEnsureNotificationBanner();
                const notificationElement = notificationBanner.querySelector('#notification-message');
                notificationElement.textContent = notificationMessage;

                notificationBanner.classList.remove('d-none');
                notificationBanner.style.cursor = 'pointer';
                notificationBanner.dataset.notificationId = notificationId || '';
                notificationBanner.onclick = function() {
                    notificationBanner.classList.add('d-none');
                    fnMoveNotificationAfterRead(notificationId, notificationGroup, notificationUrl);
                };

                setTimeout(() => {
                    notificationBanner.classList.add('d-none');
                }, 5000);
            }
        } catch (error) {
            console.error('SSE 메시지 처리 오류:', error);
        }
    });

    eventSource.onerror = function(event) {
        console.error('SSE 연결 오류:', event);

        if (event.readyState === EventSource.CONNECTING) {
            console.error('연결 재시도 중...');
        }
        else if (event.readyState === EventSource.CLOSED) {
            console.error('연결이 닫혔습니다');
        }
        console.error('상태:', event.readyState);
    };

    const notificationBanner = document.getElementById('notification-banner');
    const notificationCloseButton = notificationBanner ? notificationBanner.querySelector('.btn-close') : null;
    if (notificationCloseButton) {
        notificationCloseButton.addEventListener('click', function(event) {
            event.stopPropagation();
            const notificationBanner = document.getElementById('notification-banner');
            const notificationId = notificationBanner ? notificationBanner.dataset.notificationId : null;
            if (notificationBanner) notificationBanner.classList.add('d-none');
            fnMarkNotificationAsRead(notificationId);
        });
    }

    window.addEventListener('beforeunload', function() {
        eventSource.close();
        fnStopIncomingAudioRingtone();
        incomingAudioSystemNotifications.forEach(notification => notification.close());
        incomingAudioSystemNotifications.clear();
    });

    fnRestoreIncomingAudioCall().catch(() => {});
});

async function fnHandleAudioCallRealtime(notification) {
    const details = fnParseAudioCallUrl(notification.url);
    if (!details) return;

    if (notification.readStatus === 'READ') {
        fnCloseIncomingAudioCall(details.callId);
        return;
    }

    const activeSignal = await fnFetchIncomingAudioCall(details.roomId);
    if (!activeSignal || activeSignal.callId !== details.callId) {
        fnCloseIncomingAudioCall(details.callId);
        fnMarkNotificationAsRead(notification.id);
        return;
    }

    fnShowIncomingAudioCall({
        callId: details.callId,
        roomId: details.roomId,
        url: details.url,
        notificationId: notification.id,
        callerName: activeSignal.senderName || activeSignal.senderEmail || '상대방'
    });
}

async function fnRestoreIncomingAudioCall() {
    const signal = await fnFetchIncomingAudioCall(null);
    if (!signal || !signal.callId || !signal.roomId) return;

    fnShowIncomingAudioCall({
        callId: signal.callId,
        roomId: signal.roomId,
        url: `/chat/chatRoom?roomId=${encodeURIComponent(signal.roomId)}&callId=${encodeURIComponent(signal.callId)}`,
        notificationId: null,
        callerName: signal.senderName || signal.senderEmail || '상대방'
    });
}

async function fnFetchIncomingAudioCall(roomId) {
    const query = roomId ? `?roomId=${encodeURIComponent(roomId)}` : '';
    const response = await fetch(`/api/chat/audio/incoming${query}`, {
        method: 'GET',
        credentials: 'include'
    });
    if (response.status === 204 || !response.ok) return null;
    return response.json();
}

function fnShowIncomingAudioCall(call) {
    if (!call || !call.callId) return;
    if (activeIncomingAudioCall && activeIncomingAudioCall.callId !== call.callId) {
        fnCloseIncomingAudioCall(activeIncomingAudioCall.callId);
    }

    activeIncomingAudioCall = call;
    const overlay = fnEnsureIncomingAudioCallOverlay();
    overlay.querySelector('#incoming-audio-call-name').textContent = call.callerName;
    overlay.classList.remove('is-hidden');
    document.body.classList.add('incoming-call-open');
    fnStartIncomingAudioRingtone();
    fnShowIncomingAudioSystemNotification(call);

    if (incomingAudioCallTimeout) window.clearTimeout(incomingAudioCallTimeout);
    incomingAudioCallTimeout = window.setTimeout(() => {
        if (!activeIncomingAudioCall || activeIncomingAudioCall.callId !== call.callId) return;
        fnMarkNotificationAsRead(activeIncomingAudioCall.notificationId);
        fnCloseIncomingAudioCall(call.callId);
    }, 32000);
}

function fnCloseIncomingAudioCall(callId) {
    if (activeIncomingAudioCall && callId && activeIncomingAudioCall.callId !== callId) return;
    const resolvedCallId = callId || activeIncomingAudioCall?.callId;
    if (incomingAudioCallTimeout) window.clearTimeout(incomingAudioCallTimeout);
    incomingAudioCallTimeout = null;
    fnStopIncomingAudioRingtone();

    const overlay = document.getElementById('incoming-audio-call-overlay');
    if (overlay) overlay.classList.add('is-hidden');
    document.body.classList.remove('incoming-call-open');
    if (resolvedCallId) {
        incomingAudioSystemNotifications.get(resolvedCallId)?.close();
        incomingAudioSystemNotifications.delete(resolvedCallId);
    }
    activeIncomingAudioCall = null;
}

function fnEnsureIncomingAudioCallOverlay() {
    const existing = document.getElementById('incoming-audio-call-overlay');
    if (existing) return existing;

    const overlay = document.createElement('div');
    overlay.id = 'incoming-audio-call-overlay';
    overlay.className = 'incoming-audio-call-overlay is-hidden';
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-modal', 'true');
    overlay.setAttribute('aria-labelledby', 'incoming-audio-call-name');
    overlay.innerHTML = `
        <section class="incoming-audio-call-card">
            <span class="incoming-audio-call-eyebrow">INCOMING AUDIO CALL</span>
            <div class="incoming-audio-call-pulse" aria-hidden="true">
                <svg viewBox="0 0 24 24" role="img">
                    <path d="M6.6 10.8c1.7 3.3 3.3 4.9 6.6 6.6l2.2-2.2c.3-.3.7-.4 1.1-.2 1.2.4 2.5.7 3.8.7.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.7 21 3 13.3 3 3.7c0-.6.4-1 1-1h3.4c.6 0 1 .4 1 1 0 1.3.2 2.6.7 3.8.1.4 0 .8-.2 1.1l-2.3 2.2z"/>
                </svg>
            </div>
            <h2 id="incoming-audio-call-name">상대방</h2>
            <p>음성 통화가 왔습니다</p>
            <div class="incoming-audio-call-actions">
                <button type="button" id="incoming-audio-call-reject" class="incoming-call-action reject" aria-label="통화 거절">
                    <span class="incoming-call-action-icon">✕</span><span>거절</span>
                </button>
                <button type="button" id="incoming-audio-call-accept" class="incoming-call-action accept" aria-label="통화 받기">
                    <span class="incoming-call-action-icon">✓</span><span>받기</span>
                </button>
            </div>
        </section>`;
    overlay.querySelector('#incoming-audio-call-reject').addEventListener('click', fnRejectIncomingAudioCall);
    overlay.querySelector('#incoming-audio-call-accept').addEventListener('click', fnAcceptIncomingAudioCall);
    document.body.appendChild(overlay);
    return overlay;
}

async function fnRejectIncomingAudioCall() {
    const call = activeIncomingAudioCall;
    if (!call) return;
    fnCloseIncomingAudioCall(call.callId);
    fnMarkNotificationAsRead(call.notificationId);
    try {
        await fetch(`/api/chat/audio/${encodeURIComponent(call.callId)}/reject`, {
            method: 'POST',
            credentials: 'include'
        });
    } catch (error) {
        console.error('통화 거절 오류:', error);
    }
}

function fnAcceptIncomingAudioCall() {
    const call = activeIncomingAudioCall;
    if (!call) return;
    fnCloseIncomingAudioCall(call.callId);
    fnMarkNotificationAsRead(call.notificationId);
    const target = new URL(call.url, window.location.origin);
    target.searchParams.set('acceptAudioCall', call.callId);
    window.location.href = target.pathname + target.search;
}

function fnParseAudioCallUrl(value) {
    if (!value) return null;
    try {
        const target = new URL(value, window.location.origin);
        if (target.origin !== window.location.origin || target.pathname !== '/chat/chatRoom') return null;
        const roomId = target.searchParams.get('roomId');
        const callId = target.searchParams.get('callId');
        if (!roomId || !callId) return null;
        return {roomId, callId, url: target.pathname + target.search};
    } catch (error) {
        return null;
    }
}

function fnShowIncomingAudioSystemNotification(call) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    const notification = new Notification('수신 음성 통화', {
        body: `${call.callerName}님에게서 음성 통화가 왔습니다.`,
        tag: `audio-call-${call.callId}`,
        renotify: true,
        requireInteraction: true
    });
    incomingAudioSystemNotifications.set(call.callId, notification);
    notification.onclick = function() {
        window.focus();
        notification.close();
        fnAcceptIncomingAudioCall();
    };
}

function fnStartIncomingAudioRingtone() {
    fnStopIncomingAudioRingtone();
    if (navigator.vibrate) navigator.vibrate([400, 200, 400, 800]);
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    try {
        incomingAudioRingtoneContext = new AudioContextClass();
        const playTone = () => {
            if (!incomingAudioRingtoneContext) return;
            const oscillator = incomingAudioRingtoneContext.createOscillator();
            const gain = incomingAudioRingtoneContext.createGain();
            oscillator.frequency.value = 520;
            gain.gain.setValueAtTime(0.0001, incomingAudioRingtoneContext.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.1, incomingAudioRingtoneContext.currentTime + 0.03);
            gain.gain.exponentialRampToValueAtTime(0.0001, incomingAudioRingtoneContext.currentTime + 0.65);
            oscillator.connect(gain).connect(incomingAudioRingtoneContext.destination);
            oscillator.start();
            oscillator.stop(incomingAudioRingtoneContext.currentTime + 0.7);
        };
        incomingAudioRingtoneContext.resume().then(playTone).catch(() => {});
        incomingAudioRingtoneInterval = window.setInterval(playTone, 1900);
    } catch (error) {
        incomingAudioRingtoneContext = null;
    }
}

function fnStopIncomingAudioRingtone() {
    if (incomingAudioRingtoneInterval) window.clearInterval(incomingAudioRingtoneInterval);
    incomingAudioRingtoneInterval = null;
    if (incomingAudioRingtoneContext) incomingAudioRingtoneContext.close().catch(() => {});
    incomingAudioRingtoneContext = null;
    if (navigator.vibrate) navigator.vibrate(0);
}

function fnEnsureNotificationBanner() {
    const existing = document.getElementById('notification-banner');
    if (existing) return existing;

    const banner = document.createElement('div');
    banner.id = 'notification-banner';
    banner.className = 'alert alert-info d-none position-fixed top-0 end-0 m-3';
    banner.setAttribute('role', 'alert');
    banner.innerHTML = '<span id="notification-message">새 알림</span>'
        + '<button type="button" class="btn-close" aria-label="Close">닫기</button>';
    banner.querySelector('.btn-close').addEventListener('click', event => {
        event.stopPropagation();
        banner.classList.add('d-none');
    });
    document.body.appendChild(banner);
    return banner;
}

function fnRequestAudioCallNotificationPermission() {
    if (!('Notification' in window)) {
        alert('이 브라우저는 시스템 알림을 지원하지 않습니다.');
        return;
    }
    Notification.requestPermission().then(permission => {
        alert(permission === 'granted'
            ? '음성 통화 알림이 허용되었습니다.'
            : '브라우저 설정에서 알림 권한을 허용해주세요.');
    });
}

function fnInitNavigationSettings() {
    const settings = document.querySelector('.nav-settings');
    const toggle = document.getElementById('navSettingsToggle');
    const menu = document.getElementById('navSettingsMenu');

    if (!settings || !toggle || !menu) return;

    toggle.addEventListener('click', function(event) {
        event.stopPropagation();
        const isOpen = settings.classList.toggle('open');
        toggle.setAttribute('aria-expanded', String(isOpen));
        toggle.setAttribute('aria-label', isOpen ? '개인 설정 닫기' : '개인 설정 열기');
        menu.setAttribute('aria-hidden', String(!isOpen));
    });

    menu.addEventListener('click', function(event) {
        event.stopPropagation();
    });

    document.addEventListener('click', function() {
        fnCloseNavigationSettings();
    });

    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            fnCloseNavigationSettings();
            toggle.focus();
        }
    });
}

function fnCloseNavigationSettings() {
    const settings = document.querySelector('.nav-settings');
    const toggle = document.getElementById('navSettingsToggle');
    const menu = document.getElementById('navSettingsMenu');

    if (!settings || !toggle || !menu) return;

    settings.classList.remove('open');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.setAttribute('aria-label', '개인 설정 열기');
    menu.setAttribute('aria-hidden', 'true');
}

function fnInitThemeSelector() {
    const savedTheme = fnGetStoredTheme() === 'dark' ? 'dark' : 'light';
    fnApplyTheme(savedTheme);

    document.querySelectorAll('.themeChoice[data-theme]').forEach(function(themeChoice) {
        themeChoice.addEventListener('click', function() {
            fnApplyTheme(themeChoice.dataset.theme);
        });
    });
}

function fnGetStoredTheme() {
    try {
        return localStorage.getItem('theme');
    } catch (error) {
        return null;
    }
}

function fnSetStoredTheme(theme) {
    try {
        localStorage.setItem('theme', theme);
    } catch (error) {
        console.error('테마 저장 오류:', error);
    }
}

function fnApplyTheme(theme) {
    const selectedTheme = theme === 'dark' ? 'dark' : 'light';
    document.body.classList.toggle('dark-mode', selectedTheme === 'dark');
    fnSetStoredTheme(selectedTheme);

    document.querySelectorAll('.themeChoice[data-theme]').forEach(function(themeChoice) {
        const isSelected = themeChoice.dataset.theme === selectedTheme;
        themeChoice.classList.toggle('active', isSelected);
        themeChoice.setAttribute('aria-pressed', String(isSelected));
    });
}

function fnSetUnreadNotificationDot(unreadCount) {
    const notificationNavIcon = document.getElementById('notification-nav-icon');
    if (!notificationNavIcon) return;

    const count = Number(unreadCount);
    const hasUnread = count > 0;
    const notificationCountBadge = document.getElementById('notification-unread-count');

    notificationNavIcon.classList.toggle('active', hasUnread);
    notificationNavIcon.setAttribute('aria-label', hasUnread ? `미확인 알림 ${count}개` : '미확인 알림 없음');

    if (!notificationCountBadge) return;

    notificationCountBadge.textContent = hasUnread ? (count > 99 ? '99+' : String(count)) : '';
    notificationCountBadge.setAttribute('aria-hidden', hasUnread ? 'false' : 'true');
}

function fnUpdateUnreadNotificationDot() {
    fetch('/api/notification/count/unread', {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] <= 0) return;
        fnSetUnreadNotificationDot(json['count']);
    })
    .catch((error) => {
        console.error('읽지 않은 알림 수 조회 오류:', error);
    });
}

function fnMarkNotificationAsRead(id) {
    if (!id) return Promise.resolve(false);

    return fetch(`/api/notification/mark-as-read?id=` + encodeURIComponent(id), {
        method: 'PATCH',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        const isUpdated = json['result'] > 0;
        if (isUpdated) fnUpdateUnreadNotificationDot();
        return isUpdated;
    })
    .catch((error) => {
        console.error('알림 읽음 처리 오류:', error);
        return false;
    });
}

function fnMoveNotificationAfterRead(id, group, url) {
    fnMarkNotificationAsRead(id).finally(() => {
        fnNotificationMove(group, url);
    });
}

function fnMain() {
    location.replace(`/board/main`);
}

function fnSearch() {
    location.replace(`/member/search`);
}

function fnWrite() {
    location.replace(`/board/write`);
}

function fnChatting() {
    location.replace(`/chat/chatList`);
}

function fnNotification() {
    location.replace(`/notification/list`);
}

function fnSetMemberVisibility(visibility) {
    fetch('/api/member/visibility', {
        method: 'PATCH',
        headers: {
            'Content-Type': 'application/json; charset=utf-8',
        },
        body: JSON.stringify({ visibility: visibility }),
        credentials: 'include',
    })
    .then((response) => response.json())
    .then((json) => {
        if (json.result < 0) {
            alert(json.message || '공개 설정 저장에 실패했습니다.');
            return;
        }

        alert(visibility === 'PRIVATE' ? '프로필이 비공개로 설정되었습니다.' : '프로필이 공개로 설정되었습니다.');
        fnCloseNavigationSettings();
    })
    .catch(() => {
        alert('공개 설정 저장에 실패했습니다.');
    });
}

function fnNavigationLogout() {
    fetch('/api/member/logout', {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json; charset=utf-8',
        },
        credentials: 'include',
    })
    .then((response) => response.json())
    .then((json) => {
        if (json.result > 0) {
            location.replace('/member/login');
            return;
        }

        alert('다시 시도하여주십시오');
    })
    .catch(() => {
        alert('로그아웃에 실패했습니다.');
    });
}

function fnMyReports() {
    location.replace(`/report/my`);
}

function fnReportApply() {
    location.replace(`/admin/report`);
}

function fnDetail(email) {
    location.replace(`/member/detail?email=` + email);
}

function fnForbidden() {
    location.replace(`/forbidden/list`);
}

function fnNotificationMove(group, url) {
    if (group == "CHAT")
        location.href = `/chat/chatRoom?roomId=` + url;
    else if (group == "CALL") {
        const call = fnParseAudioCallUrl(url);
        if (call) location.href = call.url;
    }
    else if (group == "COMMENT" || group == "REPLY")
        location.replace(`/comment?id=` + url);
    else if (group == "FAVORITE")
        location.replace(`/board/view?id=` + url);
    else if (group == "TRAN")
        location.replace(`/account/transactions?account=` + encodeURIComponent(url));
    else if (group == "ADMIN" && typeof url === 'string'
            && (url.startsWith('/admin/report') || url.startsWith('/admin/appeal')))
        location.replace(url);
}
