(() => {
    'use strict';

    const element = id => document.getElementById(`github-${id}`);
    const list = element('list');
    const status = element('status');
    const previous = element('prev');
    const next = element('next');
    const refresh = element('refresh');
    const commitsButton = element('commits');
    const activityButton = element('activity');
    const dateFormat = new Intl.DateTimeFormat('ko-KR', {
        timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
    });
    const activityNames = {
        normal: 'Push', push: 'Push', force: '강제 Push', force_push: '강제 Push',
        create: '브랜치 생성', branch_creation: '브랜치 생성', delete: '브랜치 삭제',
        branch_deletion: '브랜치 삭제', pr_merge: 'PR 병합', merge_queue_merge: '병합 큐 병합',
    };
    let type = 'commits';
    let cursors = [''];
    let position = 0;
    let nextCursor = null;
    let loading = false;

    function formatDate(value) {
        if (!value) return '시각 정보 없음';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? '시각 정보 없음' : dateFormat.format(date);
    }

    function node(tag, text, className) {
        const item = document.createElement(tag);
        item.textContent = text;
        if (className) item.className = className;
        return item;
    }

    function safeGitHubLink(value) {
        try {
            const url = new URL(value);
            return url.protocol === 'https:' && url.hostname === 'github.com' && !url.username && !url.password && !url.port ? url.href : null;
        } catch (_) {
            return null;
        }
    }

    function renderEntry(entry) {
        const article = node('article', '', 'github-entry');
        const heading = node('div', '', 'github-entry-heading');
        const title = type === 'commits' ? entry.message : (activityNames[entry.type] || entry.type || '저장소 변경');
        heading.append(node('h3', title || '커밋 메시지 없음'));
        const url = safeGitHubLink(entry.url);
        if (url) {
            const link = node('a', type === 'commits' ? '커밋 상세 ↗' : '변경 비교 ↗');
            link.href = url;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            heading.append(link);
        }
        article.append(heading);
        if (type === 'commits') {
            article.append(node('p', `작성자: ${entry.author || '알 수 없음'}`));
            article.append(node('p', `작성 시각: ${formatDate(entry.authoredAt)} · 커밋 반영 시각: ${formatDate(entry.committedAt)}`, 'github-muted'));
            article.append(node('p', `SHA: ${entry.sha}`, 'github-entry-sha'));
        } else {
            article.append(node('p', `작업자: ${entry.actor || '알 수 없음'} · 브랜치: ${entry.ref || '알 수 없음'}`));
            article.append(node('p', `Push 시각: ${formatDate(entry.pushedAt)}`, 'github-muted'));
            article.append(node('p', `${entry.before || '없음'} → ${entry.after || '없음'}`, 'github-entry-sha'));
        }
        return article;
    }

    function updateControls() {
        previous.disabled = loading || position === 0;
        next.disabled = loading || !nextCursor;
        refresh.disabled = loading;
        commitsButton.disabled = loading;
        activityButton.disabled = loading;
        commitsButton.setAttribute('aria-pressed', String(type === 'commits'));
        activityButton.setAttribute('aria-pressed', String(type === 'activity'));
        list.setAttribute('aria-busy', String(loading));
    }

    async function load(targetPosition = 0, targetCursor = '') {
        if (loading) return;
        loading = true;
        status.classList.remove('error');
        status.textContent = 'GitHub 내역을 불러오는 중입니다…';
        list.replaceChildren();
        updateControls();
        try {
            const query = type === 'commits'
                ? `page=${encodeURIComponent(targetCursor || '1')}`
                : `cursor=${encodeURIComponent(targetCursor)}`;
            const response = await fetch(`/api/admin/github/${type}?${query}`, {credentials: 'same-origin', cache: 'no-store'});
            const body = await response.json().catch(() => null);
            if (response.status === 401) throw new Error('로그인이 만료되었습니다. 관리자 계정으로 다시 로그인해 주세요.');
            if (response.status === 403) throw new Error('관리자만 조회할 수 있습니다.');
            if (!response.ok) throw new Error(body?.message || 'GitHub 내역을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            if (!body || !Array.isArray(body.entries)) throw new Error('서버 응답을 확인할 수 없습니다. 다시 로그인해 주세요.');
            position = targetPosition;
            cursors[position] = targetCursor;
            cursors.length = position + 1;
            nextCursor = body.nextCursor;
            element('repository').textContent = `${body.repository} · ${body.branch}`;
            element('page').textContent = `${position + 1} 페이지`;
            if (!body.entries.length) {
                list.append(node('p', '조회할 내역이 없습니다.', 'github-muted'));
            } else {
                list.append(...body.entries.map(renderEntry));
            }
            status.textContent = `${body.entries.length}건 · 조회 시각: ${formatDate(body.fetchedAt)}`;
        } catch (error) {
            status.classList.add('error');
            status.textContent = error.message || '연결 상태를 확인한 뒤 다시 시도해 주세요.';
            nextCursor = null;
        } finally {
            loading = false;
            updateControls();
        }
    }

    function firstPage(selectedType) {
        if (loading) return;
        type = selectedType;
        cursors = [''];
        position = 0;
        nextCursor = null;
        element('page').textContent = '1 페이지';
        element('description').textContent = type === 'commits'
            ? 'GitHub에 올라온 커밋입니다. 커밋 작성·반영 시각은 실제 Push 시각과 다를 수 있습니다.'
            : 'Push, 강제 Push, 병합 및 브랜치 변경 기록입니다. 한 번의 Push에 여러 커밋이 포함될 수 있습니다.';
        return load();
    }

    commitsButton.addEventListener('click', () => firstPage('commits'));
    activityButton.addEventListener('click', () => firstPage('activity'));
    refresh.addEventListener('click', () => firstPage(type));
    previous.addEventListener('click', () => {
        if (position > 0) return load(position - 1, cursors[position - 1]);
    });
    next.addEventListener('click', () => {
        if (nextCursor) return load(position + 1, nextCursor);
    });
    load();
})();
