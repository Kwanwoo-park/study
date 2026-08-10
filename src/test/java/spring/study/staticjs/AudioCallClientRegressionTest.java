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
        assertTrue(audioCallJs.contains("관리자에 의해 통화가 종료되었습니다."));
        assertTrue(audioCallJs.contains("endCallWithMessage('상대방의 연결이 끊어져 통화가 종료되었습니다.')"));
        assertTrue(audioCallJs.contains("if (peerConnection) peerConnection.close()"));
        assertTrue(audioCallJs.contains("localStream.getTracks().forEach(track => track.stop())"));
    }
}
