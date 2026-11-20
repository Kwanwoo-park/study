package spring.study.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.board.dto.BoardBookmarkDto;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardBookmark;
import spring.study.board.repository.BoardBookmarkRepository;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardBookmarkService {
    private final BoardBookmarkRepository bookmarks;

    public Page<BoardBookmarkDto> list(Member member, int page) {
        return bookmarks.findVisible(member, CommonVisibility.PUBLIC, PageRequest.of(Math.max(0, page), 20));
    }

    public boolean contains(Member member, Long boardId) {
        return bookmarks.existsByMemberAndBoardId(member, boardId);
    }

    public List<Long> findSavedIds(Member member, List<Long> ids) {
        return ids.isEmpty() ? List.of() : bookmarks.findSavedIds(member, ids);
    }

    @Transactional
    public void save(Member member, Board board) {
        if (!bookmarks.existsByMemberAndBoardId(member, board.getId())) bookmarks.save(new BoardBookmark(member, board));
    }

    @Transactional
    public void delete(Member member, Long boardId) {
        bookmarks.deleteByMemberAndBoardId(member, boardId);
    }
}
