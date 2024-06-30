package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.Comment;
import spring.study.repository.CommentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentService {

}
