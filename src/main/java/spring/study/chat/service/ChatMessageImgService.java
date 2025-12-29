package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.repository.ChatMessageImgRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ChatMessageImgService {
    private final ChatMessageImgRepository messageImgRepository;

    @Transactional
    public ChatMessageImg save(ChatMessageImg messageImg) {
        return messageImgRepository.save(messageImg);
    }

    @Transactional
    public void saveAll(List<ChatMessageImg> list) {
        messageImgRepository.saveAll(list);
    }

    public List<ChatMessageImg> findMessage(String messageId) {
        return messageImgRepository.findByMessageId(messageId);
    }

    public Map<String, Object> findMessageImg(List<ChatMessage> list) {
        Map<String, Object> map = new HashMap<>();

        for (ChatMessage message : list) {
            map.put(message.getId(), messageImgRepository.findByMessageId(message.getId()));
        }

        return map;
    }

    public void deleteMessage(String messageId) {
        messageImgRepository.deleteByMessageId(messageId);
    }
}
