package spring.study.chat.component;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import spring.study.admin.service.IpBlockService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.common.component.IpBlockClientAddress;
import spring.study.member.entity.Member;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatChannelAuthorization {
    private static final String CLIENT_IP = ChatChannelAuthorization.class.getName() + ".clientIp";
    private static final String ROOM_PREFIX = "/sub/chat/room/";
    private static final Pattern ROOM_ID = Pattern.compile("[a-zA-Z0-9_-]{1,128}");
    private static final Set<String> CLIENT_SEND = Set.of("/api/chat/message/send", "/api/audio/signal");
    private static final Set<String> USER_SUBSCRIPTIONS = Set.of("/user/queue/audio-call");

    private final ChatRoomMemberService memberships;
    private final IpBlockClientAddress clientAddress;
    private final IpBlockService blocks;
    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public HandshakeInterceptor handshake() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler handler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    String address = clientAddress.resolve(servletRequest.getServletRequest());
                    if (address != null) attributes.put(CLIENT_IP, address);
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler handler, Exception failure) {
            }
        };
    }

    public ChannelInterceptor inbound() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                return authorizeClientFrame(message);
            }
        };
    }

    public ChannelInterceptor outbound() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;

                String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                ClientSession session = sessionId == null ? null : sessions.get(sessionId);
                if (session == null || !blocks.permits(session.ipAddress())) return null;

                String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
                if (destination != null && destination.startsWith(ROOM_PREFIX)
                        && !allowedInRoom(session.email(), destination)) return null;
                return message;
            }
        };
    }

    private Message<?> authorizeClientFrame(Message<?> message) {
        StompHeaderAccessor frame = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (frame == null || frame.getCommand() == null) return message;

        StompCommand command = frame.getCommand();
        String sessionId = frame.getSessionId();
        if (command == StompCommand.DISCONNECT) {
            if (sessionId != null) sessions.remove(sessionId);
            return message;
        }
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            if (sessionId == null || !(frame.getUser() instanceof Authentication authentication)
                    || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof Member member)
                    || member.isAccessBlocked()) {
                throw new AccessDeniedException("채팅 연결에는 로그인이 필요합니다");
            }
            Map<String, Object> attributes = frame.getSessionAttributes();
            String address = attributes == null ? null : (String) attributes.get(CLIENT_IP);
            requirePermittedAddress(address);
            sessions.put(sessionId, new ClientSession(member.getEmail(), address));
            return message;
        }

        ClientSession session = sessionId == null ? null : sessions.get(sessionId);
        if (session == null) throw new AccessDeniedException("채팅 연결에는 로그인이 필요합니다");
        requirePermittedAddress(session.ipAddress());
        String destination = frame.getDestination();
        if (command == StompCommand.SUBSCRIBE && !allowedInRoom(session.email(), destination)
                && (destination == null || !USER_SUBSCRIPTIONS.contains(destination))) {
            throw new AccessDeniedException("구독할 수 없는 채팅 경로입니다");
        }
        if (command == StompCommand.SEND && (destination == null || !CLIENT_SEND.contains(destination))) {
            throw new AccessDeniedException("전송할 수 없는 채팅 경로입니다");
        }
        return message;
    }

    private void requirePermittedAddress(String address) {
        if (address == null || !blocks.permits(address)) {
            throw new AccessDeniedException("현재 IP에서 채팅을 이용할 수 없습니다");
        }
    }

    private boolean allowedInRoom(String email, String destination) {
        if (email == null || destination == null || !destination.startsWith(ROOM_PREFIX)) return false;
        String roomId = destination.substring(ROOM_PREFIX.length());
        return ROOM_ID.matcher(roomId).matches() && memberships.canReceive(email, roomId);
    }

    private record ClientSession(String email, String ipAddress) {}
}
