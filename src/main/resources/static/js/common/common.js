(function() {
    const savedTheme = fnGetStoredTheme();
    if (savedTheme === 'dark' && document.body) {
        document.body.classList.add('dark-mode');
    }
})();

document.addEventListener('DOMContentLoaded', function() {
    fnInitThemeToggle();
    fnUpdateUnreadNotificationDot();

    const eventSource = new EventSource('/api/notification/stream');

    eventSource.addEventListener('notification', function(event) {
        try {
            let json = JSON.parse(event.data);
            const notificationId = json['id'];
            const notificationMessage = json['message'];
            const notificationGroup = json['notiGroup'];
            const notificationUrl = json['url'];

            if (notificationMessage) {
                fnUpdateUnreadNotificationDot();
                if (typeof fnHandleIncomingNotificationCount === 'function') fnHandleIncomingNotificationCount(json);

                const notificationElement = document.getElementById('notification-message');
                notificationElement.textContent = notificationMessage;

                const notificationBanner = document.getElementById('notification-banner');
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
    });
});

function fnInitThemeToggle() {
    if (fnGetStoredTheme() === 'dark') {
        document.body.classList.add('dark-mode');
    }

    const themeToggle = document.getElementById('theme-toggle');
    if (!themeToggle) return;

    fnUpdateThemeToggleLabel();
    themeToggle.addEventListener('click', function() {
        const enabledDarkMode = document.body.classList.toggle('dark-mode');
        fnSetStoredTheme(enabledDarkMode ? 'dark' : 'light');
        fnUpdateThemeToggleLabel();
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

function fnUpdateThemeToggleLabel() {
    const themeToggle = document.getElementById('theme-toggle');
    if (!themeToggle) return;

    const enabledDarkMode = document.body.classList.contains('dark-mode');
    const label = enabledDarkMode ? '라이트 모드로 전환' : '다크 모드로 전환';
    themeToggle.setAttribute('aria-label', label);
    themeToggle.setAttribute('title', label);
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

function fnMyReports() {
    location.replace(`/report/my`);
}

function fnReportProcess() {
    location.replace(`/admin/report/process`);
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
