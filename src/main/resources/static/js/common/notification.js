document.addEventListener('DOMContentLoaded', function() {
    const eventSource = new EventSource('/api/notification/stream');
    const notificationDot = document.querySelector('.notification-dot');

    eventSource.addEventListener('notification', function(event) {
        try {
            const notificationMessage = JSON.parse(event.data)['message'];

            const notificationElement = document.getElementById('notification-message');
            notificationElement.textContent = notificationMessage;

            const notificationBanner = document.getElementById('notification-banner');
            notificationBanner.classList.remove('d-none');

            if (notificationDot) {
                notificationDot.classList.add('active');
            }

            setTimeout(() => {
                notificationBanner.classList.add('d-none');
            }, 5000);
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
        console.error('상태:', event.target.readyState);
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