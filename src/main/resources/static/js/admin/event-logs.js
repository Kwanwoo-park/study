(() => {
    'use strict';
    const el = name => document.getElementById(`event-${name}`);
    const list = el('list'), status = el('status');
    const filters = ['broker', 'operation', 'outcome', 'hours'];
    const operations = {QUEUE: '발행 대기열', PUBLISH: '발행', CONSUME: '수신 처리', CONSUMER_RETRY: '소비자 오류 처리'};
    const outcomes = {QUEUED: '발행 대기', SUCCESS: '성공', FAILED: '실패', RETRY_SCHEDULED: '재시도 예정', DEAD_LETTER: 'Outbox 최종 실패', SKIPPED: '원본 알림 없음 · 건너뜀', NO_SUBSCRIBERS: 'Redis 구독자 없음', DISCARDED: '소비자 처리 포기'};
    let cursors = [null], position = 0, next = null, loading = false;
    function node(tag, text, className) {
        const result = document.createElement(tag); result.textContent = text;
        if (className) result.className = className;
        return result;
    }
    function time(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '없음'; }
    function card(entry) {
        const article = node('article', '', 'event-log-card');
        const heading = node('div', '', 'event-log-card-header');
        const tone = entry.outcome === 'SUCCESS' ? 'success' : ['FAILED', 'DEAD_LETTER', 'DISCARDED'].includes(entry.outcome) ? 'failure' : 'warning';
        heading.append(node('h2', `${entry.broker} · ${operations[entry.operation] || entry.operation}`));
        heading.append(node('span', outcomes[entry.outcome] || entry.outcome, `event-log-badge ${tone}`));
        article.append(heading);
        article.append(node('p', `${entry.destination} · ${entry.itemCount}건 · 발생 ${time(entry.occurredAt)} (서버 시각)`));
        const referenceKind = ['QUEUE', 'PUBLISH'].includes(entry.operation) && entry.broker === 'KAFKA' ? 'Outbox ID' : '알림 ID';
        const metadata = [`기록 #${entry.id}`];
        if (entry.referenceId != null) metadata.push(`${referenceKind} ${entry.referenceId}`);
        if (entry.attempt != null) metadata.push(`${entry.attempt}차 시도`);
        if (entry.subscriberCount != null) metadata.push(`Redis 구독 연결 ${entry.subscriberCount}개`);
        metadata.push(`서버 인스턴스 ${entry.instanceId}`);
        article.append(node('p', metadata.join(' · '), 'event-log-meta'));
        if (entry.errorType) article.append(node('p', `오류 유형: ${entry.errorType}`, 'error'));
        if (entry.outcome === 'NO_SUBSCRIBERS') article.append(node('p', '발행 시 구독 중인 Redis 연결이 없었습니다. 사용자 수와는 다릅니다.', 'event-log-meta'));
        if (entry.outcome === 'DEAD_LETTER') article.append(node('p', 'Outbox에 보존된 최종 실패입니다. 별도 Kafka DLT 전송을 의미하지 않습니다.', 'event-log-meta'));
        return article;
    }
    function controls() {
        el('prev').disabled = loading || position === 0;
        el('next').disabled = loading || next == null;
        el('refresh').disabled = loading;
        filters.forEach(name => { el(name).disabled = loading; });
        list.setAttribute('aria-busy', String(loading));
    }
    async function load(targetPosition = 0, beforeId = null) {
        if (loading) return;
        loading = true; controls(); status.classList.remove('error'); status.textContent = '이벤트 로그를 불러오는 중…';
        try {
            const query = new URLSearchParams();
            filters.forEach(name => { if (el(name).value) query.set(name, el(name).value); });
            if (beforeId != null) query.set('beforeId', String(beforeId));
            const response = await fetch(`/api/admin/event-logs?${query}`, {credentials: 'same-origin', cache: 'no-store'});
            const body = await response.json().catch(() => null);
            if (response.status === 401) throw new Error('로그인이 만료되었습니다. 관리자 계정으로 다시 로그인해 주세요.');
            if (response.status === 403) throw new Error('관리자만 조회할 수 있습니다.');
            if (!response.ok) throw new Error(body?.message || '이벤트 로그 조회에 실패했습니다.');
            if (!body || !Array.isArray(body.entries)) throw new Error('서버 응답을 확인할 수 없습니다. 다시 로그인해 주세요.');
            position = targetPosition; cursors[position] = beforeId; cursors.length = position + 1;
            next = body.nextBeforeId; el('page').textContent = `${position + 1} 페이지`;
            list.replaceChildren();
            if (body.entries.length) list.append(...body.entries.map(card));
            else list.append(node('p', '해당 조건의 이벤트 기록이 없습니다. 기능 적용 이후 발생한 이벤트부터 기록됩니다.', 'event-log-note'));
            const health = body.diagnostics || {};
            el('diagnostics').textContent = `최근 ${body.retentionDays}일 보관 · 현재 서버 버퍼 ${health.pending || 0}건 · 로그 누락 ${health.dropped || 0}건 · 저장/정리 실패 누적 ${health.storageFailures || 0}회 · 마지막 저장 ${time(health.lastSavedAt)}`;
            if (health.enabled === false) el('diagnostics').textContent += ' · 이벤트 로그 수집이 꺼져 있습니다.';
            el('diagnostics').classList.toggle('warning-text', health.enabled === false || health.dropped > 0 || health.storageFailures > 0);
            status.textContent = `${body.entries.length}건 표시 · 기록 반영에는 수 초가 걸릴 수 있습니다.`;
        } catch (error) {
            status.classList.add('error'); status.textContent = error.message || '서버 연결을 확인해 주세요.';
            list.replaceChildren(); next = null;
        } finally { loading = false; controls(); }
    }
    function reset() { if (!loading) { cursors = [null]; position = 0; next = null; el('page').textContent = '1 페이지'; return load(); } }
    filters.forEach(name => el(name).addEventListener('change', reset));
    el('refresh').addEventListener('click', reset);
    el('prev').addEventListener('click', () => { if (!loading && position > 0) load(position - 1, cursors[position - 1]); });
    el('next').addEventListener('click', () => { if (!loading && next != null) load(position + 1, next); });
    const timer = setInterval(() => { if (el('auto').checked && !document.hidden && !loading && position === 0) reset(); }, 10000);
    window.addEventListener('pagehide', () => clearInterval(timer));
    load();
})();
