package spring.study.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallState;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.repository.AudioCallStateStore;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;
import spring.study.notification.entity.Group;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioCallSignalingServiceTest {
    private static final String CALLER_SESSION = "caller-session";
    private static final String RECEIVER_SESSION = "receiver-session";

    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService roomMemberService = mock(ChatRoomMemberService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final AudioCallStateStore callStateStore = new InMemoryAudioCallStateStore();
    private final NotificationService notificationService = mock(NotificationService.class);

    private AudioCallSignalingService service;
    private ChatRoom room;
    private Member caller;
    private Member receiver;

    @BeforeEach
    void setUp() {
        service = new AudioCallSignalingService(
                roomService, roomMemberService, memberService, messagingTemplate,
                callStateStore, notificationService);
        room = ChatRoom.builder().id(1L).roomId("room-1").name("1:1").count(2L).build();
        caller = member(1L, "caller@test.com", "발신자");
        receiver = member(2L, "receiver@test.com", "수신자");

        when(roomService.find("room-1")).thenReturn(room);
        when(memberService.findMember(caller.getEmail())).thenReturn(caller);
        when(memberService.findMember(receiver.getEmail())).thenReturn(receiver);
        when(roomMemberService.exist(caller, room)).thenReturn(true);
        when(roomMemberService.exist(receiver, room)).thenReturn(true);
        when(roomMemberService.find(room)).thenReturn(List.of(
                ChatRoomMember.builder().member(caller).room(room).build(),
                ChatRoomMember.builder().member(receiver).room(room).build()));
    }

    @Test
    void callShouldBeForwardedOnlyToTheOtherRoomMember() {
        startCall();

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture());
        assertEquals(AudioCallSignalType.CALL, response.getValue().type());
        assertEquals(caller.getEmail(), response.getValue().senderEmail());
        assertEquals("call-1", response.getValue().callId());
        verify(notificationService).createNotification(
                receiver, "발신자님이 음성 통화를 요청했습니다.", Group.CHAT, "room-1");
        assertEquals("call-1", service.findIncomingCall(receiver.getEmail(), "room-1")
                .orElseThrow().callId());
    }

    @Test
    void offerShouldBeForwardedAfterReceiverAcceptsTheCall() {
        startCall();
        service.handle(receiver.getEmail(), RECEIVER_SESSION,
                request("call-1", AudioCallSignalType.ACCEPT));

        AudioCallSignalRequest offer = new AudioCallSignalRequest(
                "call-1", "room-1", AudioCallSignalType.OFFER,
                "offer-sdp", null, null, null);
        service.handle(caller.getEmail(), CALLER_SESSION, offer);

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals(AudioCallSignalType.OFFER, response.getAllValues().get(1).type());
        assertEquals("offer-sdp", response.getAllValues().get(1).sdp());
    }

    @Test
    void nonMemberShouldReceiveAnErrorAndNoSignalShouldReachTheReceiver() {
        when(roomMemberService.exist(caller, room)).thenReturn(false);

        startCall();

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), any());
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals("채팅방 참여자만 통화할 수 있습니다.", response.getValue().error());
    }

    @Test
    void secondCallInvolvingABusyMemberShouldBeRejected() {
        startCall();

        service.handle(receiver.getEmail(), "another-session",
                request("call-2", AudioCallSignalType.CALL));

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals("현재 다른 통화가 진행 중입니다.", response.getValue().error());
        assertTrue(callStateStore.find("call-2").isEmpty());
    }

    @Test
    void onlyReceiverCanAcceptAndOnlyOriginalCallerSessionCanOffer() {
        startCall();

        service.handle(caller.getEmail(), CALLER_SESSION,
                request("call-1", AudioCallSignalType.ACCEPT));
        service.handle(receiver.getEmail(), RECEIVER_SESSION,
                request("call-1", AudioCallSignalType.ACCEPT));
        service.handle(caller.getEmail(), "other-caller-session",
                new AudioCallSignalRequest("call-1", "room-1", AudioCallSignalType.OFFER,
                        "offer-sdp", null, null, null));

        ArgumentCaptor<AudioCallSignalResponse> errors = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(3)).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), errors.capture(), any(MessageHeaders.class));
        assertEquals("수신자만 통화에 응답할 수 있습니다.", errors.getAllValues().get(0).error());
        assertEquals(AudioCallSignalType.ACCEPT, errors.getAllValues().get(1).type());
        assertEquals("통화를 시작한 기기에서만 처리할 수 있습니다.",
                errors.getAllValues().get(2).error());
    }

    @Test
    void disconnectingAnotherTabShouldNotEndTheCall() {
        startCall();

        service.handleDisconnect(caller.getEmail(), "another-tab-session");

        assertTrue(callStateStore.find("call-1").isPresent());
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), any(AudioCallSignalResponse.class));
    }

    @Test
    void disconnectingTheCallSessionShouldEndTheCallAndNotifyTheOtherParticipant() {
        startCall();

        service.handleDisconnect(caller.getEmail(), CALLER_SESSION);

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture());
        assertEquals(AudioCallSignalType.DISCONNECTED, response.getAllValues().get(1).type());
        assertTrue(callStateStore.find("call-1").isEmpty());
    }

    @Test
    void administratorShouldForceTerminateTheCallAndNotifyBothParticipants() {
        startCall();

        service.forceTerminate("call-1");

        ArgumentCaptor<AudioCallSignalResponse> callerResponse = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), callerResponse.capture(), any(MessageHeaders.class));
        assertEquals(AudioCallSignalType.ADMIN_TERMINATED, callerResponse.getValue().type());

        ArgumentCaptor<AudioCallSignalResponse> receiverResponses =
                ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), receiverResponses.capture());
        assertEquals(AudioCallSignalType.ADMIN_TERMINATED, receiverResponses.getAllValues().get(1).type());
    }

    private void startCall() {
        service.handle(caller.getEmail(), CALLER_SESSION,
                request("call-1", AudioCallSignalType.CALL));
    }

    private AudioCallSignalRequest request(String callId, AudioCallSignalType type) {
        return new AudioCallSignalRequest(callId, "room-1", type, null, null, null, null);
    }

    private Member member(Long id, String email, String name) {
        return Member.builder().id(id).email(email).name(name).build();
    }

    private static final class InMemoryAudioCallStateStore implements AudioCallStateStore {
        private final Map<String, AudioCall> calls = new HashMap<>();
        private final Map<String, String> memberCalls = new HashMap<>();

        @Override
        public synchronized boolean create(AudioCall call, Duration ttl) {
            if (calls.containsKey(call.callId())
                    || memberCalls.containsKey(call.callerEmail())
                    || memberCalls.containsKey(call.receiverEmail())) {
                return false;
            }
            calls.put(call.callId(), call);
            memberCalls.put(call.callerEmail(), call.callId());
            memberCalls.put(call.receiverEmail(), call.callId());
            return true;
        }

        @Override
        public synchronized Optional<AudioCall> find(String callId) {
            return Optional.ofNullable(calls.get(callId));
        }

        @Override
        public synchronized Optional<AudioCall> findByMember(String memberEmail) {
            return Optional.ofNullable(memberCalls.get(memberEmail)).map(calls::get);
        }

        @Override
        public synchronized boolean transition(
                AudioCall call,
                AudioCallState expectedState,
                AudioCallState nextState,
                String receiverSessionId,
                Duration ttl
        ) {
            AudioCall current = calls.get(call.callId());
            if (current == null || current.state() != expectedState) return false;
            calls.put(call.callId(), new AudioCall(
                    current.callId(), current.roomId(),
                    current.callerEmail(), current.callerName(), current.callerSessionId(),
                    current.receiverEmail(), current.receiverName(),
                    receiverSessionId == null ? current.receiverSessionId() : receiverSessionId,
                    nextState));
            return true;
        }

        @Override
        public synchronized boolean touch(AudioCall call, AudioCallState expectedState, Duration ttl) {
            AudioCall current = calls.get(call.callId());
            return current != null && current.state() == expectedState;
        }

        @Override
        public synchronized boolean remove(AudioCall call) {
            if (calls.remove(call.callId()) == null) return false;
            memberCalls.remove(call.callerEmail(), call.callId());
            memberCalls.remove(call.receiverEmail(), call.callId());
            return true;
        }
    }
}
