package spring.study.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.facade.ChatSendFacade;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketApiController {
    private ChatSendFacade chatSendFacade;

    @MessageMapping("/chat/message/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageRequestDto message) {
        return chatSendFacade.messageSend(message);
    }
}
