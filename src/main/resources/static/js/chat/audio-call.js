(function () {
    const startButton = document.getElementById('audio-call-start');
    const panel = document.getElementById('audio-call-panel');
    const statusText = document.getElementById('audio-call-status');
    const acceptButton = document.getElementById('audio-call-accept');
    const rejectButton = document.getElementById('audio-call-reject');
    const muteButton = document.getElementById('audio-call-mute');
    const hangupButton = document.getElementById('audio-call-hangup');
    const remoteAudio = document.getElementById('audio-call-remote');
    const connectionText = document.getElementById('audio-call-connection');
    const muteStatus = document.getElementById('audio-call-mute-status');
    const durationText = document.getElementById('audio-call-duration');
    const deviceControls = document.getElementById('audio-call-devices');
    const inputSelect = document.getElementById('audio-call-input');
    const outputSelect = document.getElementById('audio-call-output');

    if (!panel) return;

    let rtcConfig = null;
    let stompClient = null;
    let subscription = null;
    let peerConnection = null;
    let localStream = null;
    let activeCallId = null;
    let incomingCaller = '';
    let pendingCandidates = [];
    let callTimeout = null;
    let callNoticeTimeout = null;
    let connectionFailureTimeout = null;
    let keepAliveInterval = null;
    let incomingCallTimeout = null;
    let durationInterval = null;
    let connectedAt = null;
    let ringtoneContext = null;
    let ringtoneInterval = null;
    let wakeLock = null;
    let isCaller = false;
    let autoAcceptCallId = new URLSearchParams(window.location.search).get('acceptAudioCall');
    const tabId = createCallId();
    const callChannel = typeof BroadcastChannel !== 'undefined'
        ? new BroadcastChannel('kwanwoo-audio-call')
        : null;

    function onStompConnected(connectedClient) {
        stompClient = connectedClient;
        if (!subscription) {
            subscription = stompClient.subscribe('/user/queue/audio-call', frame => {
                onSignalReceived(frame).catch(handleRtcError);
            });
        }
        restoreIncomingCall().catch(() => {});
    }

    function onStompDisconnected() {
        stompClient = null;
        subscription = null;
        if (activeCallId) {
            endCallWithMessage('채팅 서버 연결이 끊어져 통화가 종료되었습니다.');
        }
    }

    function sendSignal(type, extra) {
        if (!stompClient || !stompClient.connected) {
            showError('채팅 서버에 연결되지 않았습니다.');
            return false;
        }
        stompClient.send('/api/audio/signal', {}, JSON.stringify(Object.assign({
            callId: activeCallId,
            roomId: roomId,
            type: type
        }, extra || {})));
        return true;
    }

    async function startCall() {
        if (activeCallId) return;
        try {
            await ensureLocalAudio();
            activeCallId = createCallId();
            isCaller = true;
            showPanel('상대방의 응답을 기다리는 중입니다.');
            setConnectionStatus('응답 대기 중', 'connecting');
            setCallControls(false);
            hangupButton.classList.remove('is-hidden');
            if (!sendSignal('CALL')) {
                cleanupCall();
                return;
            }
            callTimeout = window.setTimeout(() => {
                if (!peerConnection && activeCallId) {
                    sendSignal('HANGUP');
                    showError('상대방이 응답하지 않았습니다.');
                    window.setTimeout(cleanupCall, 1500);
                }
            }, 30000);
        } catch (error) {
            showError(microphoneErrorMessage(error));
        }
    }

    async function acceptCall() {
        try {
            await ensureLocalAudio();
            clearCallTimeout();
            acceptButton.classList.add('is-hidden');
            rejectButton.classList.add('is-hidden');
            hangupButton.classList.remove('is-hidden');
            statusText.innerText = '연결 중입니다.';
            stopRingtone();
            clearIncomingCallTimeout();
            setConnectionStatus('연결 중', 'connecting');
            sendSignal('ACCEPT');
        } catch (error) {
            sendSignal('REJECT');
            showError(microphoneErrorMessage(error));
            window.setTimeout(cleanupCall, 1500);
        }
    }

    function rejectCall() {
        sendSignal('REJECT');
        stopRingtone();
        cleanupCall();
    }

    async function onSignalReceived(frame) {
        const signal = JSON.parse(frame.body);
        if (signal.roomId !== roomId) return;
        if (signal.error) {
            showError(signal.error);
            window.setTimeout(cleanupCall, 1800);
            return;
        }

        if (signal.type === 'CALL') {
            displayIncomingCall(signal);
            return;
        }
        if (signal.callId !== activeCallId) return;

        switch (signal.type) {
            case 'ACCEPT':
                clearCallTimeout();
                statusText.innerText = '연결 중입니다.';
                setConnectionStatus('연결 중', 'connecting');
                await createAndSendOffer();
                break;
            case 'ACCEPTED':
                notifyCallAcceptedInThisTab();
                break;
            case 'REJECT':
                showError('상대방이 통화를 거절했습니다.');
                window.setTimeout(cleanupCall, 1500);
                break;
            case 'OFFER':
                await receiveOffer(signal.sdp);
                break;
            case 'ANSWER':
                await receiveAnswer(signal.sdp);
                break;
            case 'ICE_CANDIDATE':
                await receiveCandidate(signal);
                break;
            case 'HANGUP':
                endCallWithMessage('상대방이 통화를 종료했습니다.');
                break;
            case 'DISCONNECTED':
                endCallWithMessage('상대방의 연결이 끊어져 통화가 종료되었습니다.');
                break;
            case 'ADMIN_TERMINATED':
                endCallWithMessage('관리자에 의해 통화가 종료되었습니다.');
                break;
        }
    }

    async function ensureLocalAudio() {
        await ensureRtcConfig();
        if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
            throw new Error('UNSUPPORTED');
        }
        if (!localStream) {
            const selectedDeviceId = inputSelect ? inputSelect.value : '';
            localStream = await navigator.mediaDevices.getUserMedia({
                audio: selectedDeviceId ? {deviceId: {exact: selectedDeviceId}} : true,
                video: false
            });
            await populateAudioDevices();
        }
        return localStream;
    }

    async function restoreIncomingCall() {
        if (activeCallId) return;
        const response = await fetch(
            `/api/chat/audio/incoming?roomId=${encodeURIComponent(roomId)}`,
            {method: 'GET', credentials: 'include'}
        );
        if (response.status === 204 || !response.ok) return;
        displayIncomingCall(await response.json());
    }

    function displayIncomingCall(signal) {
        if (activeCallId || !signal || !signal.callId) return;
        activeCallId = signal.callId;
        isCaller = false;
        incomingCaller = signal.senderName || signal.senderEmail || '상대방';
        showPanel(`${incomingCaller}님의 음성 통화입니다.`);
        setConnectionStatus('수신 중', 'connecting');
        acceptButton.classList.remove('is-hidden');
        rejectButton.classList.remove('is-hidden');
        startRingtone();
        startIncomingCallTimeout();
        if (autoAcceptCallId === signal.callId) {
            autoAcceptCallId = null;
            const url = new URL(window.location.href);
            url.searchParams.delete('acceptAudioCall');
            window.history.replaceState({}, '', url.pathname + url.search);
            window.setTimeout(() => acceptCall(), 0);
        }
    }

    async function ensureRtcConfig() {
        if (rtcConfig) return rtcConfig;
        const response = await fetch('/api/chat/audio/ice-servers', {
            method: 'GET',
            credentials: 'include'
        });
        if (!response.ok) throw new Error('ICE_CONFIG');
        const data = await response.json();
        if (!Array.isArray(data.iceServers) || data.iceServers.length === 0) {
            throw new Error('ICE_CONFIG');
        }
        rtcConfig = {iceServers: data.iceServers};
        return rtcConfig;
    }

    function createPeerConnection() {
        if (peerConnection) return peerConnection;
        peerConnection = new RTCPeerConnection(rtcConfig);
        localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream));
        peerConnection.ontrack = event => {
            remoteAudio.srcObject = event.streams[0];
            remoteAudio.play().catch(() => {});
        };
        peerConnection.onicecandidate = event => {
            if (!event.candidate) return;
            sendSignal('ICE_CANDIDATE', {
                candidate: event.candidate.candidate,
                sdpMid: event.candidate.sdpMid,
                sdpMLineIndex: event.candidate.sdpMLineIndex
            });
        };
        peerConnection.onconnectionstatechange = () => {
            if (!peerConnection) return;
            if (peerConnection.connectionState === 'connected') {
                clearConnectionFailureTimeout();
                statusText.innerText = `${incomingCaller || '상대방'}님과 통화 중`;
                setConnectionStatus('연결됨', 'connected');
                setCallControls(true);
                startKeepAlive();
                startDurationTimer();
                requestWakeLock();
            } else if (['failed', 'disconnected'].includes(peerConnection.connectionState)) {
                beginConnectionRecovery();
            }
        };
        return peerConnection;
    }

    async function createAndSendOffer() {
        const connection = createPeerConnection();
        const offer = await connection.createOffer();
        await connection.setLocalDescription(offer);
        sendSignal('OFFER', {sdp: offer.sdp});
    }

    async function createAndSendRestartOffer() {
        if (!peerConnection || !isCaller) return;
        const offer = await peerConnection.createOffer({iceRestart: true});
        await peerConnection.setLocalDescription(offer);
        sendSignal('OFFER', {sdp: offer.sdp});
    }

    async function receiveOffer(sdp) {
        const connection = createPeerConnection();
        await connection.setRemoteDescription({type: 'offer', sdp: sdp});
        await flushPendingCandidates();
        const answer = await connection.createAnswer();
        await connection.setLocalDescription(answer);
        sendSignal('ANSWER', {sdp: answer.sdp});
    }

    async function receiveAnswer(sdp) {
        const connection = createPeerConnection();
        await connection.setRemoteDescription({type: 'answer', sdp: sdp});
        await flushPendingCandidates();
    }

    async function receiveCandidate(signal) {
        const candidate = {
            candidate: signal.candidate,
            sdpMid: signal.sdpMid,
            sdpMLineIndex: signal.sdpMLineIndex
        };
        if (!peerConnection || !peerConnection.remoteDescription) {
            pendingCandidates.push(candidate);
            return;
        }
        await peerConnection.addIceCandidate(candidate);
    }

    async function flushPendingCandidates() {
        while (pendingCandidates.length > 0) {
            await peerConnection.addIceCandidate(pendingCandidates.shift());
        }
    }

    function hangup() {
        if (activeCallId) sendSignal('HANGUP');
        cleanupCall();
    }

    function toggleMute() {
        if (!localStream) return;
        const audioTrack = localStream.getAudioTracks()[0];
        if (!audioTrack) return;
        audioTrack.enabled = !audioTrack.enabled;
        muteButton.innerText = audioTrack.enabled ? '음소거' : '음소거 해제';
        muteStatus.classList.toggle('is-hidden', audioTrack.enabled);
    }

    function cleanupCall() {
        clearCallTimeout();
        clearIncomingCallTimeout();
        clearConnectionFailureTimeout();
        stopKeepAlive();
        stopDurationTimer();
        stopRingtone();
        releaseWakeLock();
        if (callNoticeTimeout) window.clearTimeout(callNoticeTimeout);
        callNoticeTimeout = null;
        if (peerConnection) peerConnection.close();
        if (localStream) localStream.getTracks().forEach(track => track.stop());
        remoteAudio.srcObject = null;
        peerConnection = null;
        localStream = null;
        activeCallId = null;
        incomingCaller = '';
        isCaller = false;
        pendingCandidates = [];
        muteButton.innerText = '음소거';
        muteStatus.classList.add('is-hidden');
        durationText.classList.add('is-hidden');
        durationText.textContent = '00:00';
        deviceControls.classList.add('is-hidden');
        setConnectionStatus('대기 중', 'idle');
        panel.classList.add('is-hidden');
        [acceptButton, rejectButton, muteButton, hangupButton].forEach(button => button.classList.add('is-hidden'));
        if (startButton) startButton.disabled = false;
    }

    function endCallWithMessage(message) {
        cleanupCall();
        showError(message);
        callNoticeTimeout = window.setTimeout(cleanupCall, 1500);
    }

    function showPanel(message) {
        statusText.innerText = message;
        panel.classList.remove('is-hidden');
        if (startButton) startButton.disabled = true;
    }

    function showError(message) {
        showPanel(message);
        setConnectionStatus('종료됨', 'error');
        acceptButton.classList.add('is-hidden');
        rejectButton.classList.add('is-hidden');
        muteButton.classList.add('is-hidden');
        hangupButton.classList.add('is-hidden');
    }

    function setCallControls(connected) {
        muteButton.classList.toggle('is-hidden', !connected);
        hangupButton.classList.remove('is-hidden');
    }

    function clearCallTimeout() {
        if (callTimeout) window.clearTimeout(callTimeout);
        callTimeout = null;
    }

    function beginConnectionRecovery() {
        if (connectionFailureTimeout || !activeCallId) return;
        statusText.innerText = '음성 연결을 복구하는 중입니다.';
        setConnectionStatus('재연결 중', 'recovering');
        setCallControls(false);

        if (isCaller) {
            createAndSendRestartOffer().catch(handleRtcError);
        }

        connectionFailureTimeout = window.setTimeout(() => {
            if (!peerConnection || peerConnection.connectionState === 'connected') return;
            sendSignal('HANGUP');
            endCallWithMessage('음성 연결을 복구하지 못해 통화가 종료되었습니다.');
        }, 10000);
    }

    function clearConnectionFailureTimeout() {
        if (connectionFailureTimeout) window.clearTimeout(connectionFailureTimeout);
        connectionFailureTimeout = null;
    }

    function startKeepAlive() {
        if (keepAliveInterval) return;
        keepAliveInterval = window.setInterval(() => {
            if (activeCallId && peerConnection && peerConnection.connectionState === 'connected') {
                sendSignal('KEEP_ALIVE');
            }
        }, 30000);
    }

    function stopKeepAlive() {
        if (keepAliveInterval) window.clearInterval(keepAliveInterval);
        keepAliveInterval = null;
    }

    function handleRtcError() {
        if (activeCallId) sendSignal('HANGUP');
        endCallWithMessage('음성 연결을 처리하는 중 오류가 발생해 통화가 종료되었습니다.');
    }

    function notifyCallAcceptedInThisTab() {
        if (!callChannel || !activeCallId) return;
        callChannel.postMessage({type: 'ACCEPTED', callId: activeCallId, tabId: tabId});
    }

    function startIncomingCallTimeout() {
        clearIncomingCallTimeout();
        incomingCallTimeout = window.setTimeout(() => {
            if (!activeCallId || peerConnection) return;
            stopRingtone();
            showError(`${incomingCaller || '상대방'}님의 부재중 통화가 있습니다.`);
            callNoticeTimeout = window.setTimeout(cleanupCall, 3000);
        }, 30000);
    }

    function clearIncomingCallTimeout() {
        if (incomingCallTimeout) window.clearTimeout(incomingCallTimeout);
        incomingCallTimeout = null;
    }

    function startRingtone() {
        stopRingtone();
        if (navigator.vibrate) navigator.vibrate([250, 150, 250]);
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextClass) return;
        try {
            ringtoneContext = new AudioContextClass();
            const playTone = () => {
                if (!ringtoneContext) return;
                const oscillator = ringtoneContext.createOscillator();
                const gain = ringtoneContext.createGain();
                oscillator.frequency.value = 440;
                gain.gain.setValueAtTime(0.0001, ringtoneContext.currentTime);
                gain.gain.exponentialRampToValueAtTime(0.12, ringtoneContext.currentTime + 0.03);
                gain.gain.exponentialRampToValueAtTime(0.0001, ringtoneContext.currentTime + 0.55);
                oscillator.connect(gain).connect(ringtoneContext.destination);
                oscillator.start();
                oscillator.stop(ringtoneContext.currentTime + 0.6);
            };
            ringtoneContext.resume().then(playTone).catch(() => {});
            ringtoneInterval = window.setInterval(playTone, 1800);
        } catch (error) {
            ringtoneContext = null;
        }
    }

    function stopRingtone() {
        if (ringtoneInterval) window.clearInterval(ringtoneInterval);
        ringtoneInterval = null;
        if (ringtoneContext) ringtoneContext.close().catch(() => {});
        ringtoneContext = null;
        if (navigator.vibrate) navigator.vibrate(0);
    }

    function startDurationTimer() {
        if (!connectedAt) connectedAt = Date.now();
        if (durationInterval) return;
        durationText.classList.remove('is-hidden');
        updateDuration();
        durationInterval = window.setInterval(updateDuration, 1000);
    }

    function updateDuration() {
        if (!connectedAt) return;
        const seconds = Math.max(0, Math.floor((Date.now() - connectedAt) / 1000));
        const hours = Math.floor(seconds / 3600);
        const minutes = Math.floor((seconds % 3600) / 60);
        const remainingSeconds = seconds % 60;
        durationText.textContent = hours > 0
            ? `${padTime(hours)}:${padTime(minutes)}:${padTime(remainingSeconds)}`
            : `${padTime(minutes)}:${padTime(remainingSeconds)}`;
    }

    function stopDurationTimer() {
        if (durationInterval) window.clearInterval(durationInterval);
        durationInterval = null;
        connectedAt = null;
    }

    function padTime(value) {
        return String(value).padStart(2, '0');
    }

    function setConnectionStatus(label, state) {
        connectionText.textContent = label;
        connectionText.dataset.state = state;
    }

    async function populateAudioDevices() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) return;
        const devices = await navigator.mediaDevices.enumerateDevices();
        fillDeviceSelect(inputSelect, devices.filter(device => device.kind === 'audioinput'), '마이크');
        if (typeof remoteAudio.setSinkId === 'function') {
            fillDeviceSelect(outputSelect, devices.filter(device => device.kind === 'audiooutput'), '스피커');
        } else {
            outputSelect.disabled = true;
            outputSelect.innerHTML = '<option>시스템 기본 스피커</option>';
        }
        deviceControls.classList.remove('is-hidden');
    }

    function fillDeviceSelect(select, devices, fallbackLabel) {
        const previous = select.value;
        select.innerHTML = '';
        devices.forEach((device, index) => {
            const option = document.createElement('option');
            option.value = device.deviceId;
            option.textContent = device.label || `${fallbackLabel} ${index + 1}`;
            select.appendChild(option);
        });
        if (devices.some(device => device.deviceId === previous)) select.value = previous;
    }

    async function changeInputDevice() {
        if (!inputSelect.value || !navigator.mediaDevices) return;
        const stream = await navigator.mediaDevices.getUserMedia({
            audio: {deviceId: {exact: inputSelect.value}},
            video: false
        });
        const newTrack = stream.getAudioTracks()[0];
        const oldTrack = localStream ? localStream.getAudioTracks()[0] : null;
        if (oldTrack) newTrack.enabled = oldTrack.enabled;
        if (peerConnection) {
            const sender = peerConnection.getSenders().find(item => item.track && item.track.kind === 'audio');
            if (sender) await sender.replaceTrack(newTrack);
        }
        if (localStream) localStream.getTracks().forEach(track => track.stop());
        localStream = new MediaStream([newTrack]);
    }

    async function changeOutputDevice() {
        if (typeof remoteAudio.setSinkId !== 'function') return;
        await remoteAudio.setSinkId(outputSelect.value);
    }

    async function requestWakeLock() {
        if (!('wakeLock' in navigator) || document.visibilityState !== 'visible' || wakeLock) return;
        try {
            wakeLock = await navigator.wakeLock.request('screen');
            wakeLock.addEventListener('release', () => { wakeLock = null; });
        } catch (error) {
            wakeLock = null;
        }
    }

    function releaseWakeLock() {
        if (wakeLock) wakeLock.release().catch(() => {});
        wakeLock = null;
    }

    function createCallId() {
        if (window.crypto && window.crypto.randomUUID) return window.crypto.randomUUID();
        return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    }

    function microphoneErrorMessage(error) {
        if (error && error.message === 'ICE_CONFIG') return '통화 서버 설정을 불러올 수 없습니다.';
        if (error && error.message === 'UNSUPPORTED') return '이 브라우저는 음성 통화를 지원하지 않습니다.';
        if (error && error.name === 'NotAllowedError') return '마이크 권한이 필요합니다.';
        if (error && error.name === 'NotFoundError') return '사용 가능한 마이크를 찾을 수 없습니다.';
        return '마이크를 시작할 수 없습니다.';
    }

    if (startButton) startButton.addEventListener('click', startCall);
    acceptButton.addEventListener('click', acceptCall);
    rejectButton.addEventListener('click', rejectCall);
    muteButton.addEventListener('click', toggleMute);
    hangupButton.addEventListener('click', hangup);
    inputSelect.addEventListener('change', () => changeInputDevice().catch(handleRtcError));
    outputSelect.addEventListener('change', () => changeOutputDevice().catch(() => {
        setConnectionStatus('스피커 변경 실패', 'error');
    }));
    if (callChannel) {
        callChannel.onmessage = event => {
            const message = event.data || {};
            if (message.type !== 'ACCEPTED' || message.tabId === tabId
                    || message.callId !== activeCallId) return;
            cleanupCall();
        };
    }
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState !== 'visible' || !activeCallId) return;
        remoteAudio.play().catch(() => {});
        if (peerConnection && ['failed', 'disconnected'].includes(peerConnection.connectionState)) {
            beginConnectionRecovery();
        }
        if (peerConnection && peerConnection.connectionState === 'connected') requestWakeLock();
    });
    window.addEventListener('pagehide', event => {
        if (event.persisted) {
            releaseWakeLock();
            return;
        }
        if (activeCallId) sendSignal('HANGUP');
        cleanupCall();
        if (callChannel) callChannel.close();
    });

    window.audioCallClient = {
        onStompConnected: onStompConnected,
        onStompDisconnected: onStompDisconnected
    };
    if (typeof client !== 'undefined' && client.connected) onStompConnected(client);
})();
