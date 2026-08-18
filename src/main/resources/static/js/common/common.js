(function() {
    const savedTheme = fnGetStoredTheme();
    if (savedTheme === 'dark' && document.body) {
        document.body.classList.add('dark-mode');
    }
})();

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
            const isAudioCall = notificationMessage && notificationMessage.includes('음성 통화를 요청했습니다.');

            if (notificationMessage) {
                fnUpdateUnreadNotificationDot();
                if (typeof fnHandleIncomingNotificationCount === 'function') fnHandleIncomingNotificationCount(json);
                if (isAudioCall) fnAnnounceIncomingAudioCall(notificationMessage, notificationUrl);

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
                }, isAudioCall ? 30000 : 5000);
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
    });
});

function fnAnnounceIncomingAudioCall(message, roomId) {
    if (navigator.vibrate) navigator.vibrate([250, 150, 250, 500, 250]);
    if (!('Notification' in window) || Notification.permission !== 'granted') return;

    const notification = new Notification('음성 통화 요청', {
        body: message,
        tag: `audio-call-${roomId || 'incoming'}`,
        renotify: true
    });
    notification.onclick = function() {
        window.focus();
        notification.close();
        if (roomId) location.href = `/chat/chatRoom?roomId=${encodeURIComponent(roomId)}`;
    };
    setTimeout(() => notification.close(), 30000);
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
    else if (group == "COMMENT" || group == "REPLY")
        location.replace(`/comment?id=` + url);
    else if (group == "FAVORITE")
        location.replace(`/board/view?id=` + url);
    else if (group == "TRAN")
        location.replace(`/account/transactions?account=` + encodeURIComponent(url));
}
