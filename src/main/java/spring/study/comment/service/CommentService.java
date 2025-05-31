package spring.study.comment.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.dto.CommentResponseDto;
import spring.study.board.entity.Board;
import spring.study.comment.entity.Comment;
import spring.study.member.entity.Member;
import spring.study.comment.repository.CommentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentService {
    private final CommentRepository commentRepository;

    @Transactional
    public Comment save(Comment comment) {
        return commentRepository.save(comment);
    }

    @Transactional
    public Long save(CommentRequestDto commentSaveDto) {
        return commentRepository.save(commentSaveDto.toEntity()).getId();
    }

    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<Comment> list = commentRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(CommentResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public Comment findById(Long id) {
        return commentRepository.findById(id).orElseThrow();
    }

    public List<Comment> findAll() {
        return commentRepository.findAll();
    }

    public void deleteComment(Board board) {
        commentRepository.deleteByBoard(board);
    }

    public void deleteByMember(Member member) {
        commentRepository.deleteByMember(member);
    }

    @Transactional
    public int updateComments(Long id, String comments) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new BadCredentialsException(
                "존재하지 않는 댓글입니다."
        ));

        comment.changeComments(comments);

        return comment.getId().intValue();
    }

    public void deleteById(Long id) {
        commentRepository.deleteById(id);
    }
}
