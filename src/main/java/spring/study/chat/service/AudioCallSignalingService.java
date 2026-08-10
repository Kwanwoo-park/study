package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AudioCallSignalingService {
    private static final String AUDIO_QUEUE = "/queue/audio-call";

    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, ActiveCall> activeCalls = new ConcurrentHashMap<>();

    public void handle(String senderEmail, AudioCallSignalRequest request) {
        try {
            validateRequest(request);
            Member sender = memberService.findMember(senderEmail);
            ChatRoom room = requireRoomMember(request.roomId(), sender);

            if (request.type() == AudioCallSignalType.CALL) {
                startCall(sender, room, request);
                return;
            }

            ActiveCall call = requireActiveCall(request, senderEmail);
            forward(call.other(senderEmail), AudioCallSignalResponse.from(request, senderEmail, sender.getName()));

            if (request.type() == AudioCallSignalType.REJECT || request.type() == AudioCallSignalType.HANGUP) {
                activeCalls.remove(call.callId(), call);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            forward(senderEmail, AudioCallSignalResponse.error(request, exception.getMessage()));
        }
    }

    public void handleDisconnect(String disconnectedMemberEmail) {
        if (disconnectedMemberEmail == null || disconnectedMemberEmail.isBlank()) return;

        activeCalls.forEach((callId, call) -> {
            if (call.contains(disconnectedMemberEmail) && activeCalls.remove(callId, call)) {
                forward(
                        call.other(disconnectedMemberEmail),
                        AudioCallSignalResponse.disconnected(
                                call.callId(), call.roomId(), disconnectedMemberEmail)
                );
            }
        });
    }

    public void forceTerminate(String callId) {
        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
        }

        ActiveCall call = activeCalls.remove(callId);
        if (call == null) {
            throw new IllegalArgumentException("진행 중인 통화를 찾을 수 없습니다.");
        }

        AudioCallSignalResponse signal = AudioCallSignalResponse.adminTerminated(
                call.callId(), call.roomId());
        forward(call.callerEmail(), signal);
        forward(call.receiverEmail(), signal);
    }

    private void startCall(Member sender, ChatRoom room, AudioCallSignalRequest request) {
        List<ChatRoomMember> members = roomMemberService.find(room);
        if (members.size() != 2) {
            throw new IllegalStateException("1:1 채팅방에서만 음성 통화를 시작할 수 있습니다.");
        }

        Member receiver = members.stream()
                .map(ChatRoomMember::getMember)
                .filter(member -> !member.getEmail().equals(sender.getEmail()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("통화 상대를 찾을 수 없습니다."));

        String callId = request.callId() == null || request.callId().isBlank()
                ? UUID.randomUUID().toString()
                : request.callId();
        ActiveCall call = new ActiveCall(callId, room.getRoomId(), sender.getEmail(), receiver.getEmail());

        boolean userBusy = activeCalls.values().stream().anyMatch(active -> active.contains(sender.getEmail())
                || active.contains(receiver.getEmail()));
        if (userBusy || activeCalls.putIfAbsent(callId, call) != null) {
            throw new IllegalStateException("현재 다른 통화가 진행 중입니다.");
        }

        AudioCallSignalRequest normalized = new AudioCallSignalRequest(
                callId, room.getRoomId(), AudioCallSignalType.CALL, null, null, null, null);
        forward(receiver.getEmail(), AudioCallSignalResponse.from(normalized, sender.getEmail(), sender.getName()));
    }

    private ChatRoom requireRoomMember(String roomId, Member sender) {
        ChatRoom room = roomService.find(roomId);
        if (room == null) throw new IllegalArgumentException("채팅방이 존재하지 않습니다.");
        if (!roomMemberService.exist(sender, room)) {
            throw new IllegalArgumentException("채팅방 참여자만 통화할 수 있습니다.");
        }
        return room;
    }

    private ActiveCall requireActiveCall(AudioCallSignalRequest request, String senderEmail) {
        ActiveCall call = activeCalls.get(request.callId());
        if (call == null || !call.roomId().equals(request.roomId()) || !call.contains(senderEmail)) {
            throw new IllegalStateException("유효한 통화가 아닙니다.");
        }
        return call;
    }

    private void validateRequest(AudioCallSignalRequest request) {
        if (request == null || request.type() == null || request.roomId() == null || request.roomId().isBlank()) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() == AudioCallSignalType.DISCONNECTED
                || request.type() == AudioCallSignalType.ADMIN_TERMINATED) {
            throw new IllegalArgumentException("잘못된 통화 요청입니다.");
        }
        if (request.type() != AudioCallSignalType.CALL
                && (request.callId() == null || request.callId().isBlank())) {
            throw new IllegalArgumentException("통화 ID가 필요합니다.");
        }
    }

    private void forward(String email, AudioCallSignalResponse response) {
        messagingTemplate.convertAndSendToUser(email, AUDIO_QUEUE, response);
    }

    private record ActiveCall(String callId, String roomId, String callerEmail, String receiverEmail) {
        boolean contains(String email) {
            return callerEmail.equals(email) || receiverEmail.equals(email);
        }

        String other(String email) {
            if (callerEmail.equals(email)) return receiverEmail;
            if (receiverEmail.equals(email)) return callerEmail;
            throw new IllegalArgumentException("통화 참여자가 아닙니다.");
        }
    }
}
