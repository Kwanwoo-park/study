package spring.study.chat.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import spring.study.admin.service.IpBlockService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.common.component.IpBlockClientAddress;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatChannelAuthorizationTest {
    private static final String DESTINATION = "/sub/chat/room/room-1";
    private static final String IP_ADDRESS = "203.0.113.12";
    private final ChatRoomMemberService members = mock(ChatRoomMemberService.class);
    private final IpBlockService blocks = mock(IpBlockService.class);
    private final ChatChannelAuthorization authorization =
            new ChatChannelAuthorization(members, new IpBlockClientAddress(List.of()), blocks);
    private final Member member = Member.builder().id(1L).email("member@example.test").role(Role.USER).build();

    @BeforeEach
    void allowConnection() {
        when(blocks.permits(IP_ADDRESS)).thenReturn(true);
    }

    @Test
    void roomDeliveryRequiresCurrentMembershipEvenForExistingSubscription() throws Exception {
        authorization.inbound().preSend(frame(StompCommand.CONNECT, null, member), null);
        var subscription = frame(StompCommand.SUBSCRIBE, DESTINATION, null);
        assertThatThrownBy(() -> authorization.inbound().preSend(subscription, null)).isInstanceOf(AccessDeniedException.class);

        when(members.canReceive(member.getEmail(), "room-1")).thenReturn(true);
        assertThat(authorization.inbound().preSend(subscription, null)).isSameAs(subscription);
        var outgoing = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        outgoing.setSessionId("session-1");
        outgoing.setDestination(DESTINATION);
        Message<?> delivery = MessageBuilder.createMessage(new byte[0], outgoing.getMessageHeaders());
        assertThat(authorization.outbound().preSend(delivery, null)).isSameAs(delivery);

        when(members.canReceive(member.getEmail(), "room-1")).thenReturn(false);
        assertThat(authorization.outbound().preSend(delivery, null)).isNull();
    }

    @Test
    void authenticatedClientsSendToApplicationEndpoints() throws Exception {
        assertThatThrownBy(() -> authorization.inbound().preSend(frame(StompCommand.CONNECT, null, null), null))
                .isInstanceOf(AccessDeniedException.class);
        authorization.inbound().preSend(frame(StompCommand.CONNECT, null, member), null);

        var send = frame(StompCommand.SEND, "/api/chat/message/send", null);
        assertThat(authorization.inbound().preSend(send, null)).isSameAs(send);
        assertThatThrownBy(() -> authorization.inbound().preSend(frame(StompCommand.SEND, DESTINATION, null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void blockAppliesToExistingChatSessionSendAndDelivery() throws Exception {
        authorization.inbound().preSend(frame(StompCommand.CONNECT, null, member), null);
        when(members.canReceive(member.getEmail(), "room-1")).thenReturn(true);
        var outgoing = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        outgoing.setSessionId("session-1");
        outgoing.setDestination(DESTINATION);
        Message<?> delivery = MessageBuilder.createMessage(new byte[0], outgoing.getMessageHeaders());
        assertThat(authorization.outbound().preSend(delivery, null)).isSameAs(delivery);

        when(blocks.permits(IP_ADDRESS)).thenReturn(false);
        assertThat(authorization.outbound().preSend(delivery, null)).isNull();
        assertThatThrownBy(() -> authorization.inbound().preSend(frame(StompCommand.SEND, "/api/chat/message/send", null), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<?> frame(StompCommand command, String destination, Member principal) throws Exception {
        var headers = StompHeaderAccessor.create(command);
        headers.setSessionId("session-1");
        headers.setDestination(destination);
        if (principal != null) {
            headers.setUser(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
            var request = new MockHttpServletRequest();
            request.setRemoteAddr(IP_ADDRESS);
            Map<String, Object> attributes = new HashMap<>();
            authorization.handshake().beforeHandshake(new ServletServerHttpRequest(request), null, null, attributes);
            headers.setSessionAttributes(attributes);
        }
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }
}
