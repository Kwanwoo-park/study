package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.AudioCallPreferenceRequest;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.chat.service.IceServerService;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AudioCallFacade {
    private final AudioCallSignalingService audioCallSignalingService;
    private final IceServerService iceServerService;

    public void signal(String email, String sessionId, AudioCallSignalRequest signal) {
        audioCallSignalingService.handle(email, sessionId, signal);
    }

    public ResponseEntity<?> iceServers(Member member) {
        return ResponseEntity.ok(Map.of("iceServers", iceServerService.createIceServers(member)));
    }

    public ResponseEntity<?> preference(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "enabled", member.isAudioCallEnabled()
        ));
    }

    public ResponseEntity<?> updatePreference(AudioCallPreferenceRequest preference, Member member) {
        if (preference == null || preference.enabled() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "result", -1L,
                    "message", "통화 알림 설정을 선택해 주세요"
            ));
        }

        boolean enabled = audioCallSignalingService.updateIncomingCallPreference(member.getEmail(), preference.enabled());

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "enabled", enabled,
                "message", enabled ? "통화 알림을 허용했습니다" : "통화 알림을 미허용으로 설정했습니다"
        ));
    }

    public ResponseEntity<?> incoming(String roomId, Member member) {
        return audioCallSignalingService.findIncomingCall(member.getEmail(), roomId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public ResponseEntity<?> reject(String callId, Member member) {
        audioCallSignalingService.rejectIncomingCall(callId, member.getEmail());

        return ResponseEntity.ok(Map.of("result", 1L));
    }
}
