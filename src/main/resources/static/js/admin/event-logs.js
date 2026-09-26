(() => {
    'use strict';
    const el = name => document.getElementById(`event-${name}`);
    const list = el('list'), status = el('status');
    const filters = ['broker', 'operation', 'outcome', 'hours'];
    const operations = {QUEUE: '발행 대기열', PUBLISH: '발행', CONSUME: '수신 처리', CONSUMER_RETRY: '소비자 오류 처리', REPLAY: '실패 이벤트 재발행'};
    const outcomes = {QUEUED: '발행 대기', SUCCESS: '성공', FAILED: '실패', RETRY_SCHEDULED: '재시도 예정', DEAD_LETTER: 'Outbox 최종 실패', DLT_PUBLISHED: '소비 실패 토픽에 보관', DUPLICATE: '중복 전달 방지', SKIPPED: '원본 알림 없음 · 건너뜀', NO_SUBSCRIBERS: 'Redis 구독자 없음', DISCARDED: '소비자 처리 포기 (이전 정책)'};
    const routes = {CHAT: '채팅 메시지', NOTIFICATION: '알림 이벤트', REALTIME_NOTIFICATION: '실시간 알림', UNKNOWN_KAFKA: '분류되지 않은 Kafka 경로'};
    const detail = el('detail');
    let detailRequest, detailVersion = 0, selectedId, detailOpener;
    let cursors = [null], position = 0, next = null, loading = false;
    function node(tag, text, className) {
        const result = document.createElement(tag); result.textContent = text;
        if (className) result.className = className;
        return result;
    }
    function time(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '없음'; }
    function referenceKind(entry) {
        if (entry.operation === 'REPLAY') return '소비 실패 기록 ID';
        if (entry.broker === 'KAFKA' && ['QUEUE', 'PUBLISH'].includes(entry.operation)) return 'Outbox ID';
        if (entry.route === 'NOTIFICATION' || entry.route === 'REALTIME_NOTIFICATION' || entry.broker === 'REDIS') return '알림 ID';
        return '관련 ID';
    }
    function explain(entry) {
        const meanings = {
            QUEUED: entry.operation === 'REPLAY' ? '관리자의 재발행 요청이 저장되었습니다. 아직 Kafka 발행이나 소비가 완료된 상태는 아닙니다.' : 'Outbox에 발행 요청이 저장되었습니다. 실제 Kafka 발행은 별도 작업자가 수행합니다.',
            FAILED: '이 처리 단계에서 오류가 발생했습니다. 이 기록만으로 최종 실패 여부를 단정할 수 없으며, 후속 재시도 결과를 함께 확인해야 합니다.',
            RETRY_SCHEDULED: '이번 발행은 실패했으며 재시도가 예정되어 있습니다. 이후 성공 또는 최종 실패 기록을 확인해 주세요.',
            DEAD_LETTER: 'Kafka 발행 전 Outbox에서 재시도 한도에 도달했습니다. Outbox에 보존된 기록이며 소비 실패 토픽(DLT)에 보관되었다는 뜻은 아닙니다.',
            DLT_PUBLISHED: '소비에 실패한 메시지가 DLT에 발행되었습니다. DB 보관 상태와 재발행 가능 여부는 Kafka 운영 화면에서 확인하세요.',
            DUPLICATE: '이미 전달 처리한 동일 알림 이벤트로 판단해 Redis 중복 발행을 생략했습니다.',
            SKIPPED: '현재 DB에서 원본 알림을 찾을 수 없어 처리를 건너뛰었습니다.',
            NO_SUBSCRIBERS: '발행 시 Redis 채널을 구독 중인 연결이 없었습니다. 구독 연결 수는 접속 회원 수가 아닙니다.',
            DISCARDED: '이전 오류 처리 정책에서 소비 처리를 포기한 과거 기록입니다. 이 기록만으로 DLT 보관 여부를 확인할 수 없습니다.'
        };
        if (entry.outcome !== 'SUCCESS') return meanings[entry.outcome] || '이 이벤트의 저장된 처리 결과입니다.';
        if (entry.operation === 'REPLAY') return 'Kafka가 재발행을 받아들였습니다. 원래 소비자의 처리 완료나 사용자 수신 완료를 의미하지 않습니다.';
        if (entry.operation === 'PUBLISH' && entry.broker === 'KAFKA') return 'Kafka가 발행을 받아들였습니다. 소비자 처리 결과는 별도의 수신 처리 기록에서 확인해야 합니다.';
        if (entry.operation === 'PUBLISH' && entry.broker === 'REDIS') return 'Redis Pub/Sub 발행이 완료되었습니다. 구독 연결 수는 서버 연결 수이며 사용자 수신·읽음 수가 아닙니다.';
        return '해당 수신 처리 단계가 완료되었습니다. WebSocket·SSE를 통한 최종 사용자 수신이나 읽음을 보장하지 않습니다.';
    }
    function renderDetail(entry) {
        const fields = node('dl', '', 'event-log-detail-fields');
        const labeled = (labels, value) => value == null ? '기록 없음' : `${labels[value] || value} (${value})`;
        const rows = [
            ['기록 ID', entry.id], ['발생 시각 (서버)', entry.occurredAt ? String(entry.occurredAt).replace('T', ' ') : null],
            ['시스템', entry.broker], ['이벤트 경로', labeled(routes, entry.route)], ['토픽 / 채널', entry.destination],
            ['처리 단계', labeled(operations, entry.operation)], ['처리 결과', labeled(outcomes, entry.outcome)],
            [referenceKind(entry), entry.referenceId], ['처리 대상 건수', entry.itemCount], ['시도 횟수', entry.attempt],
            ['Redis 구독 연결 수', entry.subscriberCount], ['서버 인스턴스 ID', entry.instanceId], ['오류 클래스', entry.errorType]
        ];
        rows.forEach(([label, value]) => fields.append(node('dt', label), node('dd', value == null ? '기록 없음 / 해당 없음' : String(value))));
        const explanation = node('section', '', 'event-log-detail-explanation');
        explanation.append(node('h3', '처리 결과 해석'), node('p', explain(entry)));
        if (entry.route === 'CHAT' && entry.operation === 'CONSUME') explanation.append(node('p', '채팅 수신은 배치 단위로 기록됩니다. 처리 대상 건수는 배치 입력 건수이며, 개별 메시지 ID나 실제 전송 건수는 이 로그에 저장하지 않습니다.'));
        if (entry.errorType) explanation.append(node('p', '오류 클래스만 기록되어 있어 정확한 예외 원문은 확인할 수 없습니다. 발생 시각과 서버 인스턴스를 기준으로 서버 로그를 함께 확인해 주세요.'));
        if (entry.broker === 'KAFKA' && ['FAILED', 'RETRY_SCHEDULED', 'DEAD_LETTER', 'DLT_PUBLISHED'].includes(entry.outcome)) {
            const link = node('button', 'Kafka 운영 · 실패 재처리', 'btn btn-outline-primary');
            link.type = 'button'; link.addEventListener('click', () => location.replace('/admin/kafka')); explanation.append(link);
        }
        el('detail-content').replaceChildren(explanation, fields);
    }
    async function loadDetail(id) {
        detailRequest?.abort();
        const request = new AbortController(), version = ++detailVersion;
        detailRequest = request;
        el('detail-content').replaceChildren(); el('detail-content').setAttribute('aria-busy', 'true');
        el('detail-status').classList.remove('error'); el('detail-status').textContent = '상세 정보를 불러오는 중…';
        el('detail-retry').hidden = true;
        try {
            const response = await fetch(`/api/admin/event-logs/${encodeURIComponent(id)}`, {credentials: 'same-origin', cache: 'no-store', signal: request.signal});
            const body = await response.json().catch(() => null);
            if (version !== detailVersion || !detail.open) return;
            if (response.status === 401) throw new Error('로그인이 만료되었습니다. 관리자 계정으로 다시 로그인해 주세요.');
            if (response.status === 403) throw new Error('관리자만 조회할 수 있습니다.');
            if (response.status === 404) throw new Error('기록이 없거나 보관 기간이 지나 삭제되었습니다.');
            if (!response.ok) throw new Error(body?.message || '상세 정보를 불러오지 못했습니다.');
            if (!body || String(body.id) !== String(id) || !body.operation || !body.outcome) throw new Error('서버 응답을 확인할 수 없습니다.');
            renderDetail(body); el('detail-status').textContent = '저장된 이벤트 정보입니다.';
        } catch (error) {
            if (version !== detailVersion || !detail.open || error.name === 'AbortError') return;
            el('detail-status').classList.add('error'); el('detail-status').textContent = error.message || '서버 연결을 확인해 주세요.';
            el('detail-retry').hidden = false;
        } finally {
            if (version === detailVersion) { detailRequest = null; el('detail-content').setAttribute('aria-busy', 'false'); }
        }
    }
    function openDetail(entry, opener) {
        selectedId = entry.id; detailOpener = opener;
        el('detail-title').textContent = `이벤트 로그 상세 · #${entry.id}`;
        if (!detail.open) detail.showModal();
        loadDetail(selectedId);
    }
    function card(entry) {
        const article = node('article', '', 'event-log-card');
        article.tabIndex = 0; article.setAttribute('role', 'button'); article.setAttribute('aria-haspopup', 'dialog');
        article.setAttribute('aria-label', `이벤트 로그 ${entry.id} 상세 보기`);
        article.addEventListener('click', () => openDetail(entry, article));
        article.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); openDetail(entry, article); }
        });
        const heading = node('div', '', 'event-log-card-header');
        const tone = entry.outcome === 'SUCCESS' ? 'success' : ['FAILED', 'DEAD_LETTER', 'DISCARDED'].includes(entry.outcome) ? 'failure' : 'warning';
        heading.append(node('h2', `${entry.broker} · ${operations[entry.operation] || entry.operation}`));
        heading.append(node('span', outcomes[entry.outcome] || entry.outcome, `event-log-badge ${tone}`));
        heading.append(node('span', '상세 보기 ›', 'event-log-detail-hint'));
        article.append(heading);
        article.append(node('p', `${entry.destination} · ${entry.itemCount}건 · 발생 ${time(entry.occurredAt)} (서버 시각)`));
        const metadata = [`기록 #${entry.id}`];
        if (entry.referenceId != null) metadata.push(`${referenceKind(entry)} ${entry.referenceId}`);
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
    el('detail-close').addEventListener('click', () => detail.close());
    el('detail-retry').addEventListener('click', () => loadDetail(selectedId));
    detail.addEventListener('click', event => { if (event.target === detail) detail.close(); });
    detail.addEventListener('close', () => {
        if (detail.open) return;
        detailVersion++; detailRequest?.abort(); detailRequest = null;
        el('detail-content').replaceChildren(); el('detail-content').setAttribute('aria-busy', 'false');
        if (detailOpener?.isConnected) detailOpener.focus({preventScroll: true});
        else el('refresh').focus({preventScroll: true});
    });
    const timer = setInterval(() => { if (el('auto').checked && !document.hidden && !loading && !detail.open && position === 0) reset(); }, 10000);
    window.addEventListener('pagehide', () => { clearInterval(timer); detailVersion++; detailRequest?.abort(); });
    load();
})();
