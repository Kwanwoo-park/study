package spring.study.chat.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatMessageResponseDto;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.repository.ChatMessageImgRepository;
import spring.study.member.entity.Member;

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

    @Transactional(readOnly = true)
    public List<ChatMessageImg> findMessage(String messageId) {
        return messageImgRepository.findByMessageId(messageId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findMessageImg(List<ChatMessageResponseDto> list) {
        Map<String, Object> map = new HashMap<>();

        for (ChatMessageResponseDto message : list) {
            map.put(message.getId(), messageImgRepository.findByMessageId(message.getId()));
        }

        return map;
    }

    @Transactional(readOnly = true)
    public ChatRoomImagesResponse images(ChatRoom room, Member viewer, Long cursor) {
        if (cursor != null && cursor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 사진 조회 위치를 입력해 주세요");
        }

        List<ChatRoomImageResponse> found = messageImgRepository.findVisibleRoomImages(room, viewer, cursor, PageRequest.of(0, 25));
        boolean hasNext = found.size() > 24;
        List<ChatRoomImageResponse> page = List.copyOf(found.subList(0, Math.min(24, found.size())));

        return new ChatRoomImagesResponse(page, hasNext ? page.get(page.size() - 1).id() : null);
    }

    public void deleteMessage(String messageId) {
        messageImgRepository.deleteByMessageId(messageId);
    }
}
