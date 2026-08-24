package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallMutation;
import spring.study.chat.domain.AudioCallParticipant;
import spring.study.chat.domain.AudioCallParticipantStatus;
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
import java.util.ArrayList;
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

            AudioCall call = requireCall(request, senderEmail);
            switch (request.type()) {
                case ACCEPT -> accept(call, senderEmail, sessionId, request);
                case REJECT -> reject(call, senderEmail);
                case OFFER, ANSWER, ICE_CANDIDATE -> forwardPeerSignal(
                        call, senderEmail, sessionId, request);
                case HANGUP -> hangup(call, senderEmail, sessionId);
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
            Optional<AudioCallMutation> result = callStateStore.leave(
                    call, disconnectedMemberEmail, disconnectedSessionId, ACTIVE_TTL);
            if (result.isEmpty()) return;

            AudioCall updated = result.get().call();
            AudioCallSignalResponse response = result.get().callEnded()
                    ? AudioCallSignalResponse.disconnected(
                            call.callId(), call.roomId(), disconnectedMemberEmail)
                    : AudioCallSignalResponse.participantEvent(
                            call.callId(), call.roomId(), AudioCallSignalType.PARTICIPANT_LEFT,
                            disconnectedMemberEmail, call.nameOf(disconnectedMemberEmail));
            if (result.get().callEnded()) {
                closeIncomingNotifications(call);
                forwardToAvailableParticipants(updated, disconnectedMemberEmail, response);
            } else {
                forwardToJoinedParticipants(updated, disconnectedMemberEmail, response);
            }
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

        closeIncomingNotifications(call);
        forwardToAvailableParticipants(
                call,
                null,
                AudioCallSignalResponse.adminTerminated(call.callId(), call.roomId())
        );
    }

    public Optional<AudioCallSignalResponse> findIncomingCall(String receiverEmail, String roomId) {
        if (!isIncomingCallEnabled(receiverEmail)) return Optional.empty();

        return callStateStore.findByMember(receiverEmail)
                .filter(call -> roomId == null || roomId.isBlank() || call.roomId().equals(roomId))
                .filter(call -> call.participant(receiverEmail)
                        .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                        .isPresent())
                .map(call -> AudioCallSignalResponse.from(
                        new AudioCallSignalRequest(
                                call.callId(), call.roomId(), AudioCallSignalType.CALL,
                                null, null, null, null),
                        call.initiatorEmail(), call.initiatorName()));
    }

    public void rejectIncomingCall(String callId, String receiverEmail) {
        AudioCall call = callStateStore.find(callId)
                .orElseThrow(() -> new BusinessStateException("이미 종료된 통화입니다."));
        requireInvited(call, receiverEmail);
        reject(call, receiverEmail);
    }

    public boolean updateIncomingCallPreference(String memberEmail, boolean enabled) {
        Member member = memberService.findMember(memberEmail);
        memberService.updateAudioCallEnabled(member.getId(), enabled);
        if (!enabled) {
            callStateStore.findByMember(memberEmail)
                    .filter(call -> call.participant(memberEmail)
                            .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                            .isPresent())
                    .ifPresent(call -> reject(call, memberEmail));
        }
        return enabled;
    }

    private boolean isIncomingCallEnabled(String memberEmail) {
        return memberService.findMember(memberEmail).isAudioCallEnabled();
    }

    private void startCall(Member sender, ChatRoom room, String callerSessionId,
                           AudioCallSignalRequest request) {
        List<ChatRoomMember> roomMembers = roomMemberService.find(room);
        if (roomMembers.size() < 2) {
            throw new BusinessStateException("통화할 채팅방 참여자가 없습니다.");
        }

        if (callStateStore.findByMember(sender.getEmail()).isPresent()) {
            throw new BusinessStateException("현재 다른 통화가 진행 중입니다.");
        }

        String callerName = displayName(sender);
        List<AudioCallParticipant> participants = new ArrayList<>();
        participants.add(new AudioCallParticipant(
                sender.getEmail(), callerName, callerSessionId,
                AudioCallParticipantStatus.JOINED));

        for (ChatRoomMember roomMember : roomMembers) {
            if (roomMember.getMember().getEmail().equals(sender.getEmail())) continue;
            Member candidate = memberService.findMember(roomMember.getMember().getEmail());
            if (!candidate.isAudioCallEnabled()) continue;
            if (callStateStore.findByMember(candidate.getEmail()).isPresent()) continue;
            participants.add(new AudioCallParticipant(
                    candidate.getEmail(), displayName(candidate), null,
                    AudioCallParticipantStatus.INVITED));
        }

        if (participants.size() == 1) {
            throw new BusinessStateException(roomMembers.size() == 2
                    ? "상대방이 통화 알림을 허용하지 않았거나 다른 통화 중입니다."
                    : "통화에 참여할 수 있는 회원이 없습니다.");
        }

        String callId = request.callId() == null || request.callId().isBlank()
                ? UUID.randomUUID().toString()
                : request.callId();
        AudioCall call = new AudioCall(
                callId,
                room.getRoomId(),
                sender.getEmail(),
                callerName,
                participants,
                AudioCallState.RINGING
        );
        if (!callStateStore.create(call, RINGING_TTL)) {
            throw new BusinessStateException("참여자 중 현재 다른 통화가 진행 중인 회원이 있습니다.");
        }

        AudioCallSignalResponse signal = AudioCallSignalResponse.from(
                new AudioCallSignalRequest(
                        callId, room.getRoomId(), AudioCallSignalType.CALL,
                        null, null, null, null),
                sender.getEmail(), callerName);
        for (AudioCallParticipant participant : call.invitedParticipants()) {
            forward(participant.email(), null, signal);
            createIncomingNotification(call, participant, callerName);
        }
    }

    private void accept(AudioCall call, String senderEmail, String sessionId,
                        AudioCallSignalRequest request) {
        requireInvited(call, senderEmail);
        AudioCall updated = callStateStore.join(call, senderEmail, sessionId, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException(
                        "다른 기기에서 이미 통화를 받았거나 통화가 종료되었습니다."));
        closeIncomingNotification(call, senderEmail);

        AudioCallSignalResponse accepted = AudioCallSignalResponse.from(
                request, senderEmail, updated.nameOf(senderEmail));
        forwardToJoinedParticipants(updated, senderEmail, accepted);
        forward(senderEmail, sessionId,
                AudioCallSignalResponse.accepted(call.callId(), call.roomId()));
    }

    private void reject(AudioCall call, String senderEmail) {
        requireInvited(call, senderEmail);
        AudioCallMutation result = callStateStore.reject(call, senderEmail, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException("이미 응답했거나 통화가 종료되었습니다."));
        closeIncomingNotification(call, senderEmail);

        AudioCallSignalType type = result.callEnded()
                ? AudioCallSignalType.REJECT
                : AudioCallSignalType.PARTICIPANT_REJECTED;
        AudioCallSignalResponse response = AudioCallSignalResponse.participantEvent(
                call.callId(), call.roomId(), type, senderEmail, call.nameOf(senderEmail));
        forwardToJoinedParticipants(result.call(), senderEmail, response);
    }

    private void forwardPeerSignal(AudioCall call, String senderEmail, String sessionId,
                                   AudioCallSignalRequest request) {
        requireOwnedSession(call, senderEmail, sessionId);
        if (call.state() != AudioCallState.ACTIVE) {
            throw new BusinessStateException("아직 연결할 수 없는 통화입니다.");
        }
        if (request.targetEmail() == null || request.targetEmail().isBlank()
                || request.targetEmail().equals(senderEmail)) {
            throw new IllegalArgumentException("통화 신호를 받을 참여자가 필요합니다.");
        }
        AudioCallParticipant target = call.participant(request.targetEmail())
                .filter(participant -> participant.status() == AudioCallParticipantStatus.JOINED)
                .orElseThrow(() -> new BusinessStateException("통화에 참여 중인 회원이 아닙니다."));
        forward(
                target.email(),
                target.sessionId(),
                AudioCallSignalResponse.from(request, senderEmail, call.nameOf(senderEmail))
        );
    }

    private void hangup(AudioCall call, String senderEmail, String sessionId) {
        AudioCallParticipant participant = call.participant(senderEmail)
                .orElseThrow(() -> new BusinessStateException("통화 참여자가 아닙니다."));
        if (participant.status() == AudioCallParticipantStatus.INVITED) {
            reject(call, senderEmail);
            return;
        }

        requireOwnedSession(call, senderEmail, sessionId);
        AudioCallMutation result = callStateStore.leave(
                        call, senderEmail, sessionId, ACTIVE_TTL)
                .orElseThrow(() -> new BusinessStateException("이미 종료된 통화입니다."));
        AudioCallSignalType type = result.callEnded()
                ? AudioCallSignalType.HANGUP
                : AudioCallSignalType.PARTICIPANT_LEFT;
        AudioCallSignalResponse response = AudioCallSignalResponse.participantEvent(
                call.callId(), call.roomId(), type, senderEmail, call.nameOf(senderEmail));
        if (result.callEnded()) {
            closeIncomingNotifications(call);
            forwardToAvailableParticipants(result.call(), senderEmail, response);
        } else {
            forwardToJoinedParticipants(result.call(), senderEmail, response);
        }
    }

    private void keepAlive(AudioCall call, String senderEmail, String sessionId) {
        requireOwnedSession(call, senderEmail, sessionId);
        if (!callStateStore.touch(call, senderEmail, sessionId, ACTIVE_TTL)) {
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

    private AudioCall requireCall(AudioCallSignalRequest request, String senderEmail) {
        AudioCall call = callStateStore.find(request.callId())
                .orElseThrow(() -> new IllegalStateException("유효한 통화가 아닙니다."));
        if (!call.roomId().equals(request.roomId()) || !call.contains(senderEmail)) {
            throw new BusinessStateException("유효한 통화가 아닙니다.");
        }
        return call;
    }

    private void requireInvited(AudioCall call, String memberEmail) {
        if (call.participant(memberEmail)
                .filter(participant -> participant.status() == AudioCallParticipantStatus.INVITED)
                .isEmpty()) {
            throw new BusinessStateException("통화 요청에 응답할 수 없습니다.");
        }
    }

    private void requireOwnedSession(AudioCall call, String senderEmail, String sessionId) {
        if (!call.ownsSession(senderEmail, sessionId)
                || call.participant(senderEmail)
                        .filter(participant -> participant.status() == AudioCallParticipantStatus.JOINED)
                        .isEmpty()) {
            throw new BusinessStateException("통화를 시작한 기기에서만 처리할 수 있습니다.");
        }
    }

    private void createIncomingNotification(
            AudioCall call, AudioCallParticipant receiver, String callerName) {
        try {
            notificationService.createNotification(
                    memberService.findMember(receiver.email()),
                    callerName + "님이 그룹 음성 통화를 요청했습니다.",
                    Group.CALL,
                    callNotificationUrl(call)
            );
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 저장 실패: callId={}, receiver={}",
                    call.callId(), receiver.email(), exception);
        }
    }

    private void closeIncomingNotification(AudioCall call, String receiverEmail) {
        try {
            notificationService.closeRealtimeNotification(
                    memberService.findMember(receiverEmail),
                    Group.CALL,
                    callNotificationUrl(call)
            );
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 종료 실패: callId={}, receiver={}",
                    call.callId(), receiverEmail, exception);
        }
    }

    private void closeIncomingNotifications(AudioCall call) {
        call.invitedParticipants().forEach(participant ->
                closeIncomingNotification(call, participant.email()));
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
        if (request == null || request.type() == null || request.roomId() == null
                || request.roomId().isBlank() || sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() == AudioCallSignalType.DISCONNECTED
                || request.type() == AudioCallSignalType.ADMIN_TERMINATED
                || request.type() == AudioCallSignalType.ACCEPTED
                || request.type() == AudioCallSignalType.PARTICIPANT_LEFT
                || request.type() == AudioCallSignalType.PARTICIPANT_REJECTED) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() != AudioCallSignalType.CALL
                && (request.callId() == null || request.callId().isBlank())) {
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
        }
    }

    private void forwardToJoinedParticipants(
            AudioCall call, String excludedEmail, AudioCallSignalResponse response) {
        call.joinedParticipants().stream()
                .filter(participant -> excludedEmail == null || !excludedEmail.equals(participant.email()))
                .forEach(participant -> forward(participant.email(), participant.sessionId(), response));
    }

    private void forwardToAvailableParticipants(
            AudioCall call, String excludedEmail, AudioCallSignalResponse response) {
        call.participants().stream()
                .filter(AudioCallParticipant::isAvailable)
                .filter(participant -> excludedEmail == null || !excludedEmail.equals(participant.email()))
                .forEach(participant -> forward(participant.email(), participant.sessionId(), response));
    }

    private String displayName(Member member) {
        return member.getName() == null || member.getName().isBlank()
                ? member.getEmail()
                : member.getName();
    }

    private void forward(String email, String sessionId, AudioCallSignalResponse response) {
        if (sessionId == null || sessionId.isBlank()) {
            messagingTemplate.convertAndSendToUser(email, AUDIO_QUEUE, response);
            return;
        }

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(
                email, AUDIO_QUEUE, response, headers.getMessageHeaders());
    }
}
