package spring.study.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.facade.ChatSendFacade;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.service.AudioCallSignalingService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketApiController {
    private final ChatSendFacade chatSendFacade;
    private final AudioCallSignalingService audioCallSignalingService;

    @MessageMapping("/chat/message/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageRequestDto message, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        message.setEmail(principal.getName());
        return chatSendFacade.messageSend(message);
    }

    @MessageMapping("/audio/signal")
    public void signalAudioCall(@RequestBody AudioCallSignalRequest signal, Principal principal, SimpMessageHeaderAccessor headers) {
        if (principal == null) return;
        audioCallSignalingService.handle(principal.getName(), headers.getSessionId(), signal);
    }
}
