(function () {
    const startButton = document.getElementById('audio-call-start');
    const panel = document.getElementById('audio-call-panel');
    const statusText = document.getElementById('audio-call-status');
    const acceptButton = document.getElementById('audio-call-accept');
    const rejectButton = document.getElementById('audio-call-reject');
    const muteButton = document.getElementById('audio-call-mute');
    const hangupButton = document.getElementById('audio-call-hangup');
    const remoteAudio = document.getElementById('audio-call-remote');

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

    function onStompConnected(connectedClient) {
        stompClient = connectedClient;
        if (!subscription) {
            subscription = stompClient.subscribe('/user/queue/audio-call', onSignalReceived);
        }
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
            showPanel('상대방의 응답을 기다리는 중입니다.');
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
            sendSignal('ACCEPT');
        } catch (error) {
            sendSignal('REJECT');
            showError(microphoneErrorMessage(error));
            window.setTimeout(cleanupCall, 1500);
        }
    }

    function rejectCall() {
        sendSignal('REJECT');
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
            if (activeCallId) return;
            activeCallId = signal.callId;
            incomingCaller = signal.senderName || signal.senderEmail || '상대방';
            showPanel(`${incomingCaller}님의 음성 통화입니다.`);
            acceptButton.classList.remove('is-hidden');
            rejectButton.classList.remove('is-hidden');
            return;
        }
        if (signal.callId !== activeCallId) return;

        switch (signal.type) {
            case 'ACCEPT':
                clearCallTimeout();
                statusText.innerText = '연결 중입니다.';
                await createAndSendOffer();
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
            localStream = await navigator.mediaDevices.getUserMedia({audio: true, video: false});
        }
        return localStream;
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
                statusText.innerText = `${incomingCaller || '상대방'}님과 통화 중`;
                setCallControls(true);
            } else if (['failed', 'disconnected'].includes(peerConnection.connectionState)) {
                showError('음성 연결이 끊어졌습니다.');
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
    }

    function cleanupCall() {
        clearCallTimeout();
        if (callNoticeTimeout) window.clearTimeout(callNoticeTimeout);
        callNoticeTimeout = null;
        if (peerConnection) peerConnection.close();
        if (localStream) localStream.getTracks().forEach(track => track.stop());
        remoteAudio.srcObject = null;
        peerConnection = null;
        localStream = null;
        activeCallId = null;
        incomingCaller = '';
        pendingCandidates = [];
        muteButton.innerText = '음소거';
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
    window.addEventListener('pagehide', () => {
        if (activeCallId) sendSignal('HANGUP');
        cleanupCall();
    });

    window.audioCallClient = {
        onStompConnected: onStompConnected,
        onStompDisconnected: onStompDisconnected
    };
    if (typeof client !== 'undefined' && client.connected) onStompConnected(client);
})();
