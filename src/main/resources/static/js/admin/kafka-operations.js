(() => {
    'use strict';
    const el = id => document.getElementById(`kafka-${id}`);
    const states = {NEW: '미처리', REPLAY_PENDING: '재발행 대기', REPLAYED: '재발행됨', REPLAY_FAILED: '재발행 실패'};
    let page = 0, cursors = [null], next = null, listLoading = false, overviewLoading = false, replaying = false;
    const node = (tag, text, className) => { const n = document.createElement(tag); n.textContent = text; if (className) n.className = className; return n; };
    const time = value => value ? new Date(value).toLocaleString('ko-KR') : '-';
    async function request(url, options = {}) {
        const response = await fetch(url, {credentials: 'same-origin', cache: 'no-store', ...options});
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error(body?.message || (response.status === 401 || response.status === 403 ? '관리자 인증 또는 요청 확인 정보가 만료되었습니다. 페이지를 새로고침해 주세요.' : '조회에 실패했습니다.'));
        return body;
    }
    function table(id, headers, rows) {
        const t = document.createElement('table'), head = document.createElement('thead'), tr = document.createElement('tr'), body = document.createElement('tbody');
        headers.forEach(h => tr.append(node('th', h))); head.append(tr);
        rows.forEach(values => { const row = document.createElement('tr'); values.forEach(value => row.append(node('td', value ?? '미확인'))); body.append(row); });
        t.append(head, body); el(id).replaceChildren(t);
    }
    async function overview() {
        if (overviewLoading) return;
        overviewLoading = true;
        try {
            const data = await request('/api/admin/kafka/overview');
            el('summary').replaceChildren();
            [['소비 지연', data.totalLag == null ? '미확인' : `${data.totalLag}건`], ['Outbox 대기', `${data.outboxPending}건`],
                ['최장 발행 대기', `${data.oldestOutboxSeconds}초`], ['Outbox 최종 실패', `${data.outboxFailed}건`],
                ['소비 실패 미처리', `${data.unresolvedDeadLetters}건`], ['재발행 대기', `${data.replayPending}건`]].forEach(([label, value]) => {
                const card = node('div', '', 'kafka-metric'); card.append(node('span', label), node('strong', value)); el('summary').append(card);
            });
            el('warnings').replaceChildren(...data.warnings.map(w => node('li', w)));
            table('topics', ['토픽', '파티션', '복제 수', '최소 동기 복제 수', '보관 기간', '정리 정책'], data.topics.map(t => [t.topic, t.partitions, t.replicas, t.minInSyncReplicas, Number(t.retentionMs) >= 0 ? `${Number(t.retentionMs) / 86400000}일` : t.retentionMs, t.cleanupPolicy]));
            table('lags', ['토픽 / 파티션', '소비자 그룹', '커밋 위치', '최신 위치', '지연 건수'], data.lags.map(p => [`${p.topic} / ${p.partition}`, p.group, p.committedOffset, p.endOffset, p.lag]));
            el('status').textContent = `${time(data.observedAt)} 기준 · 15초마다 갱신${data.brokerError ? ' · Kafka 조회 오류' : ''}`;
        } catch (error) { el('status').textContent = error.message; }
        finally { overviewLoading = false; }
    }
    function controls() {
        el('prev').disabled = listLoading || replaying || page === 0;
        el('next').disabled = listLoading || replaying || next == null;
        el('filter').disabled = listLoading || replaying;
        el('refresh').disabled = listLoading || replaying;
    }
    async function replay(entry, button) {
        if (replaying || !window.confirm('이 이벤트를 다시 발행할까요? 원래 발생 순서와 다르게 처리될 수 있습니다.')) return;
        replaying = true; button.disabled = true; controls();
        try {
            const csrf = await request('/api/admin/kafka/csrf');
            const result = await request(`/api/admin/kafka/dead-letters/${entry.id}/replay`, {method: 'POST', headers: {[csrf.headerName]: csrf.token}});
            await load(page, cursors[page]); await overview(); el('list-status').textContent = result.message;
        } catch (error) { el('list-status').textContent = error.message; button.disabled = false; }
        finally { replaying = false; controls(); }
    }
    function card(entry) {
        const item = node('article', '', 'event-log-card'), header = node('div', '', 'event-log-card-header');
        header.append(node('h2', `#${entry.id} · ${entry.topic}`), node('span', states[entry.status], 'event-log-badge'));
        item.append(header, node('p', `파티션 ${entry.partition} · offset ${entry.offset} · 기록 ${time(entry.createdAt)}`));
        if (entry.errorType) item.append(node('p', `오류 유형: ${entry.errorType}`, 'event-log-meta'));
        item.append(node('p', `재발행 시도 ${entry.attempts}회 · 다음 시도 ${time(entry.nextAttemptAt)} · 발행 완료 ${time(entry.replayedAt)}`, 'event-log-meta'));
        if (entry.replayable) { const button = node('button', '재발행 요청', 'btn btn-outline-primary'); button.type = 'button'; button.addEventListener('click', () => replay(entry, button)); item.append(button); }
        return item;
    }
    async function load(target = 0, before = null) {
        if (listLoading) return;
        listLoading = true; controls(); el('list-status').textContent = '불러오는 중…';
        try {
            const query = new URLSearchParams();
            if (el('filter').value) query.set('status', el('filter').value);
            if (before != null) query.set('beforeId', before);
            const data = await request(`/api/admin/kafka/dead-letters?${query}`);
            page = target; cursors[page] = before; cursors.length = page + 1; next = data.nextBeforeId;
            el('page').textContent = `${page + 1} 페이지`;
            el('dead-letters').replaceChildren(...data.entries.map(card));
            el('list-status').textContent = data.entries.length ? `${data.entries.length}건 표시` : '해당 조건의 실패 기록이 없습니다.';
        } catch (error) { el('list-status').textContent = error.message; el('dead-letters').replaceChildren(); next = null; }
        finally { listLoading = false; controls(); }
    }
    el('prev').addEventListener('click', () => { if (page > 0) load(page - 1, cursors[page - 1]); });
    el('next').addEventListener('click', () => { if (next != null) load(page + 1, next); });
    el('filter').addEventListener('change', () => load());
    el('refresh').addEventListener('click', () => { overview(); load(); });
    const timer = setInterval(() => { if (!document.hidden && !replaying) { overview(); if (page === 0) load(); } }, 15000);
    window.addEventListener('pagehide', () => clearInterval(timer));
    overview(); load();
})();
