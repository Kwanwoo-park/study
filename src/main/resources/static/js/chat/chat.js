const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const isAdmin = document.querySelector("#isAdmin").value === 'true';
const flag = document.querySelector("#flag").value;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const newMessageNotice = document.getElementById("new-message-notice");

const maxSize = 10;
const CHAT_LIMIT = 10;
const DELETE_FOR_ALL_LIMIT_MS = 30 * 60 * 1000;
const EDIT_MESSAGE_LIMIT_MS = 24 * 60 * 60 * 1000;
const CENSORED_MESSAGE = '<부적절한 내용이 포함되어 검열되었습니다>';
const PRESENCE_REFRESH_INTERVAL_MS = 60 * 1000;
const messageTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
});
const messageDateFormatter = new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'short',
});

let container = document.querySelector(".container");
let nextCursor = 1;
let height = 0;
let presenceRefreshInterval = null;
let chatImageModalSources = [];
let chatImageModalIndex = 0;
let lastKnownReadAt = '';
let lastReadMarkAt = 0;

ignoreWebSocketLogs();

if (btn)
    btn.addEventListener('click', () => upload.click());

if (newMessageNotice) {
    newMessageNotice.addEventListener('click', () => {
        scrollToBottom();
        hideNewMessageNotice();
        markCurrentRoomRead();
    });
}

document.addEventListener('click', closeChatActionMenus);

initChatImageModal();

window.onload = function() {
    loadMoreChat().then(() => {
        activateChatPresence();
        presenceRefreshInterval = setInterval(activateChatPresence, PRESENCE_REFRESH_INTERVAL_MS);
    });
}

window.addEventListener('pagehide', () => {
    deactivateChatPresence();
});

container.addEventListener('scroll', () => {
    if (container.scrollTop == 0) {
        loadPreviousChat();
    }

    if (isScrolledToBottom()) {
        hideNewMessageNotice();
        markCurrentRoomRead();
    }
})

let socket = new SockJS("/ws/chat")

const client = Stomp.over(socket)
client.debug = function() {};

client.connect({}, onConnected, onError);

function onConnected() {
    client.subscribe("/sub/chat/room/" + roomId, onMessageReceived);

    if (flag == 'true')
        enterRoom();
}

function onError(error) {
    console.error(error);
}

function activateChatPresence() {
    fetch(`/api/chat/presence/active?roomId=${encodeURIComponent(roomId)}`, {
        method: 'POST',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    }).catch((error) => {
        console.error('채팅방 접속 상태 갱신 오류:', error);
    });
}

function deactivateChatPresence() {
    if (presenceRefreshInterval) {
        clearInterval(presenceRefreshInterval);
    }

    const url = `/api/chat/presence/inactive?roomId=${encodeURIComponent(roomId)}`;
    if (navigator.sendBeacon) {
        navigator.sendBeacon(url);
        return;
    }

    fetch(url, {
        method: 'POST',
        credentials: "include",
        keepalive: true,
    }).catch((error) => {
        console.error('채팅방 접속 해제 오류:', error);
    });
}

function ignoreWebSocketLogs() {
    const webSocketLogPatterns = [
        'Opening Web Socket',
        'Web Socket Opened',
        'Web Socket Closed',
        'STOMP: connected',
        'Whoops! Lost connection',
        '>>>',
        '<<<'
    ];

    ['log', 'debug', 'info'].forEach(method => {
        const originalConsoleMethod = console[method];

        console[method] = function() {
            const message = Array.from(arguments).join(' ');

            if (webSocketLogPatterns.some(pattern => message.indexOf(pattern) !== -1)) {
                return;
            }

            originalConsoleMethod.apply(console, arguments);
        };
    });
}

function onMessageReceived(e) {
    const json = JSON.parse(e.body);

    if (json.action) {
        applyChatMessageEvent(json);
        return;
    }

    fnDraw(json)
}

async function loadMoreChat() {
    try {
        const res = await fetch(`/api/chat/load?roomId=${encodeURIComponent(roomId)}&cursor=${nextCursor - 1}&limit=${CHAT_LIMIT}`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });
        const data = await res.json();

        if (data['result'] > 0) {
            lastKnownReadAt = data.lastReadAt || '';
            fnLoadDraw(data)

            scrollToBottom();
            nextCursor = data.nextCursor;
        }
        else
            alert('다시 시도하여주십시오')
    } catch (e) {
        console.error('로드 오류', e);
    }
}

async function loadPreviousChat() {
    if (!nextCursor) return;

    try {
        const res = await fetch(`/api/chat/previous/load?roomId=${encodeURIComponent(roomId)}&cursor=${nextCursor - 1}&limit=${CHAT_LIMIT}`, {
            method: 'GET',
            headers: {
                "Content-Type": "application/json; charset=utf-8",
            },
            credentials: "include",
        });

        const data = await res.json();
        height = container.scrollHeight;

        if (data['result'] > 0) {
            fnLoadDraw(data)

            container.scrollTop = container.scrollHeight - height;
            nextCursor = data.nextCursor;
        }
        else
            alert('다시 시도하여주십시오')
    } catch (e) {
        console.error('로드 오류', e);
    }
}

function enterRoom() {
    var enterMsg = {
        type : "ENTER",
        roomId : roomId,
        email : email
    };

    msgSend(enterMsg);
}

 function send(e) {
    if (e.keyCode == 13 && !e.shiftKey) {
        event.preventDefault();

        sendMsg();
    }
}

function quit() {
    var quitMsg = {
        type : "QUIT",
        roomId : roomId,
        email : email
    };

    msgSend(quitMsg)
    history.back(-1);
}

function msgSend(msg) {
    client.send("/api/chat/message/send", {}, JSON.stringify(msg));
}

function sendMsg() {
    let content = document.getElementById('chat').value;

    fetch(`/api/chat/message/check?message=`+content, {
        method: 'GET',
        headers: {
            "Content-Type": "application/json; charset=utf-8",
        },
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (content != '') {
            document.getElementById('chat').value = null;
        }

        if (json['result'] == -1 || json['result'] == -2)
            content = "<부적절한 내용이 포함되어 검열되었습니다>";
        else if (json['result'] == -3) {
            alert("금칙어를 사용하여 계정이 정지되었습니다");
            window.location.reload();
            return ;
        }
        else if (json['result'] == -10) {
            alert("다시 시도하여주십시오");
        }

        let talkMsg = {
            type : "TALK",
            roomId : roomId,
            email : email,
            message : content
        };

        msgSend(talkMsg);
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })

    return 0;
}

function fnDraw(data) {
    if (!registerMessageForRender(data)) return;

    let msgArea = document.querySelector('.list-group-flush');
    const mine = isMyMessage(data);
    const shouldStickToBottom = isScrolledToBottom();

    let newMsgLi = document.createElement('li');
    let newMsgArea = document.createElement('span');
    let name = document.createElement('span');
    let profile = document.createElement('img');

    applyMessageDirection(newMsgLi, newMsgArea, data);

    if (data.status === 'DELETED_FOR_ALL') {
        renderDeletedMessage(newMsgLi, newMsgArea, data);
        newMsgLi.append(newMsgArea);
        msgArea.append(newMsgLi);
        refreshDateSeparators();
        return;
    }

    profile.src = data.member.profile;
    profile.className = "profile";

    name.innerText = data.member.name;

    appendMessageHeader(newMsgArea, profile, name);

    appendChatMessageMain(newMsgArea, data, normalizeChatImageSources(data.list));
    appendEditedIndicator(newMsgArea, data);
    appendMessageTime(newMsgArea, data);
    newMsgLi.append(newMsgArea);

    msgArea.append(newMsgLi);
    refreshDateSeparators();

    if (mine) {
        scrollToBottom();
        markCurrentRoomRead();
    } else if (shouldStickToBottom) {
        scrollToBottom();
        hideNewMessageNotice();
        markCurrentRoomRead();
    } else {
        showNewMessageNotice(data);
    }
}

function fnLoadDraw(json) {
    json.message.forEach(data => {
        if (!registerMessageForRender(data)) return;

        let msgArea = document.querySelector('.list-group-flush');

        let newMsgLi = document.createElement('li');
        let newMsgArea = document.createElement('span');
        let name = document.createElement('span');
        let profile = document.createElement('img');

        applyMessageDirection(newMsgLi, newMsgArea, data);

        if (data.status === 'DELETED_FOR_ALL') {
            renderDeletedMessage(newMsgLi, newMsgArea, data);
            newMsgLi.append(newMsgArea);
            msgArea.prepend(newMsgLi);
            return;
        }

        profile.src = data.member.profile;
        profile.className = "profile";

        name.innerText = data.member.name;

        appendMessageHeader(newMsgArea, profile, name);

        appendChatMessageMain(newMsgArea, data, normalizeChatImageSources(json.img[data.id]));
        appendEditedIndicator(newMsgArea, data);
        appendMessageTime(newMsgArea, data);
        newMsgLi.append(newMsgArea);

        msgArea.prepend(newMsgLi);
    });

    refreshDateSeparators();
}

const renderedMessageIds = new Set();

function registerMessageForRender(data) {
    if (!data || !data.id) return true;
    if (renderedMessageIds.has(data.id)) return false;

    renderedMessageIds.add(data.id);
    return true;
}

function applyMessageDirection(messageLi, messageArea, data) {
    const directionClass = isMyMessage(data) ? 'mine' : 'other';

    messageLi.className = "list-group-item chat-message-row " + directionClass;
    messageLi.dataset.messageDate = getMessageDateKey(data);
    messageLi.dataset.messageTime = getMessageDate(data).toISOString();
    messageLi.dataset.mine = String(isMyMessage(data));
    messageLi.dataset.messageId = data.id || '';
    messageLi.dataset.messageType = data.type || '';
    messageLi.id = data.id ? 'chat-message-' + data.id : '';
    messageArea.className = "chat-message-content";
}

function isMyMessage(data) {
    const senderEmail = data.member && data.member.email ? data.member.email : data.email;

    return senderEmail == email;
}

function scrollToBottom() {
    container.scrollTop = container.scrollHeight;
}

function isScrolledToBottom() {
    return container.scrollTop + container.clientHeight >= container.scrollHeight - 8;
}

function showNewMessageNotice(data) {
    if (!newMessageNotice) return;

    const senderName = data && data.member && data.member.name ? data.member.name : '알 수 없음';
    const message = data && data.message ? data.message : '새 메시지가 도착했습니다';

    newMessageNotice.innerText = `${senderName}: ${message}`;
    newMessageNotice.classList.remove('is-hidden');
}

function hideNewMessageNotice() {
    if (!newMessageNotice) return;

    newMessageNotice.classList.add('is-hidden');
}

function initChatImageModal() {
    if (document.getElementById('chatImageModal')) return;

    const modal = document.createElement('div');
    const dialog = document.createElement('div');
    const closeButton = document.createElement('button');
    const image = document.createElement('img');
    const previousButton = document.createElement('button');
    const nextButton = document.createElement('button');

    modal.id = 'chatImageModal';
    modal.className = 'chat-image-modal is-hidden';
    modal.setAttribute('role', 'dialog');
    modal.setAttribute('aria-modal', 'true');

    dialog.className = 'chat-image-modal-dialog';

    closeButton.type = 'button';
    closeButton.className = 'chat-image-modal-close';
    closeButton.setAttribute('aria-label', 'close image preview');
    closeButton.innerText = 'x';

    image.id = 'chatImageModalImg';
    image.className = 'chat-image-modal-img';
    image.alt = 'chat image preview';

    previousButton.type = 'button';
    previousButton.id = 'chatImageModalPrev';
    previousButton.className = 'chat-image-modal-nav chat-image-modal-prev';
    previousButton.setAttribute('aria-label', 'previous image');
    previousButton.innerText = '←';

    nextButton.type = 'button';
    nextButton.id = 'chatImageModalNext';
    nextButton.className = 'chat-image-modal-nav chat-image-modal-next';
    nextButton.setAttribute('aria-label', 'next image');
    nextButton.innerText = '→';

    closeButton.addEventListener('click', closeChatImageModal);
    previousButton.addEventListener('click', () => showPreviousChatImage());
    nextButton.addEventListener('click', () => showNextChatImage());
    if (typeof initImageSwipe === 'function') {
        initImageSwipe(image, {
            canPrevious: () => chatImageModalIndex > 0,
            canNext: () => chatImageModalIndex < chatImageModalSources.length - 1,
            getPreviousSource: () => chatImageModalSources[chatImageModalIndex - 1],
            getNextSource: () => chatImageModalSources[chatImageModalIndex + 1],
            onPrevious: () => showPreviousChatImage(true),
            onNext: () => showNextChatImage(true),
        });
    }
    modal.addEventListener('click', (event) => {
        if (event.target === modal) {
            closeChatImageModal();
        }
    });

    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !modal.classList.contains('is-hidden')) {
            closeChatImageModal();
        }

        if (event.key === 'ArrowLeft' && !modal.classList.contains('is-hidden')) {
            showPreviousChatImage();
        }

        if (event.key === 'ArrowRight' && !modal.classList.contains('is-hidden')) {
            showNextChatImage();
        }
    });

    dialog.append(closeButton, previousButton, image, nextButton);
    modal.append(dialog);
    document.body.append(modal);
}

function configureChatImagePreview(image, imageSources, initialIndex) {
    image.tabIndex = 0;
    image.setAttribute('role', 'button');
    image.setAttribute('aria-label', 'open image preview');
    image.addEventListener('click', () => openChatImageModal(imageSources, initialIndex));
    image.addEventListener('keydown', (event) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openChatImageModal(imageSources, initialIndex);
        }
    });
}

function openChatImageModal(sources, initialIndex) {
    const modal = document.getElementById('chatImageModal');
    const normalizedSources = normalizeChatImageSources(sources);

    if (!modal || normalizedSources.length === 0) return;

    chatImageModalSources = normalizedSources;
    chatImageModalIndex = Math.min(Math.max(initialIndex || 0, 0), chatImageModalSources.length - 1);
    preloadAdjacentImages(chatImageModalSources, chatImageModalIndex);
    renderChatImageModal();
    modal.classList.remove('is-hidden');
    document.body.classList.add('chat-image-modal-open');
}

function closeChatImageModal() {
    const modal = document.getElementById('chatImageModal');
    const image = document.getElementById('chatImageModalImg');

    if (!modal || !image) return;

    modal.classList.add('is-hidden');
    image.removeAttribute('src');
    chatImageModalSources = [];
    chatImageModalIndex = 0;
    document.body.classList.remove('chat-image-modal-open');
}

function showPreviousChatImage(skipAnimation) {
    if (chatImageModalSources.length <= 1 || chatImageModalIndex === 0) return;
    const image = document.getElementById('chatImageModalImg');
    if (!skipAnimation && animateImageSwipe(image, 'previous')) return;

    chatImageModalIndex -= 1;
    renderChatImageModal();
}

function showNextChatImage(skipAnimation) {
    if (chatImageModalSources.length <= 1 || chatImageModalIndex >= chatImageModalSources.length - 1) return;
    const image = document.getElementById('chatImageModalImg');
    if (!skipAnimation && animateImageSwipe(image, 'next')) return;

    chatImageModalIndex += 1;
    renderChatImageModal();
}

function renderChatImageModal() {
    const image = document.getElementById('chatImageModalImg');
    const previousButton = document.getElementById('chatImageModalPrev');
    const nextButton = document.getElementById('chatImageModalNext');

    if (!image) return;

    image.src = chatImageModalSources[chatImageModalIndex];
    preloadAdjacentImages(chatImageModalSources, chatImageModalIndex);

    if (previousButton) {
        previousButton.classList.toggle('is-hidden', chatImageModalSources.length <= 1);
        previousButton.disabled = chatImageModalIndex === 0;
    }

    if (nextButton) {
        nextButton.classList.toggle('is-hidden', chatImageModalSources.length <= 1);
        nextButton.disabled = chatImageModalIndex === chatImageModalSources.length - 1;
    }
}

function normalizeChatImageSources(images) {
    if (!Array.isArray(images)) return [];

    return images
        .map(image => typeof image === 'string' ? image : image.imgSrc)
        .filter(src => src);
}

function appendChatImageCount(container, imageSources) {
    if (imageSources.length <= 1) return;

    const count = document.createElement('span');

    count.className = 'chat-image-count';
    count.innerText = `1 / ${imageSources.length}`;
    container.append(count);
}

function appendMessageHeader(messageArea, profile, name) {
    const header = document.createElement('span');

    header.className = "chat-message-header";
    header.append(profile);
    header.append(name);
    messageArea.append(header);
}

function appendChatMessageMain(messageArea, data, imageSources) {
    const main = document.createElement('div');

    main.className = 'chat-message-main';
    appendChatMessageBody(main, data, imageSources);
    appendMessageActions(main, data);
    messageArea.append(main);
}

function appendChatMessageBody(messageMain, data, imageSources) {
    const body = document.createElement('div');
    body.className = 'chat-message-body';

    if (data.type === 'IMAGE' && imageSources.length > 0) {
        const image = document.createElement('img');
        image.src = imageSources[0];
        image.id = 'img' + data.id;
        image.className = 'chatimg';
        configureChatImagePreview(image, imageSources, 0);
        preloadAdjacentImages(imageSources, 0);

        body.append(image);
        appendChatImageCount(body, imageSources);
    } else {
        const text = document.createElement('pre');
        text.className = 'chat-message-text';
        text.innerText = data.message || '';
        body.append(text);
    }

    messageMain.append(body);
}

function appendEditedIndicator(messageArea, data) {
    if (!data.edited) return;

    const indicator = document.createElement('span');
    indicator.className = 'chat-message-edited';
    indicator.innerText = data.editedByAdmin ? '관리자에 의해 수정됨' : '수정됨';
    messageArea.append(indicator);
}

function appendMessageActions(messageArea, data) {
    if (!data || !data.id || (data.type !== 'TALK' && data.type !== 'IMAGE')) return;

    const actions = document.createElement('div');
    const toggle = document.createElement('button');
    const menu = document.createElement('div');

    actions.className = 'chat-message-actions';
    toggle.type = 'button';
    toggle.className = 'chat-message-actions-toggle';
    toggle.setAttribute('aria-label', '메시지 메뉴 열기');
    toggle.setAttribute('aria-expanded', 'false');
    toggle.innerText = '⋮';
    menu.className = 'chat-message-actions-menu';

    if (canEditMessage(data)) {
        menu.append(createMessageActionButton('수정', () => editChatMessage(data.id)));
    }

    if (!isMyMessage(data)) {
        menu.append(createMessageActionButton('신고', () => {
            location.href = `/report?targetType=CHAT_MESSAGE&targetId=${encodeURIComponent(data.id)}`;
        }));
    }

    menu.append(createMessageActionButton('나만 삭제', () => deleteChatMessage(data.id, 'ME')));

    if (isAdmin || (isMyMessage(data) && canDeleteForAll(data))) {
        menu.append(createMessageActionButton(
                '모두에게 삭제',
                () => deleteChatMessage(data.id, 'ALL'),
                'danger'
        ));
    }

    toggle.addEventListener('click', (event) => {
        event.stopPropagation();
        const shouldOpen = !actions.classList.contains('open');
        closeChatActionMenus();
        actions.classList.toggle('open', shouldOpen);
        toggle.setAttribute('aria-expanded', String(shouldOpen));
    });
    menu.addEventListener('click', (event) => event.stopPropagation());

    actions.append(toggle, menu);
    messageArea.append(actions);
}

function canDeleteForAll(data) {
    if (!data || !data.registerTime) return false;

    const sentAt = getMessageDate(data).getTime();
    const elapsed = Date.now() - sentAt;

    return elapsed >= 0 && elapsed < DELETE_FOR_ALL_LIMIT_MS;
}

function canEditMessage(data) {
    if (!data || (!isMyMessage(data) && !isAdmin) || data.type !== 'TALK' || isCensoredMessage(data)
            || !data.registerTime) {
        return false;
    }

    const sentAt = getMessageDate(data).getTime();
    const elapsed = Date.now() - sentAt;
    return elapsed >= 0 && elapsed < EDIT_MESSAGE_LIMIT_MS;
}

function isCensoredMessage(data) {
    return Boolean(data && (data.censored || data.message === CENSORED_MESSAGE));
}

function createMessageActionButton(label, handler, variant) {
    const button = document.createElement('button');
    button.type = 'button';
    button.innerText = label;
    if (variant) button.classList.add(variant);
    button.addEventListener('click', () => {
        closeChatActionMenus();
        handler();
    });
    return button;
}

function closeChatActionMenus() {
    document.querySelectorAll('.chat-message-actions.open').forEach(actions => {
        actions.classList.remove('open');
        const toggle = actions.querySelector('.chat-message-actions-toggle');
        if (toggle) toggle.setAttribute('aria-expanded', 'false');
    });
}

async function editChatMessage(messageId) {
    const row = document.getElementById('chat-message-' + messageId);
    const messageText = row ? row.querySelector('.chat-message-text') : null;
    if (!messageText) return;

    const editedMessage = window.prompt('수정할 메시지를 입력해 주세요.', messageText.innerText);
    if (editedMessage === null) return;
    if (!editedMessage.trim()) {
        alert('메시지를 입력해 주세요.');
        return;
    }

    try {
        const response = await fetch(`/api/chat/message/${encodeURIComponent(messageId)}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json; charset=utf-8',
            },
            body: JSON.stringify({ message: editedMessage.trim() }),
            credentials: 'include',
        });
        const json = await response.json();
        if (!response.ok || !json.event) {
            alert(json.message || '메시지 수정에 실패했습니다.');
            return;
        }
        applyChatMessageEvent(json.event);
    } catch (error) {
        alert('메시지 수정에 실패했습니다.');
    }
}

async function deleteChatMessage(messageId, scope) {
    if (scope === 'ALL' && !isAdmin) {
        const row = document.getElementById('chat-message-' + messageId);
        const sentAt = row && row.dataset.messageTime
            ? new Date(row.dataset.messageTime).getTime()
            : Number.NaN;

        if (!Number.isNaN(sentAt) && Date.now() - sentAt >= DELETE_FOR_ALL_LIMIT_MS) {
            alert('전송 후 30분이 지난 메시지는 모두에게 삭제할 수 없습니다.');
            return;
        }
    }

    const confirmation = scope === 'ALL'
        ? '이 메시지를 모두에게서 삭제할까요?'
        : '이 메시지를 나에게만 보이지 않게 할까요?';
    if (!window.confirm(confirmation)) return;

    try {
        const response = await fetch(
                `/api/chat/message/${encodeURIComponent(messageId)}?scope=${encodeURIComponent(scope)}`,
                {
                    method: 'DELETE',
                    credentials: 'include',
                }
        );
        const json = await response.json();
        if (!response.ok) {
            alert(json.message || '메시지 삭제에 실패했습니다.');
            return;
        }

        if (scope === 'ME') {
            removeChatMessageRow(messageId);
        } else if (json.event) {
            applyChatMessageEvent(json.event);
        }
    } catch (error) {
        alert('메시지 삭제에 실패했습니다.');
    }
}

function applyChatMessageEvent(event) {
    if (!event || !event.id) return;

    if (event.action === 'DELETED_FOR_ALL') {
        const row = document.getElementById('chat-message-' + event.id);
        const messageArea = row ? row.querySelector('.chat-message-content') : null;
        if (row && messageArea) {
            renderDeletedMessage(row, messageArea, event);
            refreshDateSeparators();
        }
        return;
    }

    if (event.action !== 'UPDATED') return;

    const row = document.getElementById('chat-message-' + event.id);
    if (!row) return;

    const messageText = row.querySelector('.chat-message-text');
    if (messageText) messageText.innerText = event.message;

    const messageArea = row.querySelector('.chat-message-content');
    if (messageArea) {
        let indicator = messageArea.querySelector('.chat-message-edited');
        if (!indicator) {
            indicator = document.createElement('span');
            indicator.className = 'chat-message-edited';
            const time = messageArea.querySelector('.chat-message-time');
            messageArea.insertBefore(indicator, time);
        }
        indicator.innerText = event.editedByAdmin ? '관리자에 의해 수정됨' : '수정됨';
    }
}

function renderDeletedMessage(row, messageArea, data) {
    const deletedText = document.createElement('span');

    row.classList.add('deleted');
    deletedText.className = 'chat-message-deleted-text';
    deletedText.innerText = data && data.deletedByAdmin
        ? '관리자에 의해 삭제된 메시지입니다'
        : '삭제된 메시지입니다';
    messageArea.replaceChildren(deletedText);
}

function removeChatMessageRow(messageId) {
    const row = document.getElementById('chat-message-' + messageId);
    if (!row) return;

    row.remove();
    refreshDateSeparators();
}

function appendMessageTime(messageArea, data) {
    const time = document.createElement('span');

    time.className = "chat-message-time";
    time.innerText = formatMessageTime(data);
    messageArea.append(time);
}

function refreshDateSeparators() {
    const msgArea = document.querySelector('.list-group-flush');
    if (!msgArea) return;

    msgArea.querySelectorAll('.chat-date-separator').forEach(separator => separator.remove());

    let previousDate = '';
    msgArea.querySelectorAll('.chat-message-row').forEach(messageRow => {
        const dateKey = messageRow.dataset.messageDate;
        if (!dateKey || dateKey === previousDate) {
            return;
        }

        const separator = document.createElement('li');
        separator.className = "chat-date-separator";
        separator.innerText = formatMessageDate(dateKey);
        msgArea.insertBefore(separator, messageRow);
        previousDate = dateKey;
    });

    refreshReadBoundary();
}

function refreshReadBoundary() {
    const msgArea = document.querySelector('.list-group-flush');
    if (!msgArea) return;

    msgArea.querySelectorAll('.chat-read-boundary').forEach(separator => separator.remove());

    if (!lastKnownReadAt) {
        return;
    }

    const readAt = new Date(lastKnownReadAt);
    if (Number.isNaN(readAt.getTime())) {
        return;
    }

    const firstUnreadMessage = Array.from(msgArea.querySelectorAll('.chat-message-row'))
        .find(messageRow => {
            const messageTime = new Date(messageRow.dataset.messageTime);

            return messageRow.dataset.mine !== 'true'
                && !Number.isNaN(messageTime.getTime())
                && messageTime > readAt;
        });

    if (!firstUnreadMessage) {
        return;
    }

    const separator = document.createElement('li');
    separator.className = "chat-read-boundary";
    separator.innerText = "여기까지 읽었습니다";
    msgArea.insertBefore(separator, firstUnreadMessage);
}

function markCurrentRoomRead() {
    const now = Date.now();

    if (now - lastReadMarkAt < 2000) {
        return;
    }

    lastReadMarkAt = now;
    activateChatPresence();
}

function getMessageDateKey(data) {
    const date = getMessageDate(data);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

function formatMessageTime(data) {
    return messageTimeFormatter.format(getMessageDate(data));
}

function formatMessageDate(dateKey) {
    return messageDateFormatter.format(new Date(`${dateKey}T00:00:00`));
}

function getMessageDate(data) {
    const value = data && data.registerTime ? data.registerTime : new Date().toISOString();
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return new Date();
    }

    return date;
}

function fnLoad(input) {
    let file;
    let imgArr;
    let size;

    let formData = new FormData();

    file = Array.from(input.files);
    size = input.files.length;

    if (size > maxSize) {
        alert('최대 ' + maxSize + "장의 사진만 업로드가 가능합니다")
        size = maxSize;
    }

    for (let i = 0; i < size; i++) {
        formData.append("file", file[i]);
    }

    fetch(`/api/chat/sendImage`, {
        method: 'POST',
        body: formData,
        credentials: "include",
    })
    .then((response) => response.json())
    .then((json) => {
        if (json['result'] > 0) {
            let talkMsg = {
                id: json['messageId'],
                type : "IMAGE",
                roomId : roomId,
                email : email,
                message: "사진을 보냈습니다",
                list: json['list']
            };
            msgSend(talkMsg)
        }
        else
            alert("이미지 전송 실패");
    })
    .catch((error) => {
        alert("다시 시도하여주십시오");
    })
}
