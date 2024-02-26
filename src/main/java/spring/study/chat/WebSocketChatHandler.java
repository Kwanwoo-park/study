package spring.study.chat;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatRoom;
import spring.study.service.ChatService;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private Set<WebSocketSession> sessions = new HashSet<>();
    private ChatMessage chatMessage;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (sessions != null)
            sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        chatMessage = objectMapper.readValue(payload, ChatMessage.class);

        if (chatMessage.getType().equals(ChatMessage.MessageType.ENTER)) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 입장했습니다.");
            sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
        } else if (chatMessage.getType().equals(ChatMessage.MessageType.QUIT)) {
            chatMessage.setMessage(chatMessage.getSender() + "님이 퇴장했습니다.");
            sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
        } else {
            sendToEachSocket(sessions, message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        chatService.deleteRoom(chatMessage.getRoomId());

        if (sessions != null)
        {
            sessions.remove(session);
        }
    }

    private void sendToEachSocket(Set<WebSocketSession> sessions, TextMessage message) {
        sessions.parallelStream().forEach( roomSession -> {
            try {
                roomSession.sendMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
