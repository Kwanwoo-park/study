package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallMutation;
import spring.study.chat.domain.AudioCallParticipant;
import spring.study.chat.domain.AudioCallParticipantStatus;
import spring.study.chat.domain.AudioCallState;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.repository.AudioCallStateStore;
import spring.study.common.exception.BusinessStateException;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AudioCallSignalingService {
    private static final Duration RINGING_TTL = Duration.ofSeconds(45);
    private static final Duration ACTIVE_TTL = Duration.ofHours(12);

    private final AudioCallStateStore callStateStore;

    public Optional<AudioCall> findByMember(String memberEmail) {
        return callStateStore.findByMember(memberEmail);
    }

    public void startCall(AudioCall call) {
        if (!callStateStore.create(call, RINGING_TTL))
            throw new BusinessStateException("참여자 중 현재 다른 통화가 진행 중인 회원이 있습니다.");
    }

    public AudioCall accept(AudioCall call, String memberEmail, String sessionId) {
        requireInvited(call, memberEmail);

        return callStateStore.join(call, memberEmail, sessionId, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException("다른 기기에서 이미 통화를 받았거나 통화가 종료되었습니다."));
    }

    public AudioCallMutation reject(AudioCall call, String memberEmail) {
        requireInvited(call, memberEmail);

        return callStateStore.reject(call, memberEmail, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException("이미 응답했거나 통화가 종료되었습니다."));
    }

    public AudioCallMutation leave(AudioCall call, String memberEmail, String sessionId) {
        requireOwnedSession(call, memberEmail, sessionId);

        return callStateStore.leave(call, memberEmail, sessionId, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException("이미 종료된 통화입니다."));
    }

    public Optional<AudioCallMutation> handleDisconnect(String memberEmail, String sessionId) {
        if (memberEmail == null || memberEmail.isBlank() || sessionId == null || sessionId.isBlank())
            return Optional.empty();

        return callStateStore.findByMember(memberEmail)
                .filter(call -> call.ownsSession(memberEmail, sessionId))
                .flatMap(call -> callStateStore.leave(call, memberEmail, sessionId, ACTIVE_TTL));
    }

    public AudioCall forceTerminate(String callId) {
        if (callId == null || callId.isBlank())
            throw new IllegalArgumentException("통화 ID가 필요합니다.");

        AudioCall call = callStateStore.find(callId)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 통화를 찾을 수 없습니다."));

        if (!callStateStore.remove(call))
            throw new IllegalArgumentException("이미 종료된 통화입니다.");

        return call;
    }

    public Optional<AudioCall> findIncomingCall(String receiverEmail, String roomId) {
        return callStateStore.findByMember(receiverEmail)
                .filter(call -> roomId == null || roomId.isBlank() || call.roomId().equals(roomId))
                .filter(call -> call.participant(receiverEmail)
                        .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                        .isPresent());
    }

    public AudioCall requireInvitedCall(String callId, String memberEmail) {
        AudioCall call = callStateStore.find(callId)
                .orElseThrow(() -> new BusinessStateException("이미 종료된 통화입니다."));
        requireInvited(call, memberEmail);
        return call;
    }

    public AudioCall requireCall(AudioCallSignalRequest request, String senderEmail) {
        AudioCall call = callStateStore.find(request.callId()).orElseThrow(() -> new IllegalStateException("유효한 통화가 아닙니다."));

        if (!call.roomId().equals(request.roomId()) || !call.contains(senderEmail))
            throw new BusinessStateException("유효한 통화가 아닙니다.");

        return call;
    }

    public AudioCallParticipant requirePeerSignalTarget(
            AudioCall call, String senderEmail, String sessionId, String targetEmail) {
        requireOwnedSession(call, senderEmail, sessionId);

        if (call.state() != AudioCallState.ACTIVE)
            throw new BusinessStateException("아직 연결할 수 없는 통화입니다.");

        if (targetEmail == null || targetEmail.isBlank() || targetEmail.equals(senderEmail))
            throw new IllegalArgumentException("통화 신호를 받을 참여자가 필요합니다.");

        return call.participant(targetEmail)
                .filter(participant -> participant.status() == AudioCallParticipantStatus.JOINED)
                .orElseThrow(() -> new BusinessStateException("통화에 참여 중인 회원이 아닙니다."));
    }

    public void keepAlive(AudioCall call, String memberEmail, String sessionId) {
        requireOwnedSession(call, memberEmail, sessionId);

        if (!callStateStore.touch(call, memberEmail, sessionId, ACTIVE_TTL))
            throw new BusinessStateException("통화 상태를 갱신할 수 없습니다.");
    }

    private void requireInvited(AudioCall call, String memberEmail) {
        if (call.participant(memberEmail)
                .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                .isEmpty())
            throw new BusinessStateException("통화 요청에 응답할 수 없습니다.");
    }

    private void requireOwnedSession(AudioCall call, String memberEmail, String sessionId) {
        if (!call.ownsSession(memberEmail, sessionId)
                || call.participant(memberEmail)
                        .filter(participant -> participant.status() == AudioCallParticipantStatus.JOINED)
                        .isEmpty())
            throw new BusinessStateException("통화를 시작한 기기에서만 처리할 수 있습니다.");
    }
}
