document.addEventListener('DOMContentLoaded', function() {
    const eventSource = new EventSource('/api/notification/stream');

    eventSource.addEventListener('notification', function(event) {
        try {
            const notificationMessage = JSON.parse(event.data)['message'];
            console.log(JSON.parse(event.data))

            if (notificationMessage) {
                const notificationElement = document.getElementById('notification-message');
                notificationElement.textContent = notificationMessage;

                const notificationBanner = document.getElementById('notification-banner');
                notificationBanner.classList.remove('d-none');

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

    document.getElementById('notification-banner')
            .querySelector('.btn-close')
            .addEventListener('click', function() {
                const notificationBanner = document.getElementById('notification-banner');
                notificationBanner.classList.add('d-none');
            });

    window.addEventListener('beforeunload', function() {
        eventSource.close();
    });
});

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

function fnDetail(email) {
    location.replace(`/member/detail?email=` + email);
}

function fnForbidden() {
    location.replace(`/forbidden/list`);
}