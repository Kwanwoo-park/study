package spring.study.chat.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;

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
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatRoomService roomService;
    private final NotificationService notificationService;
    private final MemberService memberService;
    private final Map<String, Set<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = getRoomIdFromSession(session);
        sessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        ChatMessageRequestDto requestDto = objectMapper.readValue(payload, ChatMessageRequestDto.class);

        ChatRoom room = roomService.find(requestDto.getRoomId());
        Member member = memberService.findMember(requestDto.getEmail());

        Set<WebSocketSession> set = sessions.getOrDefault(getRoomIdFromSession(session), null);

        Member otherMember = roomMemberService.findMember(room, member).getMember();

        ChatMessage chatMessage = ChatMessage.builder()
                .message(requestDto.getMessage())
                .type(requestDto.getType())
                .member(member)
                .room(room)
                .build();

        if (chatMessage.getType().equals(MessageType.ENTER)) {
            if (!roomMemberService.exist(member, room)) {
                roomMemberService.save(member, room);
                roomService.addCount(room.getId());
                chatMessage.setMessage(member.getName() + "님이 입장했습니다.");
                sendToEachSocket(set, new TextMessage(objectMapper.writeValueAsString(chatMessage)), chatMessage);
            }
        } else if (chatMessage.getType().equals(MessageType.QUIT)) {
            roomMemberService.delete(member, room);

            if (room.getCount() -1 > 0) {
                roomService.subCount(room.getId());

                chatMessage.setMessage(chatMessage.getMember().getName() + "님이 퇴장했습니다.");
                sendToEachSocket(set, new TextMessage(objectMapper.writeValueAsString(chatMessage)), chatMessage);
            }
            else {
                messageService.deleteByRoom(room);
                roomService.delete(room.getRoomId());
            }
        } else {
            sendToEachSocket(set, new TextMessage(objectMapper.writeValueAsString(chatMessage)), chatMessage);

            Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 메시지를 보냈습니다");
            notification.addMember(otherMember);
        }
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

    private void sendToEachSocket(Set<WebSocketSession> sessions, TextMessage message, ChatMessage chatMessage) {
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
