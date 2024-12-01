package spring.study.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.dto.chat.ChatMessageRequestDto;
import spring.study.entity.*;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomMemberService;
import spring.study.service.ChatRoomService;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatRoomService roomService;
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
            ChatRoom room = chatMessage.getRoom();
            Member member = chatMessage.getMember();

            if (!roomMemberService.exist(member, room)) {
                roomMemberService.save(member, room);
                roomService.addCount(room.getId());
                chatMessage.setMessage(member.getName() + "님이 입장했습니다.");
                sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
            }
        } else if (chatMessage.getType().equals(MessageType.QUIT)) {
            ChatRoom room = chatMessage.getRoom();
            Member member = chatMessage.getMember();

            roomMemberService.delete(member, room);

            if (room.getCount() -1 > 0) {
                roomService.subCount(room.getId());

                chatMessage.setMessage(chatMessage.getMember().getName() + "님이 퇴장했습니다.");
                sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
            }
            else {
                messageService.deleteByRoom(room);
                roomService.delete(room.getRoomId());
            }
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
                messageService.save(chatMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
