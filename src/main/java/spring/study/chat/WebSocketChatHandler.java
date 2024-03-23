package spring.study.chat;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import spring.study.entity.chat.ChatMember;
import spring.study.entity.chat.ChatMessage;
import spring.study.service.ChatMemberService;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomService;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChatHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final ChatMemberService chatMemberService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
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

            List<ChatMember> list = chatMemberService.findMember(chatMessage.getRoomId());

            if (list.size() >= 1) {
                boolean flag = true;
                for (ChatMember member : list) {
                    if(chatMessage.getEmail().equals(member.getEmail())) {
                        flag = false;
                        break;
                    }
                }

                if (flag) {
                    ChatMember saveMem = new ChatMember();

                    saveMem.setMemName(chatMessage.getSender());
                    saveMem.setRoomId(chatMessage.getRoomId());
                    saveMem.setEmail(chatMessage.getEmail());

                    chatMemberService.save(saveMem);
                    chatRoomService.updateRoomCountAdd(chatMessage.getRoomId());
                }
            }
            else {
                ChatMember saveMem = new ChatMember();

                saveMem.setMemName(chatMessage.getSender());
                saveMem.setRoomId(chatMessage.getRoomId());
                saveMem.setEmail(chatMessage.getEmail());

                chatMemberService.save(saveMem);
            }
        } else if (chatMessage.getType().equals(ChatMessage.MessageType.QUIT)) {
            chatMemberService.deleteRoomMember(chatMessage.getRoomId(), chatMessage.getSender());

            if (chatRoomService.findRoom(chatMessage.getRoomId()).getCount()-1 > 0) {
                chatRoomService.updateRoomCountSub(chatMessage.getRoomId());
                chatMessage.setMessage(chatMessage.getSender() + "님이 퇴장했습니다.");
                sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(chatMessage)));
            }
            else
                chatRoomService.deleteRoom(chatMessage.getRoomId());
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
                chatMessageService.save(chatMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
