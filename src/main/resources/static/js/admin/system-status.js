const systemUsageWindow = 30 * 60_000;
const systemStatusRefresh = 30_000;
const systemUsageTime = new Intl.DateTimeFormat('ko-KR', {hour: '2-digit', minute: '2-digit', hourCycle: 'h23'});

window.addEventListener('DOMContentLoaded', () => {
    document.getElementById('system-disk-details').addEventListener('click', () => openSystemDetails('disk'));
    document.getElementById('system-memory-details').addEventListener('click', () => openSystemDetails('processes'));
    loadSystemStatus();
    setInterval(loadSystemStatus, systemStatusRefresh);
});

async function loadSystemStatus() {
    const errorMessage = document.getElementById('system-status-error');
    try {
        const response = await fetch('/api/admin/system/status', {credentials: 'include'});
        if (!response.ok) throw new Error('서버 상태 조회 실패');
        const data = await response.json();
        if (data.result !== 1) throw new Error('서버 상태 조회 실패');

        drawSystemStatus(data);
        errorMessage.hidden = true;
    } catch (error) {
        console.error(error);
        errorMessage.textContent = '서버 상태를 불러오지 못했습니다. 잠시 후 다시 시도합니다.';
        errorMessage.hidden = false;
    }
}

function drawSystemStatus({sampledAt, resourceHistory, cpu, memory, disk, jvm, traffic, queues}) {
    const updated = document.getElementById('system-status-updated');
    const sampleTime = new Date(sampledAt);
    updated.dateTime = sampleTime.toISOString();
    updated.textContent = `최근 측정 ${sampleTime.toLocaleTimeString('ko-KR')}`;

    drawResourceTrend(resourceHistory, sampledAt);
    drawDiskUsage(disk);

    drawUsageRing('cpu', cpu.systemPercent);
    drawUsageRing('memory', memory.usedPercent);
    drawUsageRing('heap', jvm.heapUsedPercent);
    setSystemText('cpu-cores', `${cpu.cores} 코어`);
    setSystemText('cpu-process', `앱 프로세스 ${formatUsagePercent(cpu.processPercent)}`);
    setSystemText('memory-capacity', memory.usedPercent === null ? '—' : formatCapacity(memory.usedBytes, memory.totalBytes));
    setSystemText('heap-capacity', formatCapacity(jvm.heapUsedBytes, jvm.heapMaxBytes));
    setSystemText('uptime', jvm.uptime);
    setSystemText('started-at', `시작 ${formatDateTime(jvm.startedAt)}`);
    setSystemText('sessions', `${traffic.activeSessions}명`);
    setSystemText('websockets', `WebSocket ${traffic.activeWebSockets}개`);
    setSystemText('queue', `${queues.kafkaPending}건`);
    setSystemText('failed', `Kafka 실패 ${queues.kafkaDeadLetters}건`);
}

function drawDiskUsage(disk) {
    const percent = disk.totalBytes > 0 ? disk.usedPercent : null;
    setSystemText('disk-percent', formatUsagePercent(percent));
    setSystemText('disk-capacity', percent === null ? '—' : `전체 ${formatBytes(disk.totalBytes)}`);
    setSystemText('disk-used', percent === null ? '—' : formatBytes(disk.usedBytes));
    setSystemText('disk-free', percent === null ? '—' : formatBytes(disk.totalBytes - disk.usedBytes));

    const meter = document.getElementById('system-disk-meter');
    meter.style.setProperty('--usage', `${percent ?? 0}%`);
    const warning = percent >= 80;
    const note = document.getElementById('system-disk-note');
    note.classList.toggle('is-warning', warning);
    note.textContent = warning ? '사용량 주의 · 기준 80%' : '주의 기준 80%';
}

function drawUsageRing(metric, percent) {
    document.getElementById(`system-${metric}-ring`).style.setProperty('--usage', `${percent ?? 0}%`);
    setSystemText(`${metric}-percent`, formatUsagePercent(percent));
}

function drawResourceTrend(history, end) {
    const start = end - systemUsageWindow;
    setSystemText('usage-start', systemUsageTime.format(start));
    setSystemText('usage-middle', systemUsageTime.format((start + end) / 2));
    setSystemText('usage-end', systemUsageTime.format(end));

    const points = history.filter(point => point.sampledAt >= start && point.sampledAt <= end);
    const cpuCount = drawUsageSeries('cpu', points, start);
    const memoryCount = drawUsageSeries('memory', points, start);
    document.getElementById('system-usage-note').hidden = Math.max(cpuCount, memoryCount) >= 2;
}

function drawUsageSeries(metric, samples, start) {
    const path = [];
    let lastTime = -Infinity;
    let lastPoint;
    for (const sample of samples) {
        const percent = sample[`${metric}Percent`];
        if (!Number.isFinite(percent)) {
            lastTime = -Infinity;
            continue;
        }
        const x = ((sample.sampledAt - start) / systemUsageWindow * 1000).toFixed(1);
        const y = (160 - percent * 1.6).toFixed(1);
        const command = sample.sampledAt - lastTime <= 2 * systemStatusRefresh ? 'L' : 'M';
        path.push(`${command}${x} ${y}`);
        lastPoint = {x, y};
        lastTime = sample.sampledAt;
    }

    document.getElementById(`system-${metric}-trend`).setAttribute('d', path.join(' '));
    const dot = document.getElementById(`system-${metric}-point`);
    dot.setAttribute('visibility', lastPoint ? 'visible' : 'hidden');
    if (lastPoint) {
        dot.setAttribute('cx', lastPoint.x);
        dot.setAttribute('cy', lastPoint.y);
    }
    return path.length;
}

function setSystemText(name, value) {
    document.getElementById(`system-${name}`).textContent = value;
}

function formatUsagePercent(value) {
    return Number.isFinite(value) ? `${value}%` : '—';
}

function formatCapacity(used, total) {
    return `${formatBytes(used)} / ${formatBytes(total)}`;
}
