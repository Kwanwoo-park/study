package spring.study.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallMutation;
import spring.study.chat.domain.AudioCallParticipant;
import spring.study.chat.domain.AudioCallParticipantStatus;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.repository.AudioCallStateStore;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioCallSignalingServiceTest {
    private static final String CALLER_SESSION = "caller-session";
    private static final String RECEIVER_SESSION = "receiver-session";
    private static final String THIRD_SESSION = "third-session";

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
    private Member third;

    @BeforeEach
    void setUp() {
        service = new AudioCallSignalingService(
                roomService, roomMemberService, memberService, messagingTemplate,
                callStateStore, notificationService);
        room = ChatRoom.builder().id(1L).roomId("room-1").name("group").count(3L).build();
        caller = member(1L, "caller@test.com", "발신자");
        receiver = member(2L, "receiver@test.com", "수신자");
        third = member(3L, "third@test.com", "세 번째");

        when(roomService.find("room-1")).thenReturn(room);
        when(memberService.findMember(caller.getEmail())).thenReturn(caller);
        when(memberService.findMember(receiver.getEmail())).thenReturn(receiver);
        when(memberService.findMember(third.getEmail())).thenReturn(third);
        when(roomMemberService.exist(caller, room)).thenReturn(true);
        when(roomMemberService.exist(receiver, room)).thenReturn(true);
        when(roomMemberService.exist(third, room)).thenReturn(true);
        useRoomMembers(caller, receiver, third);
    }

    @Test
    void groupCallShouldInviteEveryAvailableRoomMember() {
        startCall();

        assertCallInvitation(receiver);
        assertCallInvitation(third);
        verify(notificationService).createNotification(
                receiver,
                "발신자님이 그룹 음성 통화를 요청했습니다.",
                Group.CALL,
                "/chat/chatRoom?roomId=room-1&callId=call-1");
        verify(notificationService).createNotification(
                third,
                "발신자님이 그룹 음성 통화를 요청했습니다.",
                Group.CALL,
                "/chat/chatRoom?roomId=room-1&callId=call-1");
        assertEquals("call-1", service.findIncomingCall(third.getEmail(), "room-1")
                .orElseThrow().callId());
    }

    @Test
    void unavailableMembersShouldBeSkippedWhenAnotherMemberCanJoin() {
        receiver.changeAudioCallEnabled(false);

        startCall();

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), any(AudioCallSignalResponse.class));
        assertCallInvitation(third);
        assertFalse(callStateStore.find("call-1").orElseThrow().contains(receiver.getEmail()));
    }

    @Test
    void callShouldFailWhenNoOtherMemberCanJoin() {
        receiver.changeAudioCallEnabled(false);
        third.changeAudioCallEnabled(false);

        startCall();

        assertTrue(callStateStore.find("call-1").isEmpty());
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals("통화에 참여할 수 있는 회원이 없습니다.", response.getValue().error());
    }

    @Test
    void acceptingMemberShouldBeAnnouncedAndTargetedOfferShouldReachOnlyThatMember() {
        startCall();
        clearInvocations(messagingTemplate);

        accept(receiver, RECEIVER_SESSION);

        ArgumentCaptor<AudioCallSignalResponse> callerSignal = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), callerSignal.capture(), any(MessageHeaders.class));
        assertEquals(AudioCallSignalType.ACCEPT, callerSignal.getValue().type());
        assertEquals(receiver.getEmail(), callerSignal.getValue().senderEmail());

        clearInvocations(messagingTemplate);
        service.handle(caller.getEmail(), CALLER_SESSION,
                signal(AudioCallSignalType.OFFER, receiver.getEmail(), "offer-sdp"));

        ArgumentCaptor<AudioCallSignalResponse> receiverSignal = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), receiverSignal.capture(), any(MessageHeaders.class));
        assertEquals(AudioCallSignalType.OFFER, receiverSignal.getValue().type());
        assertEquals(caller.getEmail(), receiverSignal.getValue().senderEmail());
        assertEquals(receiver.getEmail(), receiverSignal.getValue().targetEmail());
        assertEquals("offer-sdp", receiverSignal.getValue().sdp());
        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(third.getEmail()), eq("/queue/audio-call"), any(), any(MessageHeaders.class));
    }

    @Test
    void laterJoinerShouldBeAnnouncedToEveryExistingParticipant() {
        startCall();
        accept(receiver, RECEIVER_SESSION);
        clearInvocations(messagingTemplate);

        accept(third, THIRD_SESSION);

        assertSessionSignal(caller, AudioCallSignalType.ACCEPT, third.getEmail());
        assertSessionSignal(receiver, AudioCallSignalType.ACCEPT, third.getEmail());
        assertSessionSignal(third, AudioCallSignalType.ACCEPTED, null);
        assertEquals(3, callStateStore.find("call-1").orElseThrow().joinedParticipants().size());
    }

    @Test
    void rejectingOneInvitationShouldKeepTheGroupCallAlive() {
        startCall();
        clearInvocations(messagingTemplate);

        service.rejectIncomingCall("call-1", receiver.getEmail());

        assertTrue(callStateStore.find("call-1").isPresent());
        assertSessionSignal(caller, AudioCallSignalType.PARTICIPANT_REJECTED, receiver.getEmail());
        assertTrue(service.findIncomingCall(third.getEmail(), "room-1").isPresent());
        verify(notificationService).closeRealtimeNotification(
                receiver, Group.CALL, "/chat/chatRoom?roomId=room-1&callId=call-1");
    }

    @Test
    void rejectingTheLastInvitationShouldEndTheCall() {
        startCall();
        service.rejectIncomingCall("call-1", receiver.getEmail());
        clearInvocations(messagingTemplate);

        service.rejectIncomingCall("call-1", third.getEmail());

        assertTrue(callStateStore.find("call-1").isEmpty());
        assertSessionSignal(caller, AudioCallSignalType.REJECT, third.getEmail());
    }

    @Test
    void oneParticipantLeavingShouldNotEndAThreePersonCall() {
        startCall();
        accept(receiver, RECEIVER_SESSION);
        accept(third, THIRD_SESSION);
        clearInvocations(messagingTemplate);

        service.handle(receiver.getEmail(), RECEIVER_SESSION,
                request(AudioCallSignalType.HANGUP));

        assertTrue(callStateStore.find("call-1").isPresent());
        assertSessionSignal(caller, AudioCallSignalType.PARTICIPANT_LEFT, receiver.getEmail());
        assertSessionSignal(third, AudioCallSignalType.PARTICIPANT_LEFT, receiver.getEmail());
        assertEquals(2, callStateStore.find("call-1").orElseThrow().joinedParticipants().size());
    }

    @Test
    void lastPeerLeavingShouldEndTheCall() {
        useRoomMembers(caller, receiver);
        startCall();
        accept(receiver, RECEIVER_SESSION);
        clearInvocations(messagingTemplate);

        service.handle(receiver.getEmail(), RECEIVER_SESSION,
                request(AudioCallSignalType.HANGUP));

        assertTrue(callStateStore.find("call-1").isEmpty());
        assertSessionSignal(caller, AudioCallSignalType.HANGUP, receiver.getEmail());
    }

    @Test
    void disconnectingOneParticipantShouldNotifyTheRemainingGroup() {
        startCall();
        accept(receiver, RECEIVER_SESSION);
        accept(third, THIRD_SESSION);
        clearInvocations(messagingTemplate);

        service.handleDisconnect(receiver.getEmail(), RECEIVER_SESSION);

        assertTrue(callStateStore.find("call-1").isPresent());
        assertSessionSignal(caller, AudioCallSignalType.PARTICIPANT_LEFT, receiver.getEmail());
        assertSessionSignal(third, AudioCallSignalType.PARTICIPANT_LEFT, receiver.getEmail());
    }

    @Test
    void disconnectingAnotherTabShouldNotRemoveTheParticipant() {
        startCall();
        clearInvocations(messagingTemplate);

        service.handleDisconnect(caller.getEmail(), "another-tab");

        assertTrue(callStateStore.find("call-1").isPresent());
        verify(messagingTemplate, never()).convertAndSendToUser(
                any(), eq("/queue/audio-call"), any(AudioCallSignalResponse.class), any(MessageHeaders.class));
    }

    @Test
    void administratorShouldTerminateTheWholeGroupCall() {
        startCall();
        accept(receiver, RECEIVER_SESSION);
        accept(third, THIRD_SESSION);
        clearInvocations(messagingTemplate);

        service.forceTerminate("call-1");

        assertTrue(callStateStore.find("call-1").isEmpty());
        assertSessionSignal(caller, AudioCallSignalType.ADMIN_TERMINATED, null);
        assertSessionSignal(receiver, AudioCallSignalType.ADMIN_TERMINATED, null);
        assertSessionSignal(third, AudioCallSignalType.ADMIN_TERMINATED, null);
    }

    @Test
    void nonMemberShouldReceiveAnErrorWithoutCreatingACall() {
        when(roomMemberService.exist(caller, room)).thenReturn(false);

        startCall();

        assertTrue(callStateStore.find("call-1").isEmpty());
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals("채팅방 참여자만 통화할 수 있습니다.", response.getValue().error());
    }

    private void startCall() {
        service.handle(caller.getEmail(), CALLER_SESSION, request(AudioCallSignalType.CALL));
    }

    private void accept(Member member, String sessionId) {
        service.handle(member.getEmail(), sessionId, request(AudioCallSignalType.ACCEPT));
    }

    private AudioCallSignalRequest request(AudioCallSignalType type) {
        return new AudioCallSignalRequest(
                "call-1", "room-1", type, null, null, null, null, null);
    }

    private AudioCallSignalRequest signal(AudioCallSignalType type, String targetEmail, String sdp) {
        return new AudioCallSignalRequest(
                "call-1", "room-1", type, sdp, null, null, null, targetEmail);
    }

    private void assertCallInvitation(Member member) {
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(member.getEmail()), eq("/queue/audio-call"), response.capture());
        assertEquals(AudioCallSignalType.CALL, response.getValue().type());
        assertEquals(caller.getEmail(), response.getValue().senderEmail());
    }

    private void assertSessionSignal(Member member, AudioCallSignalType type, String senderEmail) {
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(member.getEmail()), eq("/queue/audio-call"), response.capture(), any(MessageHeaders.class));
        assertEquals(type, response.getValue().type());
        assertEquals(senderEmail, response.getValue().senderEmail());
    }

    private void useRoomMembers(Member... members) {
        when(roomMemberService.find(room)).thenReturn(List.of(members).stream()
                .map(member -> ChatRoomMember.builder().member(member).room(room).build())
                .toList());
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
                    || call.participants().stream().anyMatch(participant ->
                    memberCalls.containsKey(participant.email()))) {
                return false;
            }
            calls.put(call.callId(), call);
            call.participants().forEach(participant ->
                    memberCalls.put(participant.email(), call.callId()));
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
        public synchronized Optional<AudioCall> join(
                AudioCall call, String memberEmail, String sessionId, Duration ttl) {
            AudioCall current = calls.get(call.callId());
            if (!hasStatus(current, memberEmail, AudioCallParticipantStatus.INVITED)) {
                return Optional.empty();
            }
            AudioCall updated = current.withParticipant(
                    memberEmail, sessionId, AudioCallParticipantStatus.JOINED);
            calls.put(updated.callId(), updated);
            return Optional.of(updated);
        }

        @Override
        public synchronized Optional<AudioCallMutation> reject(
                AudioCall call, String memberEmail, Duration ttl) {
            AudioCall current = calls.get(call.callId());
            if (!hasStatus(current, memberEmail, AudioCallParticipantStatus.INVITED)) {
                return Optional.empty();
            }
            AudioCall updated = current.withParticipant(
                    memberEmail, null, AudioCallParticipantStatus.REJECTED);
            memberCalls.remove(memberEmail, call.callId());
            boolean ended = updated.participants().stream()
                    .filter(AudioCallParticipant::isAvailable)
                    .count() <= 1;
            saveOrRemove(updated, ended);
            return Optional.of(new AudioCallMutation(updated, ended));
        }

        @Override
        public synchronized Optional<AudioCallMutation> leave(
                AudioCall call, String memberEmail, String sessionId, Duration ttl) {
            AudioCall current = calls.get(call.callId());
            if (current == null || !current.ownsSession(memberEmail, sessionId)
                    || !hasStatus(current, memberEmail, AudioCallParticipantStatus.JOINED)) {
                return Optional.empty();
            }
            AudioCall updated = current.withParticipant(
                    memberEmail, null, AudioCallParticipantStatus.LEFT);
            memberCalls.remove(memberEmail, call.callId());
            long joined = updated.joinedParticipants().size();
            long invited = updated.invitedParticipants().size();
            boolean ended = joined == 0 || (joined == 1 && invited == 0);
            saveOrRemove(updated, ended);
            return Optional.of(new AudioCallMutation(updated, ended));
        }

        @Override
        public synchronized boolean touch(
                AudioCall call, String memberEmail, String sessionId, Duration ttl) {
            AudioCall current = calls.get(call.callId());
            return current != null && current.ownsSession(memberEmail, sessionId)
                    && hasStatus(current, memberEmail, AudioCallParticipantStatus.JOINED);
        }

        @Override
        public synchronized boolean remove(AudioCall call) {
            AudioCall removed = calls.remove(call.callId());
            if (removed == null) return false;
            removed.participants().forEach(participant ->
                    memberCalls.remove(participant.email(), call.callId()));
            return true;
        }

        private boolean hasStatus(
                AudioCall call, String memberEmail, AudioCallParticipantStatus status) {
            return call != null && call.participant(memberEmail)
                    .filter(participant -> participant.status() == status)
                    .isPresent();
        }

        private void saveOrRemove(AudioCall call, boolean ended) {
            if (!ended) {
                calls.put(call.callId(), call);
                return;
            }
            calls.remove(call.callId());
            call.participants().forEach(participant ->
                    memberCalls.remove(participant.email(), call.callId()));
        }
    }
}
