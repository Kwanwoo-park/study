package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.CommentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentService {
    private HashMap<String, Object> comment = new HashMap<>();

    private List<Comment> list;
    private final CommentRepository commentRepository;

    @Transactional
    public Long save(CommentRequestDto commentRequestDto) {return commentRepository.save(commentRequestDto.toEntity()).getId(); }

    public HashMap<String, Object> findAll() {
        list = commentRepository.findAll();

        comment.put("list", list.stream().map(CommentResponseDto::new).collect(Collectors.toList()));

        return comment;
    }

    public HashMap<String, Object> findComment(Long bid) {
        list = commentRepository.findByBid(bid, Sort.by("id").descending());

        comment.put("list", list.stream().map(CommentResponseDto::new).toList());

        return comment;
    }
}
