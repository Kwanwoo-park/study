(() => {
    'use strict';

    const api = '/api/admin/memos';
    const byId = id => document.getElementById(`memo-${id}`);
    const form = byId('form');
    const title = byId('title');
    const content = byId('content');
    const colors = [...document.querySelectorAll('.memo-color')];
    let active = null;
    let baseline = '';
    let busy = false;
    let page = 0;
    let totalPages = 0;
    let query = '';
    let pinnedOnly = false;
    let listRequest = 0;
    let searchTimer;
    let allowLeave = false;

    function values() {
        return {title: title.value, content: content.value, color: active.color, pinned: active.pinned, version: active.version};
    }

    function fingerprint() {
        if (!active) return '';
        const draft = values();
        return JSON.stringify([draft.title, draft.content, draft.color, draft.pinned]);
    }

    function dirty() {
        return active !== null && fingerprint() !== baseline;
    }

    function status(message, error = false) {
        byId('status').textContent = message;
        byId('status').classList.toggle('is-error', error);
    }

    function date(value) {
        if (!value) return '';
        const parsed = new Date(value);
        return Number.isNaN(parsed.getTime()) ? '' : new Intl.DateTimeFormat('ko-KR', {
            month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit', hour12: false,
        }).format(parsed);
    }

    function icon(name) {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        svg.setAttribute('aria-hidden', 'true');
        use.setAttribute('href', `#memo-icon-${name}`);
        svg.append(use);
        return svg;
    }

    function updateControls() {
        const changed = dirty();
        byId('save').disabled = busy || !active || !changed || !(title.value.trim() || content.value.trim());
        byId('save-label').textContent = busy ? '처리 중…' : '메모 저장';
        byId('delete').disabled = busy || !active?.id;
        byId('pin').disabled = busy;
        byId('pin').setAttribute('aria-pressed', String(Boolean(active?.pinned)));
        byId('pin-label').textContent = active?.pinned ? '고정됨' : '상단 고정';
        byId('new').disabled = busy;
        byId('welcome-new').disabled = busy;
        title.disabled = busy;
        content.disabled = busy;
        for (const color of colors) {
            color.disabled = busy;
            color.setAttribute('aria-pressed', String(color.dataset.color === active?.color));
        }
        byId('edit-dot').classList.toggle('is-dirty', changed);
        byId('save-state').textContent = busy ? '처리하고 있어요…' : changed ? '저장하지 않은 변경사항' : active?.id ? '모든 변경사항 저장됨' : '새 메모';
        byId('character-count').textContent = `${content.value.length.toLocaleString('ko-KR')} / 20,000자`;
    }

    function select(memo) {
        active = {...memo};
        title.value = memo.title || '';
        content.value = memo.content || '';
        baseline = fingerprint();
        byId('welcome').hidden = true;
        form.hidden = false;
        byId('workspace').classList.add('is-editing');
        byId('app').classList.add('is-editing');
        byId('reload').hidden = true;
        byId('editor-meta').textContent = memo.id
            ? `작성 ${date(memo.createdAt)} · 최근 저장 ${date(memo.updatedAt || memo.createdAt)}`
            : '아직 저장하지 않은 메모입니다.';
        markSelection();
        updateControls();
    }

    function markSelection() {
        for (const card of document.querySelectorAll('.memo-card')) {
            const selected = card.dataset.id === String(active?.id);
            card.classList.toggle('is-selected', selected);
            card.setAttribute('aria-pressed', String(selected));
        }
    }

    function clearEditor() {
        active = null;
        baseline = '';
        form.hidden = true;
        byId('welcome').hidden = false;
        byId('workspace').classList.remove('is-editing');
        byId('app').classList.remove('is-editing');
        markSelection();
        updateControls();
    }

    function canDiscard() {
        if (busy) return false;
        return !dirty() || window.confirm('저장하지 않은 변경사항이 있어요. 저장하지 않고 이동할까요?');
    }

    function createDraft() {
        if (!canDiscard()) return;
        select({id: null, title: '', content: '', color: 'LAVENDER', pinned: false, version: null});
        title.focus();
        status('새로운 페이지가 열렸어요. 작성 후 저장을 눌러주세요.');
    }

    async function request(path = '', options = {}) {
        const headers = {Accept: 'application/json', ...options.headers};
        if (options.method && options.method !== 'GET') {
            const csrf = await request('/csrf');
            headers[csrf.headerName] = csrf.token;
        }
        const response = await fetch(api + path, {credentials: 'same-origin', cache: 'no-store', ...options, headers});
        if (response.status === 204) return null;
        const body = await response.json().catch(() => null);
        if (!response.ok || !body) {
            const message = response.status === 401 ? '로그인이 만료되었습니다. 관리자 계정으로 다시 로그인해 주세요.'
                : response.status === 403 ? '관리자 권한 또는 요청 인증을 확인할 수 없습니다. 페이지를 새로고침해 주세요.'
                : body?.message || '요청을 처리하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.';
            const error = new Error(message);
            error.status = response.status;
            throw error;
        }
        return body;
    }

    function renderList(notes) {
        const list = byId('list');
        list.replaceChildren();
        for (const memo of notes) {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'memo-card';
            card.dataset.id = String(memo.id);
            card.dataset.color = ['LAVENDER', 'MINT', 'AMBER', 'ROSE'].includes(memo.color) ? memo.color : 'LAVENDER';
            const heading = document.createElement('h3');
            heading.textContent = memo.title || '제목 없는 메모';
            const preview = document.createElement('p');
            preview.textContent = memo.preview?.trim() || '내용이 없는 메모입니다.';
            const meta = document.createElement('div');
            meta.className = 'memo-card-meta';
            const time = document.createElement('span');
            time.textContent = date(memo.updatedAt || memo.createdAt);
            meta.append(time);
            if (memo.pinned) {
                const pin = icon('pin');
                pin.setAttribute('aria-label', '고정된 메모');
                pin.removeAttribute('aria-hidden');
                meta.append(pin);
            }
            card.append(heading, preview, meta);
            card.addEventListener('click', () => openMemo(memo.id));
            list.append(card);
        }
        if (!notes.length) {
            const empty = document.createElement('p');
            empty.className = 'memo-list-empty';
            empty.textContent = query ? '검색 결과가 없어요.\n다른 검색어로 찾아보세요.'
                : pinnedOnly ? '고정한 메모가 아직 없어요.\n중요한 메모의 핀 버튼을 눌러보세요.'
                : '아직 메모가 없어요.\n상단의 + 새 메모로 시작해 보세요.';
            list.append(empty);
        }
        markSelection();
    }

    async function loadList(targetPage = page) {
        const sequence = ++listRequest;
        byId('list').setAttribute('aria-busy', 'true');
        byId('prev').disabled = true;
        byId('next').disabled = true;
        const parameters = new URLSearchParams({query, pinnedOnly: String(pinnedOnly), page: String(targetPage)});
        try {
            const data = await request(`?${parameters}`);
            if (sequence !== listRequest) return false;
            if (!data.content.length && targetPage > 0) return loadList(Math.max(0, Math.min(targetPage - 1, data.totalPages - 1)));
            page = data.page;
            totalPages = data.totalPages;
            byId('total').textContent = data.totalCount.toLocaleString('ko-KR');
            byId('pinned-total').textContent = data.pinnedCount.toLocaleString('ko-KR');
            byId('results').textContent = `${data.matchedCount.toLocaleString('ko-KR')}개의 메모`;
            byId('page-number').textContent = `${page + 1} / ${Math.max(totalPages, 1)}`;
            byId('prev').disabled = page === 0;
            byId('next').disabled = page + 1 >= totalPages;
            renderList(data.content);
            return true;
        } catch (error) {
            if (sequence !== listRequest) return false;
            status(error.message, true);
            byId('results').textContent = '불러오기 실패';
            return false;
        } finally {
            if (sequence === listRequest) byId('list').setAttribute('aria-busy', 'false');
        }
    }

    async function openMemo(id, reload = false) {
        if (active?.id === id && !reload || !canDiscard()) return;
        busy = true;
        updateControls();
        try {
            const memo = await request(`/${encodeURIComponent(id)}`);
            select(memo);
            status('메모를 열었어요. 변경사항은 저장 버튼으로 보관해 주세요.');
        } catch (error) {
            status(error.message, true);
        } finally {
            busy = false;
            updateControls();
        }
    }

    async function saveMemo(event) {
        event?.preventDefault();
        if (busy || !active || !dirty() || !(title.value.trim() || content.value.trim())) return;
        if (title.value.length > 120 || content.value.length > 20000) {
            status('제목은 120자, 내용은 20,000자 이내로 작성해 주세요.', true);
            return;
        }
        const isNew = !active.id;
        const payload = values();
        payload.title = payload.title.trim() || '제목 없는 메모';
        busy = true;
        updateControls();
        try {
            const memo = await request(isNew ? '' : `/${encodeURIComponent(active.id)}`, {
                method: isNew ? 'POST' : 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload),
            });
            select(memo);
            const listed = await loadList(isNew ? 0 : page);
            status(listed ? '메모를 안전하게 저장했어요.' : '메모는 저장됐지만 목록을 불러오지 못했어요. 목록을 새로고침해 주세요.', !listed);
        } catch (error) {
            status(error.message, true);
            if (error.status === 409) byId('reload').hidden = false;
        } finally {
            busy = false;
            updateControls();
        }
    }

    async function deleteMemo() {
        if (busy || !active?.id) return;
        if (!window.confirm('이 메모를 영구 삭제할까요? 저장하지 않은 변경사항도 사라지며 복구할 수 없습니다.')) return;
        busy = true;
        updateControls();
        try {
            await request(`/${encodeURIComponent(active.id)}?version=${encodeURIComponent(active.version)}`, {method: 'DELETE'});
            clearEditor();
            const listed = await loadList();
            status(listed ? '메모를 삭제했어요.' : '메모는 삭제됐지만 목록을 불러오지 못했어요. 목록을 새로고침해 주세요.', !listed);
            byId('new').focus();
        } catch (error) {
            status(error.message, true);
            if (error.status === 409) byId('reload').hidden = false;
        } finally {
            busy = false;
            updateControls();
        }
    }

    function filter(pinned) {
        pinnedOnly = pinned;
        byId('filter-all').classList.toggle('is-active', !pinned);
        byId('filter-pinned').classList.toggle('is-active', pinned);
        byId('filter-all').setAttribute('aria-pressed', String(!pinned));
        byId('filter-pinned').setAttribute('aria-pressed', String(pinned));
        loadList(0);
    }

    byId('new').addEventListener('click', createDraft);
    byId('welcome-new').addEventListener('click', createDraft);
    form.addEventListener('submit', saveMemo);
    title.addEventListener('input', updateControls);
    content.addEventListener('input', updateControls);
    byId('pin').addEventListener('click', () => {
        if (!active || busy) return;
        active.pinned = !active.pinned;
        updateControls();
    });
    for (const color of colors) color.addEventListener('click', () => {
        if (!active || busy) return;
        active.color = color.dataset.color;
        updateControls();
    });
    byId('delete').addEventListener('click', deleteMemo);
    byId('reload').addEventListener('click', () => active?.id && openMemo(active.id, true));
    byId('editor-back').addEventListener('click', () => {
        if (!canDiscard()) return;
        clearEditor();
        byId('new').focus();
    });
    byId('filter-all').addEventListener('click', () => filter(false));
    byId('filter-pinned').addEventListener('click', () => filter(true));
    byId('refresh').addEventListener('click', async () => {
        if (await loadList()) status('메모 목록을 새로 불러왔어요. 편집 중인 내용은 그대로 유지됩니다.');
    });
    byId('prev').addEventListener('click', () => page > 0 && loadList(page - 1));
    byId('next').addEventListener('click', () => page + 1 < totalPages && loadList(page + 1));
    byId('search').addEventListener('input', () => {
        query = byId('search').value.trim();
        // Invalidate in-flight responses immediately, not only after the debounce.
        listRequest++;
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => loadList(0), 250);
    });
    byId('theme').setAttribute('aria-pressed', String(document.body.classList.contains('dark-mode')));
    byId('theme').addEventListener('click', () => {
        const dark = document.body.classList.toggle('dark-mode');
        byId('theme').setAttribute('aria-pressed', String(dark));
        try { localStorage.setItem('theme', dark ? 'dark' : 'light'); } catch (error) { /* Theme still works without storage. */ }
    });
    document.addEventListener('keydown', event => {
        if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 's') {
            event.preventDefault();
            saveMemo();
        }
    });
    document.addEventListener('click', event => {
        if (!event.target.closest('a[data-page-back]')) return;
        if (!canDiscard()) {
            event.preventDefault();
            event.stopImmediatePropagation();
        } else allowLeave = true;
    }, true);
    window.addEventListener('beforeunload', event => {
        if (!allowLeave && (busy || dirty())) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
    loadList(0);
})();
