package spring.study.chat.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.service.ModerationService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacade {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatMessageImgService messageImgService;
    private final MemberService memberService;
    private final ModerationService moderationService;
    private final ImageS3Service imageS3Service;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;

    public ResponseEntity<?> loadChatting(String roomId, Member member) {
        ChatRoom room = roomService.find(roomId);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "채팅방이 존재하지 않습니다"
            ));
        }

        List<ChatMessage> list = messageService.find(room);

        String key = "chat:message:roomId:" + roomId;

        ListOperations<String, Object> ops = objectRedisTemplate.opsForList();

        Long size = ops.size(key);

        if (size != null && size > 0) {
            List<Object> messages = ops.range(key, 0, size-1);

            if (messages != null) {
                list.addAll(
                        messages.stream()
                                .map(obj -> {
                                    try {
                                        return objectMapper.readValue(obj.toString(), ChatMessageRequestDto.class);
                                    } catch (JsonProcessingException e) {
                                        log.error("Redis message parse error", e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .map(ChatMessageRequestDto::toEntity)
                                .toList()
                );
            }
        }

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "member", member,
                "message", list.stream().sorted(Comparator.comparing(ChatMessage::getRegisterTime)).toList(),
                "img", messageImgService.findMessageImg(list.stream().filter(item -> item.getType().equals(MessageType.IMAGE)).toList())
        ));
    }

    public ResponseEntity<?> createRoom(String name, Member member) {
        ChatRoom room = roomService.createRoom(name, 1L);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId()
        ));
    }

    public ResponseEntity<?> createRoom(MemberRequestDto dto, Member member) {
        Member searchMember = memberService.findMember(dto.getEmail());

        ChatRoom search = roomService.findByName(member, searchMember);

        if (search != null) {
            return ResponseEntity.ok(Map.of(
                    "result", search.getId(),
                    "room", search
            ));
        }

        String name = member.getEmail() + " " + searchMember.getEmail();

        ChatRoom room = roomService.createRoom(name, 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(searchMember, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "room", room
        ));
    }

    public ResponseEntity<?> messageCheck(String message, Member member, HttpServletRequest request) {
        int risk = moderationService.validate(message, member, request);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -10L,
                        "message", "메시지가 입력되지 않았습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }

    public ResponseEntity<?> sendImage(List<MultipartFile> files) {
        if (files.size() > 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -2,
                    "message", "업로드 파일 갯수 초과"
            ));
        }

        if (imageS3Service.findFormatCheck(files)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "지원하지 않는 파일 형식"
            ));
        }

        List<ChatMessageImg> list = new ArrayList<>();
        String messageId = UUID.randomUUID().toString();

        try {
            for (MultipartFile file : files) {
                list.add(ChatMessageImg.builder()
                        .imgSrc(imageS3Service.uploadImageToS3(file))
                        .messageId(messageId)
                        .build());
            }

            messageImgService.saveAll(list);

            return ResponseEntity.ok(Map.of(
                    "result", 1,
                    "messageId", messageId,
                    "list", list.stream().map(ChatMessageImg::getImgSrc).toList()
            ));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "오류가 발생하였습니다"
            ));
        }
    }
}
