package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
import spring.study.chat.dto.AudioCallPreferenceRequest;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.chat.service.IceServerService;
import spring.study.common.exception.BusinessStateException;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioCallFacade {
    private static final String AUDIO_QUEUE = "/queue/audio-call";

    private final AudioCallSignalingService audioCallSignalingService;
    private final MemberService memberService;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final IceServerService iceServerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    public void signal(String email, String sessionId, AudioCallSignalRequest signal) {
        try {
            validateRequest(signal, sessionId);

            if (signal.type() == AudioCallSignalType.CALL) {
                Member sender = memberService.findMember(email);
                ChatRoom room = roomService.find(signal.roomId());

                if (sender == null)
                    throw new IllegalArgumentException("존재하지 않는 회원입니다.");

                if (room == null)
                    throw new IllegalArgumentException("채팅방이 존재하지 않습니다.");

                if (!roomMemberService.exist(sender, room))
                    throw new IllegalArgumentException("채팅방 참여자만 통화할 수 있습니다.");

                startCall(sender, room, sessionId, signal);
                return;
            }

            AudioCall call = audioCallSignalingService.requireCall(signal, email);

            switch (signal.type()) {
                case ACCEPT -> accept(call, email, sessionId, signal);
                case REJECT -> reject(call, email);
                case OFFER, ANSWER, ICE_CANDIDATE -> forwardPeerSignal(call, email, sessionId, signal);
                case HANGUP -> hangup(call, email, sessionId);
                case KEEP_ALIVE -> audioCallSignalingService.keepAlive(call, email, sessionId);
                default -> throw new IllegalArgumentException("잘못된 통화 요청입니다.");
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            forward(email, sessionId, AudioCallSignalResponse.error(signal, exception.getMessage()));
        }
    }

    public void handleDisconnect(String memberEmail, String sessionId) {
        audioCallSignalingService.handleDisconnect(memberEmail, sessionId).ifPresent(result -> {
            AudioCall call = result.call();
            AudioCallSignalResponse response = result.callEnded()
                    ? AudioCallSignalResponse.disconnected(call.callId(), call.roomId(), memberEmail)
                    : AudioCallSignalResponse.participantEvent(call.callId(), call.roomId(), AudioCallSignalType.PARTICIPANT_LEFT, memberEmail, call.nameOf(memberEmail));

            if (result.callEnded()) {
                closeIncomingNotifications(call);
                forwardToAvailableParticipants(call, memberEmail, response);
            } else {
                forwardToJoinedParticipants(call, memberEmail, response);
            }
        });
    }

    public void forceTerminate(String callId) {
        AudioCall call = audioCallSignalingService.forceTerminate(callId);

        closeIncomingNotifications(call);
        forwardToAvailableParticipants(call, null, AudioCallSignalResponse.adminTerminated(call.callId(), call.roomId()));
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

        boolean enabled = preference.enabled();
        Member currentMember = memberService.findMember(member.getEmail());
        memberService.updateAudioCallEnabled(currentMember.getId(), enabled);

        if (!enabled)
            audioCallSignalingService.findIncomingCall(member.getEmail(), null).ifPresent(call -> reject(call, member.getEmail()));

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "enabled", enabled,
                "message", enabled ? "통화 알림을 허용했습니다" : "통화 알림을 미허용으로 설정했습니다"
        ));
    }

    public ResponseEntity<?> incoming(String roomId, Member member) {
        if (!memberService.findMember(member.getEmail()).isAudioCallEnabled())
            return ResponseEntity.noContent().build();

        return audioCallSignalingService.findIncomingCall(member.getEmail(), roomId)
                .map(call -> AudioCallSignalResponse.from(new AudioCallSignalRequest(call.callId(), call.roomId(), AudioCallSignalType.CALL, null, null, null, null), call.initiatorEmail(), call.initiatorName()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    public ResponseEntity<?> reject(String callId, Member member) {
        AudioCall call = audioCallSignalingService.requireInvitedCall(callId, member.getEmail());

        reject(call, member.getEmail());

        return ResponseEntity.ok(Map.of("result", 1L));
    }

    private void startCall(Member sender, ChatRoom room, String sessionId, AudioCallSignalRequest request) {
        List<ChatRoomMember> roomMembers = roomMemberService.find(room);

        if (roomMembers.size() < 2)
            throw new BusinessStateException("통화할 채팅방 참여자가 없습니다.");

        if (audioCallSignalingService.findByMember(sender.getEmail()).isPresent())
            throw new BusinessStateException("현재 다른 통화가 진행 중입니다.");

        String callerName = displayName(sender);
        List<AudioCallParticipant> participants = new ArrayList<>();
        List<Member> memberList = new ArrayList<>();

        participants.add(new AudioCallParticipant(sender.getEmail(), callerName, sessionId, AudioCallParticipantStatus.JOINED));

        for (ChatRoomMember roomMember : roomMembers) {
            Member candidate = roomMember.getMember();

            if (candidate.getEmail().equals(sender.getEmail()))
                continue;

            if (!candidate.isAudioCallEnabled())
                continue;

            if (audioCallSignalingService.findByMember(candidate.getEmail()).isPresent())
                continue;

            memberList.add(candidate);
            participants.add(new AudioCallParticipant(candidate.getEmail(), displayName(candidate), null, AudioCallParticipantStatus.INVITED));
        }

        if (participants.size() == 1) {
            throw new BusinessStateException(roomMembers.size() == 2
                    ? "상대방이 통화 알림을 허용하지 않았거나 다른 통화 중입니다."
                    : "통화에 참여할 수 있는 회원이 없습니다.");
        }

        String callId = request.callId() == null || request.callId().isBlank() ? UUID.randomUUID().toString() : request.callId();
        AudioCall call = new AudioCall(callId, room.getRoomId(), sender.getEmail(), callerName, participants, AudioCallState.RINGING);

        audioCallSignalingService.startCall(call);

        AudioCallSignalResponse signal = AudioCallSignalResponse.from(new AudioCallSignalRequest(callId, room.getRoomId(), AudioCallSignalType.CALL, null, null, null, null), sender.getEmail(), callerName);

        for (AudioCallParticipant participant : call.invitedParticipants()) {
            forward(participant.email(), null, signal);
        }

        createIncomingNotification(call, memberList, callerName);
    }

    private void accept(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        AudioCall updated = audioCallSignalingService.accept(call, senderEmail, sessionId);
        closeIncomingNotification(call, senderEmail);

        AudioCallSignalResponse accepted = AudioCallSignalResponse.from(request, senderEmail, updated.nameOf(senderEmail));
        forwardToJoinedParticipants(updated, senderEmail, accepted);
        forward(senderEmail, sessionId, AudioCallSignalResponse.accepted(call.callId(), call.roomId()));
    }

    private void reject(AudioCall call, String senderEmail) {
        AudioCallMutation result = audioCallSignalingService.reject(call, senderEmail);
        closeIncomingNotification(call, senderEmail);

        AudioCallSignalType type = result.callEnded()
                ? AudioCallSignalType.REJECT
                : AudioCallSignalType.PARTICIPANT_REJECTED;
        AudioCallSignalResponse response = AudioCallSignalResponse.participantEvent(call.callId(), call.roomId(), type, senderEmail, call.nameOf(senderEmail));
        forwardToJoinedParticipants(result.call(), senderEmail, response);
    }

    private void forwardPeerSignal(AudioCall call, String senderEmail, String sessionId, AudioCallSignalRequest request) {
        AudioCallParticipant target = audioCallSignalingService.requirePeerSignalTarget(call, senderEmail, sessionId, request.targetEmail());
        forward(target.email(), target.sessionId(), AudioCallSignalResponse.from(request, senderEmail, call.nameOf(senderEmail)));
    }

    private void hangup(AudioCall call, String senderEmail, String sessionId) {
        AudioCallParticipant participant = call.participant(senderEmail).orElseThrow(() -> new BusinessStateException("통화 참여자가 아닙니다."));

        if (participant.status() == AudioCallParticipantStatus.INVITED) {
            reject(call, senderEmail);
            return;
        }

        AudioCallMutation result = audioCallSignalingService.leave(call, senderEmail, sessionId);
        AudioCallSignalType type = result.callEnded()
                ? AudioCallSignalType.HANGUP
                : AudioCallSignalType.PARTICIPANT_LEFT;
        AudioCallSignalResponse response = AudioCallSignalResponse.participantEvent(call.callId(), call.roomId(), type, senderEmail, call.nameOf(senderEmail));

        if (result.callEnded()) {
            closeIncomingNotifications(call);
            forwardToAvailableParticipants(result.call(), senderEmail, response);
        } else {
            forwardToJoinedParticipants(result.call(), senderEmail, response);
        }
    }

    private void createIncomingNotification(AudioCall call, List<Member> receiverList, String callerName) {
        Member errorReceiver = null;
        try {
            for (Member receiver : receiverList) {
                errorReceiver = receiver;
                notificationService.createNotification(
                        receiver,
                        callerName + "님이 그룹 음성 통화를 요청했습니다.",
                        Group.CALL,
                        callNotificationUrl(call)
                );
            }
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 저장 실패: callId={}, receiver={}", call.callId(), errorReceiver.getEmail(), exception);
        }
    }

    private void closeIncomingNotification(AudioCall call, String receiverEmail) {
        try {
            Member receiver = memberService.findMember(receiverEmail);
            notificationService.closeRealtimeNotification(receiver, Group.CALL, callNotificationUrl(call));
        } catch (RuntimeException exception) {
            log.warn("음성 통화 수신 알림 종료 실패: callId={}, receiver={}", call.callId(), receiverEmail, exception);
        }
    }

    private void closeIncomingNotifications(AudioCall call) {
        call.invitedParticipants().forEach(participant -> closeIncomingNotification(call, participant.email()));
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
        if ((request == null || request.type() == null || request.roomId() == null || request.roomId().isBlank())
                || (sessionId == null || sessionId.isBlank()))
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");

        if (request.type() == AudioCallSignalType.DISCONNECTED
                || request.type() == AudioCallSignalType.ADMIN_TERMINATED
                || request.type() == AudioCallSignalType.ACCEPTED
                || request.type() == AudioCallSignalType.PARTICIPANT_LEFT
                || request.type() == AudioCallSignalType.PARTICIPANT_REJECTED)
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");

        if (request.type() != AudioCallSignalType.CALL && (request.callId() == null || request.callId().isBlank()))
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
    }

    private void forwardToJoinedParticipants(AudioCall call, String excludedEmail, AudioCallSignalResponse response) {
        call.joinedParticipants().stream()
                .filter(participant -> excludedEmail == null || !excludedEmail.equals(participant.email()))
                .forEach(participant -> forward(participant.email(), participant.sessionId(), response));
    }

    private void forwardToAvailableParticipants(AudioCall call, String excludedEmail, AudioCallSignalResponse response) {
        call.participants().stream()
                .filter(AudioCallParticipant::isAvailable)
                .filter(participant -> excludedEmail == null || !excludedEmail.equals(participant.email()))
                .forEach(participant -> forward(participant.email(), participant.sessionId(), response));
    }

    private String displayName(Member member) {
        return member.getName() == null || member.getName().isBlank() ? member.getEmail() : member.getName();
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
