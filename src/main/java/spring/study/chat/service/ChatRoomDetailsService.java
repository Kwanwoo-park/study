package spring.study.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatRoomDetailsResponse;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.repository.ChatMessageImgRepository;
import spring.study.chat.repository.ChatRoomMemberRepository;
import spring.study.chat.repository.ChatRoomRepository;
import spring.study.member.entity.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomDetailsService {
    private static final int IMAGE_PAGE_SIZE = 24;
    private final ChatRoomRepository rooms;
    private final ChatRoomMemberRepository participants;
    private final ChatMessageImgRepository images;

    public ChatRoomDetailsResponse details(String roomId, Member viewer) {
        ChatRoom room = requireParticipant(roomId, viewer);
        var members = participants.findParticipantsByRoom(room).stream()
                .map(participant -> participant.getMember())
                .map(member -> new ChatRoomDetailsResponse.Participant(member.getName(), member.getEmail(),
                        member.getProfile(), member.getId().equals(viewer.getId())))
                .toList();
        return new ChatRoomDetailsResponse(room.getRoomId(), room.getName(), members);
    }

    public ChatRoomImagesResponse images(String roomId, Member viewer, Long cursor) {
        ChatRoom room = requireParticipant(roomId, viewer);
        if (cursor != null && cursor <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바른 사진 조회 위치를 입력해 주세요");
        }
        List<ChatRoomImageResponse> found = images.findVisibleRoomImages(room, viewer, cursor,
                MessageType.IMAGE, ChatMessageStatus.ACTIVE, ChatMessage.CENSORED_MESSAGE,
                PageRequest.of(0, IMAGE_PAGE_SIZE + 1));
        boolean hasNext = found.size() > IMAGE_PAGE_SIZE;
        List<ChatRoomImageResponse> page = List.copyOf(found.subList(0, Math.min(IMAGE_PAGE_SIZE, found.size())));
        return new ChatRoomImagesResponse(page, hasNext ? page.get(page.size() - 1).id() : null);
    }

    private ChatRoom requireParticipant(String roomId, Member viewer) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        ChatRoom room = rooms.findByRoomId(roomId);
        if (room == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다");
        // Including subsequent gallery pages: leaving a room immediately removes read access.
        if (!participants.existsByMemberAndRoom(viewer, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 채팅방만 조회할 수 있습니다");
        }
        return room;
    }
}
