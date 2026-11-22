package spring.study.chat.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatRoomResponseDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.chat.repository.ChatRoomRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public ChatRoom createRoom(String name, Long count) {
        return createRoom(name, count, false);
    }

    @Transactional
    public ChatRoom createDirectRoom(Member first, Member second) {
        return createRoom(first.getEmail() + " " + second.getEmail(), 2L, true);
    }

    private ChatRoom createRoom(String name, long count, boolean direct) {
        ChatRoom room = ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .name(name)
                .count(count)
                .build();
        room.setDirectRoom(direct);

        return chatRoomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<ChatRoom> list = chatRoomRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(ChatRoomResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll(Sort.by("id").descending());
    }

    @Transactional(readOnly = true)
    public ChatRoom find(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ChatRoom findForUpdate(String roomId) {
        return chatRoomRepository.findForUpdateByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public ChatRoom findByName(Member sessionMember, Member searchMember) {
        String name1 = sessionMember.getEmail() + " " + searchMember.getEmail();
        String name2 = searchMember.getEmail() + " " + sessionMember.getEmail();

        ChatRoom search1 = chatRoomRepository.findByName(name1);
        if (search1 != null)
            return search1;

        return chatRoomRepository.findByName(name2);
    }

    @Transactional(readOnly = true)
    public Map<String, ChatRoom> findByRoomIdIn(Set<String> roomIdList) {
        return chatRoomRepository.findByRoomIdIn(roomIdList).stream().collect(Collectors.toMap(ChatRoom::getRoomId, Function.identity()));
    }

    public void delete(String roomId) {
        chatRoomRepository.deleteByRoomId(roomId);
    }

    @Transactional
    public void updateLastTime(String roomId, LocalDateTime time) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId);

        room.setLastChatTime(time);
    }

    @Transactional
    public void updateLastMessage(String roomId, String message) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId);

        room.setLastMessage(message);
    }

    @Transactional
    public void addCount(Long id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 채팅방입니다."
        ));

        chatRoom.addCount();
    }

    @Transactional
    public void subCount(Long id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 채팅방입니다"
        ));

        chatRoom.subCount();
    }
}
