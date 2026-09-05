(() => {
    'use strict';

    const api = '/api/admin/files';
    const input = document.getElementById('admin-file-input');
    const uploadButton = document.getElementById('admin-file-upload');
    const status = document.getElementById('admin-file-status');
    const progress = document.getElementById('admin-file-progress');
    const list = document.getElementById('admin-file-list');
    const previous = document.getElementById('admin-file-prev');
    const next = document.getElementById('admin-file-next');
    const refresh = document.getElementById('admin-file-refresh');
    let maxFileSize = 0;
    let page = 0;
    let uploading = false;

    function formatSize(bytes) {
        if (bytes < 1024) return `${bytes} B`;
        const unit = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), 3);
        return `${(bytes / (1024 ** unit)).toFixed(1)} ${['B', 'KB', 'MB', 'GB'][unit]}`;
    }

    function showStatus(message, error = false) {
        status.textContent = message;
        status.classList.toggle('error', error);
    }

    function errorMessage(code, body) {
        if (code === 401) return '로그인이 만료되었습니다. 관리자 계정으로 다시 로그인해 주세요.';
        if (code === 403) return '관리자 권한 또는 요청 인증을 확인할 수 없습니다. 페이지를 새로고침해 주세요.';
        if (code === 413) return '업로드 용량 제한을 초과했습니다. 서버 및 프록시의 용량 설정을 확인해 주세요.';
        return body?.message || '요청을 처리하지 못했습니다. 연결 상태를 확인해 주세요.';
    }

    async function getJson(url) {
        const response = await fetch(url, {credentials: 'same-origin', cache: 'no-store'});
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error(errorMessage(response.status, body));
        if (!body) throw new Error('서버 응답을 확인할 수 없습니다. 다시 로그인해 주세요.');
        return body;
    }

    function updateSelection() {
        const file = input.files[0];
        document.getElementById('admin-file-selection').textContent = file ? `${file.name} · ${formatSize(file.size)}` : '선택된 파일이 없습니다.';
        uploadButton.disabled = uploading || !file || !maxFileSize || file.size > maxFileSize;
        if (file && maxFileSize && file.size > maxFileSize) showStatus(`파일당 최대 ${formatSize(maxFileSize)}까지 업로드할 수 있습니다.`, true);
    }

    async function loadSettings() {
        const settings = await getJson(`${api}/csrf`);
        maxFileSize = settings.maxFileSize;
        document.getElementById('admin-file-limit').textContent = `형식 제한 없음 · 파일당 최대 ${formatSize(maxFileSize)} (요청 전체 제한도 적용됩니다)`;
        updateSelection();
        return settings;
    }

    async function loadFiles(targetPage = page) {
        previous.disabled = true;
        next.disabled = true;
        refresh.disabled = true;
        try {
            const data = await getJson(`${api}?page=${targetPage}`);
            page = data.number;
            list.replaceChildren();
            document.getElementById('admin-file-count').textContent = `(${data.totalElements}개)`;
            for (const file of data.content) {
                const row = document.createElement('article');
                row.className = 'admin-file-row';
                const details = document.createElement('div');
                details.className = 'admin-file-details';
                const name = document.createElement('strong');
                name.className = 'admin-file-name';
                name.textContent = file.originalFilename;
                const meta = document.createElement('span');
                meta.className = 'admin-file-meta';
                meta.textContent = `${formatSize(file.size)} · ${file.createdAt.replace('T', ' ').slice(0, 19)} · 관리자 #${file.uploadedBy}`;
                const download = document.createElement('a');
                download.className = 'btn btn-outline-primary btn-sm';
                download.textContent = '다운로드';
                download.setAttribute('aria-label', `${file.originalFilename} 다운로드`);
                download.href = `${api}/${encodeURIComponent(file.id)}/download`;
                details.append(name, meta);
                row.append(details, download);
                list.append(row);
            }
            if (!data.content.length) {
                const empty = document.createElement('p');
                empty.className = 'admin-files-empty';
                empty.textContent = '보관된 파일이 없습니다.';
                list.append(empty);
            }
            document.getElementById('admin-file-page').textContent = `${page + 1} / ${Math.max(data.totalPages, 1)}`;
            previous.disabled = data.first;
            next.disabled = data.last;
        } catch (error) {
            list.textContent = '목록을 불러오지 못했습니다. 새로고침으로 다시 시도해 주세요.';
            throw error;
        } finally {
            refresh.disabled = false;
        }
    }

    function upload(file, settings) {
        return new Promise((resolve, reject) => {
            const request = new XMLHttpRequest();
            request.open('POST', api);
            request.setRequestHeader(settings.headerName, settings.token);
            request.responseType = 'json';
            request.upload.addEventListener('progress', event => {
                if (event.lengthComputable) {
                    progress.value = Math.round(event.loaded / event.total * 100);
                    showStatus(progress.value === 100 ? '업로드 전송 완료. 서버에 저장 중입니다…' : `업로드 중… ${progress.value}%`);
                }
            });
            request.addEventListener('load', () => {
                if (request.status === 201) resolve();
                else reject(new Error(errorMessage(request.status, request.response)));
            });
            request.addEventListener('error', () => reject(new Error('연결이 끊겼습니다. 파일 목록에서 저장 여부를 확인한 뒤 다시 시도해 주세요.')));
            const data = new FormData();
            data.append('file', file);
            request.send(data);
        });
    }

    input.addEventListener('change', () => { showStatus(''); updateSelection(); });
    document.getElementById('admin-file-form').addEventListener('submit', async event => {
        event.preventDefault();
        const file = input.files[0];
        if (uploading || !file || !maxFileSize || file.size > maxFileSize) return;
        uploading = true;
        input.disabled = true;
        updateSelection();
        progress.hidden = false;
        progress.value = 0;
        showStatus('업로드를 준비하고 있습니다…');
        try {
            const settings = await loadSettings();
            await upload(file, settings);
            input.value = '';
            showStatus('파일을 안전하게 보관했습니다.');
            await loadFiles(0).catch(error => showStatus(`업로드 완료. 목록 갱신 실패: ${error.message}`, true));
        } catch (error) {
            showStatus(error.message, true);
        } finally {
            uploading = false;
            input.disabled = false;
            progress.hidden = true;
            updateSelection();
        }
    });
    refresh.addEventListener('click', () => loadFiles().catch(error => showStatus(error.message, true)));
    previous.addEventListener('click', () => loadFiles(page - 1).catch(error => showStatus(error.message, true)));
    next.addEventListener('click', () => loadFiles(page + 1).catch(error => showStatus(error.message, true)));
    Promise.all([loadSettings(), loadFiles(0)]).catch(error => showStatus(error.message, true));
})();
