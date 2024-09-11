package spring.study.component;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.dto.chat.ChatMessageRequestDto;
import spring.study.dto.chat.ChatMessageResponseDto;
import spring.study.entity.ChatMessage;
import spring.study.entity.ChatRoom;
import spring.study.entity.MessageType;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomService;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
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
        chatMessage = objectMapper.readValue(payload, ChatMessageRequestDto.class).toEntity();

        if (chatMessage.getType().equals(MessageType.ENTER)) {
            chatMessage.setMessage(chatMessage.getMember().getName() + "님이 입장했습니다.");
            sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
        } else if (chatMessage.getType().equals(MessageType.QUIT)) {
            chatMessage.setMessage(chatMessage.getMember().getName() + "님이 퇴장했습니다.");
            sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
        } else {
            sendToEachSocket(sessions, message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
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
