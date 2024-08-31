package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.repository.CommentRepository;

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

    public List<Comment> findAll() {
        return commentRepository.findAll();
    }

    public void deleteComment(Board board) {
        commentRepository.deleteByBoard(board);
    }

    public void deleteByMember(Member member) {
        commentRepository.deleteByMember(member);
    }
}
