package spring.study.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.chat.ChatMessageResponseDto;
import spring.study.entity.ChatMessage;
import spring.study.repository.ChatMessageRepository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public Long save(ChatMessage chatMessage) { return chatMessageRepository.save(chatMessage).getId(); }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findMessage(String roomId) {
        HashMap<String, Object> message = new HashMap<>();

        List<ChatMessage> list = chatMessageRepository.findByRoomId(roomId);

        message.put("list", list.stream().map(ChatMessageResponseDto::new).collect(Collectors.toList()));

        return message;
    }

    public void deleteMessage(String roomId) { chatMessageRepository.deleteMessage(roomId); }
}
