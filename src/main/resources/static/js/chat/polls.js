(function () {
    const room = document.getElementById('room').value;
    const overlay = document.getElementById('chat-details-overlay');
    const list = document.getElementById('chat-polls-list');
    const status = document.getElementById('chat-polls-status');
    const form = document.getElementById('chat-poll-form');
    const api = `/api/chat/rooms/${encodeURIComponent(room)}/polls`;
    let requestVersion = 0;
    let saving = false;

    async function request(path, method, payload) {
        const options = {method, credentials: 'include', cache: 'no-store'};
        if (payload) {
            options.headers = {'Content-Type': 'application/json'};
            options.body = JSON.stringify(payload);
        }
        const response = await fetch(api + path, options);
        if (response.status === 204) return;
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || '투표를 처리하지 못했습니다.');
        return data;
    }

    function node(tag, className, text) {
        const element = document.createElement(tag);
        element.className = className;
        element.textContent = text;
        return element;
    }

    function render(poll) {
        const card = node('article', 'chat-poll-card', '');
        card.append(node('strong', '', poll.title));
        const ends = new Date(poll.endsAt).toLocaleString('ko-KR');
        card.append(node('p', 'chat-details-muted', `${poll.closed ? '투표 종료' : '진행 중'} · ${ends}`));
        for (const [index, label] of poll.options.entries()) {
            const button = node('button', 'btn btn-outline-primary btn-sm',
                `${label} · ${poll.counts[index]}표${poll.myChoice === index ? ' (내 선택)' : ''}`);
            button.type = 'button';
            button.disabled = poll.closed;
            button.setAttribute('aria-pressed', String(poll.myChoice === index));
            button.addEventListener('click', () => mutate(`/${poll.id}/vote`, 'PUT', {optionIndex: index}, button));
            card.append(button);
        }
        return card;
    }

    async function load() {
        const version = ++requestVersion;
        try {
            const polls = await request('', 'GET');
            if (version !== requestVersion || overlay.hidden) return;
            list.replaceChildren(...polls.map(render));
            status.textContent = polls.length ? '최근 투표 최대 20개를 표시합니다.' : '아직 투표가 없어요.';
        } catch (error) {
            if (version !== requestVersion) return;
            list.replaceChildren();
            status.textContent = error.message;
        }
    }

    async function mutate(path, method, payload, button) {
        if (saving) return;
        saving = true;
        button.disabled = true;
        try {
            await request(path, method, payload);
            if (method === 'POST') {
                form.reset();
                document.getElementById('chat-polls-create').open = false;
            }
            await load();
        } catch (error) {
            status.textContent = error.message;
        } finally {
            saving = false;
            button.disabled = false;
        }
    }

    form.addEventListener('submit', event => {
        event.preventDefault();
        const title = document.getElementById('chat-poll-title').value.trim();
        const options = document.getElementById('chat-poll-options').value.split('\n').map(line => line.trim()).filter(Boolean);
        const durationHours = Number(document.getElementById('chat-poll-duration').value);
        mutate('', 'POST', {title, options, durationHours}, form.querySelector('button[type="submit"]'));
    });
    document.getElementById('chat-details-open').addEventListener('click', load);
    document.getElementById('chat-polls-refresh').addEventListener('click', load);
    window.addEventListener('chat:polls-updated', () => { if (!overlay.hidden && !saving) load(); });
})();
