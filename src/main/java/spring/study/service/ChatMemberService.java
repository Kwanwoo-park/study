package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatMember;
import spring.study.entity.chat.ChatMemberRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMemberService {
    private final ChatMemberRepository chatMemberRepository;

    @Transactional
    public Long save(ChatMember chatMember) { return chatMemberRepository.save(chatMember).getId(); }

    public List<ChatMember> findMember(String roomId) { return chatMemberRepository.findByRoomId(roomId); }

    public void deleteRoomMember(String roomId, String name) { chatMemberRepository.deleteByRoomId(roomId, name); }
}
