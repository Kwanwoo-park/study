(function () {
    const startButton = document.getElementById('audio-call-start');
    const panel = document.getElementById('audio-call-panel');
    const statusText = document.getElementById('audio-call-status');
    const acceptButton = document.getElementById('audio-call-accept');
    const rejectButton = document.getElementById('audio-call-reject');
    const muteButton = document.getElementById('audio-call-mute');
    const hangupButton = document.getElementById('audio-call-hangup');
    const remoteAudioContainer = document.getElementById('audio-call-remotes');
    const participantList = document.getElementById('audio-call-participants');
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
    let localStream = null;
    let activeCallId = null;
    let incomingCaller = '';
    let callTimeout = null;
    let callNoticeTimeout = null;
    let keepAliveInterval = null;
    let incomingCallTimeout = null;
    let durationInterval = null;
    let connectedAt = null;
    let ringtoneContext = null;
    let ringtoneInterval = null;
    let wakeLock = null;
    let joinedCall = false;
    let isInitiator = false;
    let autoAcceptCallId = new URLSearchParams(window.location.search).get('acceptAudioCall');
    const peerConnections = new Map();
    const participantStates = new Map();
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
            endCallWithMessage('채팅 서버 연결이 끊어져 그룹 통화가 종료되었습니다.');
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
            isInitiator = true;
            joinedCall = true;
            participantStates.set(email, {name: '나', state: 'connected'});
            renderParticipants();
            showPanel('그룹 음성 통화 응답을 기다리는 중입니다.');
            setConnectionStatus('응답 대기 중', 'connecting');
            setCallControls(false);
            hangupButton.classList.remove('is-hidden');
            if (!sendSignal('CALL')) {
                cleanupCall();
                return;
            }
            callTimeout = window.setTimeout(() => {
                if (peerConnections.size === 0 && activeCallId) {
                    sendSignal('HANGUP');
                    showError('통화에 응답한 참여자가 없습니다.');
                    window.setTimeout(cleanupCall, 1800);
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
            statusText.innerText = '그룹 통화에 연결하는 중입니다.';
            stopRingtone();
            clearIncomingCallTimeout();
            setConnectionStatus('연결 중', 'connecting');
            joinedCall = true;
            participantStates.set(email, {name: '나', state: 'connected'});
            renderParticipants();
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
            if (signal.targetEmail) {
                if (peerConnections.has(signal.targetEmail)) {
                    removePeer(signal.targetEmail, 'failed');
                }
                statusText.innerText = signal.error;
                return;
            }
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
                statusText.innerText = `${signal.senderName || signal.senderEmail}님을 연결하는 중입니다.`;
                setConnectionStatus('연결 중', 'connecting');
                participantStates.set(signal.senderEmail, {
                    name: signal.senderName || signal.senderEmail,
                    state: 'connecting'
                });
                renderParticipants();
                await createAndSendOffer(signal.senderEmail, signal.senderName);
                break;
            case 'ACCEPTED':
                joinedCall = true;
                notifyCallAcceptedInThisTab();
                startKeepAlive();
                startDurationTimer();
                requestWakeLock();
                break;
            case 'REJECT':
                showError('통화에 참여할 수 있는 상대가 없습니다.');
                window.setTimeout(cleanupCall, 1800);
                break;
            case 'PARTICIPANT_REJECTED':
                participantStates.set(signal.senderEmail, {
                    name: signal.senderName || signal.senderEmail,
                    state: 'rejected'
                });
                renderParticipants();
                statusText.innerText = `${signal.senderName || signal.senderEmail}님이 통화에 참여하지 않았습니다.`;
                break;
            case 'OFFER':
                await receiveOffer(signal.senderEmail, signal.senderName, signal.sdp);
                break;
            case 'ANSWER':
                await receiveAnswer(signal.senderEmail, signal.sdp);
                break;
            case 'ICE_CANDIDATE':
                await receiveCandidate(signal.senderEmail, signal.senderName, signal);
                break;
            case 'PARTICIPANT_LEFT':
                removePeer(signal.senderEmail, 'left');
                statusText.innerText = `${signal.senderName || signal.senderEmail}님이 그룹 통화에서 나갔습니다.`;
                updateGroupCallStatus();
                break;
            case 'HANGUP':
                endCallWithMessage('통화를 유지할 참여자가 없어 그룹 통화가 종료되었습니다.');
                break;
            case 'DISCONNECTED':
                endCallWithMessage('참여자의 연결 종료로 그룹 통화가 종료되었습니다.');
                break;
            case 'ADMIN_TERMINATED':
                endCallWithMessage('관리자에 의해 그룹 통화가 종료되었습니다.');
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
        isInitiator = false;
        joinedCall = false;
        incomingCaller = signal.senderName || signal.senderEmail || '상대방';
        participantStates.set(signal.senderEmail, {name: incomingCaller, state: 'waiting'});
        renderParticipants();
        showPanel(`${incomingCaller}님의 그룹 음성 통화입니다.`);
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

    function createPeerConnection(peerEmail, peerName, offerer) {
        const existing = peerConnections.get(peerEmail);
        if (existing) {
            if (peerName) existing.name = peerName;
            if (offerer) existing.offerer = true;
            return existing;
        }

        const connection = new RTCPeerConnection(rtcConfig);
        const metadata = {
            connection: connection,
            name: peerName || peerEmail,
            offerer: Boolean(offerer),
            pendingCandidates: [],
            failureTimeout: null
        };
        peerConnections.set(peerEmail, metadata);
        participantStates.set(peerEmail, {name: metadata.name, state: 'connecting'});
        renderParticipants();

        localStream.getTracks().forEach(track => connection.addTrack(track, localStream));
        connection.ontrack = event => {
            const audio = getOrCreateRemoteAudio(peerEmail);
            audio.srcObject = event.streams[0];
            applyOutputDevice(audio).catch(() => {});
            audio.play().catch(() => {});
        };
        connection.onicecandidate = event => {
            if (!event.candidate) return;
            sendSignal('ICE_CANDIDATE', {
                targetEmail: peerEmail,
                candidate: event.candidate.candidate,
                sdpMid: event.candidate.sdpMid,
                sdpMLineIndex: event.candidate.sdpMLineIndex
            });
        };
        connection.onconnectionstatechange = () => {
            if (!peerConnections.has(peerEmail)) return;
            if (connection.connectionState === 'connected') {
                clearPeerConnectionFailureTimeout(peerEmail);
                participantStates.set(peerEmail, {name: metadata.name, state: 'connected'});
                renderParticipants();
                updateGroupCallStatus();
                setCallControls(true);
                startKeepAlive();
                startDurationTimer();
                requestWakeLock();
            } else if (['failed', 'disconnected'].includes(connection.connectionState)) {
                beginConnectionRecovery(peerEmail);
            }
        };
        return metadata;
    }

    function getOrCreateRemoteAudio(peerEmail) {
        let audio = Array.from(remoteAudioContainer.querySelectorAll('audio'))
            .find(item => item.dataset.peerEmail === peerEmail);
        if (audio) return audio;
        audio = document.createElement('audio');
        audio.autoplay = true;
        audio.dataset.peerEmail = peerEmail;
        remoteAudioContainer.appendChild(audio);
        return audio;
    }

    async function createAndSendOffer(peerEmail, peerName, iceRestart) {
        const metadata = createPeerConnection(peerEmail, peerName, true);
        const offer = await metadata.connection.createOffer(iceRestart ? {iceRestart: true} : undefined);
        await metadata.connection.setLocalDescription(offer);
        sendSignal('OFFER', {targetEmail: peerEmail, sdp: offer.sdp});
    }

    async function receiveOffer(peerEmail, peerName, sdp) {
        const metadata = createPeerConnection(peerEmail, peerName, false);
        await metadata.connection.setRemoteDescription({type: 'offer', sdp: sdp});
        await flushPendingCandidates(metadata);
        const answer = await metadata.connection.createAnswer();
        await metadata.connection.setLocalDescription(answer);
        sendSignal('ANSWER', {targetEmail: peerEmail, sdp: answer.sdp});
    }

    async function receiveAnswer(peerEmail, sdp) {
        const metadata = peerConnections.get(peerEmail);
        if (!metadata) throw new Error('연결할 통화 참여자를 찾을 수 없습니다.');
        await metadata.connection.setRemoteDescription({type: 'answer', sdp: sdp});
        await flushPendingCandidates(metadata);
    }

    async function receiveCandidate(peerEmail, peerName, signal) {
        const candidate = {
            candidate: signal.candidate,
            sdpMid: signal.sdpMid,
            sdpMLineIndex: signal.sdpMLineIndex
        };
        const metadata = peerConnections.get(peerEmail);
        if (!metadata || !metadata.connection.remoteDescription) {
            const pending = metadata || createPeerConnection(peerEmail, peerName, false);
            pending.pendingCandidates.push(candidate);
            return;
        }
        await metadata.connection.addIceCandidate(candidate);
    }

    async function flushPendingCandidates(metadata) {
        while (metadata.pendingCandidates.length > 0) {
            await metadata.connection.addIceCandidate(metadata.pendingCandidates.shift());
        }
    }

    function hangup() {
        if (activeCallId) sendSignal(joinedCall ? 'HANGUP' : 'REJECT');
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
        stopKeepAlive();
        stopDurationTimer();
        stopRingtone();
        releaseWakeLock();
        if (callNoticeTimeout) window.clearTimeout(callNoticeTimeout);
        callNoticeTimeout = null;
        peerConnections.forEach(metadata => {
            if (metadata.failureTimeout) window.clearTimeout(metadata.failureTimeout);
            metadata.connection.close();
        });
        peerConnections.clear();
        if (localStream) localStream.getTracks().forEach(track => track.stop());
        remoteAudioContainer.replaceChildren();
        localStream = null;
        activeCallId = null;
        incomingCaller = '';
        joinedCall = false;
        isInitiator = false;
        participantStates.clear();
        renderParticipants();
        muteButton.innerText = '음소거';
        muteStatus.classList.add('is-hidden');
        durationText.classList.add('is-hidden');
        durationText.textContent = '00:00';
        deviceControls.classList.add('is-hidden');
        setConnectionStatus('대기 중', 'idle');
        panel.classList.add('is-hidden');
        [acceptButton, rejectButton, muteButton, hangupButton]
            .forEach(button => button.classList.add('is-hidden'));
        if (startButton) startButton.disabled = false;
    }

    function removePeer(peerEmail, state) {
        const metadata = peerConnections.get(peerEmail);
        if (metadata) {
            if (metadata.failureTimeout) window.clearTimeout(metadata.failureTimeout);
            metadata.connection.close();
            peerConnections.delete(peerEmail);
        }
        Array.from(remoteAudioContainer.querySelectorAll('audio'))
            .filter(audio => audio.dataset.peerEmail === peerEmail)
            .forEach(audio => audio.remove());
        const current = participantStates.get(peerEmail);
        if (current) participantStates.set(peerEmail, {name: current.name, state: state || 'left'});
        renderParticipants();
    }

    function endCallWithMessage(message) {
        cleanupCall();
        showError(message);
        callNoticeTimeout = window.setTimeout(cleanupCall, 1800);
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

    function beginConnectionRecovery(peerEmail) {
        const metadata = peerConnections.get(peerEmail);
        if (!metadata || metadata.failureTimeout || !activeCallId) return;
        participantStates.set(peerEmail, {name: metadata.name, state: 'recovering'});
        renderParticipants();
        setConnectionStatus('일부 연결 복구 중', 'recovering');

        if (metadata.offerer) {
            createAndSendOffer(peerEmail, metadata.name, true).catch(() => {});
        }

        metadata.failureTimeout = window.setTimeout(() => {
            const current = peerConnections.get(peerEmail);
            if (!current || current.connection.connectionState === 'connected') return;
            removePeer(peerEmail, 'failed');
            updateGroupCallStatus();
        }, 10000);
    }

    function clearPeerConnectionFailureTimeout(peerEmail) {
        const metadata = peerConnections.get(peerEmail);
        if (!metadata || !metadata.failureTimeout) return;
        window.clearTimeout(metadata.failureTimeout);
        metadata.failureTimeout = null;
    }

    function startKeepAlive() {
        if (keepAliveInterval) return;
        keepAliveInterval = window.setInterval(() => {
            if (activeCallId && joinedCall) sendSignal('KEEP_ALIVE');
        }, 30000);
    }

    function stopKeepAlive() {
        if (keepAliveInterval) window.clearInterval(keepAliveInterval);
        keepAliveInterval = null;
    }

    function handleRtcError(error) {
        console.error('그룹 음성 통화 처리 오류:', error);
        if (activeCallId) sendSignal(joinedCall ? 'HANGUP' : 'REJECT');
        endCallWithMessage('음성 연결을 처리하는 중 오류가 발생해 그룹 통화가 종료되었습니다.');
    }

    function notifyCallAcceptedInThisTab() {
        if (!callChannel || !activeCallId) return;
        callChannel.postMessage({type: 'ACCEPTED', callId: activeCallId, tabId: tabId});
    }

    function startIncomingCallTimeout() {
        clearIncomingCallTimeout();
        incomingCallTimeout = window.setTimeout(() => {
            if (!activeCallId || joinedCall) return;
            sendSignal('REJECT');
            stopRingtone();
            showError(`${incomingCaller || '상대방'}님의 부재중 그룹 통화가 있습니다.`);
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

    function updateGroupCallStatus() {
        const connectedPeers = Array.from(peerConnections.values())
            .filter(metadata => metadata.connection.connectionState === 'connected').length;
        if (connectedPeers > 0) {
            statusText.innerText = `${connectedPeers + 1}명이 그룹 통화 중`;
            setConnectionStatus('연결됨', 'connected');
            return;
        }
        statusText.innerText = '다른 참여자의 연결을 기다리는 중입니다.';
        setConnectionStatus('연결 대기 중', 'connecting');
    }

    function renderParticipants() {
        if (!participantList) return;
        participantList.replaceChildren();
        participantStates.forEach((participant, participantEmail) => {
            const item = document.createElement('span');
            item.className = 'audio-call-participant';
            item.dataset.state = participant.state;
            item.textContent = participantEmail === email ? '나' : participant.name;
            participantList.appendChild(item);
        });
        participantList.classList.toggle('is-hidden', participantStates.size === 0);
    }

    async function populateAudioDevices() {
        if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) return;
        const devices = await navigator.mediaDevices.enumerateDevices();
        fillDeviceSelect(inputSelect, devices.filter(device => device.kind === 'audioinput'), '마이크');
        const supportsSink = typeof HTMLMediaElement !== 'undefined'
            && typeof HTMLMediaElement.prototype.setSinkId === 'function';
        if (supportsSink) {
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
        for (const metadata of peerConnections.values()) {
            const sender = metadata.connection.getSenders()
                .find(item => item.track && item.track.kind === 'audio');
            if (sender) await sender.replaceTrack(newTrack);
        }
        if (localStream) localStream.getTracks().forEach(track => track.stop());
        localStream = new MediaStream([newTrack]);
    }

    async function applyOutputDevice(audio) {
        if (typeof audio.setSinkId !== 'function' || !outputSelect.value) return;
        await audio.setSinkId(outputSelect.value);
    }

    async function changeOutputDevice() {
        const audios = remoteAudioContainer.querySelectorAll('audio');
        await Promise.all(Array.from(audios).map(applyOutputDevice));
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
        remoteAudioContainer.querySelectorAll('audio').forEach(audio => audio.play().catch(() => {}));
        peerConnections.forEach((metadata, peerEmail) => {
            if (['failed', 'disconnected'].includes(metadata.connection.connectionState)) {
                beginConnectionRecovery(peerEmail);
            }
        });
        if (Array.from(peerConnections.values())
                .some(metadata => metadata.connection.connectionState === 'connected')) requestWakeLock();
    });
    window.addEventListener('pagehide', event => {
        if (event.persisted) {
            releaseWakeLock();
            return;
        }
        if (activeCallId) sendSignal(joinedCall ? 'HANGUP' : 'REJECT');
        cleanupCall();
        if (callChannel) callChannel.close();
    });

    window.audioCallClient = {
        onStompConnected: onStompConnected,
        onStompDisconnected: onStompDisconnected
    };
    if (typeof client !== 'undefined' && client.connected) onStompConnected(client);
})();
