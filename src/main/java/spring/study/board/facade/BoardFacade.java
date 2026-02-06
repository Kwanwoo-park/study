package spring.study.board.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardFacade {
    private final BoardService boardService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public ResponseEntity<?> write(BoardRequestDto dto, Member member) {
        int risk = validateContent(dto.getContent(), member);

        if (risk != 0)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));

        Board board = dto.toEntity();
        board.addMember(member);

        Board saved = boardService.save(board);

        if (saved == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -10,
                    "message", "게시글이 저장되지 않았습니다"
            ));

        return ResponseEntity.ok(Map.of(
                "result", saved.getId()
        ));
    }

    public ResponseEntity<?> update(BoardRequestDto dto, Member member) {
        int risk = validateContent(dto.getContent(), member);

        if (risk != 0)
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));

        return ResponseEntity.ok(Map.of(
                "result", boardService.updateBoard(dto.getId(), dto.getContent())
        ));
    }

    private int validateContent(String content, Member member) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용이 비어 있습니다");
        }

        int risk = forbiddenService.findWordList(Status.APPROVAL, content);

        if (risk != 0) {
            if (risk == 3) {
                notificationService.createNotification(
                        memberService.findAdministrator(),
                        member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                        Group.ADMIN
                );

                memberService.updateRole(member.getId(), Role.DENIED);
            }
        }

        return risk;
    }
}
