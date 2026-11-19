(() => {
    'use strict';

    const api = '/api/memos';
    const byId = id => document.getElementById(`memo-${id}`);
    const form = byId('form');
    const title = byId('title');
    const content = byId('content');
    const colors = [...document.querySelectorAll('.memo-color')];
    const saveHint = '입력을 멈추면 자동으로 저장돼요. 바로 저장하려면 ‘지금 저장’을 눌러주세요.';
    const conflictHint = '다른 화면에서 메모가 변경됐어요. 작성한 내용을 복사한 뒤 저장본을 다시 열어 주세요.';
    let active = null;
    let baseline = null;
    let busy = false;
    let saving = false;
    let saveMode = '';
    let autosaveTimer;
    let statusTimer;
    let firstDirtyAt = 0;
    let lastInputAt = 0;
    let composing = false;
    let blocked = false;
    let retryAttempt = 0;
    let retryRequest = null;
    let forceSave = false;
    let csrfPromise = null;
    let listedMemos = [];
    let page = 0;
    let totalPages = 0;
    let query = '';
    let pinnedOnly = false;
    let listColor = '';
    let listRequest = 0;
    let searchTimer;
    let allowLeave = false;

    function values() {
        return {title: title.value, content: content.value, color: active.color, pinned: active.pinned, version: active.version, creationKey: active.creationKey};
    }

    function fingerprint(draft) {
        return JSON.stringify([draft.title, draft.content, draft.color, draft.pinned]);
    }

    function dirty() {
        return active !== null && fingerprint(values()) !== fingerprint(baseline);
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

    function saveState(changed) {
        if (busy) return '처리하고 있어요…';
        if (saving && saveMode === 'draft') return '임시 저장 중…';
        if (saving) return '저장 중…';
        if (active?.conflicted || blocked) return '저장 확인 필요 · 내용은 유지됩니다';
        if (changed) return '자동 저장 대기 중';
        if (active?.pending) return '임시 저장됨';
        if (active?.id) return '저장 완료';
        return '새 메모';
    }

    function updateControls() {
        const changed = dirty();
        const locked = busy || saving || Boolean(blocked);
        byId('save').disabled = locked || !active || (!changed && !active.pending);
        byId('save-label').textContent = busy || saving ? '처리 중…' : '지금 저장';
        byId('delete').disabled = busy || saving || !active?.id;
        byId('pin').disabled = locked || Boolean(retryRequest);
        byId('pin').setAttribute('aria-pressed', String(Boolean(active?.pinned)));
        byId('pin-label').textContent = active?.pinned ? '고정됨' : '상단 고정';
        byId('new').disabled = busy || saving;
        byId('welcome-new').disabled = busy || saving;
        title.disabled = busy;
        content.disabled = busy;
        for (const color of colors) {
            color.disabled = locked || Boolean(retryRequest);
            color.setAttribute('aria-pressed', String(color.dataset.color === active?.color));
        }
        byId('edit-dot').classList.toggle('is-dirty', changed);
        byId('save-state').textContent = saveState(changed);
        byId('character-count').textContent = `${content.value.length.toLocaleString('ko-KR')} / 20,000자`;
    }

    function select(memo) {
        clearTimeout(autosaveTimer);
        clearTimeout(statusTimer);
        firstDirtyAt = 0;
        blocked = false;
        retryRequest = null;
        retryAttempt = 0;
        active = {...memo};
        title.value = memo.title;
        content.value = memo.content;
        baseline = values();
        byId('welcome').hidden = true;
        form.hidden = false;
        byId('workspace').classList.add('is-editing');
        byId('app').classList.add('is-editing');
        byId('reload').hidden = true;
        updateEditorMeta(memo);
        markSelection();
        updateControls();
        scheduleStatus();
    }

    function updateEditorMeta(memo) {
        byId('editor-meta').textContent = memo.id
            ? `작성 ${date(memo.createdAt)} · 최근 저장 ${date(memo.updatedAt)}`
            : '아직 저장하지 않은 메모입니다.';
    }

    function markSelection() {
        for (const card of document.querySelectorAll('.memo-card')) {
            const selected = card.dataset.id === String(active?.id);
            card.classList.toggle('is-selected', selected);
            card.setAttribute('aria-pressed', String(selected));
        }
    }

    function clearEditor() {
        clearTimeout(autosaveTimer);
        clearTimeout(statusTimer);
        retryRequest = null;
        active = null;
        baseline = null;
        form.hidden = true;
        byId('welcome').hidden = false;
        byId('workspace').classList.remove('is-editing');
        byId('app').classList.remove('is-editing');
        markSelection();
        updateControls();
    }

    function canDiscard() {
        if (busy || saving) return false;
        return !dirty() || window.confirm('저장하지 않은 변경사항이 있어요. 저장하지 않고 이동할까요?');
    }

    function createDraft() {
        if (!canDiscard()) return;
        select({id: null, title: '', content: '', color: listColor || 'LAVENDER', pinned: false, version: null, creationKey: crypto.randomUUID()});
        title.focus();
        status(saveHint);
    }

    async function request(path = '', options = {}, csrfRetried = false) {
        const headers = {Accept: 'application/json', ...options.headers};
        const mutation = options.method && options.method !== 'GET';
        if (mutation) {
            csrfPromise ||= request('/csrf').catch(error => { csrfPromise = null; throw error; });
            const csrf = await csrfPromise;
            headers[csrf.headerName] = csrf.token;
        }
        const response = await fetch(api + path, {credentials: 'same-origin', cache: 'no-store', ...options, headers});
        if (response.status === 204) return null;
        const body = await response.json().catch(() => null);
        if (response.status === 403 && mutation && body?.code === 'CSRF_INVALID' && !csrfRetried) {
            csrfPromise = null;
            // Reuse the exact URL, body and operationKey; typing during recovery stays in the editor.
            return request(path, options, true);
        }
        if (!response.ok || !body) {
            if (response.status === 403) csrfPromise = null;
            const error = new Error(requestErrorMessage(response.status, body));
            error.status = response.status;
            throw error;
        }
        return body;
    }

    function requestErrorMessage(code, body) {
        if (code === 401) return '로그인이 만료되었습니다. 다시 로그인해 주세요. 작성 내용은 복사해서 보관해 주세요.';
        if (code === 403) {
            if (body?.code === 'FORBIDDEN') return '메모를 사용할 권한이 없습니다. 계정 상태를 확인해 주세요.';
            return '사용 권한 또는 요청 인증을 확인할 수 없습니다. 작성 내용을 보관한 뒤 새로고침해 주세요.';
        }
        return body?.message || '요청을 처리하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.';
    }

    function renderList(notes) {
        notes = notes.map(note => note.id === active?.id && active.version > note.version
            ? {...note, title: active.title, preview: active.content.slice(0, 160), color: active.color, pinned: active.pinned, version: active.version} : note);
        if (listColor) notes = notes.filter(note => note.color === listColor);
        listedMemos = notes;
        const list = byId('list');
        list.replaceChildren();
        for (const memo of notes) {
            const card = document.createElement('button');
            card.type = 'button';
            card.className = 'memo-card';
            card.dataset.id = String(memo.id);
            card.dataset.color = memo.color;
            const heading = document.createElement('h3');
            heading.textContent = memo.title;
            const preview = document.createElement('p');
            preview.textContent = memo.preview.trim() || '내용이 없는 메모입니다.';
            const meta = document.createElement('div');
            meta.className = 'memo-card-meta';
            const time = document.createElement('span');
            time.textContent = date(memo.updatedAt);
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
            empty.textContent = '아직 메모가 없어요.\n상단의 + 새 메모로 시작해 보세요.';
            if (query || pinnedOnly || listColor) empty.textContent = '조건에 맞는 메모가 없어요.\n검색어나 필터를 바꿔보세요.';
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
        if (listColor) parameters.set('color', listColor);
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
        if (reload && active?.conflicted && !window.confirm('작성 중인 내용을 복사해 두셨나요? 저장본을 다시 열면 지금 편집 중인 내용이 저장된 내용으로 바뀝니다.')) return;
        busy = true;
        updateControls();
        try {
            const memo = reload && active?.conflicted
                ? await request(`/${encodeURIComponent(id)}/reload?version=${encodeURIComponent(active.version)}`, {method: 'POST'})
                : await request(`/${encodeURIComponent(id)}`);
            select(memo);
            status(memo.conflicted ? conflictHint : saveHint);
            if (memo.conflicted) { blocked = true; byId('reload').hidden = false; }
        } catch (error) {
            status(error.message, true);
        } finally {
            busy = false;
            updateControls();
        }
    }

    function normalizedTitle(value) { return value.trim() || '제목 없는 메모'; }

    function inputChanged() {
        if (blocked === 400) blocked = false;
        lastInputAt = Date.now();
        if (!firstDirtyAt) firstDirtyAt = lastInputAt;
        updateControls();
        scheduleAutosave();
    }

    function scheduleAutosave() {
        if (retryRequest) return;
        clearTimeout(autosaveTimer);
        if (!active || blocked || composing || !dirty()) return;
        const now = Date.now();
        firstDirtyAt ||= now;
        const delay = Math.max(0, Math.min(1000 - (now - lastInputAt), 5000 - (now - firstDirtyAt)));
        autosaveTimer = setTimeout(() => performSave('draft'), delay);
    }

    function saveMemo(event) {
        event?.preventDefault();
        if (saving) { forceSave = true; return; }
        if (retryRequest) { forceSave = true; return executeSave(retryRequest); }
        return performSave('manual');
    }

    async function performSave(mode) {
        if (busy || saving || blocked || !active) return false;
        if (retryRequest) return executeSave(retryRequest);
        if (!dirty() && (mode === 'draft' || !active.pending)) return false;
        if (title.value.length > 120 || content.value.length > 20000) {
            status('제목은 120자, 내용은 20,000자 이내로 작성해 주세요.', true);
            return false;
        }
        return executeSave(createSaveOperation(mode, active.id, values()));
    }

    function createSaveOperation(mode, id, snapshot) {
        const isNew = !id;
        const operationKey = crypto.randomUUID();
        const operation = {mode, snapshot, isNew};
        const path = isNew ? '' : `/${encodeURIComponent(id)}`;
        const version = snapshot.version;

        if (mode === 'settings' && !isNew) {
            return {...operation, method: 'PATCH', path: path + '/settings',
                payload: {color: snapshot.color, pinned: snapshot.pinned, version, operationKey}};
        }
        if (mode === 'draft' && !isNew) {
            return {...operation, method: 'PATCH', path: path + '/draft',
                payload: {title: normalizedTitle(snapshot.title), content: snapshot.content, version, operationKey}};
        }
        return {...operation, method: isNew ? 'POST' : 'PUT',
            path: isNew && mode === 'draft' ? '?draft=true' : path,
            payload: {...snapshot, title: normalizedTitle(snapshot.title), operationKey}};
    }

    async function executeSave(operation) {
        if (busy || saving || blocked || !active) return false;
        saving = true;
        saveMode = operation.mode;
        clearTimeout(autosaveTimer);
        clearTimeout(statusTimer);
        firstDirtyAt = 0;
        updateControls();
        try {
            const data = await request(operation.path, {
                method: operation.method, headers: {'Content-Type': 'application/json'}, body: JSON.stringify(operation.payload),
            });
            const memo = !operation.isNew && operation.mode === 'draft'
                ? {...active, ...data, title: operation.payload.title, content: operation.payload.content} : data;
            Object.assign(active, memo);
            // A response acknowledges its snapshot, never text entered while that request was in flight.
            if (operation.mode === 'settings' && !operation.isNew) {
                baseline = {...baseline, color: memo.color, pinned: memo.pinned};
            } else {
                const snapshotTitle = operation.snapshot.title;
                baseline = {
                    ...operation.snapshot,
                    title: normalizedTitle(snapshotTitle) === memo.title ? snapshotTitle : memo.title,
                    content: memo.content,
                    color: memo.color,
                    pinned: memo.pinned,
                };
            }
            retryRequest = null;
            retryAttempt = 0;
            blocked = memo.conflicted ? 409 : false;
            updateEditorMeta(memo);
            byId('reload').hidden = !memo.conflicted;
            updateLocalCard(memo);
            // Creating a row or changing list ordering is exceptional; body autosaves never reload the list.
            if (operation.isNew || operation.mode === 'settings') loadList(operation.isNew ? 0 : page);
            status(memo.pending ? '내용을 임시로 저장했어요. 저장을 마무리하고 있어요.' : '메모를 저장했어요.');
            return true;
        } catch (error) {
            status(error.message, true);
            blocked = [400, 401, 403, 404, 409].includes(error.status) ? error.status : false;
            if (error.status === 409) { active.conflicted = true; byId('reload').hidden = false; }
            if (!blocked) { retryRequest = operation; retryAttempt++; }
            return false;
        } finally {
            saving = false;
            updateControls();
            if (retryRequest) {
                const delay = Math.min(60000, 1000 * 2 ** Math.min(retryAttempt - 1, 6));
                autosaveTimer = setTimeout(() => retryRequest && executeSave(retryRequest), delay);
            } else if (forceSave && !blocked) {
                forceSave = false;
                performSave('manual');
            } else scheduleAutosave();
            scheduleStatus();
        }
    }

    async function saveSettings(changes) {
        if (busy || saving || blocked || retryRequest || !active) return;
        const previous = {color: active.color, pinned: active.pinned};
        const settings = {...previous, ...changes};
        if (settings.color === previous.color && settings.pinned === previous.pinned) return;
        Object.assign(active, settings);
        const saved = await performSave(active.id ? 'settings' : 'manual');
        if (!saved && !retryRequest) { Object.assign(active, previous); updateControls(); }
    }

    function updateLocalCard(memo) {
        const notes = listedMemos.map(note => note.id === memo.id ? {...note, ...memo, preview: memo.content.slice(0, 160)} : note);
        renderList(notes);
    }

    function scheduleStatus() {
        clearTimeout(statusTimer);
        if (!active?.id || !active.pending || blocked || dirty() || saving || retryRequest) return;
        statusTimer = setTimeout(checkStatus, 11000);
    }

    async function checkStatus() {
        if (!active?.id || saving || dirty() || blocked) return;
        const id = active.id;
        const version = active.version;
        try {
            const saved = await request(`/${encodeURIComponent(id)}/status`);
            if (active?.id !== id || active.version !== version || saving || dirty()) return;
            if (saved.version !== version || saved.conflicted) {
                blocked = true;
                active.conflicted = true;
                byId('reload').hidden = false;
                status(conflictHint, true);
            } else {
                active.pending = saved.pending;
                if (!saved.pending) status('메모를 저장했어요.');
            }
        } catch (error) { status(error.message, true); }
        finally { updateControls(); scheduleStatus(); }
    }

    async function deleteMemo() {
        if (busy || saving || retryRequest || !active?.id) return;
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
    title.addEventListener('input', inputChanged);
    content.addEventListener('input', inputChanged);
    for (const input of [title, content]) {
        input.addEventListener('compositionstart', () => { composing = true; clearTimeout(autosaveTimer); });
        input.addEventListener('compositionend', () => { composing = false; inputChanged(); });
    }
    byId('pin').addEventListener('click', () => saveSettings({pinned: !active?.pinned}));
    for (const color of colors) color.addEventListener('click', () => saveSettings({color: color.dataset.color}));
    byId('delete').addEventListener('click', deleteMemo);
    byId('reload').addEventListener('click', () => active?.id && openMemo(active.id, true));
    byId('editor-back').addEventListener('click', () => {
        if (!canDiscard()) return;
        clearEditor();
        byId('new').focus();
    });
    byId('filter-all').addEventListener('click', () => filter(false));
    byId('filter-pinned').addEventListener('click', () => filter(true));
    byId('list-color').addEventListener('change', () => {
        listColor = byId('list-color').value;
        return loadList(0);
    });
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
        if (!event.target.closest('a[data-page-back], a[data-memo-navigation]')) return;
        if (!canDiscard()) {
            event.preventDefault();
            event.stopImmediatePropagation();
        } else allowLeave = true;
    }, true);
    window.addEventListener('beforeunload', event => {
        if (!allowLeave && (busy || saving || retryRequest || dirty())) {
            event.preventDefault();
            event.returnValue = '';
        }
    });
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'hidden' && dirty()) performSave('draft');
    });
    loadList(0);
})();
