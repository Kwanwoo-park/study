package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallState;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.repository.AudioCallStateStore;
import spring.study.common.exception.BusinessStateException;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioCallSignalingService {
    private static final String AUDIO_QUEUE = "/queue/audio-call";
    private static final Duration RINGING_TTL = Duration.ofSeconds(45);
    private static final Duration ACTIVE_TTL = Duration.ofHours(12);

    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AudioCallStateStore callStateStore;
    private final NotificationService notificationService;

    public void handle(String senderEmail, String sessionId, AudioCallSignalRequest request) {
        try {
            validateRequest(request, sessionId);
            if (request.type() == AudioCallSignalType.CALL) {
                Member sender = memberService.findMember(senderEmail);
                ChatRoom room = requireRoomMember(request.roomId(), sender);
                startCall(sender, room, sessionId, request);
                return;
            }

            AudioCall call = requireActiveCall(request, senderEmail);
            switch (request.type()) {
                case ACCEPT -> accept(call, senderEmail, sessionId, request);
                case REJECT -> reject(call, senderEmail, sessionId, request);
                case OFFER -> offer(call, senderEmail, sessionId, request);
                case ANSWER -> answer(call, senderEmail, sessionId, request);
                case ICE_CANDIDATE -> iceCandidate(call, senderEmail, sessionId, request);
                case HANGUP -> hangup(call, senderEmail, sessionId, request);
                case KEEP_ALIVE -> keepAlive(call, senderEmail, sessionId);
                default -> throw new IllegalArgumentException("잘못된 통화 요청입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            forward(senderEmail, sessionId, AudioCallSignalResponse.error(request, exception.getMessage()));
        }
    }

    public void handleDisconnect(String disconnectedMemberEmail, String disconnectedSessionId) {
        if (disconnectedMemberEmail == null || disconnectedMemberEmail.isBlank()
                || disconnectedSessionId == null || disconnectedSessionId.isBlank()) {
            return;
        }

        callStateStore.findByMember(disconnectedMemberEmail).ifPresent(call -> {
            if (!call.ownsSession(disconnectedMemberEmail, disconnectedSessionId)) return;
            if (!callStateStore.remove(call)) return;

            closeIncomingNotification(call);

            forward(
                    call.otherEmail(disconnectedMemberEmail),
                    call.otherSessionId(disconnectedMemberEmail),
                    AudioCallSignalResponse.disconnected(
                            call.callId(), call.roomId(), disconnectedMemberEmail)
            );
        });
    }

    public void forceTerminate(String callId) {
        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
        }

        AudioCall call = callStateStore.find(callId)
                .orElseThrow(() -> new IllegalArgumentException("진행 중인 통화를 찾을 수 없습니다."));
        if (!callStateStore.remove(call)) {
            throw new IllegalArgumentException("이미 종료된 통화입니다.");
        }

        closeIncomingNotification(call);

        AudioCallSignalResponse signal = AudioCallSignalResponse.adminTerminated(
                call.callId(), call.roomId());
        forward(call.callerEmail(), call.callerSessionId(), signal);
        forward(call.receiverEmail(), call.receiverSessionId(), signal);
    }

    public Optional<AudioCallSignalResponse> findIncomingCall(String receiverEmail, String roomId) {
        return callStateStore.findByMember(receiverEmail)
                .filter(call -> call.isReceiver(receiverEmail))
                .filter(call -> roomId == null || roomId.isBlank() || call.roomId().equals(roomId))
                .filter(call -> call.state() == AudioCallState.RINGING)
                .map(call -> {
                    AudioCallSignalRequest request = new AudioCallSignalRequest(
                            call.callId(), call.roomId(), AudioCallSignalType.CALL,
                            null, null, null, null);
                    return AudioCallSignalResponse.from(
                            request, call.callerEmail(), call.callerName());
                });
    }

    public void rejectIncomingCall(String callId, String receiverEmail) {
        AudioCall call = callStateStore.find(callId)
                .orElseThrow(() -> new BusinessStateException("이미 종료된 통화입니다."));
        requireReceiver(call, receiverEmail);
        requireState(call, AudioCallState.RINGING);
        if (!callStateStore.remove(call)) {
            throw new BusinessStateException("이미 종료된 통화입니다.");
        }

        closeIncomingNotification(call);
        AudioCallSignalRequest request = new AudioCallSignalRequest(
                call.callId(), call.roomId(), AudioCallSignalType.REJECT,
                null, null, null, null);
        forward(call.callerEmail(), call.callerSessionId(),
                AudioCallSignalResponse.from(request, receiverEmail, call.receiverName()));
    }

    private void startCall(Member sender, ChatRoom room, String callerSessionId, AudioCallSignalRequest request) {
        List<ChatRoomMember> members = roomMemberService.find(room);
        if (members.size() != 2) {
            throw new BusinessStateException("1:1 채팅방에서만 음성 통화를 시작할 수 있습니다.");
        }

        Member receiver = members.stream()
                .map(ChatRoomMember::getMember)
                .filter(member -> !member.getEmail().equals(sender.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("통화 상대를 찾을 수 없습니다."));

        String callId = request.callId() == null || request.callId().isBlank()
                ? UUID.randomUUID().toString()
                : request.callId();
        String callerName = sender.getName() == null || sender.getName().isBlank()
                ? sender.getEmail()
                : sender.getName();
        AudioCall call = new AudioCall(
                callId,
                room.getRoomId(),
                sender.getEmail(),
                callerName,
                callerSessionId,
                receiver.getEmail(),
                receiver.getName(),
                null,
                AudioCallState.RINGING
        );

        if (!callStateStore.create(call, RINGING_TTL)) {
            throw new BusinessStateException("현재 다른 통화가 진행 중입니다.");
        }

        AudioCallSignalRequest normalized = new AudioCallSignalRequest(
                callId, room.getRoomId(), AudioCallSignalType.CALL, null, null, null, null);
        forward(receiver.getEmail(), null,
                AudioCallSignalResponse.from(normalized, sender.getEmail(), sender.getName()));
        try {
            notificationService.createNotification(
                    receiver,
                    callerName + "님이 음성 통화를 요청했습니다.",
                    Group.CALL,
                    callNotificationUrl(call)
            );
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 저장 실패: callId={}", callId, exception);
        }
    }

    private void accept(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        requireReceiver(call, senderEmail);
        requireState(call, AudioCallState.RINGING);
        if (!callStateStore.transition(
                call, AudioCallState.RINGING, AudioCallState.CONNECTING, sessionId, ACTIVE_TTL)) {
            throw new BusinessStateException("다른 기기에서 이미 통화를 받았거나 통화가 종료되었습니다.");
        }
        closeIncomingNotification(call);
        forwardToOther(call, senderEmail, request);
        forward(senderEmail, sessionId, AudioCallSignalResponse.accepted(call.callId(), call.roomId()));
    }

    private void reject(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        requireReceiver(call, senderEmail);
        requireState(call, AudioCallState.RINGING);
        if (!callStateStore.remove(call)) throw new BusinessStateException("이미 종료된 통화입니다.");
        closeIncomingNotification(call);
        forwardToOther(call, senderEmail, request);
    }

    private void offer(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        requireCaller(call, senderEmail);
        requireOwnedSession(call, senderEmail, sessionId);
        if (call.state() != AudioCallState.CONNECTING && call.state() != AudioCallState.ACTIVE) {
            throw new BusinessStateException("현재 통화 연결을 갱신할 수 없습니다.");
        }
        forwardToOther(call, senderEmail, request);
    }

    private void answer(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        requireReceiver(call, senderEmail);
        requireOwnedSession(call, senderEmail, sessionId);
        if (call.state() == AudioCallState.CONNECTING) {
            if (!callStateStore.transition(
                    call, AudioCallState.CONNECTING, AudioCallState.ACTIVE, null, ACTIVE_TTL)) {
                throw new BusinessStateException("통화 연결 상태가 변경되었습니다.");
            }
        } else if (call.state() != AudioCallState.ACTIVE) {
            throw new BusinessStateException("현재 통화 연결에 응답할 수 없습니다.");
        }
        forwardToOther(call, senderEmail, request);
    }

    private void iceCandidate(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        requireOwnedSession(call, senderEmail, sessionId);
        if (call.state() != AudioCallState.CONNECTING && call.state() != AudioCallState.ACTIVE) {
            throw new BusinessStateException("아직 연결할 수 없는 통화입니다.");
        }
        forwardToOther(call, senderEmail, request);
    }

    private void hangup(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        boolean receiverIsClosingRingingCall = call.state() == AudioCallState.RINGING
                && call.isReceiver(senderEmail);
        if (!receiverIsClosingRingingCall) {
            requireOwnedSession(call, senderEmail, sessionId);
        }
        if (!callStateStore.remove(call)) throw new BusinessStateException("이미 종료된 통화입니다.");
        closeIncomingNotification(call);
        forwardToOther(call, senderEmail, request);
    }

    private void keepAlive(AudioCall call, String senderEmail, String sessionId) {
        requireOwnedSession(call, senderEmail, sessionId);
        requireState(call, AudioCallState.ACTIVE);
        if (!callStateStore.touch(call, AudioCallState.ACTIVE, ACTIVE_TTL)) {
            throw new BusinessStateException("통화 상태를 갱신할 수 없습니다.");
        }
    }

    private ChatRoom requireRoomMember(String roomId, Member sender) {
        ChatRoom room = roomService.find(roomId);
        if (room == null) throw new IllegalArgumentException("채팅방이 존재하지 않습니다.");
        if (!roomMemberService.exist(sender, room)) {
            throw new IllegalArgumentException("채팅방 참여자만 통화할 수 있습니다.");
        }
        return room;
    }

    private AudioCall requireActiveCall(AudioCallSignalRequest request, String senderEmail) {
        AudioCall call = callStateStore.find(request.callId())
                .orElseThrow(() -> new IllegalStateException("유효한 통화가 아닙니다."));
        if (!call.roomId().equals(request.roomId()) || !call.contains(senderEmail)) {
            throw new BusinessStateException("유효한 통화가 아닙니다.");
        }
        return call;
    }

    private void requireCaller(AudioCall call, String senderEmail) {
        if (!call.isCaller(senderEmail)) {
            throw new BusinessStateException("발신자만 통화 연결을 시작할 수 있습니다.");
        }
    }

    private void requireReceiver(AudioCall call, String senderEmail) {
        if (!call.isReceiver(senderEmail)) {
            throw new BusinessStateException("수신자만 통화에 응답할 수 있습니다.");
        }
    }

    private void requireOwnedSession(AudioCall call, String senderEmail, String sessionId) {
        if (!call.ownsSession(senderEmail, sessionId)) {
            throw new BusinessStateException("통화를 시작한 기기에서만 처리할 수 있습니다.");
        }
    }

    private void requireState(AudioCall call, AudioCallState state) {
        if (call.state() != state) throw new BusinessStateException("올바르지 않은 통화 상태입니다.");
    }

    private void closeIncomingNotification(AudioCall call) {
        try {
            Member receiver = memberService.findMember(call.receiverEmail());
            notificationService.closeRealtimeNotification(
                    receiver,
                    Group.CALL,
                    callNotificationUrl(call)
            );
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 종료 실패: callId={}", call.callId(), exception);
        }
    }

    private String callNotificationUrl(AudioCall call) {
        return UriComponentsBuilder.fromPath("/chat/chatRoom")
                .queryParam("roomId", call.roomId())
                .queryParam("callId", call.callId())
                .build()
                .encode()
                .toUriString();
    }

    private void validateRequest(AudioCallSignalRequest request, String sessionId) {
        if (request == null || request.type() == null || request.roomId() == null || request.roomId().isBlank()
                || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() == AudioCallSignalType.DISCONNECTED
                || request.type() == AudioCallSignalType.ADMIN_TERMINATED
                || request.type() == AudioCallSignalType.ACCEPTED) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() != AudioCallSignalType.CALL
                && (request.callId() == null || request.callId().isBlank())) {
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
        }
    }

    private void forwardToOther(AudioCall call, String senderEmail, AudioCallSignalRequest request) {
        forward(
                call.otherEmail(senderEmail),
                call.otherSessionId(senderEmail),
                AudioCallSignalResponse.from(request, senderEmail, call.nameOf(senderEmail))
        );
    }

    private void forward(String email, String sessionId, AudioCallSignalResponse response) {
        if (sessionId == null || sessionId.isBlank()) {
            messagingTemplate.convertAndSendToUser(email, AUDIO_QUEUE, response);
            return;
        }

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(email, AUDIO_QUEUE, response, headers.getMessageHeaders());
    }
}
