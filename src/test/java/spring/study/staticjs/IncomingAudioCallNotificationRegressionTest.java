package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomingAudioCallNotificationRegressionTest {
    @Test
    void incomingCallShouldUseDedicatedFullScreenUiAfterServerValidation() throws IOException {
        String commonJs = read("src/main/resources/static/js/common/common.js");
        String commonCss = read("src/main/resources/static/css/common/common.css");

        assertTrue(commonJs.contains("notificationGroup === 'CALL'"));
        assertTrue(commonJs.contains("fnFetchIncomingAudioCall(details.roomId)"));
        assertTrue(commonJs.contains("id = 'incoming-audio-call-overlay'"));
        assertTrue(commonJs.contains("INCOMING AUDIO CALL"));
        assertTrue(commonJs.contains("fnStartIncomingAudioRingtone()"));
        assertTrue(commonCss.contains(".incoming-audio-call-overlay"));
        assertTrue(commonCss.contains("position: fixed;\n    inset: 0;\n    z-index: 5000;"));
    }

    @Test
    void endedCallEventShouldCloseEveryTabAndItsSystemNotification() throws IOException {
        String commonJs = read("src/main/resources/static/js/common/common.js");
        String service = read("src/main/java/spring/study/chat/service/AudioCallSignalingService.java");

        assertTrue(commonJs.contains("notification.readStatus === 'READ'"));
        assertTrue(commonJs.contains("fnCloseIncomingAudioCall(details.callId)"));
        assertTrue(commonJs.contains("incomingAudioSystemNotifications.get(resolvedCallId)?.close()"));
        assertTrue(service.contains("closeIncomingNotification(call);"));
        assertTrue(service.contains("notificationService.closeRealtimeNotification("));
    }

    @Test
    void incomingCallCanBeAcceptedOrRejectedFromGlobalCallScreen() throws IOException {
        String commonJs = read("src/main/resources/static/js/common/common.js");
        String audioCallJs = read("src/main/resources/static/js/chat/audio-call.js");

        assertTrue(commonJs.contains("/api/chat/audio/${encodeURIComponent(call.callId)}/reject"));
        assertTrue(commonJs.contains("target.searchParams.set('acceptAudioCall', call.callId)"));
        assertTrue(audioCallJs.contains("get('acceptAudioCall')"));
        assertTrue(audioCallJs.contains("window.setTimeout(() => acceptCall(), 0)"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
