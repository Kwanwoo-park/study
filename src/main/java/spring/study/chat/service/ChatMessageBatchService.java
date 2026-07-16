package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.chat.repository.ChatRoomRepository;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageBatchService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public List<ChatMessageRequestDto> saveBatch(List<ChatMessageRequestDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Map<String, ChatRoom> rooms = chatRoomRepository.findByRoomIdIn(
                        messages.stream()
                                .map(ChatMessageRequestDto::getRoomId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(ChatRoom::getRoomId, Function.identity()));
        Map<String, Member> members = memberRepository.findByEmailIn(
                        messages.stream()
                                .map(ChatMessageRequestDto::getEmail)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Member::getEmail, Function.identity()));

        Set<String> existingIds = new HashSet<>();
        chatMessageRepository.findAllById(
                        messages.stream()
                                .map(ChatMessageRequestDto::getId)
                                .filter(Objects::nonNull)
                                .toList()
                ).forEach(message -> existingIds.add(message.getId()));

        List<ChatMessageRequestDto> validMessages = new ArrayList<>();
        List<ChatMessage> newMessages = new ArrayList<>();
        Set<String> scheduledIds = new HashSet<>(existingIds);

        for (ChatMessageRequestDto message : messages) {
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
}
