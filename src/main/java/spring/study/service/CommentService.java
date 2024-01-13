package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    private final CommentRepository commentRepository;

    @Transactional
    public Long save(CommentRequestDto commentRequestDto) {return commentRepository.save(commentRequestDto.toEntity()).getId(); }

    public HashMap<String, Object> findAll() {
        HashMap<String, Object> comment = new HashMap<>();

        List<Comment> list = commentRepository.findAll();

        comment.put("list", list.stream().map(CommentResponseDto::new).collect(Collectors.toList()));

        return comment;
    }

    public Comment findComment(Long bid) {return commentRepository.findByBid(bid);}
}
