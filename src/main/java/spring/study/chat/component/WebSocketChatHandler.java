package spring.study.chat.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.service.MemberService;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomIdFromSession(session);
        sessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);

        System.out.println();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = getRoomIdFromSession(session);
        sessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).remove(session);
    }

    private String getRoomIdFromSession(WebSocketSession session) {
        String query = Objects.requireNonNull(session.getUri()).getQuery();
        if (query == null) return null;

        for (String param : query.split("&")) {
            String[] params = param.split("=");

            if (params.length == 2 && params[0].equals("roomId")) return params[1];
        }

        return null;
    }

    public void sendToEachSocket(@Payload ChatMessage message) {
        sessions.get(message.getRoom().getRoomId()).parallelStream().forEach( roomSession -> {
            try {
                roomSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
