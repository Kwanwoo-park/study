document.addEventListener('DOMContentLoaded', function() {
    const root = document.getElementById('todoPage');
    if (!root) return;

    const form = document.getElementById('todoCreateForm');
    const content = document.getElementById('todoContent');
    const list = document.getElementById('todoList');
    const filters = Array.from(document.querySelectorAll('#todoFilters button'));
    const message = document.getElementById('todoMessage');
    const empty = document.getElementById('todoEmpty');
    const count = document.getElementById('todoCount');
    const previous = document.getElementById('todoPrevious');
    const next = document.getElementById('todoNext');
    const pageNumber = document.getElementById('todoPageNumber');
    const retry = document.getElementById('todoRetry');
    let completed = '';
    let page = 0;
    let totalPages = 0;
    let loading = false;
    let mutating = false;
    let loadFailed = false;
    let generation = 0;
    let activeController;

    function updateControls() {
        const busy = loading || mutating;
        root.setAttribute('aria-busy', String(busy));
        root.querySelectorAll('button, input').forEach(element => { element.disabled = busy; });
        previous.disabled = busy || loadFailed || page === 0;
        next.disabled = busy || loadFailed || page + 1 >= totalPages;
        filters.forEach(button => {
            const selected = button.dataset.completed === completed;
            button.classList.toggle('active', selected);
            button.setAttribute('aria-pressed', String(selected));
        });
    }

    function showMessage(text, error = false) {
        message.textContent = text;
        message.className = text ? `todo-message alert ${error ? 'alert-danger' : 'alert-info'}` : 'todo-message';
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, { credentials: 'include', cache: 'no-store', ...options });
        if (response.status === 401) throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.');
        let body;
        try {
            body = await response.json();
        } catch (error) {
            throw new Error('서버 응답을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.');
        }
        if (!response.ok) throw new Error(body.message || '요청을 처리하지 못했습니다.');
        return body;
    }

    function button(text, className, action) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = className;
        element.textContent = text;
        element.addEventListener('click', action);
        return element;
    }

    function createRow(todo) {
        const row = document.createElement('li');
        row.className = `todo-row${todo.completed ? ' is-completed' : ''}`;
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.className = 'todo-checkbox';
        checkbox.id = `todo-${todo.id}`;
        checkbox.checked = todo.completed;
        const label = document.createElement('label');
        label.className = 'todo-label';
        label.htmlFor = checkbox.id;
        label.textContent = todo.content;
        checkbox.addEventListener('change', function() {
            checkbox.checked = todo.completed;
            mutate(`/api/todo/${todo.id}/completion`, 'PATCH', { completed: !todo.completed },
                    todo.completed ? '미완료로 변경했습니다.' : '완료한 할 일로 표시했습니다.');
        });
        const actions = document.createElement('div');
        actions.className = 'todo-row-actions';
        actions.append(
                button('수정', 'btn btn-outline-secondary btn-sm', () => edit(row, todo)),
                button('삭제', 'btn btn-outline-danger btn-sm', () => {
                    if (!loading && !mutating && window.confirm('이 할 일을 삭제할까요?')) {
                        mutate(`/api/todo/${todo.id}`, 'DELETE', undefined, '할 일을 삭제했습니다.');
                    }
                })
        );
        row.append(checkbox, label, actions);
        return row;
    }

    function edit(row, todo) {
        if (loading || mutating) return;
        const editForm = document.createElement('form');
        editForm.className = 'todo-edit-form';
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'form-control';
        input.maxLength = 255;
        input.required = true;
        input.value = todo.content;
        input.setAttribute('aria-label', '할 일 내용 수정');
        const save = document.createElement('button');
        save.type = 'submit';
        save.className = 'btn btn-primary btn-sm';
        save.textContent = '저장';
        const cancel = () => { if (!mutating) row.replaceWith(createRow(todo)); };
        editForm.append(input, save, button('취소', 'btn btn-outline-secondary btn-sm', cancel));
        editForm.addEventListener('submit', function(event) {
            event.preventDefault();
            if (!input.value.trim()) {
                showMessage('할 일을 입력해주세요.', true);
                input.focus();
                return;
            }
            mutate(`/api/todo/${todo.id}`, 'PATCH', { content: input.value.trim() }, '할 일을 수정했습니다.');
        });
        input.addEventListener('keydown', event => {
            if (event.key === 'Escape') cancel();
        });
        row.replaceChildren(editForm);
        input.focus();
    }

    async function load(requestedPage = page) {
        if (activeController) activeController.abort();
        const requestGeneration = ++generation;
        activeController = new AbortController();
        page = requestedPage;
        loading = true;
        loadFailed = false;
        list.replaceChildren();
        empty.classList.add('is-hidden');
        retry.classList.add('is-hidden');
        count.textContent = '';
        showMessage('할 일을 불러오는 중입니다.');
        updateControls();
        try {
            const query = new URLSearchParams({ page: String(page), size: '20' });
            if (completed !== '') query.set('completed', completed);
            const body = await requestJson(`/api/todo?${query}`, { signal: activeController.signal });
            if (requestGeneration !== generation) return false;
            if (!Array.isArray(body.list) || !Number.isInteger(body.totalPages) || body.totalPages < 0
                    || !Number.isInteger(body.totalElements) || body.totalElements < 0
                    || !body.list.every(todo => todo && Number.isSafeInteger(todo.id) && todo.id > 0
                            && typeof todo.content === 'string' && typeof todo.completed === 'boolean')) {
                throw new Error('할 일 목록 형식이 올바르지 않습니다.');
            }
            totalPages = body.totalPages;
            if (page > 0 && page >= totalPages) return await load(Math.max(0, totalPages - 1));
            body.list.forEach(todo => list.append(createRow(todo)));
            count.textContent = `${body.totalElements}개`;
            pageNumber.textContent = `${totalPages === 0 ? 0 : page + 1} / ${totalPages}`;
            empty.textContent = completed === 'true' ? '완료한 할 일이 없습니다.'
                    : completed === 'false' ? '미완료 할 일이 없습니다.' : '아직 등록된 할 일이 없습니다.';
            empty.classList.toggle('is-hidden', body.list.length > 0);
            showMessage('');
            return true;
        } catch (error) {
            if (requestGeneration !== generation || error.name === 'AbortError') return false;
            loadFailed = true;
            pageNumber.textContent = '—';
            showMessage(error.message || '할 일을 불러오지 못했습니다.', true);
            retry.classList.remove('is-hidden');
            return false;
        } finally {
            if (requestGeneration === generation) {
                loading = false;
                activeController = null;
                updateControls();
            }
        }
    }

    async function mutate(url, method, payload, successMessage, onSuccess = () => {}) {
        if (loading || mutating) return;
        mutating = true;
        updateControls();
        try {
            const options = { method };
            if (payload !== undefined) {
                options.headers = { 'Content-Type': 'application/json' };
                options.body = JSON.stringify(payload);
            }
            await requestJson(url, options);
            onSuccess();
            const refreshed = await load(page);
            showMessage(refreshed ? successMessage : `${successMessage} 목록은 다시 불러와 주세요.`, !refreshed);
        } catch (error) {
            showMessage(error.message || '요청을 처리하지 못했습니다.', true);
        } finally {
            mutating = false;
            updateControls();
        }
    }

    form.addEventListener('submit', function(event) {
        event.preventDefault();
        if (!content.value.trim()) {
            showMessage('할 일을 입력해주세요.', true);
            content.focus();
            return;
        }
        mutate('/api/todo', 'POST', { content: content.value.trim() }, '할 일을 추가했습니다.', () => {
            content.value = '';
            page = 0;
            completed = '';
        });
    });
    filters.forEach(filter => filter.addEventListener('click', () => {
        if (loading || mutating) return;
        completed = filter.dataset.completed;
        load(0);
    }));
    previous.addEventListener('click', () => { if (!previous.disabled) load(page - 1); });
    next.addEventListener('click', () => { if (!next.disabled) load(page + 1); });
    retry.addEventListener('click', () => load(page));
    window.addEventListener('pagehide', () => {
        generation++;
        if (activeController) activeController.abort();
    });
    window.addEventListener('pageshow', event => {
        if (event.persisted && !mutating) load(page);
    });
    load(0);
});
