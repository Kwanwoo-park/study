package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatMessageResponseDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageHidden;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.entity.MessageType;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.chat.repository.ChatMessageHiddenRepository;
import spring.study.chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private static final long DELETE_FOR_ALL_LIMIT_MINUTES = 30L;
    private static final long EDIT_LIMIT_DAYS = 1L;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageHiddenRepository chatMessageHiddenRepository;

    @Transactional
    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public ChatMessage findById(String id) {
        return findRequired(id);
    }

    public List<ChatMessage> find(ChatRoom room) {
        return chatMessageRepository.findByRoom(room);
    }

    public List<ChatMessage> findActiveChatting(LocalDateTime start, LocalDateTime end) {
        return chatMessageRepository.findByRegisterTimeBetween(start, end);
    }

    public List<ChatMessageResponseDto> loadChatting(int cursor, int limit, ChatRoom room, Member member) {
        return chatMessageRepository.findVisibleByRoom(
                        room,
                        member,
                        PageRequest.of(cursor, limit, Sort.by("registerTime").descending())
                )
                .stream()
                .map(ChatMessageResponseDto::new)
                .toList();
    }

    public long countUnread(ChatRoom room, Member member, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return chatMessageRepository.countVisibleUnread(room, member, ChatMessageStatus.ACTIVE);
        }

        return chatMessageRepository.countVisibleUnreadAfter(room, member, lastReadAt, ChatMessageStatus.ACTIVE);
    }

    @Transactional
    public ChatMessage edit(String id, String content, Member member) {
        ChatMessage message = findRequired(id);
        validateEditable(message, member);

        message.edit(content, member.getRole() == Role.ADMIN);
        return message;
    }

    public void validateEditable(ChatMessage message, Member member) {
        validateOwner(message, member);

        if (message.getType() != MessageType.TALK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "텍스트 메시지만 수정할 수 있습니다.");
        }
        if (message.getStatus() == ChatMessageStatus.DELETED_FOR_ALL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "삭제된 메시지는 수정할 수 없습니다.");
        }
        if (message.isCensored()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "금칙어 사용으로 검열된 메시지는 수정할 수 없습니다.");
        }
        if (message.getRegisterTime() == null
                || !message.getRegisterTime().plusDays(EDIT_LIMIT_DAYS).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "전송 후 1일이 지난 메시지는 수정할 수 없습니다.");
        }
    }

    @Transactional
    public ChatMessage hideForMember(String id, Member member) {
        ChatMessage message = findRequired(id);
        if (!chatMessageHiddenRepository.existsByMessageAndMember(message, member)) {
            chatMessageHiddenRepository.save(ChatMessageHidden.builder()
                    .message(message)
                    .member(member)
                    .hiddenAt(LocalDateTime.now())
                    .build());
        }
        return message;
    }

    @Transactional
    public ChatMessage deleteForAll(String id, Member member) {
        ChatMessage message = findRequired(id);
        boolean isOwner = message.getMember().getId().equals(member.getId());
        if (!isOwner && member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 메시지만 모두에게 삭제할 수 있습니다.");
        }
        if (message.getType() == MessageType.ENTER || message.getType() == MessageType.QUIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입장 및 퇴장 메시지는 삭제할 수 없습니다.");
        }
        if (member.getRole() != Role.ADMIN
                && (message.getRegisterTime() == null
                || !message.getRegisterTime().isAfter(LocalDateTime.now().minusMinutes(DELETE_FOR_ALL_LIMIT_MINUTES)))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "전송 후 30분이 지난 메시지는 모두에게 삭제할 수 없습니다."
            );
        }

        message.deleteForAll(member.getRole() == Role.ADMIN);
        return message;
    }

    public java.util.Optional<ChatMessage> findLatestVisible(ChatRoom room) {
        return chatMessageRepository.findFirstByRoomAndStatusOrderByRegisterTimeDesc(
                room,
                ChatMessageStatus.ACTIVE
        );
    }

    private ChatMessage findRequired(String id) {
        return chatMessageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));
    }

    private void validateOwner(ChatMessage message, Member member) {
        if (!message.getMember().getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 또는 관리자만 메시지를 수정할 수 있습니다.");
        }
    }

    public void deleteByRoom(ChatRoom room) {
        chatMessageRepository.deleteByRoom(room);
    }

    public void deleteByMember(Member member) {
        chatMessageRepository.deleteByMember(member);
    }

    public void deleteById(String id) {
        chatMessageRepository.deleteById(id);
    }
}
