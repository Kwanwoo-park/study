package spring.study.service.comment.reply;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.dto.comment.reply.ReplyRequestDto;
import spring.study.entity.comment.Comment;
import spring.study.entity.comment.reply.Reply;
import spring.study.entity.member.Member;
import spring.study.repository.comment.reply.ReplyRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReplyService {
    private final ReplyRepository replyRepository;

    @Transactional
    public Reply save(Reply reply) {
        return replyRepository.save(reply);
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

    public void deleteReply(Member member) {
        replyRepository.deleteByMember(member);
    }

    public void deleteReply(Comment comment) {
        replyRepository.deleteByComment(comment);
    }

    public void deleteReply(Long id) {
        replyRepository.deleteById(id);
    }
}
