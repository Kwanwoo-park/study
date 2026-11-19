document.addEventListener('DOMContentLoaded', function() {
    const root = document.getElementById('todoPage');
    const form = document.getElementById('todoCreateForm');
    const content = document.getElementById('todoContent');
    const message = document.getElementById('todoMessage');
    const sections = [createSection('todoPending', false), createSection('todoCompleted', true)];
    let mutating = false;

    function createSection(prefix, completed) {
        return {
            completed, page: 0, totalPages: 0, loading: false, loadFailed: false, generation: 0, controller: null,
            list: document.getElementById(`${prefix}List`),
            empty: document.getElementById(`${prefix}Empty`),
            count: document.getElementById(`${prefix}Count`),
            previous: document.getElementById(`${prefix}Previous`),
            next: document.getElementById(`${prefix}Next`),
            pageNumber: document.getElementById(`${prefix}PageNumber`),
            retry: document.getElementById(`${prefix}Retry`),
        };
    }

    function isLoading() {
        return sections.some(section => section.loading);
    }

    function updateControls() {
        const busy = isLoading() || mutating;
        root.setAttribute('aria-busy', String(busy));
        root.querySelectorAll('button, input').forEach(element => { element.disabled = busy; });
        sections.forEach(section => {
            section.previous.disabled = busy || section.loadFailed || section.page === 0;
            section.next.disabled = busy || section.loadFailed || section.page + 1 >= section.totalPages;
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
        const details = document.createElement('div');
        details.className = 'todo-details';
        details.append(label);
        if (todo.completed && todo.completedAt) {
            const time = document.createElement('time');
            time.className = 'todo-time';
            time.dateTime = todo.completedAt;
            time.textContent = `완료 · ${new Date(todo.completedAt).toLocaleString('ko-KR')}`;
            details.append(time);
        }
        checkbox.addEventListener('change', function() {
            checkbox.checked = todo.completed;
            mutate(`/api/todo/${todo.id}/completion`, 'PATCH', { completed: !todo.completed },
                    todo.completed ? '미완료 구역으로 옮겼습니다.' : '완료 구역으로 옮겼습니다.', () => {
                        sections.find(section => section.completed !== todo.completed).page = 0;
                    });
        });
        const actions = document.createElement('div');
        actions.className = 'todo-row-actions';
        actions.append(
                button('수정', 'btn btn-outline-secondary btn-sm', () => edit(row, todo)),
                button('삭제', 'btn btn-outline-danger btn-sm', () => {
                    if (!isLoading() && !mutating && window.confirm('이 할 일을 삭제할까요?')) {
                        mutate(`/api/todo/${todo.id}`, 'DELETE', undefined, '할 일을 삭제했습니다.');
                    }
                })
        );
        row.append(checkbox, details, actions);
        return row;
    }

    function edit(row, todo) {
        if (isLoading() || mutating) return;
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

    async function load(section, requestedPage = section.page) {
        if (section.controller) section.controller.abort();
        const requestGeneration = ++section.generation;
        section.controller = new AbortController();
        section.page = requestedPage;
        section.loading = true;
        section.loadFailed = false;
        section.list.replaceChildren();
        section.empty.classList.add('is-hidden');
        section.retry.classList.add('is-hidden');
        section.count.textContent = '불러오는 중…';
        updateControls();
        try {
            const query = new URLSearchParams({page: String(section.page), size: '20', completed: String(section.completed)});
            const body = await requestJson(`/api/todo?${query}`, {signal: section.controller.signal});
            if (requestGeneration !== section.generation) return false;
            section.totalPages = body.totalPages;
            if (section.page > 0 && section.page >= section.totalPages) return await load(section, Math.max(0, section.totalPages - 1));
            body.list.forEach(todo => section.list.append(createRow(todo)));
            section.count.textContent = `${body.totalElements}개`;
            section.pageNumber.textContent = `${Math.min(section.page + 1, section.totalPages)} / ${section.totalPages}`;
            section.empty.classList.toggle('is-hidden', body.list.length > 0);
            return true;
        } catch (error) {
            if (requestGeneration !== section.generation || error.name === 'AbortError') return false;
            section.loadFailed = true;
            section.pageNumber.textContent = '—';
            section.count.textContent = '';
            showMessage(error.message || '할 일을 불러오지 못했습니다.', true);
            section.retry.classList.remove('is-hidden');
            return false;
        } finally {
            if (requestGeneration === section.generation) {
                section.loading = false;
                section.controller = null;
                updateControls();
            }
        }
    }

    async function mutate(url, method, payload, successMessage, onSuccess = () => {}) {
        if (isLoading() || mutating) return;
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
            const results = await Promise.all(sections.map(section => load(section)));
            const refreshed = results.every(Boolean);
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
            sections[0].page = 0;
        });
    });
    sections.forEach(section => {
        section.previous.addEventListener('click', () => { if (!section.previous.disabled) load(section, section.page - 1); });
        section.next.addEventListener('click', () => { if (!section.next.disabled) load(section, section.page + 1); });
        section.retry.addEventListener('click', async () => {
            showMessage('');
            await load(section);
        });
    });
    window.addEventListener('pagehide', () => {
        sections.forEach(section => {
            section.generation++;
            if (section.controller) section.controller.abort();
        });
    });
    window.addEventListener('pageshow', event => {
        if (event.persisted && !mutating) sections.forEach(section => load(section));
    });
    sections.forEach(section => load(section, 0));
});
