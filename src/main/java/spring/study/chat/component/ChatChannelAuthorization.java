package spring.study.chat.component;

import lombok.RequiredArgsConstructor;
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
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.member.entity.Member;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatChannelAuthorization {
    private static final String ROOM_PREFIX = "/sub/chat/room/";
    private static final Pattern ROOM_ID = Pattern.compile("[a-zA-Z0-9_-]{1,128}");
    private static final Set<String> CLIENT_SEND = Set.of("/api/chat/message/send", "/api/audio/signal");
    private static final Set<String> USER_SUBSCRIPTIONS = Set.of("/user/queue/audio-call");

    private final ChatRoomMemberService memberships;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

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
                String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE
                        || destination == null || !destination.startsWith(ROOM_PREFIX)) return message;

                String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                String email = sessionId == null ? null : sessions.get(sessionId);
                return allowedInRoom(email, destination) ? message : null;
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
            sessions.put(sessionId, member.getEmail());
            return message;
        }

        String email = sessionId == null ? null : sessions.get(sessionId);
        if (email == null) throw new AccessDeniedException("채팅 연결에는 로그인이 필요합니다");
        String destination = frame.getDestination();
        if (command == StompCommand.SUBSCRIBE && !allowedInRoom(email, destination)
                && (destination == null || !USER_SUBSCRIPTIONS.contains(destination))) {
            throw new AccessDeniedException("구독할 수 없는 채팅 경로입니다");
        }
        if (command == StompCommand.SEND && (destination == null || !CLIENT_SEND.contains(destination))) {
            throw new AccessDeniedException("전송할 수 없는 채팅 경로입니다");
        }
        return message;
    }

    private boolean allowedInRoom(String email, String destination) {
        if (email == null || destination == null || !destination.startsWith(ROOM_PREFIX)) return false;
        String roomId = destination.substring(ROOM_PREFIX.length());
        return ROOM_ID.matcher(roomId).matches() && memberships.canReceive(email, roomId);
    }
}
