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
    systemDetailsState.previousFocus = document.activeElement;
    systemDetailsState.type = type;
    systemDetailsState.root = 'project';
    systemDetailsState.path = '';
    systemDetailsState.page = 0;
    systemDetailsElement('system-details-controls').replaceChildren();
    overlay.hidden = false;
    systemDetailsElement('system-details-title').textContent = type === 'processes' ? '실행 중인 프로세스' : '설치 프로그램 · 파일';
    systemDetailsElement('system-details-description').textContent = type === 'processes'
        ? '메모리 사용량 순 상위 100개입니다. 실행 인자와 파일 내용은 표시하지 않습니다.'
        : '조회 경로를 선택해 파일 이름·크기·수정 시각을 확인할 수 있습니다. 폴더 용량은 재귀 계산하지 않습니다.';
    systemDetailsElement('system-details-close').focus();
    loadSystemDetails();
}

function closeSystemDetails() {
    systemDetailsState.request?.abort();
    systemDetailsState.request = null;
    systemDetailsElement('system-details').hidden = true;
    if (systemDetailsState.previousFocus?.isConnected) systemDetailsState.previousFocus.focus();
}

async function loadSystemDetails(diskLocation = systemDetailsState) {
    systemDetailsState.request?.abort();
    const request = new AbortController();
    systemDetailsState.request = request;
    const type = systemDetailsState.type;
    diagnosticsMessage('불러오는 중입니다.');
    const query = new URLSearchParams({root: diskLocation.root, path: diskLocation.path, page: String(diskLocation.page)});
    const url = type === 'processes' ? '/api/admin/system/processes' : `/api/admin/system/disk?${query}`;
    try {
        const response = await fetch(url, {credentials: 'include', signal: request.signal});
        if (!response.ok) throw new Error('상세 정보를 불러오지 못했습니다.');
        const data = await response.json();
        if (request !== systemDetailsState.request || systemDetailsElement('system-details').hidden) return;
        if (type === 'processes') renderSystemProcesses(data);
        else renderSystemDisk(data);
    } catch (error) {
        if (request === systemDetailsState.request && error.name !== 'AbortError') {
            diagnosticsMessage(error.message || '상세 정보를 불러오지 못했습니다.', true);
        }
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
    summary.textContent = `전체 ${data.process.totalCount}개 · 메모리 사용량 상위 ${data.process.processes.length}개`;
    controls.append(summary, diagnosticsButton('새로고침', () => loadSystemDetails()));
    const content = systemDetailsElement('system-details-content');
    content.replaceChildren();
    if (!data.process.processes.length) return diagnosticsMessage('실행 중인 프로세스가 없습니다.');
    const list = document.createElement('div');
    list.className = 'admin-diagnostics-list';
    list.append(createDiagnosticsRow(['PID', '프로세스', '메모리'], true));
    data.process.processes.forEach(process => list.append(createDiagnosticsRow([
        String(process.pid), process.name, formatBytes(process.memoryBytes)
    ])));
    content.append(list);
}

function renderSystemDisk({disk}) {
    const {root, path, page} = disk;
    Object.assign(systemDetailsState, {root, path, page});
    const controls = systemDetailsElement('system-details-controls');
    controls.replaceChildren();
    const select = document.createElement('select');
    select.className = 'custom-select custom-select-sm admin-diagnostics-root';
    select.setAttribute('aria-label', '조회 경로');
    disk.roots.forEach(choice => {
        const option = document.createElement('option');
        option.value = choice.id;
        option.textContent = choice.label;
        select.append(option);
    });
    select.value = root;
    select.addEventListener('change', () => {
        const nextRoot = select.value;
        select.value = root;
        loadSystemDetails({root: nextRoot, path: '', page: 0});
    });
    const back = diagnosticsButton('상위 폴더', () => {
        loadSystemDetails({root, path: path.split('/').slice(0, -1).join('/'), page: 0});
    }, !path);
    const currentPath = document.createElement('span');
    currentPath.className = 'admin-diagnostics-path';
    currentPath.textContent = `/${path}`;
    controls.append(select, back, currentPath, diagnosticsButton('새로고침', () => loadSystemDetails()));

    const content = systemDetailsElement('system-details-content');
    content.replaceChildren();
    if (!disk.entries.length && disk.totalCount === 0) return diagnosticsMessage('이 폴더는 비어 있습니다.');
    const list = document.createElement('div');
    list.className = 'admin-diagnostics-list admin-diagnostics-files';
    list.append(createDiagnosticsRow(['이름', '크기', '수정 시각'], true));
    disk.entries.forEach(entry => {
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
                loadSystemDetails({root, path: path ? `${path}/${entry.name}` : entry.name, page: 0});
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
        diagnosticsButton('이전', () => loadSystemDetails({root, path, page: page - 1}), page === 0),
        document.createTextNode(`${page + 1} / ${Math.max(1, Math.ceil(disk.totalCount / disk.pageSize))} · ${disk.totalCount}개`),
        diagnosticsButton('다음', () => loadSystemDetails({root, path, page: page + 1}),
            (page + 1) * disk.pageSize >= disk.totalCount)
    );
    content.append(paging);
}

systemDetailsElement('system-details-close').addEventListener('click', closeSystemDetails);
systemDetailsElement('system-details').addEventListener('click', event => {
    if (event.target.id === 'system-details') closeSystemDetails();
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !systemDetailsElement('system-details').hidden) closeSystemDetails();
});
