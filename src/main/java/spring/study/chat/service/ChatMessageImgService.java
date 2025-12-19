package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.repository.ChatMessageImgRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ChatMessageImgService {
    private final ChatMessageImgRepository messageImgRepository;

    @Transactional
    public ChatMessageImg save(ChatMessageImg messageImg) {
        return messageImgRepository.save(messageImg);
    }

    public List<ChatMessageImg> findMessage(ChatMessage message) {
        return messageImgRepository.findByMessage(message);
    }

    public void deleteMessage(ChatMessage message) {
        messageImgRepository.deleteByMessage(message);
    }
}
