package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.MessageType;
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

    public List<ChatMessageImg> findMessage(String messageId) {
        return messageImgRepository.findByMessageId(messageId);
    }

    public Map<String, Object> findMessageImg(List<ChatMessage> list) {
        Map<String, Object> map = new HashMap<>();

        map.put("list", list);

        for (ChatMessage message : list) {
            if (message.getType() == MessageType.IMAGE) {
                map.put(message.getId(), messageImgRepository.findByMessageId(message.getId()));
            }
        }

        return map;
    }

    public void deleteMessage(ChatMessage message) {
        messageImgRepository.deleteByMessage(message);
    }
}
