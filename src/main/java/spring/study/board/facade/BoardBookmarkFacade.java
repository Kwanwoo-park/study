package spring.study.board.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardBookmarkService;
import spring.study.board.service.BoardService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.entity.Member;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardBookmarkFacade {
    private final BoardBookmarkService bookmarks;
    private final BoardService boards;
    private final VisibilityAccessPolicy visibilityAccessPolicy;

    public String view(Model model, Member member, int page) {
        model.addAttribute("email", member.getEmail());
        model.addAttribute("profile", member.getProfile());
        model.addAttribute("bookmarks", bookmarks.list(member, page));
        return "board/bookmarks";
    }

    public ResponseEntity<?> save(Long boardId, Member member) {
        Board board = boards.findById(boardId);
        if (!visibilityAccessPolicy.canViewBoard(board, member)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "볼 수 없는 게시글입니다.");
        }
        try {
            bookmarks.save(member, board);
        } catch (DataIntegrityViolationException exception) {
            if (!bookmarks.contains(member, boardId)) throw exception;
        }
        return ResponseEntity.ok(Map.of("saved", true));
    }

    public ResponseEntity<?> delete(Long boardId, Member member) {
        bookmarks.delete(member, boardId);
        return ResponseEntity.ok(Map.of("saved", false));
    }
}
