(() => {
    'use strict';
    const el = id => document.getElementById(`kafka-${id}`);
    const states = {NEW: '미처리', REPLAY_PENDING: '재발행 대기', REPLAYED: '재발행됨', REPLAY_FAILED: '재발행 실패'};
    const groupStates = {STABLE: '안정', EMPTY: '연결된 소비자 없음', DEAD: '종료', UNKNOWN: '미확인',
        PREPARING_REBALANCE: '리밸런싱 준비', COMPLETING_REBALANCE: '리밸런싱 중'};
    let page = 0, cursors = [null], next = null, listLoading = false, overviewLoading = false, replaying = false;
    let snapshot = null, selectedTopic = null, selectedConsumer = null;
    const node = (tag, text, className) => { const n = document.createElement(tag); n.textContent = text; if (className) n.className = className; return n; };
    const time = value => value ? new Date(value).toLocaleString('ko-KR') : '-';
    const count = value => value == null ? '미확인' : `${value}건`;
    const joined = values => values?.length ? values.join(', ') : '-';
    async function request(url, options = {}) {
        const response = await fetch(url, {credentials: 'same-origin', cache: 'no-store', ...options});
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error(body?.message || (response.status === 401 || response.status === 403 ? '관리자 인증 또는 요청 확인 정보가 만료되었습니다. 페이지를 새로고침해 주세요.' : '조회에 실패했습니다.'));
        return body;
    }
    function table(id, headers, rows, empty = '조회할 수 있는 정보가 없습니다.') {
        if (!rows.length) { el(id).replaceChildren(node('p', empty, 'kafka-empty')); return; }
        const t = document.createElement('table'), head = document.createElement('thead'), tr = document.createElement('tr'), body = document.createElement('tbody');
        headers.forEach(h => { const th = node('th', h); th.setAttribute('scope', 'col'); tr.append(th); }); head.append(tr);
        rows.forEach(values => {
            const row = document.createElement('tr');
            values.forEach(value => {
                const td = document.createElement('td');
                if (value && typeof value.addEventListener === 'function') td.append(value);
                else td.textContent = value ?? '미확인';
                row.append(td);
            });
            body.append(row);
        });
        t.append(head, body); el(id).replaceChildren(t);
    }
    function detailLink(text, action) {
        const button = node('button', text, 'kafka-topic-link');
        button.type = 'button'; button.addEventListener('click', action);
        return button;
    }
    function lagTable(id, lags) {
        table(id, ['토픽 / 파티션', '소비자 그룹', '커밋 위치', '최신 위치', '지연 건수'],
            lags.map(p => [`${p.topic} / ${p.partition}`, p.group, p.committedOffset, p.endOffset, count(p.lag)]));
    }
    function renderTopics() {
        const search = el('topic-search').value.trim().toLowerCase();
        const topics = (snapshot?.topics || []).filter(topic => topic.topic.toLowerCase().includes(search));
        table('topics', ['토픽 · 상세 보기', '파티션 / 설정', '복제 수', '복제 지연 파티션', '보관 기간', '상태'],
            topics.map(topic => [
                detailLink(topic.topic, () => { selectedTopic = topic.topic; renderTopics(); }),
                topic.partitionDetails?.length ? `${topic.partitions} / ${topic.configuredPartitions}` : '미확인',
                topic.partitionDetails?.length ? topic.replicas : '미확인',
                topic.partitionDetails?.length ? topic.underReplicatedPartitions : '미확인',
                topic.retentionMs == null ? '미확인' : Number(topic.retentionMs) < 0 ? '무제한' : `${Number(topic.retentionMs) / 86400000}일`,
                topic.error || '조회 완료'
            ]), search ? '검색 결과가 없습니다.' : '토픽 정보를 확인할 수 없습니다.');
        el('topic-list').hidden = selectedTopic != null;
        el('topic-detail').hidden = selectedTopic == null;
        if (selectedTopic == null) return;
        const topic = (snapshot?.topics || []).find(value => value.topic === selectedTopic);
        el('topic-title').textContent = selectedTopic;
        el('topic-error').textContent = topic?.error || (topic ? '' : '토픽 정보를 확인할 수 없습니다. 새로고침해 주세요.');
        table('partitions', ['파티션', '리더', '복제본', '동기화된 복제본', '시작 오프셋', '최신 오프셋', '상태'],
            (topic?.partitionDetails || []).map(p => [p.partition, p.leader, joined(p.replicas), joined(p.inSyncReplicas),
                p.startOffset, p.endOffset, p.leader == null ? '리더 없음' : p.inSyncReplicas.length < p.replicas.length ? '복제 지연' : '정상']));
        table('configs', ['설정', '현재 값'], Object.entries(topic?.configs || {}).sort(([a], [b]) => a.localeCompare(b)),
            '설정을 조회할 수 없거나 공개 가능한 설정이 없습니다.');
    }
    function renderConsumers() {
        const search = el('consumer-search').value.trim().toLowerCase();
        const groups = (snapshot?.consumerGroups || []).filter(group => group.groupId.toLowerCase().includes(search));
        table('consumers', ['그룹 · 상세 보기', '상태', '연결된 소비자', '코디네이터', '할당 방식', '지연'],
            groups.map(group => [
                detailLink(group.groupId, () => { selectedConsumer = group.groupId; renderConsumers(); }),
                groupStates[group.state] || group.state, group.memberCount, group.coordinatorId, group.assignor || '-', count(group.totalLag)
            ]), search ? '검색 결과가 없습니다.' : '소비자 그룹 정보를 확인할 수 없습니다.');
        el('consumer-list').hidden = selectedConsumer != null;
        el('consumer-detail').hidden = selectedConsumer == null;
        if (selectedConsumer == null) return;
        const group = (snapshot?.consumerGroups || []).find(value => value.groupId === selectedConsumer);
        el('consumer-title').textContent = selectedConsumer;
        el('consumer-error').textContent = group?.error || (group ? '' : '그룹 정보를 확인할 수 없습니다. 새로고침해 주세요.');
        table('members', ['소비자 ID', '클라이언트 ID', '접속 호스트', '할당된 파티션'],
            (group?.members || []).map(member => [member.consumerId, member.clientId, member.host, joined(member.assignments)]),
            group?.memberCount === 0 ? '현재 연결된 소비자가 없습니다.' : '연결된 소비자 정보를 확인할 수 없습니다.');
        lagTable('consumer-lags', group?.lags || []);
    }
    function renderOverview(data) {
        el('cluster').textContent = `클러스터 ${data?.clusterId ?? '미확인'} · 컨트롤러 ${data?.controllerId ?? '미확인'} · 읽기 전용`;
        const brokers = data?.brokers || [];
        const metrics = [
            ['조회 브로커', brokers.length ? `${brokers.length}대` : '미확인'],
            ['운영 토픽', data?.topics?.length ? `${data.topics.filter(t => t.partitionDetails?.length).length} / ${data.topics.length}개` : '미확인'],
            ['소비 지연', count(data?.totalLag)], ['Outbox 대기', count(data?.outboxPending)],
            ['최장 발행 대기', data?.oldestOutboxSeconds == null ? '미확인' : `${data.oldestOutboxSeconds}초`],
            ['Outbox 최종 실패', count(data?.outboxFailed)], ['소비 실패 미처리', count(data?.unresolvedDeadLetters)],
            ['재발행 대기', count(data?.replayPending)]
        ];
        el('summary').replaceChildren(...metrics.map(([label, value]) => {
            const card = node('div', '', 'kafka-metric'); card.append(node('span', label), node('strong', value)); return card;
        }));
        const warnings = data?.warnings || [];
        el('warnings').replaceChildren(...warnings.map(warning => node('li', warning)));
        el('warning-box').hidden = warnings.length === 0;
        table('brokers', ['브로커 ID', '호스트', '포트', '랙', '역할', '운영 토픽 리더', '운영 토픽 복제본'],
            brokers.map(broker => [broker.id, broker.host, broker.port, broker.rack || '-', broker.controller ? '컨트롤러' : '브로커',
                broker.leaderPartitions, broker.replicaPartitions]));
        lagTable('lags', data?.lags || []);
        renderTopics(); renderConsumers();
    }
    async function overview() {
        if (overviewLoading) return;
        overviewLoading = true; controls();
        try {
            const data = await request('/api/admin/kafka/overview');
            snapshot = data; renderOverview(data);
            el('status').textContent = `${time(data.observedAt)} 기준 · ${data.brokerError ? '일부 Kafka 조회 실패' : '조회 완료'}`;
        } catch (error) {
            snapshot = null; renderOverview(null);
            el('status').textContent = error.message;
        } finally { overviewLoading = false; controls(); }
    }
    function controls() {
        el('prev').disabled = listLoading || replaying || page === 0;
        el('next').disabled = listLoading || replaying || next == null;
        el('filter').disabled = listLoading || replaying;
        el('refresh').disabled = listLoading || replaying || overviewLoading;
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
    el('refresh').addEventListener('click', () => { overview(); load(page, cursors[page]); });
    const timer = setInterval(() => { if (!document.hidden && !replaying && el('auto-refresh').checked) { overview(); if (page === 0) load(); } }, 15000);
    window.addEventListener('pagehide', () => clearInterval(timer));
    const tabs = ['overview', 'brokers', 'topics', 'consumers', 'failures'];
    function selectTab(selected, focus = false) {
        tabs.forEach(tab => {
            const active = tab === selected;
            el('tab-' + tab).setAttribute('aria-selected', String(active));
            el('tab-' + tab).tabIndex = active ? 0 : -1;
            el('panel-' + tab).hidden = !active;
        });
        if (focus) el('tab-' + selected).focus();
    }
    tabs.forEach((tab, index) => {
        el('tab-' + tab).addEventListener('click', () => selectTab(tab));
        el('tab-' + tab).addEventListener('keydown', event => {
            let next;
            if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
            if (event.key === 'Home') next = 0;
            if (event.key === 'End') next = tabs.length - 1;
            if (next != null) { event.preventDefault(); selectTab(tabs[next], true); }
        });
    });
    el('topic-search').addEventListener('input', renderTopics);
    el('consumer-search').addEventListener('input', renderConsumers);
    el('topic-back').addEventListener('click', () => { selectedTopic = null; renderTopics(); el('topic-search').focus(); });
    el('consumer-back').addEventListener('click', () => { selectedConsumer = null; renderConsumers(); el('consumer-search').focus(); });
    selectTab('overview');
    overview(); load();
})();
