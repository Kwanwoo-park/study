(() => {
    'use strict';
    const get = name => document.getElementById(`chat-details-${name}`);
    const overlay = get('overlay');
    const panel = get('panel');
    const opener = get('open');
    if (!overlay || !panel || !opener) return;

    const room = document.getElementById('room').value;
    const api = `/api/chat/rooms/${encodeURIComponent(room)}`;
    const members = get('members');
    const gallery = get('images');
    const membersStatus = get('members-status');
    const imagesStatus = get('images-status');
    const more = get('more');
    const update = get('update');
    let opened = false;
    let generation = 0;
    let hideTimer = null;
    let membersRequest = null;
    let imagesRequest = null;
    let photos = [];
    let nextCursor = null;
    let galleryPreview = false;
    const excludedMessages = new Set();

    function textElement(tag, text, className) {
        const element = document.createElement(tag);
        element.textContent = text;
        if (className) element.className = className;
        return element;
    }

    function safeImageUrl(value) {
        if (!value) return null;
        try {
            const url = new URL(value, window.location.origin);
            return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : null;
        } catch (_) { return null; }
    }

    function status(element, message, error = false) {
        element.textContent = message;
        element.classList.toggle('error', error);
    }

    async function request(path, signal) {
        const response = await fetch(api + path, {credentials: 'include', cache: 'no-store', signal});
        const body = await response.json().catch(() => null);
        if (!response.ok) {
            const error = new Error(response.status === 401 ? '로그인이 필요합니다. 다시 로그인해 주세요.'
                : response.status === 403 ? '참여 중인 채팅방만 조회할 수 있습니다.'
                : body?.message || '불러오지 못했습니다. 새로고침으로 다시 시도해 주세요.');
            error.denied = [401, 403, 404].includes(response.status);
            throw error;
        }
        if (!body) throw new Error('서버 응답을 확인할 수 없습니다. 다시 시도해 주세요.');
        return body;
    }

    function closePreview() {
        if (galleryPreview && typeof window.closeChatImageModal === 'function') window.closeChatImageModal();
        galleryPreview = false;
    }

    function accessDenied(error) {
        if (!error.denied) return;
        closePreview();
        generation++;
        membersRequest?.abort();
        imagesRequest?.abort();
        membersRequest = null;
        imagesRequest = null;
        photos = [];
        nextCursor = null;
        members.replaceChildren();
        gallery.replaceChildren();
        get('member-count').textContent = '';
        more.hidden = true;
        status(membersStatus, error.message, true);
        status(imagesStatus, error.message, true);
    }

    async function loadMembers() {
        membersRequest?.abort();
        const controller = new AbortController();
        membersRequest = controller;
        const version = generation;
        status(membersStatus, '참여자를 불러오는 중입니다…');
        try {
            const data = await request('/details', controller.signal);
            if (!opened || version !== generation || controller !== membersRequest) return;
            if (!Array.isArray(data.participants)) throw new Error('참여자 정보를 확인할 수 없습니다.');
            members.replaceChildren();
            get('room-name').textContent = data.name || '채팅방';
            get('member-count').textContent = `(${data.participants.length}명)`;
            for (const member of data.participants) {
                const row = document.createElement('li');
                const link = document.createElement('a');
                link.className = 'chat-details-member';
                link.href = member.me
                    ? `/member/detail?email=${encodeURIComponent(member.email)}`
                    : `/member/search/detail?email=${encodeURIComponent(member.email)}&source=chat`;
                const avatar = textElement('span', (member.name || '?').slice(0, 1), 'chat-details-avatar');
                const source = safeImageUrl(member.profile);
                if (source) {
                    const image = document.createElement('img');
                    image.src = source;
                    image.alt = '';
                    image.loading = 'lazy';
                    image.addEventListener('error', () => image.remove());
                    avatar.append(image);
                }
                link.append(avatar, textElement('span', member.name || '알 수 없는 회원', 'chat-details-member-name'));
                if (member.me) link.append(textElement('span', '나', 'chat-details-me'));
                row.append(link);
                members.append(row);
            }
            status(membersStatus, data.participants.length ? '' : '참여자가 없습니다.');
        } catch (error) {
            if (!opened || version !== generation || controller !== membersRequest || error.name === 'AbortError') return;
            accessDenied(error);
            status(membersStatus, error.message, true);
        } finally {
            if (membersRequest === controller) membersRequest = null;
        }
    }

    function renderPhotos() {
        gallery.replaceChildren();
        for (const photo of photos) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'chat-details-photo';
            const label = `${photo.senderName || '알 수 없는 회원'} · ${(photo.sentAt || '').replace('T', ' ')}`;
            button.title = label;
            button.setAttribute('aria-label', `${label} 사진 확대`);
            const image = document.createElement('img');
            image.src = photo.imgSrc;
            image.alt = '채팅방에서 주고받은 사진';
            image.loading = 'lazy';
            image.decoding = 'async';
            image.addEventListener('error', () => {
                button.textContent = '사진을 불러올 수 없습니다';
                button.disabled = true;
            });
            button.append(image);
            button.addEventListener('click', () => {
                const index = photos.findIndex(item => item.id === photo.id);
                if (index < 0 || typeof window.openChatImageModal !== 'function') return;
                galleryPreview = true;
                window.openChatImageModal(photos.map(item => item.imgSrc), index);
            });
            gallery.append(button);
        }
        more.hidden = !nextCursor;
        status(imagesStatus, photos.length ? `${photos.length}장 불러옴` : '주고받은 사진이 없습니다.');
    }

    async function loadPhotos(reset = false) {
        if (imagesRequest && !reset) return;
        if (reset) {
            imagesRequest?.abort();
            photos = [];
            nextCursor = null;
            gallery.replaceChildren();
            more.hidden = true;
        }
        const controller = new AbortController();
        imagesRequest = controller;
        const version = generation;
        status(imagesStatus, '사진을 불러오는 중입니다…');
        more.disabled = true;
        const requestedCursor = nextCursor;
        try {
            const data = await request(`/images${requestedCursor ? `?cursor=${encodeURIComponent(requestedCursor)}` : ''}`, controller.signal);
            if (!opened || version !== generation || controller !== imagesRequest) return;
            if (!Array.isArray(data.images)) throw new Error('사진 정보를 확인할 수 없습니다.');
            const known = new Set(photos.map(photo => photo.id));
            for (const photo of data.images) {
                const source = safeImageUrl(photo.imgSrc);
                if (source && !known.has(photo.id) && !excludedMessages.has(photo.messageId)) {
                    photos.push({...photo, imgSrc: source});
                    known.add(photo.id);
                }
            }
            nextCursor = data.nextCursor && data.nextCursor !== requestedCursor ? data.nextCursor : null;
            renderPhotos();
        } catch (error) {
            if (!opened || version !== generation || controller !== imagesRequest || error.name === 'AbortError') return;
            accessDenied(error);
            status(imagesStatus, error.message, true);
        } finally {
            if (imagesRequest === controller) {
                imagesRequest = null;
                more.disabled = false;
            }
        }
    }

    function refresh() {
        if (!opened) return;
        closePreview();
        update.hidden = true;
        loadMembers();
        loadPhotos(true);
    }

    function open() {
        if (opened) return;
        clearTimeout(hideTimer);
        opened = true;
        generation++;
        overlay.hidden = false;
        overlay.inert = false;
        panel.getBoundingClientRect();
        overlay.classList.add('is-open');
        document.body.classList.add('chat-details-open');
        opener.setAttribute('aria-expanded', 'true');
        get('close').focus();
        refresh();
    }

    function close() {
        if (!opened) return;
        opened = false;
        generation++;
        closePreview();
        membersRequest?.abort();
        imagesRequest?.abort();
        membersRequest = null;
        imagesRequest = null;
        overlay.classList.remove('is-open');
        overlay.inert = true;
        document.body.classList.remove('chat-details-open');
        opener.setAttribute('aria-expanded', 'false');
        opener.focus();
        hideTimer = setTimeout(() => { overlay.hidden = true; }, 210);
        members.replaceChildren();
        gallery.replaceChildren();
        photos = [];
        nextCursor = null;
    }

    function imagePreviewOpen() {
        const modal = document.getElementById('chatImageModal');
        return modal && !modal.classList.contains('is-hidden');
    }

    opener.addEventListener('click', open);
    get('close').addEventListener('click', close);
    get('backdrop').addEventListener('click', close);
    get('refresh').addEventListener('click', refresh);
    more.addEventListener('click', () => loadPhotos());
    document.addEventListener('keydown', event => {
        if (!opened || event.defaultPrevented || imagePreviewOpen()) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            close();
        } else if (event.key === 'Tab') {
            const focusable = Array.from(panel.querySelectorAll('button:not(:disabled), a[href]')).filter(item => !item.hidden);
            const first = focusable[0];
            const last = focusable[focusable.length - 1];
            if (event.shiftKey && (document.activeElement === first || document.activeElement === panel)) {
                event.preventDefault();
                last?.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first?.focus();
            }
        }
    });
    document.addEventListener('focusin', event => {
        if (opened && !imagePreviewOpen() && !panel.contains(event.target)) get('close').focus();
    });
    window.addEventListener('chat:image-preview-closed', () => {
        galleryPreview = false;
        if (opened && !panel.contains(document.activeElement)) get('close').focus();
    });
    window.addEventListener('chat:room-details-change', event => {
        const change = event.detail || {};
        if (change.roomId && change.roomId !== room) return;
        if (change.action === 'DELETED_FOR_ALL' || change.action === 'DELETED_FOR_ME') {
            excludedMessages.add(change.id);
            if (opened) {
                closePreview();
                photos = photos.filter(photo => !excludedMessages.has(photo.messageId));
                renderPhotos();
            }
        } else if (opened && ['IMAGE', 'ENTER', 'QUIT'].includes(change.type)) {
            update.textContent = '채팅방에 변경 사항이 있습니다. 새로고침하면 최신 내역을 볼 수 있습니다.';
            update.hidden = false;
            if (change.type !== 'IMAGE') loadMembers();
        }
    });
    window.addEventListener('pagehide', close);
})();
