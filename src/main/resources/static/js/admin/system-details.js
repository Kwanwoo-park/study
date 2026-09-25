const systemDetailsState = {type: null, root: 'project', path: '', page: 0, request: null, previousFocus: null};

function systemDetailsElement(id) {
    return document.getElementById(id);
}

function diagnosticsButton(label, onClick, disabled = false) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn btn-outline-secondary btn-sm';
    button.textContent = label;
    button.disabled = disabled;
    button.addEventListener('click', onClick);
    return button;
}

function diagnosticsMessage(message, error = false) {
    const content = systemDetailsElement('system-details-content');
    content.replaceChildren();
    const paragraph = document.createElement('p');
    paragraph.className = error ? 'admin-diagnostics-error' : 'admin-diagnostics-empty';
    paragraph.textContent = message;
    content.append(paragraph);
}

function openSystemDetails(type) {
    const overlay = systemDetailsElement('system-details');
    if (!overlay || !['processes', 'disk'].includes(type)) return;
    systemDetailsState.previousFocus = document.activeElement;
    systemDetailsState.type = type;
    systemDetailsState.root = 'project';
    systemDetailsState.path = '';
    systemDetailsState.page = 0;
    overlay.hidden = false;
    systemDetailsElement('system-details-title').textContent = type === 'processes' ? '실행 중인 프로세스' : '설치 프로그램 · 파일';
    systemDetailsElement('system-details-description').textContent = type === 'processes'
        ? '메모리 사용량 순 상위 100개입니다. 실행 인자와 파일 내용은 표시하지 않습니다.'
        : '실행 프로젝트와 설치 경로의 파일 이름·크기를 확인할 수 있습니다. 폴더 용량은 재귀 계산하지 않습니다.';
    systemDetailsElement('system-details-close').focus();
    loadSystemDetails();
}

function closeSystemDetails() {
    systemDetailsState.request?.abort();
    systemDetailsState.request = null;
    systemDetailsElement('system-details').hidden = true;
    if (systemDetailsState.previousFocus?.isConnected) systemDetailsState.previousFocus.focus();
}

async function loadSystemDetails() {
    systemDetailsState.request?.abort();
    const request = new AbortController();
    systemDetailsState.request = request;
    const type = systemDetailsState.type;
    diagnosticsMessage('불러오는 중입니다.');
    systemDetailsElement('system-details-controls').replaceChildren();
    const query = new URLSearchParams({root: systemDetailsState.root, path: systemDetailsState.path,
        page: String(systemDetailsState.page)});
    const url = type === 'processes' ? '/api/admin/system/processes' : `/api/admin/system/disk?${query}`;
    try {
        const response = await fetch(url, {credentials: 'include', signal: request.signal});
        if (!response.ok) throw new Error('상세 정보를 불러오지 못했습니다.');
        const data = await response.json();
        if (request !== systemDetailsState.request || systemDetailsElement('system-details').hidden) return;
        if (type === 'processes') renderSystemProcesses(data);
        else renderSystemDisk(data);
    } catch (error) {
        if (error.name !== 'AbortError') diagnosticsMessage(error.message || '상세 정보를 불러오지 못했습니다.', true);
    } finally {
        if (request === systemDetailsState.request) systemDetailsState.request = null;
    }
}

function createDiagnosticsRow(cells, heading = false) {
    const row = document.createElement('div');
    row.className = `admin-diagnostics-row${heading ? ' is-heading' : ''}`;
    cells.forEach(value => {
        const cell = document.createElement('span');
        cell.textContent = value;
        row.append(cell);
    });
    return row;
}

function renderSystemProcesses(data) {
    const controls = systemDetailsElement('system-details-controls');
    controls.replaceChildren();
    const summary = document.createElement('span');
    summary.textContent = `전체 ${data.totalCount}개 · 메모리 사용량 상위 ${data.processes.length}개`;
    controls.append(summary, diagnosticsButton('새로고침', loadSystemDetails));
    const content = systemDetailsElement('system-details-content');
    content.replaceChildren();
    if (!data.processes.length) return diagnosticsMessage('실행 중인 프로세스가 없습니다.');
    const list = document.createElement('div');
    list.className = 'admin-diagnostics-list';
    list.append(createDiagnosticsRow(['PID', '프로세스', '메모리'], true));
    data.processes.forEach(process => list.append(createDiagnosticsRow([
        String(process.pid), process.name, formatBytes(process.memoryBytes)
    ])));
    content.append(list);
}

function renderSystemDisk(data) {
    const controls = systemDetailsElement('system-details-controls');
    controls.replaceChildren();
    const select = document.createElement('select');
    select.className = 'form-select form-select-sm admin-diagnostics-root';
    select.setAttribute('aria-label', '조회 경로');
    data.roots.forEach(root => {
        const option = document.createElement('option');
        option.value = root.id;
        option.textContent = root.label;
        select.append(option);
    });
    select.value = data.root;
    select.addEventListener('change', () => {
        systemDetailsState.root = select.value;
        systemDetailsState.path = '';
        systemDetailsState.page = 0;
        loadSystemDetails();
    });
    const back = diagnosticsButton('상위 폴더', () => {
        systemDetailsState.path = data.path.split('/').slice(0, -1).join('/');
        systemDetailsState.page = 0;
        loadSystemDetails();
    }, !data.path);
    const path = document.createElement('span');
    path.className = 'admin-diagnostics-path';
    path.textContent = data.path ? `/${data.path}` : '/';
    controls.append(select, back, path, diagnosticsButton('새로고침', loadSystemDetails));

    const content = systemDetailsElement('system-details-content');
    content.replaceChildren();
    if (!data.entries.length && data.totalCount === 0) return diagnosticsMessage('이 폴더는 비어 있습니다.');
    const list = document.createElement('div');
    list.className = 'admin-diagnostics-list admin-diagnostics-files';
    list.append(createDiagnosticsRow(['이름', '크기', '수정 시각'], true));
    data.entries.forEach(entry => {
        const row = createDiagnosticsRow([
            `${entry.directory ? '📁 ' : '📄 '}${entry.name}`,
            entry.directory ? '폴더' : formatBytes(entry.sizeBytes),
            entry.modifiedAt ? new Date(entry.modifiedAt).toLocaleString('ko-KR') : '-'
        ]);
        if (entry.directory) {
            row.classList.add('is-directory');
            row.tabIndex = 0;
            row.setAttribute('role', 'button');
            const navigate = () => {
                systemDetailsState.path = data.path ? `${data.path}/${entry.name}` : entry.name;
                systemDetailsState.page = 0;
                loadSystemDetails();
            };
            row.addEventListener('click', navigate);
            row.addEventListener('keydown', event => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    navigate();
                }
            });
        }
        list.append(row);
    });
    content.append(list);
    const paging = document.createElement('div');
    paging.className = 'admin-diagnostics-paging';
    paging.append(
        diagnosticsButton('이전', () => { systemDetailsState.page--; loadSystemDetails(); }, data.page === 0),
        document.createTextNode(`${data.page + 1} / ${Math.max(1, Math.ceil(data.totalCount / data.pageSize))} · ${data.totalCount}개`),
        diagnosticsButton('다음', () => { systemDetailsState.page++; loadSystemDetails(); },
            (data.page + 1) * data.pageSize >= data.totalCount)
    );
    content.append(paging);
}

systemDetailsElement('system-details-close')?.addEventListener('click', closeSystemDetails);
systemDetailsElement('system-details')?.addEventListener('click', event => {
    if (event.target.id === 'system-details') closeSystemDetails();
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !systemDetailsElement('system-details')?.hidden) closeSystemDetails();
});
