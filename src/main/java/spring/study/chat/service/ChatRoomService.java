package spring.study.chat.service;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {
    private Map<String, ChatRoom> chatRoomMap;
    private final ChatRoomRepository chatRoomRepository;

    @PostConstruct
    void init() {
        chatRoomMap = new LinkedHashMap<>();
    }

    @Transactional
    public ChatRoom createRoom(String name, Long count) {
        String randomId = UUID.randomUUID().toString();

        ChatRoom room = ChatRoom.builder()
                .roomId(randomId)
                .name(name)
                .count(count)
                .build();

        ChatRoom save = chatRoomRepository.save(room);

        chatRoomMap.put(randomId, save);

        return save;
    }

    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<ChatRoom> list = chatRoomRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(ChatRoomResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public List<ChatRoom> findAll() {
        return chatRoomRepository.findAll(Sort.by("id").descending());
    }

    public ChatRoom find(String roomId) {
        return chatRoomRepository.findByRoomId(roomId);
    }

    public ChatRoom findByName(Member sessionMember, Member searchMember) {
        String name1 = sessionMember.getEmail() + " " + searchMember.getEmail();
        String name2 = searchMember.getEmail() + " " + sessionMember.getEmail();

        ChatRoom search1 = chatRoomRepository.findByName(name1);
        ChatRoom search2 = chatRoomRepository.findByName(name2);

        if (search1 != null)
            return search1;
        else if (search2 != null)
            return search2;
        else
            return null;
    }

    public void delete(String roomId) {
        chatRoomRepository.deleteByRoomId(roomId);
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
