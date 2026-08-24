package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioCallClientRegressionTest {
    private static final Path CHAT_JS = Path.of("src/main/resources/static/js/chat/chat.js");
    private static final Path AUDIO_CALL_JS = Path.of("src/main/resources/static/js/chat/audio-call.js");

    @Test
    void websocketDisconnectShouldImmediatelyCleanUpAnActiveAudioCall() throws Exception {
        String chatJs = Files.readString(CHAT_JS);
        String audioCallJs = Files.readString(AUDIO_CALL_JS);

        assertTrue(chatJs.contains("window.audioCallClient.onStompDisconnected()"));
        assertTrue(audioCallJs.contains("function onStompDisconnected()"));
        assertTrue(audioCallJs.contains("case 'DISCONNECTED':"));
        assertTrue(audioCallJs.contains("case 'ADMIN_TERMINATED':"));
        assertTrue(audioCallJs.contains("관리자에 의해 그룹 통화가 종료되었습니다."));
        assertTrue(audioCallJs.contains("채팅 서버 연결이 끊어져 그룹 통화가 종료되었습니다."));
        assertTrue(audioCallJs.contains("metadata.connection.close()"));
        assertTrue(audioCallJs.contains("localStream.getTracks().forEach(track => track.stop())"));
    }

    @Test
    void rtcFailureShouldAttemptRecoveryAndEventuallyReleaseCallResources() throws Exception {
        String audioCallJs = Files.readString(AUDIO_CALL_JS);

        assertTrue(audioCallJs.contains("beginConnectionRecovery(peerEmail)"));
        assertTrue(audioCallJs.contains("{iceRestart: true}"));
        assertTrue(audioCallJs.contains("sendSignal('HANGUP')"));
        assertTrue(audioCallJs.contains("clearPeerConnectionFailureTimeout(peerEmail)"));
        assertTrue(audioCallJs.contains("startKeepAlive()"));
        assertTrue(audioCallJs.contains("sendSignal('KEEP_ALIVE')"));
    }

    @Test
    void audioCallClientShouldProvideCallUxDevicesAndMobileRecovery() throws Exception {
        String chatJs = Files.readString(CHAT_JS);
        String audioCallJs = Files.readString(AUDIO_CALL_JS);

        assertTrue(audioCallJs.contains("startRingtone()"));
        assertTrue(audioCallJs.contains("startIncomingCallTimeout()"));
        assertTrue(audioCallJs.contains("startDurationTimer()"));
        assertTrue(audioCallJs.contains("enumerateDevices()"));
        assertTrue(audioCallJs.contains("replaceTrack(newTrack)"));
        assertTrue(audioCallJs.contains("audio.setSinkId(outputSelect.value)"));
        assertTrue(audioCallJs.contains("navigator.wakeLock.request('screen')"));
        assertTrue(audioCallJs.contains("document.addEventListener('visibilitychange'"));
        assertTrue(audioCallJs.contains("/api/chat/audio/incoming"));
        assertTrue(chatJs.contains("client.heartbeat.outgoing = 10000"));
        assertTrue(chatJs.contains("scheduleChatReconnect()"));
    }

    @Test
    void groupCallShouldMaintainOnePeerConnectionAndAudioElementPerParticipant() throws Exception {
        String audioCallJs = Files.readString(AUDIO_CALL_JS);

        assertTrue(audioCallJs.contains("const peerConnections = new Map()"));
        assertTrue(audioCallJs.contains("targetEmail: peerEmail"));
        assertTrue(audioCallJs.contains("case 'PARTICIPANT_LEFT':"));
        assertTrue(audioCallJs.contains("case 'PARTICIPANT_REJECTED':"));
        assertTrue(audioCallJs.contains("getOrCreateRemoteAudio(peerEmail)"));
        assertTrue(audioCallJs.contains("peerConnections.forEach(metadata =>"));
        assertTrue(audioCallJs.contains("participantStates.forEach"));
    }
}
