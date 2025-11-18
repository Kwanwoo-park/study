package spring.study.reply.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
        Reply reply = replaceReply(dto, member);

        member.addReply(reply);
        comment.addReply(reply);

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

    public void deleteReplay(List<Comment> list) {
        for (Comment comment : list) {
            replyRepository.deleteByComment(comment);
        }
    }

    public void deleteReply(Long id) {
        replyRepository.deleteById(id);
    }

    private Reply replaceReply(ReplyRequestDto dto, Member commetMember) {
        dto.setReply(dto.getReply().replace("@"+commetMember.getName()+" ", ""));
        return dto.toEntity();
    }
}
