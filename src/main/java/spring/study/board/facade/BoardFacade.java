package spring.study.board.facade;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.aws.service.ImageCleanupService;
import org.springframework.transaction.annotation.Transactional;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.reply.service.ReplyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardFacade {
    private final MemberService memberService;
    private final BoardService boardService;
    private final ReplyService replyService;
    private final FollowService followService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;
    private final FavoriteService favoriteService;
    private final ImageS3Service imageS3Service;
    private final ModerationService moderationService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;
    private final ImageCleanupService imageCleanupService;

    public ResponseEntity<?> load(int cursor, int limit, Member member) {
        List<Member> memberList = followService.getMemberList(member);

        List<Board> list = boardService.getBoard(cursor, limit, memberList);
        long totalCount = boardService.countByMembers(memberList);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "boards", list.stream().map(BoardResponseDto::new).toList(),
                "nextCursor", nextCursor,
                "totalCount", totalCount,
                "like", checkFavorite(list, member),
                "like_count", favoriteService.countFavorites(list),
                "comment_count", commentService.countComments(list),
                "email", member.getEmail(),
                "result", 10L
        ));
    }

    public ResponseEntity<?> loadMemberBoards(int cursor, int limit, String email, Member loginMember) {
        Member targetMember = memberService.findMember(email);
        if (!visibilityAccessPolicy.canViewMember(targetMember, loginMember)) {
            return forbiddenVisibility();
        }

        boolean includePrivate = isOwnerOrFollower(loginMember, targetMember);
        List<BoardResponseDto> list = boardService.getBoardByMember(cursor, limit, targetMember, includePrivate)
                .stream()
                .map(BoardResponseDto::new)
                .toList();
        long totalCount = boardService.countByMember(targetMember, includePrivate);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "boards", list,
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

    public ResponseEntity<?> detail(Long id, Member member) {
        if (!boardService.existBoard(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -1,
                    "message", "존재하지 않는 게시글"
            ));
        }

        Board board = boardService.findById(id);
        boolean includePrivate = isOwnerOrFollower(member, board.getMember());
        if (!visibilityAccessPolicy.canViewBoard(board, member, includePrivate)) {
            return forbiddenVisibility();
        }
        long[] ids = boardService.getBoardIdList(id, board.getMember(), includePrivate);

        return ResponseEntity.ok(Map.of(
                "result", 10,
                "board", new BoardResponseDto(board),
                "like", checkFavorite(board, member),
                "like_count", favoriteService.countFavorites(board),
                "comment_count", commentService.countComments(board),
                "email", member.getEmail(),
                "previous", ids[0],
                "next", ids[1]
        ));
    }

    public ResponseEntity<?> write(BoardRequestDto dto, Member member, HttpServletResponse response) {
        int risk = moderationService.validate(dto.getContent(), member, response);

        if (risk != 0) {
            if (risk == -99)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", risk,
                        "message", "게시글 내용이 없습니다"
                ));

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        dto.setMember(member);
        Board board = dto.toEntity();

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

    public ResponseEntity<?> update(BoardRequestDto dto, Member member, HttpServletResponse response) {
        Board board = boardService.findById(dto.getId());
        if (!board.getMember().getId().equals(member.getId()) && member.getRole() != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -1,
                    "message", "본인 게시글만 수정할 수 있습니다"
            ));
        }

        int risk = moderationService.validate(dto.getContent(), member, response);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", risk,
                        "message", "게시글 내용이 없습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", boardService.updateBoard(dto.getId(), dto.getContent(), dto.getVisibility())
        ));
    }

    @Transactional
    public ResponseEntity<?> deleteBoard(Long boardId, Member member) {
        Board board = boardService.findById(boardId);

        if (!board.getMember().getId().equals(member.getId()) && member.getRole() != Role.ADMIN)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -1,
                    "message", "본인 게시글만 지울 수 있습니다"
            ));

        favoriteService.deleteByBoard(board);
        replyService.deleteReplay(board.getComment());
        commentService.deleteComment(board);
        imageCleanupService.enqueueAll(board.getImg().stream().map(img -> img.getImgSrc()).toList());
        boardImgService.deleteBoard(board);
        boardService.deleteById(boardId);

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "email", member.getEmail()
        ));
    }

    private HashMap<Long, Boolean> checkFavorite(List<Board> boardList, Member member) {
        HashMap<Long, Boolean> map = new HashMap<>();
        boardList.forEach(board -> map.put(board.getId(), false));
        favoriteService.findLikedBoardIds(member, boardList).forEach(boardId -> map.put(boardId, true));

        return map;
    }

    public Boolean checkFavorite(Board board, Member member) {
        return favoriteService.existFavorite(member, board);
    }

    public boolean canView(Board board, Member member) {
        return visibilityAccessPolicy.canViewBoard(
                board,
                member,
                isOwnerOrFollower(member, board.getMember())
        );
    }

    public boolean canViewPrivateBoards(Member viewer, Member author) {
        return isOwnerOrFollower(viewer, author);
    }

    private boolean isOwnerOrFollower(Member viewer, Member author) {
        if (viewer == null || author == null || viewer.getId() == null || author.getId() == null) {
            return false;
        }
        return viewer.getId().equals(author.getId()) || followService.existFollow(viewer, author);
    }

    private ResponseEntity<?> forbiddenVisibility() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -1,
                "message", "비공개 설정으로 조회할 수 없습니다."
        ));
    }
}
