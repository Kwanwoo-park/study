package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.repository.ChatRoomMemberRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public ChatRoomMember save(ChatRoomMember chatRoomMember) {
        return chatRoomMemberRepository.save(chatRoomMember);
    }

    public ChatRoomMember find(Member member) {
        return chatRoomMemberRepository.findByMember(member);
    }

    public ChatRoomMember find(ChatRoom room) {
        return chatRoomMemberRepository.findByRoom(room);
    }
}
