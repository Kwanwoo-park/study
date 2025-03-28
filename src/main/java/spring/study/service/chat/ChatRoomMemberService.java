package spring.study.service.chat;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.member.Member;
import spring.study.repository.chat.ChatRoomMemberRepository;

import java.util.*;

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
        List<ChatRoom> list = new ArrayList<>(chatRoomMemberRepository.findByMember(member).stream().map(ChatRoomMember::getRoom).sorted(Comparator.comparing(c -> c.getMessages().get(c.getMessages().size() - 1).getRegisterTime())).toList());
        Collections.reverse(list);
        return list;
    }

    public HashMap<String, Member> findMember(List<ChatRoom> rooms, Member member) {
        HashMap<String, Member> map = new HashMap<>();

        for (ChatRoom room : rooms) {
            map.put(room.getRoomId(), chatRoomMemberRepository.findByRoomAndMemberNot(room, member).getMember());
        }

        return map;
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
