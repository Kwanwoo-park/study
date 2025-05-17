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
import spring.study.entity.chat.ChatMessage;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.MessageType;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Status;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.chat.ChatMessageService;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.chat.ChatRoomService;
import spring.study.service.forbidden.ForbiddenService;
import spring.study.service.member.MemberService;
import spring.study.service.notification.NotificationService;

import java.io.IOException;
import java.util.List;
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
    private final ForbiddenService forbiddenService;
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

        List<Forbidden> wordList = forbiddenService.findWordList(Status.APPROVAL);

        for (Forbidden word : wordList) {
            if (requestDto.getMessage().contains(word.getWord())) {
                requestDto.setMessage("<부적절한 내용이 포함되어 검열되었습니다>");
            }
        }

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
