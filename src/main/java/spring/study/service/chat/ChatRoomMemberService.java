package spring.study.service.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.member.Member;
import spring.study.repository.chat.ChatRoomMemberRepository;

import java.util.Comparator;
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

    public Boolean exist(Member member, ChatRoom room) {
        return chatRoomMemberRepository.existsByMemberAndRoom(member, room);
    }

    public ChatRoomMember find(Member member, ChatRoom room) {
        return chatRoomMemberRepository.findByMemberAndRoom(member, room);
    }

    public List<ChatRoom> findRoom(Member member) {
        return chatRoomMemberRepository.findByMember(member).stream().map(ChatRoomMember::getRoom).sorted(Comparator.comparingLong(ChatRoom::getId).reversed()).toList();
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

    public void delete(Member member) {
        chatRoomMemberRepository.deleteByMember(member);
    }
}
