package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.repository.ChatRoomMemberRepository;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomMemberService {
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public ChatRoomMember save(Member member, ChatRoom room) {
        return chatRoomMemberRepository.save(ChatRoomMember.builder()
                .member(member)
                .room(room)
                .build()
        );
    }

    public ChatRoomMember find(Member member, ChatRoom room) {
        return chatRoomMemberRepository.findByMemberAndRoom(member, room);
    }

    public List<ChatRoomMember> find(Member member) {
        return chatRoomMemberRepository.findByMember(member);
    }

    public List<ChatRoomMember> find(ChatRoom room) {
        return chatRoomMemberRepository.findByRoom(room);
    }

    public void delete(Member member, ChatRoom room) {
        chatRoomMemberRepository.deleteByMemberAndRoom(member, room);
    }
}
