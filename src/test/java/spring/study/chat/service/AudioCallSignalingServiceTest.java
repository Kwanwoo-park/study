package spring.study.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalResponse;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioCallSignalingServiceTest {
    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService roomMemberService = mock(ChatRoomMemberService.class);
    private final MemberService memberService = mock(MemberService.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

    private AudioCallSignalingService service;
    private ChatRoom room;
    private Member caller;
    private Member receiver;

    @BeforeEach
    void setUp() {
        service = new AudioCallSignalingService(
                roomService, roomMemberService, memberService, messagingTemplate);
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
        service.handle(caller.getEmail(), request("call-1", AudioCallSignalType.CALL));

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture());
        assertEquals(AudioCallSignalType.CALL, response.getValue().type());
        assertEquals(caller.getEmail(), response.getValue().senderEmail());
        assertEquals("call-1", response.getValue().callId());
    }

    @Test
    void offerShouldBeForwardedToTheOtherParticipantOfAnActiveCall() {
        service.handle(caller.getEmail(), request("call-1", AudioCallSignalType.CALL));
        AudioCallSignalRequest offer = new AudioCallSignalRequest(
                "call-1", "room-1", AudioCallSignalType.OFFER,
                "offer-sdp", null, null, null);

        service.handle(caller.getEmail(), offer);

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, org.mockito.Mockito.times(2)).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(receiver.getEmail()),
                org.mockito.ArgumentMatchers.eq("/queue/audio-call"), response.capture());
        assertEquals(AudioCallSignalType.OFFER, response.getAllValues().get(1).type());
        assertEquals("offer-sdp", response.getAllValues().get(1).sdp());
    }

    @Test
    void nonMemberShouldReceiveAnErrorAndNoSignalShouldReachTheReceiver() {
        when(roomMemberService.exist(caller, room)).thenReturn(false);

        service.handle(caller.getEmail(), request("call-1", AudioCallSignalType.CALL));

        verify(messagingTemplate, never()).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(receiver.getEmail()), any(), any());
        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), response.capture());
        assertEquals("채팅방 참여자만 통화할 수 있습니다.", response.getValue().error());
    }

    @Test
    void websocketDisconnectShouldEndTheCallAndNotifyTheOtherParticipant() {
        service.handle(caller.getEmail(), request("call-1", AudioCallSignalType.CALL));

        service.handleDisconnect(caller.getEmail());

        ArgumentCaptor<AudioCallSignalResponse> response = ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), response.capture());
        AudioCallSignalResponse disconnectSignal = response.getAllValues().get(1);
        assertEquals(AudioCallSignalType.DISCONNECTED, disconnectSignal.type());
        assertEquals("call-1", disconnectSignal.callId());
        assertEquals(caller.getEmail(), disconnectSignal.senderEmail());

        service.handle(receiver.getEmail(), request("call-1", AudioCallSignalType.HANGUP));
        ArgumentCaptor<AudioCallSignalResponse> responsesAfterDisconnect =
                ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(3)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), responsesAfterDisconnect.capture());
        assertEquals("유효한 통화가 아닙니다.", responsesAfterDisconnect.getAllValues().get(2).error());
    }

    @Test
    void administratorShouldForceTerminateTheCallAndNotifyBothParticipants() {
        service.handle(caller.getEmail(), request("call-1", AudioCallSignalType.CALL));

        service.forceTerminate("call-1");

        ArgumentCaptor<AudioCallSignalResponse> callerResponse =
                ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(caller.getEmail()), eq("/queue/audio-call"), callerResponse.capture());
        assertEquals(AudioCallSignalType.ADMIN_TERMINATED, callerResponse.getValue().type());

        ArgumentCaptor<AudioCallSignalResponse> receiverResponses =
                ArgumentCaptor.forClass(AudioCallSignalResponse.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                eq(receiver.getEmail()), eq("/queue/audio-call"), receiverResponses.capture());
        assertEquals(
                AudioCallSignalType.ADMIN_TERMINATED,
                receiverResponses.getAllValues().get(1).type()
        );
    }

    private AudioCallSignalRequest request(String callId, AudioCallSignalType type) {
        return new AudioCallSignalRequest(callId, "room-1", type, null, null, null, null);
    }

    private Member member(Long id, String email, String name) {
        return Member.builder().id(id).email(email).name(name).build();
    }
}
