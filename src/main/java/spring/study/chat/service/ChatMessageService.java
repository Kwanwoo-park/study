package spring.study.chat.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.dto.ChatMessageResponseDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.entity.MessageType;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.chat.repository.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;

    @Transactional
    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessageRequestDto> checkMessage(List<ChatMessageRequestDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        for (ChatMessageRequestDto message : messages) {
            if (message == null || message.getId() == null || message.getId().isBlank() || message.getRoomId() == null || message.getEmail() == null || message.getType() == null)
                throw new IllegalArgumentException("Invalid chat event");
        }

        return messages;
    }

    @Transactional
    public List<ChatMessageRequestDto> saveBatch(Map<String, ChatRoom> rooms, Map<String, Member> members, Map<String, ChatMessage> existing, List<ChatMessageRequestDto> messages) {
        List<ChatMessageRequestDto> validMessages = new ArrayList<>();
        List<ChatMessage> newMessages = new ArrayList<>();
        Set<String> scheduledIds = new HashSet<>(existing.keySet());
        Set<String> deliveredIds = new HashSet<>();

        for (ChatMessageRequestDto message : messages) {
            if (!deliveredIds.add(message.getId()))
                continue;

            ChatMessage saved = existing.get(message.getId());
            // Old/replayed creation events must not reveal a subsequently deleted or edited message.
            if (saved != null && (saved.isEdited() || saved.getStatus() == ChatMessageStatus.DELETED_FOR_ALL))
                continue;

            if (saved != null)
                message.setMessage(saved.getMessage());

            ChatRoom room = rooms.get(message.getRoomId());
            Member member = members.get(message.getEmail());

            if (room == null || member == null) {
                log.warn("Skipping chat event because its room or member no longer exists. messageId={}, roomId={}, email={}",
                        message.getId(), message.getRoomId(), message.getEmail());
                continue;
            }

            message.setRoom(room);
            message.setMember(member);
            validMessages.add(message);

            if (scheduledIds.add(message.getId())) {
                newMessages.add(message.toEntity());
            }
        }

        chatMessageRepository.saveAll(newMessages);
        updateRoomSummaries(validMessages, rooms);

        return validMessages;
    }

    @Transactional(readOnly = true)
    public ChatMessage findById(String id) {
        return chatMessageRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> find(ChatRoom room) {
        return chatMessageRepository.findByRoom(room);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> findActiveChatting(LocalDateTime start, LocalDateTime end) {
        return chatMessageRepository.findByRegisterTimeBetween(start, end);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public long countUnread(ChatRoom room, Member member, LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return chatMessageRepository.countVisibleUnread(room, member, ChatMessageStatus.ACTIVE);
        }

        return chatMessageRepository.countVisibleUnreadAfter(room, member, lastReadAt, ChatMessageStatus.ACTIVE);
    }

    @Transactional
    public ChatMessage edit(String id, String content, Member member) {
        ChatMessage message = chatMessageRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));

        validateEditable(message, member);

        message.edit(content, member.getRole() == Role.ADMIN);

        return message;
    }

    private void validateEditable(ChatMessage message, Member member) {
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
                || !message.getRegisterTime().plusDays(1L).isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "전송 후 1일이 지난 메시지는 수정할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, ChatMessage> findAllById(List<String> idList) {
        Map<String, ChatMessage> existing = new HashMap<>();

        chatMessageRepository.findAllById(idList).forEach(message -> existing.put(message.getId(), message));

        return existing;
    }

    @Transactional
    public ChatMessage deleteForAll(String id, Member member) {
        ChatMessage message = chatMessageRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."));
        boolean isOwner = message.getMember().getId().equals(member.getId());

        if (!isOwner && member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 메시지만 모두에게 삭제할 수 있습니다.");
        } else if (message.getType() == MessageType.ENTER || message.getType() == MessageType.QUIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입장 및 퇴장 메시지는 삭제할 수 없습니다.");
        } else if (member.getRole() != Role.ADMIN && (message.getRegisterTime() == null || !message.getRegisterTime().isAfter(LocalDateTime.now().minusMinutes(30L)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "전송 후 30분이 지난 메시지는 모두에게 삭제할 수 없습니다.");
        }

        message.deleteForAll(member.getRole() == Role.ADMIN);
        return message;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<ChatMessage> findLatestVisible(ChatRoom room) {
        return chatMessageRepository.findFirstByRoomAndStatusOrderByRegisterTimeDesc(room, ChatMessageStatus.ACTIVE);
    }

    private void updateRoomSummaries(List<ChatMessageRequestDto> messages, Map<String, ChatRoom> rooms) {
        Map<String, ChatMessageRequestDto> latestByRoom = new HashMap<>();

        for (ChatMessageRequestDto message : messages) {
            if (message.getRegisterTime() == null) {
                continue;
            }

            ChatMessageRequestDto current = latestByRoom.get(message.getRoomId());

            if (current == null || message.getRegisterTime().isAfter(current.getRegisterTime())) {
                latestByRoom.put(message.getRoomId(), message);
            }
        }

        latestByRoom.forEach((roomId, latestMessage) -> {
            ChatRoom room = rooms.get(roomId);
            LocalDateTime currentLastChatTime = room.getLastChatTime();

            if (currentLastChatTime == null || latestMessage.getRegisterTime().isAfter(currentLastChatTime)) {
                room.setLastChatTime(latestMessage.getRegisterTime());
                room.setLastMessage(latestMessage.getMessage());
            }
        });
    }

    public void validateOwner(ChatMessage message, Member member) {
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
