package spring.study.reply.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.comment.dto.reply.ReplyRequestDto;
import spring.study.comment.entity.Comment;
import spring.study.reply.entity.Reply;
import spring.study.member.entity.Member;
import spring.study.reply.repository.ReplyRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReplyService {
    private final ReplyRepository replyRepository;

    @Transactional
    public Reply save(ReplyRequestDto dto, Member member, Comment comment) {
        dto.setMember(member);
        dto.setComment(comment);

        return replyRepository.save(dto.toEntity());
    }

    @Transactional
    public Reply save(ReplyRequestDto replyRequestDto) {
        return replyRepository.save(replyRequestDto.toEntity());
    }

    public Reply findById(Long id) {
        return replyRepository.findById(id).orElseThrow();
    }

    public List<Reply> findReply(Comment comment) {
        return replyRepository.findByComment(comment);
    }

    public List<Reply> getReplies(Comment comment, int cursor, int limit) {
        return replyRepository.findByComment(
                comment,
                PageRequest.of(cursor, limit, Sort.by("id").ascending())
        );
    }

    public long countReplies(Comment comment) {
        return replyRepository.countByComment(comment);
    }

    public void deleteReply(Member member) {
        replyRepository.deleteByMember(member);
    }

    public void deleteReplay(List<Comment> list) {
        for (Comment comment : list) {
            replyRepository.deleteByComment(comment);
        }
    }

    public void deleteReply(Long id) {
        replyRepository.deleteById(id);
    }
}
