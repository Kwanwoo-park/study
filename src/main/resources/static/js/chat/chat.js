const roomId = document.querySelector("#room").value;
const email = document.querySelector("#email").value;
const isAdmin = document.querySelector("#isAdmin").value === 'true';
const flag = document.querySelector("#flag").value;

const upload = document.getElementById("upload");
const btn = document.getElementById("btn");
const newMessageNotice = document.getElementById("new-message-notice");
const chatSendStatus = document.getElementById('chat-send-status');
const chatSendRetry = document.getElementById('chat-send-retry');

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

const container = document.querySelector(".container");
let nextCursor = 1;
let presenceRefreshInterval = null;
let chatImageModalSources = [];
let chatImageModalIndex = 0;
let chatImageModalReturnFocus = null;
let lastKnownReadAt = '';
let lastReadMarkAt = 0;
let client = null;
let reconnectTimer = null;
let reconnectAttempt = 0;
let pageLeaving = false;
let isSendingImages = false;
let hasConnected = false;
let latestMessage = null;
let recoveringMessages = false;
let checkingText = false;
let pendingText = null;
let receiptTimer = null;

btn.addEventListener('click', () => upload.click());
chatSendRetry.addEventListener('click', sendPendingText);
newMessageNotice.addEventListener('click', () => {
    scrollToBottom();
    hideNewMessageNotice();
    markCurrentRoomRead();
});

document.addEventListener('click', closeChatActionMenus);

initChatImageModal();

window.onload = function() {
    loadMoreChat().then(() => {
        activateChatPresence();
        presenceRefreshInterval = setInterval(activateChatPresence, PRESENCE_REFRESH_INTERVAL_MS);
    });
}

window.addEventListener('pagehide', () => {
    pageLeaving = true;
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
    clearTimeout(receiptTimer);
    deactivateChatPresence();
});

window.addEventListener('pageshow', event => {
    if (!event.persisted) return;
    pageLeaving = false;
    if (!client || !client.connected) connectChatSocket();
    else recoverMessages(latestMessage?.id);
    if (pendingText) checkTextReceipt(pendingText);
    activateChatPresence();
    presenceRefreshInterval = setInterval(activateChatPresence, PRESENCE_REFRESH_INTERVAL_MS);
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

connectChatSocket();

function connectChatSocket() {
    if (pageLeaving || reconnectTimer) return;
    const socket = new SockJS('/ws/chat');
    client = Stomp.over(socket);
    client.debug = function() {};
    client.heartbeat.outgoing = 10000;
    client.heartbeat.incoming = 10000;
    client.connect({}, onConnected, onError);
}

function onConnected() {
    const reconnecting = hasConnected;
    const lastSeenId = latestMessage?.id;
    hasConnected = true;
    reconnectAttempt = 0;
    reconnectTimer = null;
    client.subscribe("/sub/chat/room/" + roomId, onMessageReceived);

    if (window.audioCallClient) {
        window.audioCallClient.onStompConnected(client);
    }

    if (flag == 'true')
        enterRoom();
    if (reconnecting) recoverMessages(lastSeenId);
}

function onError(error) {
    console.error(error);
    if (window.audioCallClient) {
        window.audioCallClient.onStompDisconnected();
    }
    scheduleChatReconnect();
}

function scheduleChatReconnect() {
    if (pageLeaving || reconnectTimer) return;
    const delay = Math.min(30000, 1000 * Math.pow(2, reconnectAttempt));
    reconnectAttempt += 1;
    reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null;
        connectChatSocket();
    }, delay);
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
    clearInterval(presenceRefreshInterval);

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

function onMessageReceived(e) {
    const json = JSON.parse(e.body);

    if (json.action === 'POLL_UPDATED') {
        window.dispatchEvent(new Event('chat:polls-updated'));
        return;
    }
    if (json.action) {
        applyChatMessageEvent(json);
        return;
    }

    fnDraw(json)
    window.dispatchEvent(new CustomEvent('chat:room-details-change', {detail: {roomId, type: json.type}}));
}

async function loadMoreChat() {
    try {
        const res = await fetch(`/api/chat/load?roomId=${encodeURIComponent(roomId)}&cursor=0&limit=${CHAT_LIMIT}`, {
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
            credentials: "include",
        });

        const data = await res.json();
        const height = container.scrollHeight;

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

async function recoverMessages(after) {
    if (recoveringMessages) return;
    if (!after) {
        await loadMoreChat();
        return;
    }
    recoveringMessages = true;
    try {
        const messages = document.querySelector('.list-group-flush');
        const stickToBottom = isScrolledToBottom();
        do {
            const url = `/api/chat/missed?roomId=${encodeURIComponent(roomId)}&after=${encodeURIComponent(after)}`;
            const response = await fetch(url, {credentials: 'include'});
            if (!response.ok) throw new Error('누락된 메시지를 불러오지 못했습니다.');
            const data = await response.json();
            for (const item of data.message) {
                if (registerMessageForRender(item)) insertChatMessage(messages, item, data.img[item.id]);
            }
            after = data.hasMore ? data.message.at(-1)?.id : null;
        } while (after && !pageLeaving);
        refreshDateSeparators();
        if (stickToBottom) {
            scrollToBottom();
            markCurrentRoomRead();
        }
    } catch (error) {
        console.error('채팅 복구 오류:', error);
        if (!pendingText) setSendStatus('최근 메시지를 확인하지 못했어요. 새로고침해 주세요.');
    } finally {
        recoveringMessages = false;
    }
}

function enterRoom() {
    const enterMsg = {
        type : "ENTER",
        roomId : roomId,
        email : email
    };

    msgSend(enterMsg);
}

function send(e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
        e.preventDefault();

        sendMsg();
    }
}

function quit() {
    const quitMsg = {
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

async function sendMsg() {
    if (checkingText || pendingText) return;
    const input = document.getElementById('chat');
    const content = input.value;
    const replyToId = window.ChatQuotes?.current();
    if (!content.trim()) return;
    if (!client?.connected) {
        setSendStatus('연결된 후 다시 전송해 주세요.');
        return;
    }

    checkingText = true;
    try {
        const response = await fetch('/api/chat/message/check', {
            method: 'POST', credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({message: content})
        });
        const data = await response.json();
        if (data.result === -3) {
            alert('금칙어를 사용하여 계정이 정지되었습니다');
            window.location.reload();
            return;
        }
        const censored = data.result === -1 || data.result === -2;
        if (!censored && (!response.ok || data.result !== 1)) {
            throw new Error(data.message || '메시지를 보낼 수 없습니다.');
        }
        pendingText = {id: crypto.randomUUID(), type: 'TALK', roomId, message: censored ? CENSORED_MESSAGE : content};
        if (replyToId) {
            pendingText.replyToId = replyToId;
            window.ChatQuotes.clear(replyToId);
        }
        if (input.value === content) input.value = '';
        sendPendingText();
    } catch (error) {
        setSendStatus(error.message || '메시지를 보내지 못했습니다.');
    } finally {
        checkingText = false;
    }
}

function sendPendingText() {
    if (!pendingText) return;
    if (!client?.connected) {
        setSendStatus('연결이 끊겼어요. 연결된 후 다시 시도해 주세요.', true);
        return;
    }
    try {
        const message = pendingText;
        setSendStatus('전송 중…');
        msgSend(message);
        clearTimeout(receiptTimer);
        receiptTimer = setTimeout(() => checkTextReceipt(message), 8000);
    } catch (error) {
        setSendStatus('전송에 실패했어요. 다시 시도해 주세요.', true);
    }
}

async function checkTextReceipt(message) {
    try {
        const response = await fetch(`/api/chat/message/${encodeURIComponent(message.id)}/receipt?roomId=${encodeURIComponent(roomId)}`,
            {credentials: 'include'});
        if (response.ok) {
            fnDraw(await response.json());
            return;
        }
    } catch (error) {
        console.error('메시지 저장 확인 오류:', error);
    }
    if (pendingText === message) setSendStatus('저장 여부를 확인하지 못했어요. 다시 시도해 주세요.', true);
}

function setSendStatus(message, retry = false) {
    chatSendStatus.textContent = message;
    chatSendRetry.hidden = !retry;
}

function fnDraw(data) {
    if (!registerMessageForRender(data)) return;

    const msgArea = document.querySelector('.list-group-flush');
    const mine = isMyMessage(data);
    const shouldStickToBottom = isScrolledToBottom();

    insertChatMessage(msgArea, data, data.list);
    refreshDateSeparators();
    if (data.status === 'DELETED_FOR_ALL') return;

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

function insertChatMessage(list, data, images) {
    const row = createChatMessage(data, images);
    const rows = list.querySelectorAll('.chat-message-row');
    if (!rows.length || !comesAfter(rows[rows.length - 1], row)) {
        list.append(row);
        return;
    }
    list.insertBefore(row, Array.from(rows).find(item => comesAfter(item, row)));
}

async function jumpToChatReference(id) {
    try {
        const response = await fetch(`/api/chat/message/${encodeURIComponent(id)}/reference?roomId=${encodeURIComponent(roomId)}`,
            {credentials: 'include', cache: 'no-store'});
        if (response.status === 404 || response.status === 403) {
            window.ChatQuotes.invalidate(id);
            return;
        }
        if (!response.ok) throw new Error('메시지를 불러오지 못했습니다.');
        const {message, img} = await response.json();
        if (!document.getElementById('chat-message-' + id)) {
            const list = document.querySelector('.list-group-flush');
            renderedMessageIds.add(id);
            insertChatMessage(list, message, img[id]);
            refreshDateSeparators();
        }
        const row = document.getElementById('chat-message-' + id);
        row.tabIndex = -1;
        row.focus({preventScroll: true});
        row.scrollIntoView({block: 'center'});
    } catch (error) {
        alert(error.message);
    }
}

function comesAfter(item, earlier) {
    return item.dataset.messageOrder > earlier.dataset.messageOrder
        || (item.dataset.messageOrder === earlier.dataset.messageOrder && item.dataset.messageId > earlier.dataset.messageId);
}

function fnLoadDraw(json) {
    const msgArea = document.querySelector('.list-group-flush');
    json.message.forEach(data => {
        if (!registerMessageForRender(data)) return;
        msgArea.prepend(createChatMessage(data, json.img[data.id]));
    });

    refreshDateSeparators();
}

function createChatMessage(data, images) {
    const row = document.createElement('li');
    const area = document.createElement('span');
    applyMessageDirection(row, area, data);
    if (data.status === 'DELETED_FOR_ALL') {
        renderDeletedMessage(row, area, data);
    } else {
        const profile = document.createElement('img');
        profile.src = data.member.profile;
        profile.className = 'profile';
        const name = document.createElement('span');
        name.innerText = data.member.name;
        appendMessageHeader(area, profile, name, data);
        if (data.replyToId && window.ChatQuotes) area.append(window.ChatQuotes.quote(data.replyToId, jumpToChatReference));
        appendChatMessageMain(area, data, normalizeChatImageSources(images));
        appendEditedIndicator(area, data);
        appendMessageTime(area, data);
    }
    row.append(area);
    return row;
}

const renderedMessageIds = new Set();

function registerMessageForRender(data) {
    if (!data || !data.id) return true;
    if (pendingText?.id === data.id && isMyMessage(data)) {
        clearTimeout(receiptTimer);
        pendingText = null;
        setSendStatus('');
    }
    if (renderedMessageIds.has(data.id)) return false;

    renderedMessageIds.add(data.id);
    const time = data.registerTime;
    if (!latestMessage || time > latestMessage.time || (time === latestMessage.time && data.id > latestMessage.id)) {
        latestMessage = {id: data.id, time};
    }
    return true;
}

function applyMessageDirection(messageLi, messageArea, data) {
    const directionClass = isMyMessage(data) ? 'mine' : 'other';

    messageLi.className = "list-group-item chat-message-row " + directionClass;
    messageLi.dataset.messageDate = getMessageDateKey(data);
    messageLi.dataset.messageTime = getMessageDate(data).toISOString();
    messageLi.dataset.messageOrder = data.registerTime;
    messageLi.dataset.mine = String(isMyMessage(data));
    messageLi.dataset.messageId = data.id || '';
    messageLi.dataset.messageType = data.type || '';
    messageLi.dataset.messageAuthor = data.member?.name || '';
    messageLi.id = data.id ? 'chat-message-' + data.id : '';
    messageArea.className = "chat-message-content";
}

function isMyMessage(data) {
    const senderEmail = data.member?.email || data.email;

    return senderEmail == email;
}

function scrollToBottom() {
    container.scrollTop = container.scrollHeight;
}

function isScrolledToBottom() {
    return container.scrollTop + container.clientHeight >= container.scrollHeight - 8;
}

function showNewMessageNotice(data) {
    const senderName = data.member.name || '알 수 없음';
    const message = data.message || '새 메시지가 도착했습니다';

    newMessageNotice.innerText = `${senderName}: ${message}`;
    newMessageNotice.classList.remove('is-hidden');
}

function hideNewMessageNotice() {
    newMessageNotice.classList.add('is-hidden');
}

function initChatImageModal() {
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
            event.preventDefault();
            event.stopImmediatePropagation();
            closeChatImageModal();
        }

        if (event.key === 'Tab' && !modal.classList.contains('is-hidden')) {
            const controls = Array.from(modal.querySelectorAll('button:not(:disabled):not(.is-hidden)'));
            const first = controls[0];
            const last = controls[controls.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
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

    if (normalizedSources.length === 0) return;

    chatImageModalReturnFocus = document.activeElement;
    chatImageModalSources = normalizedSources;
    chatImageModalIndex = Math.min(Math.max(initialIndex || 0, 0), chatImageModalSources.length - 1);
    preloadAdjacentImages(chatImageModalSources, chatImageModalIndex);
    renderChatImageModal();
    modal.classList.remove('is-hidden');
    document.body.classList.add('chat-image-modal-open');
    modal.querySelector('.chat-image-modal-close').focus();
}

function closeChatImageModal() {
    const modal = document.getElementById('chatImageModal');
    const image = document.getElementById('chatImageModalImg');

    modal.classList.add('is-hidden');
    image.removeAttribute('src');
    chatImageModalSources = [];
    chatImageModalIndex = 0;
    document.body.classList.remove('chat-image-modal-open');
    if (chatImageModalReturnFocus?.isConnected) chatImageModalReturnFocus.focus();
    chatImageModalReturnFocus = null;
    window.dispatchEvent(new Event('chat:image-preview-closed'));
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

    image.src = chatImageModalSources[chatImageModalIndex];
    preloadAdjacentImages(chatImageModalSources, chatImageModalIndex);

    previousButton.classList.toggle('is-hidden', chatImageModalSources.length <= 1);
    previousButton.disabled = chatImageModalIndex === 0;
    nextButton.classList.toggle('is-hidden', chatImageModalSources.length <= 1);
    nextButton.disabled = chatImageModalIndex === chatImageModalSources.length - 1;
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

function appendMessageHeader(messageArea, profile, name, data) {
    const header = document.createElement('span');

    header.className = "chat-message-header";
    if (!isMyMessage(data) && data.member && data.member.email) {
        const memberLink = document.createElement('a');

        memberLink.className = 'chat-message-member-link';
        memberLink.href = `/member/search/detail?email=${encodeURIComponent(data.member.email)}&source=chat`;
        memberLink.setAttribute('aria-label', `${data.member.name || '상대방'} 회원 상세 보기`);
        memberLink.append(profile, name);
        header.append(memberLink);
    } else {
        header.append(profile, name);
    }
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
    if (window.ChatQuotes) menu.append(createMessageActionButton('답장', () => window.ChatQuotes.select(data.id)));

    if (canEditMessage(data)) {
        menu.append(createMessageActionButton('수정', () => editChatMessage(data.id)));
    }

    if (!isMyMessage(data)) {
        menu.append(createMessageActionButton('신고', () => {
            window.location.href = `/report?targetType=CHAT_MESSAGE&targetId=${encodeURIComponent(data.id)}`;
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
            window.ChatQuotes?.invalidate(messageId);
            removeChatMessageRow(messageId);
            window.dispatchEvent(new CustomEvent('chat:room-details-change', {detail: {roomId, id: messageId, action: 'DELETED_FOR_ME'}}));
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
        window.ChatQuotes?.invalidate(event.id);
        window.dispatchEvent(new CustomEvent('chat:room-details-change', {detail: event}));
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
    window.ChatQuotes?.refresh();

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
    deletedText.innerText = data.deletedByAdmin
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
    window.ChatQuotes?.refresh();
    const msgArea = document.querySelector('.list-group-flush');
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

async function fnLoad(input) {
    if (isSendingImages) return;
    const selectedFiles = Array.from(input.files);
    if (selectedFiles.length === 0) return;

    if (selectedFiles.length > maxSize) {
        alert('최대 ' + maxSize + "장의 사진만 업로드가 가능합니다");
    }

    isSendingImages = true;
    input.disabled = true;
    btn.disabled = true;
    try {
        const files = await ImageUpload.snapshotFiles(selectedFiles.slice(0, maxSize));
        input.value = '';
        const response = await fetch(`/api/chat/sendImage`, {
            method: 'POST',
            body: ImageUpload.buildFormData(files),
            credentials: "include",
        });
        const json = await response.json();
        if (response.ok && json['result'] > 0) {
            const talkMsg = {
                id: json['messageId'],
                type : "IMAGE",
                roomId : roomId,
                email : email,
                message: "사진을 보냈습니다",
                list: json['list']
            };
            msgSend(talkMsg)
        }
        else {
            alert(json.message || "이미지 전송 실패");
        }
    } catch (error) {
        alert(error.message || "이미지를 전송하지 못했습니다. 다시 시도하여 주십시오.");
    } finally {
        isSendingImages = false;
        input.value = '';
        input.disabled = false;
        btn.disabled = false;
    }
}
