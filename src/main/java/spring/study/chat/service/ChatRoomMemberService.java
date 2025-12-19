package spring.study.chat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.member.entity.Member;
import spring.study.chat.repository.ChatRoomMemberRepository;

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

    public HashMap<String, List<Member>> findMember(List<ChatRoom> rooms, Member member) {
        HashMap<String, List<Member>> map = new HashMap<>();

        for (ChatRoom room : rooms) {
            map.put(room.getRoomId(), chatRoomMemberRepository.findByRoomAndMemberNot(room, member).stream().map(ChatRoomMember::getMember).toList());
        }

        return map;
    }

    public List<ChatRoomMember> findMember(ChatRoom room, Member member) {
        return chatRoomMemberRepository.findByRoomAndMemberNot(room, member);
    }

    @Transactional
    public void subCount(Member member) {
        for (ChatRoomMember roomMember : chatRoomMemberRepository.findByMember(member)) {
            roomMember.getRoom().subCount();
        }
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
