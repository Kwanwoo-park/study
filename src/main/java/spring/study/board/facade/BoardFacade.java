package spring.study.board.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardFacade {
    private final BoardService boardService;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public long write(BoardRequestDto dto, Member member) {
        validateContent(dto.getContent(), member);

        Board board = dto.toEntity();
        board.addMember(member);

        Board saved = boardService.save(board);

        if (saved == null) throw new IllegalStateException("게시글 저장 실패");

        return saved.getId();
    }

    public long update(BoardRequestDto dto, Member member) {
        validateContent(dto.getContent(), member);

        return boardService.updateBoard(dto.getId(), dto.getContent());
    }

    private void validateContent(String content, Member member) {
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

                throw new IllegalArgumentException("금칙어 사용으로 차단됨");
            }

            throw new IllegalArgumentException("금칙어 포함");
        }
    }
}
